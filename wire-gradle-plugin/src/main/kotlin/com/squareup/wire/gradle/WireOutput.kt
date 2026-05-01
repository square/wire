/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.gradle

import com.squareup.wire.kotlin.EnumMode
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.schema.CustomTarget
import com.squareup.wire.schema.JavaTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Target
import com.squareup.wire.schema.newSchemaHandler
import java.io.File
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Specifies Wire's outputs (expressed as a list of [Target] objects) using Gradle's DSL (expressed
 * as destination directories and configuration options). This includes registering output
 * directories with the project so they can be compiled after they are generated.
 */
abstract class WireOutput {
  @get:Inject
  protected abstract val objectFactory: ObjectFactory

  val out: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java)
  }

  /** Set this to override the default output directory for this [WireOutput]. */
  fun setOut(value: String?) {
    out.set(value)
  }

  fun out(value: String) {
    out.set(value)
  }

  internal fun outputDirectory(
    projectDir: File,
    defaultOutputDirectory: Provider<String>,
  ): Provider<String> = out.map { relativizeOutputDirectory(it, projectDir) }.orElse(defaultOutputDirectory)

  /**
   * Transforms this [WireOutput] into a [Target] for which Wire will generate code. The [Target]
   * should use [outputDirectory] instead of [WireOutput.out] in all cases for its output directory.
   */
  abstract fun toTarget(outputDirectory: String): Target
}

internal fun relativizeOutputDirectory(
  outputDirectory: String,
  projectDir: File,
): String {
  val file = File(outputDirectory)
  if (!file.isAbsolute) return outputDirectory
  return runCatching { file.relativeTo(projectDir).path }
    .getOrElse { outputDirectory }
}

abstract class JavaOutput @Inject constructor() : WireOutput() {
  val includes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val excludes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val exclusive: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val android: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val androidAnnotations: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val compact: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val emitDeclaredOptions: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val emitAppliedOptions: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val buildersOnly: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  /** See [com.squareup.wire.schema.Target.includes] */
  fun setIncludes(values: List<String>?) {
    includes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.excludes] */
  fun setExcludes(values: List<String>?) {
    excludes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.exclusive] */
  fun setExclusive(value: Boolean) {
    exclusive.set(value)
  }

  /** True for emitted types to implement `android.os.Parcelable`. */
  fun setAndroid(value: Boolean) {
    android.set(value)
  }

  /** True to enable the `androidx.annotation.Nullable` annotation where applicable. */
  fun setAndroidAnnotations(value: Boolean) {
    androidAnnotations.set(value)
  }

  /**
   * True to emit code that uses reflection for reading, writing, and toString methods which are
   * normally implemented with generated code.
   */
  fun setCompact(value: Boolean) {
    compact.set(value)
  }

  /** True to emit types for options declared on messages, fields, etc. */
  fun setEmitDeclaredOptions(value: Boolean) {
    emitDeclaredOptions.set(value)
  }

  /** True to emit annotations for options applied on messages, fields, etc. */
  fun setEmitAppliedOptions(value: Boolean) {
    emitAppliedOptions.set(value)
  }

  /** If true, the constructor of all generated types will be non-public. */
  fun setBuildersOnly(value: Boolean) {
    buildersOnly.set(value)
  }

  override fun toTarget(outputDirectory: String): JavaTarget = JavaTarget(
    includes = includes.orNull ?: listOf("*"),
    excludes = excludes.orNull ?: listOf(),
    exclusive = exclusive.get(),
    outDirectory = outputDirectory,
    android = android.get(),
    androidAnnotations = androidAnnotations.get(),
    compact = compact.get(),
    emitDeclaredOptions = emitDeclaredOptions.get(),
    emitAppliedOptions = emitAppliedOptions.get(),
    buildersOnly = buildersOnly.get(),
  )
}

abstract class KotlinOutput @Inject constructor() : WireOutput() {
  val includes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val excludes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val exclusive: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val android: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val javaInterop: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val emitDeclaredOptions: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val emitAppliedOptions: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  val rpcCallStyle: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java).convention("suspending")
  }

  val rpcRole: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java).convention("client")
  }

  val singleMethodServices: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val boxOneOfsMinSize: Property<Int> by lazy(NONE) {
    objectFactory.property(Int::class.javaObjectType).convention(5_000)
  }

  @Deprecated("See https://square.github.io/wire/wire_grpc/#wire-grpc-server")
  val grpcServerCompatible: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val nameSuffix: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java)
  }

  val buildersOnly: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val escapeKotlinKeywords: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val enumMode: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java).convention("enum_class")
  }

  val emitProtoReader32: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val mutableTypes: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val explicitStreamingCalls: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(false)
  }

  val makeImmutableCopies: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.javaObjectType).convention(true)
  }

  /** See [com.squareup.wire.schema.Target.includes] */
  fun setIncludes(values: List<String>?) {
    includes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.excludes] */
  fun setExcludes(values: List<String>?) {
    excludes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.exclusive] */
  fun setExclusive(value: Boolean) {
    exclusive.set(value)
  }

  /** True for emitted types to implement `android.os.Parcelable`. */
  fun setAndroid(value: Boolean) {
    android.set(value)
  }

  /** True for emitted types to implement APIs for easier migration from the Java target. */
  fun setJavaInterop(value: Boolean) {
    javaInterop.set(value)
  }

  /** True to emit types for options declared on messages, fields, etc. */
  fun setEmitDeclaredOptions(value: Boolean) {
    emitDeclaredOptions.set(value)
  }

  /** True to emit annotations for options applied on messages, fields, etc. */
  fun setEmitAppliedOptions(value: Boolean) {
    emitAppliedOptions.set(value)
  }

  /** Blocking or suspending. */
  fun setRpcCallStyle(value: String) {
    rpcCallStyle.set(value)
  }

  /** Client, server, or none. */
  fun setRpcRole(value: String) {
    rpcRole.set(value)
  }

  /** True for emitted services to implement one interface per RPC. */
  fun setSingleMethodServices(value: Boolean) {
    singleMethodServices.set(value)
  }

  /**
   * If a oneof has more than or [boxOneOfsMinSize] fields, it will be generated using boxed oneofs
   * as defined in [OneOf][com.squareup.wire.OneOf].
   */
  fun setBoxOneOfsMinSize(value: Int) {
    boxOneOfsMinSize.set(value)
  }

  @Deprecated("See https://square.github.io/wire/wire_grpc/#wire-grpc-server")
  fun setGrpcServerCompatible(value: Boolean) {
    grpcServerCompatible.set(value)
  }

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  fun setNameSuffix(value: String?) {
    nameSuffix.set(value)
  }

  /**
   * If true, the constructor of all generated types will be non-public, and they will be
   * instantiable via their builders, regardless of the value of [javaInterop].
   */
  fun setBuildersOnly(value: Boolean) {
    buildersOnly.set(value)
  }

  /** If true, Kotlin keywords are escaped with backticks. If false, an underscore is added as a suffix. */
  fun setEscapeKotlinKeywords(value: Boolean) {
    escapeKotlinKeywords.set(value)
  }

  /** enum_class or sealed_class. See [EnumMode][com.squareup.wire.kotlin.EnumMode]. */
  fun setEnumMode(value: String) {
    enumMode.set(value)
  }

  /**
   * If true, adapters will generate decode functions for `ProtoReader32`. Use this optimization
   * when targeting Kotlin/JS, where `Long` cursors are inefficient.
   */
  fun setEmitProtoReader32(value: Boolean) {
    emitProtoReader32.set(value)
  }

  /** If true, the generated classes will be mutable. */
  fun setMutableTypes(value: Boolean) {
    mutableTypes.set(value)
  }

  /**
   * If true, the generated gRPC client will use explicit classes for client, server,
   * and bidirectional streaming calls.
   */
  fun setExplicitStreamingCalls(value: Boolean) {
    explicitStreamingCalls.set(value)
  }

  /**
   * If false, repeated and map fields will not have immutable copies made when constructing
   * a [com.squareup.wire.Message].
   */
  fun setMakeImmutableCopies(value: Boolean) {
    makeImmutableCopies.set(value)
  }

  override fun toTarget(outputDirectory: String): KotlinTarget {
    if (grpcServerCompatible.get()) {
      throw IllegalArgumentException(
        "grpcServerCompatible is no longer valid.\n" +
          "Please migrate by following the steps defined in https://square.github.io/wire/wire_grpc/#wire-grpc-server",
      )
    }

    val rpcCallStyleValue = RpcCallStyle.values()
      .singleOrNull { it.toString().equals(rpcCallStyle.get(), ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown rpcCallStyle ${rpcCallStyle.get()}. Valid values: ${RpcCallStyle.values().contentToString()}",
      )
    val rpcRoleValue = RpcRole.values()
      .singleOrNull { it.toString().equals(rpcRole.get(), ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown rpcRole ${rpcRole.get()}. Valid values: ${RpcRole.values().contentToString()}",
      )

    val enumModeValue = EnumMode.values()
      .singleOrNull { it.toString().equals(enumMode.get(), ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown enumMode ${enumMode.get()}. Valid values: ${EnumMode.values().contentToString()}",
      )

    return KotlinTarget(
      includes = includes.orNull ?: listOf("*"),
      excludes = excludes.orNull ?: listOf(),
      exclusive = exclusive.get(),
      outDirectory = outputDirectory,
      android = android.get(),
      javaInterop = javaInterop.get(),
      emitDeclaredOptions = emitDeclaredOptions.get(),
      emitAppliedOptions = emitAppliedOptions.get(),
      rpcCallStyle = rpcCallStyleValue,
      rpcRole = rpcRoleValue,
      singleMethodServices = singleMethodServices.get(),
      boxOneOfsMinSize = boxOneOfsMinSize.get(),
      nameSuffix = nameSuffix.orNull,
      buildersOnly = buildersOnly.get(),
      escapeKotlinKeywords = escapeKotlinKeywords.get(),
      enumMode = enumModeValue,
      emitProtoReader32 = emitProtoReader32.get(),
      mutableTypes = mutableTypes.get(),
      explicitStreamingCalls = explicitStreamingCalls.get(),
      makeImmutableCopies = makeImmutableCopies.get(),
    )
  }
}

abstract class ProtoOutput @Inject constructor() : WireOutput() {
  override fun toTarget(outputDirectory: String): ProtoTarget = ProtoTarget(outDirectory = outputDirectory)
}

abstract class CustomOutput @Inject constructor() : WireOutput() {
  val includes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val excludes: ListProperty<String> by lazy(NONE) {
    objectFactory.listProperty(String::class.java)
  }

  val exclusive: Property<Boolean> by lazy(NONE) {
    objectFactory.property(Boolean::class.java).convention(true)
  }

  val options: MapProperty<String, String> by lazy(NONE) {
    objectFactory.mapProperty(String::class.java, String::class.java)
  }

  val schemaHandlerFactory: Property<SchemaHandler.Factory> by lazy(NONE) {
    objectFactory.property(SchemaHandler.Factory::class.java)
  }

  val schemaHandlerFactoryClass: Property<String> by lazy(NONE) {
    objectFactory.property(String::class.java)
  }

  /** See [com.squareup.wire.schema.Target.includes] */
  fun setIncludes(values: List<String>?) {
    includes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.excludes] */
  fun setExcludes(values: List<String>?) {
    excludes.set(values)
  }

  /** See [com.squareup.wire.schema.Target.exclusive] */
  fun setExclusive(value: Boolean) {
    exclusive.set(value)
  }

  fun exclusive(value: Boolean) {
    exclusive.set(value)
  }

  /**
   * Black boxed payload which a caller can set for the custom [SchemaHandler.Factory] to receive.
   */
  fun options(value: Map<String, String>) {
    options.set(value)
  }

  fun setOptions(value: Map<String, String>?) {
    options.set(value)
  }

  /** Assign the schema handler factory instance. */
  fun setSchemaHandlerFactory(value: SchemaHandler.Factory?) {
    schemaHandlerFactory.set(value)
  }

  /**
   * Assign the schema handler factory by name. If you use a class name, that class must have a
   * no-arguments constructor.
   */
  fun setSchemaHandlerFactoryClass(value: String?) {
    schemaHandlerFactoryClass.set(value)
  }

  fun schemaHandlerFactoryClass(value: String) {
    schemaHandlerFactoryClass.set(value)
  }

  override fun toTarget(outputDirectory: String): CustomTarget {
    val configuredSchemaHandlerFactory = schemaHandlerFactory.orNull
    val configuredSchemaHandlerFactoryClass = schemaHandlerFactoryClass.orNull

    check(configuredSchemaHandlerFactory != null || configuredSchemaHandlerFactoryClass != null) {
      "schemaHandlerFactory or schemaHandlerFactoryClass required"
    }

    return CustomTarget(
      includes = includes.orNull ?: listOf("*"),
      excludes = excludes.orNull ?: listOf(),
      exclusive = exclusive.orElse(true).get(),
      outDirectory = outputDirectory,
      options = options.orNull ?: mapOf(),
      schemaHandlerFactory = configuredSchemaHandlerFactory ?: newSchemaHandler(configuredSchemaHandlerFactoryClass!!),
    )
  }
}
