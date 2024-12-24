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
import javax.inject.Inject

/**
 * Specifies Wire's outputs (expressed as a list of [Target] objects) using Gradle's DSL (expressed
 * as destination directories and configuration options). This includes registering output
 * directories with the project so they can be compiled after they are generated.
 */
abstract class WireOutput {
  /** Set this to override the default output directory for this [WireOutput]. */
  var out: String? = null

  /**
   * Transforms this [WireOutput] into a [Target] for which Wire will generate code. The [Target]
   * should use [outputDirectory] instead of [WireOutput.out] in all cases for its output directory.
   */
  abstract fun toTarget(outputDirectory: String): Target
}

open class JavaOutput @Inject constructor() : WireOutput() {
  /** See [com.squareup.wire.schema.Target.includes] */
  var includes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.excludes] */
  var excludes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.exclusive] */
  var exclusive: Boolean = true

  /** True for emitted types to implement `android.os.Parcelable`. */
  var android: Boolean = false

  /** True to enable the `androidx.annotation.Nullable` annotation where applicable. */
  var androidAnnotations: Boolean = false

  /**
   * True to emit code that uses reflection for reading, writing, and toString methods which are
   * normally implemented with generated code.
   */
  var compact: Boolean = false

  /** True to emit types for options declared on messages, fields, etc. */
  var emitDeclaredOptions: Boolean = true

  /** True to emit annotations for options applied on messages, fields, etc. */
  var emitAppliedOptions: Boolean = true

  /** If true, the constructor of all generated types will be non-public. */
  var buildersOnly: Boolean = false

  override fun toTarget(outputDirectory: String): JavaTarget {
    return JavaTarget(
      includes = includes ?: listOf("*"),
      excludes = excludes ?: listOf(),
      exclusive = exclusive,
      outDirectory = outputDirectory,
      android = android,
      androidAnnotations = androidAnnotations,
      compact = compact,
      emitDeclaredOptions = emitDeclaredOptions,
      emitAppliedOptions = emitAppliedOptions,
      buildersOnly = buildersOnly,
    )
  }
}

open class KotlinOutput @Inject constructor() : WireOutput() {
  /** See [com.squareup.wire.schema.Target.includes] */
  var includes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.excludes] */
  var excludes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.exclusive] */
  var exclusive: Boolean = true

  /** True for emitted types to implement `android.os.Parcelable`. */
  var android: Boolean = false

  /** True for emitted types to implement APIs for easier migration from the Java target. */
  var javaInterop: Boolean = false

  /** True to emit types for options declared on messages, fields, etc. */
  var emitDeclaredOptions: Boolean = true

  /** True to emit annotations for options applied on messages, fields, etc. */
  var emitAppliedOptions: Boolean = true

  /** Blocking or suspending. */
  var rpcCallStyle: String = "suspending"

  /** Client, server, or none. */
  var rpcRole: String = "client"

  /** True for emitted services to implement one interface per RPC. */
  var singleMethodServices: Boolean = false

  /**
   * If a oneof has more than or [boxOneOfsMinSize] fields, it will be generated using boxed oneofs
   * as defined in [OneOf][com.squareup.wire.OneOf].
   */
  var boxOneOfsMinSize: Int = 5_000

  @Deprecated("See https://square.github.io/wire/wire_grpc/#wire-grpc-server")
  var grpcServerCompatible: Boolean = false

  /**
   * If present, generated services classes will use this as a suffix instead of inferring one
   * from the [rpcRole].
   */
  var nameSuffix: String? = null

  /**
   * If true, the constructor of all generated types will be non-public, and they will be
   * instantiable via their builders, regardless of the value of [javaInterop].
   */
  var buildersOnly: Boolean = false

  /** If true, Kotlin keywords are escaped with backticks. If false, an underscore is added as a suffix. */
  var escapeKotlinKeywords: Boolean = false

  /** enum_class or sealed_class. See [EnumMode][com.squareup.wire.kotlin.EnumMode]. */
  var enumMode: String = "enum_class"

  /**
   * If true, adapters will generate decode functions for `ProtoReader32`. Use this optimization
   * when targeting Kotlin/JS, where `Long` cursors are inefficient.
   */
  var emitProtoReader32: Boolean = false

  /**
   * If true, the generated classes will be mutable..
   */
  var mutableTypes: Boolean = false

  override fun toTarget(outputDirectory: String): KotlinTarget {
    if (grpcServerCompatible) {
      throw IllegalArgumentException(
        "grpcServerCompatible is no longer valid.\n" +
          "Please migrate by following the steps defined in https://square.github.io/wire/wire_grpc/#wire-grpc-server",
      )
    }

    val rpcCallStyle = RpcCallStyle.values()
      .singleOrNull { it.toString().equals(rpcCallStyle, ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown rpcCallStyle $rpcCallStyle. Valid values: ${RpcCallStyle.values().contentToString()}",
      )
    val rpcRole = RpcRole.values()
      .singleOrNull { it.toString().equals(rpcRole, ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown rpcRole $rpcRole. Valid values: ${RpcRole.values().contentToString()}",
      )

    val enumMode = EnumMode.values()
      .singleOrNull { it.toString().equals(enumMode, ignoreCase = true) }
      ?: throw IllegalArgumentException(
        "Unknown enumMode $enumMode. Valid values: ${EnumMode.values().contentToString()}",
      )

    return KotlinTarget(
      includes = includes ?: listOf("*"),
      excludes = excludes ?: listOf(),
      exclusive = exclusive,
      outDirectory = outputDirectory,
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
      enumMode = enumMode,
      emitProtoReader32 = emitProtoReader32,
      mutableTypes = mutableTypes,
    )
  }
}

open class ProtoOutput @Inject constructor() : WireOutput() {
  override fun toTarget(outputDirectory: String): ProtoTarget {
    return ProtoTarget(outDirectory = outputDirectory)
  }
}

open class CustomOutput @Inject constructor() : WireOutput() {
  /** See [com.squareup.wire.schema.Target.includes] */
  var includes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.excludes] */
  var excludes: List<String>? = null

  /** See [com.squareup.wire.schema.Target.exclusive] */
  var exclusive: Boolean = true

  /**
   * Black boxed payload which a caller can set for the custom [SchemaHandler.Factory] to receive.
   */
  var options: Map<String, String>? = null

  /** Assign the schema handler factory instance. */
  var schemaHandlerFactory: SchemaHandler.Factory? = null

  /**
   * Assign the schema handler factory by name. If you use a class name, that class must have a
   * no-arguments constructor.
   */
  var schemaHandlerFactoryClass: String? = null

  override fun toTarget(outputDirectory: String): CustomTarget {
    check((schemaHandlerFactory != null) || (schemaHandlerFactoryClass != null)) {
      "schemaHandlerFactory or schemaHandlerFactoryClass required"
    }
    return CustomTarget(
      includes = includes ?: listOf("*"),
      excludes = excludes ?: listOf(),
      exclusive = exclusive,
      outDirectory = outputDirectory,
      options = options ?: mapOf(),
      schemaHandlerFactory = schemaHandlerFactory ?: newSchemaHandler(schemaHandlerFactoryClass!!),
    )
  }
}
