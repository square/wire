/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import io.grpc.MethodDescriptor

object MethodDescriptor {
  internal fun addMethodDescriptor(
    builder: TypeSpec.Builder,
    service: Service,
    rpc: Rpc
  ): TypeSpec.Builder {
    val requestType = ClassName.bestGuess(rpc.requestType.toString())
    val responseType = ClassName.bestGuess(rpc.responseType.toString())
    return builder
      .addProperty(
        PropertySpec.builder(
          name = "get${rpc.name}Method",
          type = methodDescriptorType(requestType, responseType).copy(nullable = true),
          modifiers = listOf(KModifier.PRIVATE)
        )
          .mutable(true)
          .addAnnotation(Volatile::class)
          .initializer("null")
          .build()
      )
      .addFunction(
        FunSpec.builder("get${rpc.name}Method")
          .returns(methodDescriptorType(requestType, responseType))
          .addCode(methodDescriptorCodeBlock(service, rpc))
          .build()
      )
  }

  private fun methodDescriptorCodeBlock(service: Service, rpc: Rpc): CodeBlock {
    val requestType = ClassName.bestGuess(rpc.requestType.toString())
    val responseType = ClassName.bestGuess(rpc.responseType.toString())
    val codeBlock = CodeBlock.builder()
    codeBlock.addStatement(
      "var result: %T = get${rpc.name}Method",
      methodDescriptorType(requestType, responseType).copy(nullable = true)
    )
    codeBlock.add(
      """
        if (result == null) {
          synchronized(${service.name}WireGrpc::class) {
            result = get${rpc.name}Method
            if (result == null) {
              get${rpc.name}Method = MethodDescriptor.newBuilder<%T, %T>()
                .setType(MethodDescriptor.MethodType.${methodType(rpc)})
                .setFullMethodName(
                  MethodDescriptor.generateFullMethodName(
                    "${service.type}", "${rpc.name}"
                  )
                )
                .setSampledToLocalTracing(true)
                .setRequestMarshaller(${service.name}ImplBase.${rpc.requestType!!.simpleName}Marshaller())
                .setResponseMarshaller(${service.name}ImplBase.${rpc.responseType!!.simpleName}Marshaller())
                .build()
            }
          }
        }
        return get${rpc.name}Method!!
      """.trimIndent(),
      requestType,
      responseType
    )
    return codeBlock.build()
  }

  private fun methodDescriptorType(
    requestType: ClassName,
    responseType: ClassName
  ) = MethodDescriptor::class
    .asTypeName()
    .parameterizedBy(requestType, responseType)

  private fun methodType(rpc: Rpc): String = when {
    rpc.requestStreaming && rpc.responseStreaming -> "BIDI_STREAMING"
    rpc.requestStreaming -> "CLIENT_STREAMING"
    rpc.responseStreaming -> "SERVER_STREAMING"
    else -> "UNARY"
  }
}