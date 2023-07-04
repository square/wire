package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.Path
import java.io.IOException

class KotlinSchemaHandler(
  private val outDirectory: String,

  /** True for emitted types to implement `android.os.Parcelable`. */
  private val android: Boolean = false,

  /** True for emitted types to implement APIs for easier migration from the Java target. */
  private val javaInterop: Boolean = false,

  /** True to emit types for options declared on messages, fields, etc. */
  private val emitDeclaredOptions: Boolean = true,

  /** True to emit annotations for options applied on messages, fields, etc. */
  private val emitAppliedOptions: Boolean = true,

  /** Blocking or suspending. */
  private val rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,

  /** Client or server. */
  private val rpcRole: RpcRole = RpcRole.CLIENT,

  /** True for emitted services to implement one interface per RPC. */
  private val singleMethodServices: Boolean = false,

  /**
   * If a oneof has more than or [boxOneOfsMinSize] fields, it will be generated using boxed oneofs
   * as defined in [OneOf][com.squareup.wire.OneOf].
   */
  private val boxOneOfsMinSize: Int = 5_000,

  /** True to also generate gRPC server-compatible classes. Experimental feature. */
  private val grpcServerCompatible: Boolean = false,

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  private val nameSuffix: String? = null,

  /**
   * If true, the constructor of all generated types will be non-public, and they will be
   * instantiable via their builders, regardless of the value of [javaInterop].
   */
  private val buildersOnly: Boolean = false,
) : SchemaHandler() {
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
      buildersOnly = buildersOnly,
      singleMethodServices = singleMethodServices,
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

    if (grpcServerCompatible) {
      val map = kotlinGenerator.generateGrpcServerAdapter(service)
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
      .addFileComment(CODE_GENERATED_BY_WIRE)
      .addFileComment("\nSource: %L in %L", source, location.withPathOnly())
      .addType(typeSpec)
      .build()
    val filePath = modulePath /
      kotlinFile.packageName.replace(".", "/") /
      "${kotlinFile.name}.kt"

    context.logger.artifactHandled(
      modulePath,
      "${kotlinFile.packageName}.${(kotlinFile.members.first() as TypeSpec).name}",
      "Kotlin",
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

  companion object {
    private const val CODE_GENERATED_BY_WIRE = "Code generated by Wire protocol buffer compiler, do not edit."
  }
}
