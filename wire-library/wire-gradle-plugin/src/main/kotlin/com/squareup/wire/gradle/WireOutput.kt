/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.gradle

import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.schema.CustomTargetBeta
import com.squareup.wire.schema.JavaTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.ProtoTarget
import com.squareup.wire.schema.Target
import javax.inject.Inject

/**
 * Specifies Wire's outputs (expressed as a list of [Target] objects) using Gradle's DSL (expressed
 * as destination directories and configuration options). This includes registering output
 * directories with the project so they can be compiled after they are generated.
 */
abstract class WireOutput {
  /** This will be set to a non-null value before [toTarget] is invoked. */
  var out: String? = null

  /** Create a target for the WireCompiler to use when emitting sources. */
  abstract fun toTarget(): Target
}

open class JavaOutput @Inject constructor() : WireOutput() {
  var includes: List<String>? = null
  var excludes: List<String>? = null
  var exclusive: Boolean = true
  var android: Boolean = false
  var androidAnnotations: Boolean = false
  var compact: Boolean = false
  var emitDeclaredOptions: Boolean = true
  var emitAppliedOptions: Boolean = false

  override fun toTarget(): JavaTarget {
    return JavaTarget(
        includes = includes ?: listOf("*"),
        excludes = excludes ?: listOf(),
        exclusive = exclusive,
        outDirectory = out!!,
        android = android,
        androidAnnotations = androidAnnotations,
        compact = compact,
        emitDeclaredOptions = emitDeclaredOptions,
        emitAppliedOptions = emitAppliedOptions
    )
  }
}

open class KotlinOutput @Inject constructor() : WireOutput() {
  var includes: List<String>? = null
  var excludes: List<String>? = null
  var exclusive: Boolean = true
  var android: Boolean = false
  var javaInterop: Boolean = false
  var emitDeclaredOptions: Boolean = true
  var emitAppliedOptions: Boolean = false
  var rpcCallStyle: String = "suspending"
  var rpcRole: String = "client"
  var singleMethodServices: Boolean = false
  var boxOneOfsMinSize: Int = 5_000
  var grpcServerCompatible: Boolean = false
  var nameSuffix: String? = null

  override fun toTarget(): KotlinTarget {
    val rpcCallStyle = RpcCallStyle.values()
        .singleOrNull { it.toString().equals(rpcCallStyle, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "Unknown rpcCallStyle $rpcCallStyle. Valid values: ${RpcCallStyle.values().contentToString()}")
    val rpcRole = RpcRole.values()
        .singleOrNull { it.toString().equals(rpcRole, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "Unknown rpcRole $rpcRole. Valid values: ${RpcRole.values().contentToString()}")

    return KotlinTarget(
        includes = includes ?: listOf("*"),
        excludes = excludes ?: listOf(),
        exclusive = exclusive,
        outDirectory = out!!,
        android = android,
        javaInterop = javaInterop,
        emitDeclaredOptions = emitDeclaredOptions,
        emitAppliedOptions = emitAppliedOptions,
        rpcCallStyle = rpcCallStyle,
        rpcRole = rpcRole,
        singleMethodServices = singleMethodServices,
        boxOneOfsMinSize = boxOneOfsMinSize,
        grpcServerCompatible = grpcServerCompatible,
        nameSuffix = nameSuffix,
    )
  }
}

open class ProtoOutput @Inject constructor() : WireOutput() {
  override fun toTarget(): ProtoTarget {
    return ProtoTarget(outDirectory = out!!)
  }
}

open class CustomOutput @Inject constructor() : WireOutput() {
  var includes: List<String>? = null
  var excludes: List<String>? = null
  var exclusive: Boolean = true
  var customHandlerClass: String? = null

  override fun toTarget(): CustomTargetBeta {
    return CustomTargetBeta(
        includes = includes ?: listOf("*"),
        excludes = excludes ?: listOf(),
        exclusive = exclusive,
        outDirectory = out!!,
        customHandlerClass = customHandlerClass
            ?: throw IllegalArgumentException("customHandlerClass required")
    )
  }
}
