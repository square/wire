/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.WireCompiler
import com.squareup.wire.java.JavaSchemaHandler
import com.squareup.wire.kotlin.KotlinSchemaHandler
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.swift.SwiftGenerator
import io.outfoxx.swiftpoet.FileSpec as SwiftFileSpec
import java.io.IOException
import okio.Path

/** Generate `.java` sources. */
data class JavaTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),

  override val exclusive: Boolean = true,

  override val outDirectory: String,

  /** True for emitted types to implement `android.os.Parcelable`. */
  val android: Boolean = false,

  /** True to enable the `androidx.annotation.Nullable` annotation where applicable. */
  val androidAnnotations: Boolean = false,

  /**
   * True to emit code that uses reflection for reading, writing, and toString methods which are
   * normally implemented with generated code.
   */
  val compact: Boolean = false,

  /** True to emit types for options declared on messages, fields, etc. */
  val emitDeclaredOptions: Boolean = true,

  /** True to emit annotations for options applied on messages, fields, etc. */
  val emitAppliedOptions: Boolean = true,

  /** If true, the constructor of all generated types will be non-public. */
  val buildersOnly: Boolean = false,
) : Target() {
  override fun newHandler(): SchemaHandler {
    return JavaSchemaHandler(
      android = android,
      androidAnnotations = androidAnnotations,
      compact = compact,
      emitDeclaredOptions = emitDeclaredOptions,
      emitAppliedOptions = emitAppliedOptions,
      buildersOnly = buildersOnly,
    )
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
  ): Target {
    return copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }
}

/** Generate `.kt` sources. */
data class KotlinTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),

  override val exclusive: Boolean = true,

  override val outDirectory: String,

  /** True for emitted types to implement `android.os.Parcelable`. */
  val android: Boolean = false,

  /** True for emitted types to implement APIs for easier migration from the Java target. */
  val javaInterop: Boolean = false,

  /** True to emit types for options declared on messages, fields, etc. */
  val emitDeclaredOptions: Boolean = true,

  /** True to emit annotations for options applied on messages, fields, etc. */
  val emitAppliedOptions: Boolean = true,

  /** Blocking or suspending. */
  val rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,

  /** Client or server. */
  val rpcRole: RpcRole = RpcRole.CLIENT,

  /** True for emitted services to implement one interface per RPC. */
  val singleMethodServices: Boolean = false,

  /**
   * If a oneof has more than or [boxOneOfsMinSize] fields, it will be generated using boxed oneofs
   * as defined in [OneOf][com.squareup.wire.OneOf].
   */
  val boxOneOfsMinSize: Int = 5_000,

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  val nameSuffix: String? = null,

  /**
   * If true, the constructor of all generated types will be non-public, and they will be
   * instantiable via their builders, regardless of the value of [javaInterop].
   */
  val buildersOnly: Boolean = false,

  /** If true, Kotlin keywords are escaped with backticks. If false, an underscore is added as a suffix. */
  val escapeKotlinKeywords: Boolean = false,
) : Target() {
  override fun newHandler(): SchemaHandler {
    return KotlinSchemaHandler(
      outDirectory = outDirectory,
      android = android,
      javaInterop = javaInterop,
      emitDeclaredOptions = emitDeclaredOptions,
      emitAppliedOptions = emitAppliedOptions,
      rpcCallStyle = rpcCallStyle,
      rpcRole = rpcRole,
      singleMethodServices = singleMethodServices,
      boxOneOfsMinSize = boxOneOfsMinSize,
      nameSuffix = nameSuffix,
      buildersOnly = buildersOnly,
      escapeKotlinKeywords = escapeKotlinKeywords,
    )
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
  ): Target {
    return copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }
}

// TODO(Benoit) Get SwiftGenerator to expose a factory from its module. Code should not be here.
data class SwiftTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),
  override val exclusive: Boolean = true,
  override val outDirectory: String,
) : Target() {
  override fun newHandler(): SchemaHandler {
    return object : SchemaHandler() {
      private lateinit var generator: SwiftGenerator

      override fun handle(schema: Schema, context: Context) {
        generator = SwiftGenerator(schema, context.module?.upstreamTypes ?: mapOf())
        context.fileSystem.createDirectories(context.outDirectory)
        super.handle(schema, context)
      }

      override fun handle(type: Type, context: Context): Path? {
        if (SwiftGenerator.builtInType(type.type)) return null

        val modulePath = context.outDirectory
        val typeName = generator.generatedTypeName(type)
        val swiftFile = SwiftFileSpec.builder(typeName.moduleName, typeName.simpleName)
          .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
          .addComment("\nSource: %L in %L", type.type, type.location.withPathOnly())
          .indent("    ")
          .apply {
            generator.generateTypeTo(type, this)
          }
          .build()

        val filePath = modulePath / "${swiftFile.name}.swift"
        try {
          context.fileSystem.write(filePath) {
            writeUtf8(swiftFile.toString())
          }
        } catch (e: IOException) {
          throw IOException(
            "Error emitting ${swiftFile.moduleName}.${typeName.canonicalName} to $modulePath",
            e,
          )
        }

        context.logger.artifactHandled(
          outputPath = modulePath,
          qualifiedName = "${swiftFile.moduleName}.${typeName.canonicalName} declared in ${type.location.withPathOnly()}",
          targetName = "Swift",
        )
        return filePath
      }

      override fun handle(service: Service, context: Context) = emptyList<Path>()
      override fun handle(
        extend: Extend,
        field: Field,
        context: Context,
      ): Path? = null
    }
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
  ): Target {
    return copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }
}

data class ProtoTarget(
  override val outDirectory: String,
) : Target() {
  override val includes: List<String> = listOf()
  override val excludes: List<String> = listOf()
  override val exclusive: Boolean = false

  override fun newHandler(): SchemaHandler {
    return object : SchemaHandler() {
      override fun handle(schema: Schema, context: Context) {
        context.fileSystem.createDirectories(context.outDirectory)
        val outDirectory = context.outDirectory

        for (protoFile in schema.protoFiles) {
          if (!context.inSourcePath(protoFile) ||
            protoFile.isEmpty() ||
            // We never emit the `.proto` files we are embedding within Wire.
            isWireRuntimeProto(protoFile.location.path)
          ) {
            continue
          }

          val relativePath = protoFile.location.path
            .substringBeforeLast("/", missingDelimiterValue = ".")
          val outputDirectory = outDirectory / relativePath
          val outputFilePath = outputDirectory / "${protoFile.name()}.proto"
          context.logger.artifactHandled(outputDirectory, protoFile.location.path, "Proto")

          try {
            context.fileSystem.createDirectories(outputFilePath.parent!!)
            context.fileSystem.write(outputFilePath) {
              writeUtf8(protoFile.toSchema())
            }
          } catch (e: IOException) {
            throw IOException("Error emitting $outputFilePath to $outDirectory", e)
          }
        }
      }

      private fun ProtoFile.isEmpty() = types.isEmpty() && services.isEmpty() && extendList.isEmpty()

      override fun handle(type: Type, context: Context): Path? = null

      override fun handle(service: Service, context: Context): List<Path> = listOf()

      override fun handle(extend: Extend, field: Field, context: Context): Path? = null
    }
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
  ): Target {
    return copy(
      outDirectory = outDirectory,
    )
  }
}

data class CustomTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),
  override val exclusive: Boolean = true,
  override val outDirectory: String,
  val options: Map<String, String> = mapOf(),
  val schemaHandlerFactory: SchemaHandler.Factory,
) : Target() {
  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
  ): Target {
    return this.copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }

  override fun newHandler(): SchemaHandler {
    return schemaHandlerFactory.create(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
      options = options,
    )
  }
}

/**
 * Create and return an instance of [SchemaHandler.Factory].
 *
 * @param schemaHandlerFactoryClass a fully qualified class name for a class that implements
 *     [SchemaHandler.Factory]. The class must have a no-arguments public constructor.
 */
fun newSchemaHandler(schemaHandlerFactoryClass: String): SchemaHandler.Factory {
  return ClassNameSchemaHandlerFactory(schemaHandlerFactoryClass)
}

/**
 * This schema handler factory is serializable (so Gradle can cache targets that use it). It works
 * even if the delegate handler class is itself not serializable.
 */
private class ClassNameSchemaHandlerFactory(
  private val schemaHandlerFactoryClass: String,
) : SchemaHandler.Factory {
  @Transient private var cachedDelegate: SchemaHandler.Factory? = null

  private val delegate: SchemaHandler.Factory
    get() {
      val cachedResult = cachedDelegate
      if (cachedResult != null) return cachedResult

      val schemaHandlerType = try {
        Class.forName(schemaHandlerFactoryClass)
      } catch (exception: ClassNotFoundException) {
        throw IllegalArgumentException("Couldn't find SchemaHandlerClass '$schemaHandlerFactoryClass'")
      }

      val constructor = try {
        schemaHandlerType.getConstructor()
      } catch (exception: NoSuchMethodException) {
        throw IllegalArgumentException("No public constructor on $schemaHandlerFactoryClass")
      }

      val result = constructor.newInstance() as? SchemaHandler.Factory
        ?: throw IllegalArgumentException("$schemaHandlerFactoryClass does not implement SchemaHandler.Factory")
      this.cachedDelegate = result
      return result
    }

  @Deprecated("Wire does not call this method anymore. Implement the other 'create' method to receive the payload associated with the schema handler.")
  override fun create(): SchemaHandler {
    @Suppress("DEPRECATION")
    return delegate.create()
  }

  override fun create(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
    options: Map<String, String>,
  ): SchemaHandler {
    return delegate.create(includes, excludes, exclusive, outDirectory, options)
  }
}
