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
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.swift.SwiftGenerator
import okio.Path
import java.io.IOException
import java.io.Serializable
import io.outfoxx.swiftpoet.FileSpec as SwiftFileSpec

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

  /**
   * Directory where this target will write its output.
   *
   * In Gradle, when this class is serialized, this is relative to the project to improve build
   * cacheability. Callers must use [copyTarget] to resolve it to real path prior to use.
   */
  abstract val outDirectory: String

  /**
   * Returns a new Target object that is a copy of this one, but with the given fields updated.
   */
  // TODO(Benoit) We're only ever copying outDirectory, maybe we can remove other fields and rename
  //  the method?
  abstract fun copyTarget(
    includes: List<String> = this.includes,
    excludes: List<String> = this.excludes,
    exclusive: Boolean = this.exclusive,
    outDirectory: String = this.outDirectory,
  ): Target

  abstract fun newHandler(): SchemaHandler
}

// TODO(Benoit) Get JavaGenerator to expose a factory from its module. Code should not be here.
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
  val emitAppliedOptions: Boolean = true
) : Target() {
  override fun newHandler(): SchemaHandler {
    return object : SchemaHandler() {
      private lateinit var javaGenerator: JavaGenerator

      override fun handle(schema: Schema, context: Context) {
        val profileName = if (android) "android" else "java"
        val profile = context.profileLoader!!.loadProfile(profileName, schema)
        javaGenerator = JavaGenerator.get(schema)
          .withProfile(profile)
          .withAndroid(android)
          .withAndroidAnnotations(androidAnnotations)
          .withCompact(compact)
          .withOptions(emitDeclaredOptions, emitAppliedOptions)

        context.fileSystem.createDirectories(context.outDirectory)

        super.handle(schema, context)
      }

      override fun handle(type: Type, context: Context): Path? {
        if (JavaGenerator.builtInType(type.type)) return null

        val typeSpec = javaGenerator.generateType(type)
        val javaTypeName = javaGenerator.generatedTypeName(type)
        return write(javaTypeName, typeSpec, type.type, type.location, context)
      }

      override fun handle(service: Service, context: Context): List<Path> {
        // Service handling isn't supporting in Java.
        return emptyList()
      }

      override fun handle(extend: Extend, field: Field, context: Context): Path? {
        val typeSpec = javaGenerator.generateOptionType(extend, field) ?: return null
        val javaTypeName = javaGenerator.generatedTypeName(extend.member(field))
        return write(javaTypeName, typeSpec, field.qualifiedName, field.location, context)
      }

      private fun write(
        javaTypeName: com.squareup.javapoet.ClassName,
        typeSpec: com.squareup.javapoet.TypeSpec,
        source: Any,
        location: Location,
        context: Context,
      ): Path {
        val outDirectory = context.outDirectory
        val javaFile = JavaFile.builder(javaTypeName.packageName(), typeSpec)
          .addFileComment("\$L", WireCompiler.CODE_GENERATED_BY_WIRE)
          .addFileComment("\nSource: \$L in \$L", source, location.withPathOnly())
          .build()
        val filePath = outDirectory /
          javaFile.packageName.replace(".", "/") /
          "${javaTypeName.simpleName()}.java"

        context.logger.artifactHandled(
          outDirectory, "${javaFile.packageName}.${javaFile.typeSpec.name}", "Java"
        )
        try {
          context.fileSystem.createDirectories(filePath.parent!!)
          context.fileSystem.write(filePath) {
            writeUtf8(javaFile.toString())
          }
        } catch (e: IOException) {
          throw IOException(
            "Error emitting ${javaFile.packageName}.${javaFile.typeSpec.name} to $outDirectory", e
          )
        }
        return filePath
      }
    }
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String
  ): Target {
    return copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }
}
data class KotlinProtocTarget(
  override val includes: List<String> = listOf("*"),
  override val excludes: List<String> = listOf(),

  override val exclusive: Boolean = true,

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

  /** True to also generate gRPC server-compatible classes. Experimental feature. */
  val grpcServerCompatible: Boolean = false,

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  val nameSuffix: String? = null,
  override val outDirectory: String = "dne",
) : Target() {
  override fun copyTarget(includes: List<String>, excludes: List<String>, exclusive: Boolean, outDirectory: String): Target {
    TODO("copyTarget")
  }

  override fun newHandler(): SchemaHandler {
    return object : SchemaHandler() {
      private lateinit var kotlinGenerator: KotlinGenerator

      override fun handle(schema: Schema, context: Context) {
        val profileName = if (android) "android" else "java"
        val profile = context.profileLoader?.loadProfile(profileName, schema) ?: Profile()
        kotlinGenerator = KotlinGenerator(
          schema = schema,
          profile = profile,
          emitAndroid = android,
          javaInterop = javaInterop,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions,
          rpcCallStyle = rpcCallStyle,
          rpcRole = rpcRole,
          boxOneOfsMinSize = boxOneOfsMinSize,
          grpcServerCompatible = grpcServerCompatible,
          nameSuffix = nameSuffix,
        )
        super.handle(schema, context)
      }

      override fun handle(type: Type, context: Context): Path? {
        if (KotlinGenerator.builtInType(type.type)) return null

        val typeSpec = kotlinGenerator.generateType(type)
        val className = kotlinGenerator.generatedTypeName(type)
        return write(className, typeSpec, type.type, type.location, context)
      }

      override fun handle(service: Service, context: Context): List<Path> {
        if (rpcRole === RpcRole.NONE) return emptyList()

        val generatedPaths = mutableListOf<Path>()

        if (singleMethodServices) {
          service.rpcs.forEach { rpc ->
            val map = kotlinGenerator.generateServiceTypeSpecs(service, rpc)
            for ((className, typeSpec) in map) {
              generatedPaths.add(
                write(className, typeSpec, service.type, service.location, context)
              )
            }
          }
        } else {
          val map = kotlinGenerator.generateServiceTypeSpecs(service, null)
          for ((className, typeSpec) in map) {
            generatedPaths.add(write(className, typeSpec, service.type, service.location, context))
          }
        }

        return generatedPaths
      }

      override fun handle(extend: Extend, field: Field, context: Context): Path? {
        val typeSpec = kotlinGenerator.generateOptionType(extend, field) ?: return null
        val name = kotlinGenerator.generatedTypeName(extend.member(field))
        return write(name, typeSpec, field.qualifiedName, field.location, context)
      }

      private fun write(
        name: ClassName,
        typeSpec: TypeSpec,
        source: Any,
        location: Location,
        context: Context,
      ): Path {
        val modulePath = context.outDirectory
        val kotlinFile = FileSpec.builder(name.packageName, name.simpleName)
          .addFileComment(WireCompiler.CODE_GENERATED_BY_WIRE)
          .addFileComment("\nSource: %L in %L", source, location.withPathOnly())
          .addType(typeSpec)
          .build()
        val filePath = modulePath /
          kotlinFile.packageName.replace(".", "/") /
          "${kotlinFile.name}.kt"
        try {
          context.write(filePath, kotlinFile.toString())
        } catch (e: IOException) {
          throw IOException("Error emitting ${kotlinFile.packageName}.$source to $outDirectory", e)
        }
        return filePath
      }
    }
  }
}

// TODO(Benoit) Get kotlinGenerator to expose a factory from its module. Code should not be here.
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

  /** True to also generate gRPC server-compatible classes. Experimental feature. */
  val grpcServerCompatible: Boolean = false,

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  val nameSuffix: String? = null,
) : Target() {
  override fun newHandler(): SchemaHandler {
    return object : SchemaHandler() {
      private lateinit var kotlinGenerator: KotlinGenerator

      override fun handle(schema: Schema, context: Context) {
        val profileName = if (android) "android" else "java"
        val profile = context.profileLoader!!.loadProfile(profileName, schema)
        kotlinGenerator = KotlinGenerator(
          schema = schema,
          profile = profile,
          emitAndroid = android,
          javaInterop = javaInterop,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions,
          rpcCallStyle = rpcCallStyle,
          rpcRole = rpcRole,
          boxOneOfsMinSize = boxOneOfsMinSize,
          grpcServerCompatible = grpcServerCompatible,
          nameSuffix = nameSuffix,
        )
        context.fileSystem.createDirectories(context.outDirectory)
        super.handle(schema, context)
      }

      override fun handle(type: Type, context: Context): Path? {
        if (KotlinGenerator.builtInType(type.type)) return null

        val typeSpec = kotlinGenerator.generateType(type)
        val className = kotlinGenerator.generatedTypeName(type)
        return write(className, typeSpec, type.type, type.location, context)
      }

      override fun handle(service: Service, context: Context): List<Path> {
        if (rpcRole === RpcRole.NONE) return emptyList()

        val generatedPaths = mutableListOf<Path>()

        if (singleMethodServices) {
          service.rpcs.forEach { rpc ->
            val map = kotlinGenerator.generateServiceTypeSpecs(service, rpc)
            for ((className, typeSpec) in map) {
              generatedPaths.add(
                write(className, typeSpec, service.type, service.location, context)
              )
            }
          }
        } else {
          val map = kotlinGenerator.generateServiceTypeSpecs(service, null)
          for ((className, typeSpec) in map) {
            generatedPaths.add(write(className, typeSpec, service.type, service.location, context))
          }
        }

        return generatedPaths
      }

      override fun handle(extend: Extend, field: Field, context: Context): Path? {
        val typeSpec = kotlinGenerator.generateOptionType(extend, field) ?: return null
        val name = kotlinGenerator.generatedTypeName(extend.member(field))
        return write(name, typeSpec, field.qualifiedName, field.location, context)
      }

      private fun write(
        name: ClassName,
        typeSpec: TypeSpec,
        source: Any,
        location: Location,
        context: Context,
      ): Path {
        val modulePath = context.outDirectory
        val kotlinFile = FileSpec.builder(name.packageName, name.simpleName)
          .addFileComment(WireCompiler.CODE_GENERATED_BY_WIRE)
          .addFileComment("\nSource: %L in %L", source, location.withPathOnly())
          .addType(typeSpec)
          .build()
        val filePath = modulePath /
          kotlinFile.packageName.replace(".", "/") /
          "${kotlinFile.name}.kt"
        context.logger.artifactHandled(
          modulePath, "${kotlinFile.packageName}.${(kotlinFile.members.first() as TypeSpec).name}",
          "Kotlin"
        )
        try {
          context.fileSystem.createDirectories(filePath.parent!!)
          context.fileSystem.write(filePath) {
            writeUtf8(kotlinFile.toString())
          }
        } catch (e: IOException) {
          throw IOException("Error emitting ${kotlinFile.packageName}.$source to $outDirectory", e)
        }
        return filePath
      }
    }
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String
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
  override val outDirectory: String
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
            "Error emitting ${swiftFile.moduleName}.${typeName.canonicalName} to $modulePath", e
          )
        }

        context.logger.artifactHandled(
          modulePath, "${swiftFile.moduleName}.${typeName.canonicalName}", "Swift"
        )
        return filePath
      }

      override fun handle(service: Service, context: Context) = emptyList<Path>()
      override fun handle(
        extend: Extend,
        field: Field,
        context: Context
      ): Path? = null
    }
  }

  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String
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
  override val outDirectory: String
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
          if (!context.inSourcePath(protoFile) || protoFile.isEmpty()) continue

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
    outDirectory: String
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
  val schemaHandlerFactory: SchemaHandler.Factory,
) : Target() {
  override fun copyTarget(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String
  ): Target {
    return this.copy(
      includes = includes,
      excludes = excludes,
      exclusive = exclusive,
      outDirectory = outDirectory,
    )
  }

  override fun newHandler(): SchemaHandler {
    return schemaHandlerFactory.create()
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
  private val schemaHandlerFactoryClass: String
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

  override fun create(): SchemaHandler {
    return delegate.create()
  }

}
