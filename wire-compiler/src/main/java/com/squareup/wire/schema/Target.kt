/*
 * Copyright 2018 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.WireCompiler
import com.squareup.wire.WireLogger
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.java.Profile
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import okio.buffer
import okio.sink
import java.io.IOException
import java.io.Serializable
import java.nio.file.FileSystem
import java.nio.file.Files

sealed class Target : Serializable {
  /**
   * Proto types to include generated sources for. Types listed here will be generated for this
   * target and not for subsequent targets in the task.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val includes: List<String>

  /**
   * Proto types to excluded generated sources for. Types listed here will not be generated for this
   * target.
   *
   * This list should contain package names (suffixed with `.*`) and type names only. It should
   * not contain member names.
   */
  abstract val excludes: List<String>

  /**
   * True if types emitted for this target should not also be emitted for other targets. Use this
   * to cause multiple outputs to be emitted for the same input type.
   */
  abstract val exclusive: Boolean

  internal abstract fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler

  interface SchemaHandler {
    fun handle(type: Type)
    fun handle(service: Service)
    /**
     * This will handle all [Type]s and [Service]s of the `protoFile` in respect to the emitting
     * rules. If exclusive, the handled [Type]s and [Service]s should be added to the consumed set.
     * Consumed types and services themselves are to be omitted by this handler.
     */
    fun handle(
      protoFile: ProtoFile,
      emittingRules: EmittingRules,
      claimedDefinitions: ClaimedDefinitions,
      isExclusive: Boolean
    ) {
      protoFile.types
          .filter { it !in claimedDefinitions && emittingRules.includes(it.type) }
          .forEach { type ->
            handle(type)
            // We don't let other targets handle this one.
            if (isExclusive) claimedDefinitions.claim(type)
          }

      protoFile.services
          .filter { it !in claimedDefinitions && emittingRules.includes(it.type()) }
          .forEach { service ->
            handle(service)
            // We don't let other targets handle this one.
            if (isExclusive) claimedDefinitions.claim(service)
          }
    }
  }
}

/** Generate `.java` sources. */
data class JavaTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),

  override val exclusive: Boolean = true,

  val outDirectory: String,

  /** True for emitted types to implement `android.os.Parcelable`. */
  val android: Boolean = false,

  /** True to enable the `androidx.annotation.Nullable` annotation where applicable. */
  val androidAnnotations: Boolean = false,

  /**
   * True to emit code that uses reflection for reading, writing, and toString methods which are
   * normally implemented with generated code.
   */
  val compact: Boolean = false
) : Target() {
  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler {
    val profileName = if (android) "android" else "java"
    val profile = newProfileLoader.loadProfile(profileName, schema)

    val javaGenerator = JavaGenerator.get(schema)
        .withProfile(profile)
        .withAndroid(android)
        .withAndroidAnnotations(androidAnnotations)
        .withCompact(compact)

    return object : SchemaHandler {
      override fun handle(type: Type) {
        val typeSpec = javaGenerator.generateType(type)
        val javaTypeName = javaGenerator.generatedTypeName(type)
        val javaFile = JavaFile.builder(javaTypeName.packageName(), typeSpec)
            .addFileComment("\$L", WireCompiler.CODE_GENERATED_BY_WIRE)
            .apply {
              addFileComment("\nSource file: \$L", type.location.withPathOnly())
            }.build()

        val path = fs.getPath(outDirectory)
        logger.artifact(path, javaFile)
        try {
          javaFile.writeTo(path)
        } catch (e: IOException) {
          throw IOException("Error emitting ${javaFile.packageName}.${javaFile.typeSpec.name} " +
              "to $outDirectory", e)
        }
      }

      override fun handle(service: Service) {
        // Service handling isn't supporting in Java.
      }
    }
  }
}

/** Generate `.kt` sources. */
data class KotlinTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),

  override val exclusive: Boolean = true,

  val outDirectory: String,

  /** True for emitted types to implement `android.os.Parcelable`. */
  val android: Boolean = false,

  /** True for emitted types to implement APIs for easier migration from the Java target. */
  val javaInterop: Boolean = false,

  /** Blocking or suspending. */
  val rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,

  /** Client or server. */
  val rpcRole: RpcRole = RpcRole.CLIENT,

  /** True for emitted services to implement one interface per RPC. */
  val singleMethodServices: Boolean = false
) : Target() {
  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler {
    val kotlinGenerator = KotlinGenerator(
        schema = schema,
        emitAndroid = android,
        javaInterop = javaInterop,
        rpcCallStyle = rpcCallStyle,
        rpcRole = rpcRole
    )

    return object : SchemaHandler {
      override fun handle(type: Type) {
        val typeSpec = kotlinGenerator.generateType(type)
        val className = kotlinGenerator.generatedTypeName(type)
        val kotlinFile = FileSpec.builder(className.packageName, typeSpec.name!!)
            .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
            .apply {
              addComment("\nSource file: %L", type.location.withPathOnly())
            }
            .addType(typeSpec)
            .build()

        val path = fs.getPath(outDirectory)
        logger.artifact(path, kotlinFile)

        try {
          kotlinFile.writeTo(path)
        } catch (e: IOException) {
          throw IOException("Error emitting " +
              "${kotlinFile.packageName}.${className.canonicalName} to $outDirectory", e)
        }
      }

      override fun handle(service: Service) {
        if (singleMethodServices) {
          service.rpcs().forEach { rpc ->
            write(
                service,
                kotlinGenerator.generatedServiceName(service, rpc),
                kotlinGenerator.generateService(service, rpc)
            )
          }
        } else {
          write(
              service,
              kotlinGenerator.generatedServiceName(service),
              kotlinGenerator.generateService(service)
          )
        }
      }

      private fun write(service: Service, name: ClassName, typeSpec: TypeSpec) {
        val kotlinFile = FileSpec.builder(name.packageName, name.simpleName)
            .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
            .addComment("\nSource file: %L", service.location().withPathOnly())
            .addType(typeSpec)
            .build()

        val path = fs.getPath(outDirectory)
        logger.artifact(path, kotlinFile)

        try {
          kotlinFile.writeTo(path)
        } catch (e: IOException) {
          throw IOException("Error emitting " +
              "${kotlinFile.packageName}.${service.type()} to $outDirectory", e)
        }
      }
    }
  }
}

data class ProtoTarget(
  val outDirectory: String
) : Target() {
  override val includes: List<String> = listOf()
  override val excludes: List<String> = listOf()
  override val exclusive: Boolean = false

  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler {
    return object : SchemaHandler {
      override fun handle(type: Type) = Unit
      override fun handle(service: Service) = Unit
      override fun handle(
        protoFile: ProtoFile,
        emittingRules: EmittingRules,
        claimedDefinitions: ClaimedDefinitions,
        isExclusive: Boolean
      ) {
        if (protoFile.isEmpty()) return

        val relativePath: String = protoFile.location.path
            .substringBeforeLast("/", missingDelimiterValue = ".")
        val outputDirectory = fs.getPath(outDirectory, relativePath)

        require(Files.notExists(outputDirectory) || Files.isDirectory(outputDirectory)) {
          "path $outputDirectory exists but is not a directory."
        }
        Files.createDirectories(outputDirectory)

        val outputFilePath = outputDirectory.resolve("${protoFile.name()}.proto")

        logger.artifact(outputDirectory, protoFile.location.path)

        outputFilePath.sink().buffer().use { sink ->
          try {
            sink.writeUtf8(protoFile.toSchema())
          } catch (e: IOException) {
            throw IOException("Error emitting $outputFilePath to $outDirectory", e)
          }
        }
      }
    }
  }

  private fun ProtoFile.isEmpty() = types.isEmpty() && services.isEmpty() && extendList.isEmpty()
}

/** Omit code generation for these sources. Use this for a dry-run. */
data class NullTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf()
) : Target() {
  override val exclusive: Boolean = true

  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler {
    return object : SchemaHandler {
      override fun handle(type: Type) {
        logger.artifactSkipped(type.type)
      }

      override fun handle(service: Service) {
        logger.artifactSkipped(service.type())
      }
    }
  }
}

/**
 * Generate something custom defined by an external class.
 *
 * This API is currently unstable. We will be changing this API in the future.
 */
data class CustomTargetBeta(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),
  override val exclusive: Boolean = true,
  val outDirectory: String,
  /**
   * A fully qualified class name for a class that implements [CustomHandlerBeta]. The class must
   * have a no-arguments public constructor.
   */
  val customHandlerClass: String
) : Target() {
  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): SchemaHandler {
    val customHandlerType = try {
      Class.forName(customHandlerClass)
    } catch (exception: ClassNotFoundException) {
      throw IllegalArgumentException("Couldn't find CustomHandlerClass '$customHandlerClass'")
    }

    val constructor = try {
      customHandlerType.getConstructor()
    } catch (exception: NoSuchMethodException) {
      throw IllegalArgumentException("No public constructor on $customHandlerClass")
    }

    val instance = constructor.newInstance() as? CustomHandlerBeta
        ?: throw IllegalArgumentException(
            "$customHandlerClass does not implement CustomHandlerBeta")

    return instance.newHandler(schema, fs, outDirectory, logger, newProfileLoader)
  }
}

/**
 * Implementations of this interface must have a no-arguments public constructor.
 *
 * This API is currently unstable. We will be changing this API in the future.
 */
interface CustomHandlerBeta {
  fun newHandler(
    schema: Schema,
    fs: FileSystem,
    outDirectory: String,
    logger: WireLogger,
    newProfileLoader: NewProfileLoader
  ): Target.SchemaHandler
}

// TODO: merge this interface with Loader.
interface NewProfileLoader {
  fun loadProfile(name: String, schema: Schema): Profile
}
