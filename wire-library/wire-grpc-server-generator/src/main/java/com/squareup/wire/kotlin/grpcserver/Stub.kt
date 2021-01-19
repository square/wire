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
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.kotlin.grpcserver.ImplBase.addImplBaseRpcSignature
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service

object Stub {
  internal fun addStub(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder = builder
    .addFunction(
      FunSpec.builder("newStub")
        .addParameter("channel", ClassName("io.grpc", "Channel"))
        .returns(ClassName("", "${service.name}Stub"))
        .addCode("return %T(channel)", ClassName("", "${service.name}Stub"))
        .build()
    )
    .addType(
      TypeSpec.classBuilder("${service.name}Stub")
        .apply {
          addAbstractStubConstructor(this, service)
          addStubRpcCalls(this, service)
        }
        .build()
    )

  internal fun addAbstractStubConstructor(builder: TypeSpec.Builder, service: Service) = builder
    // Really this is a superclass, just want to add secondary constructors.
    .addSuperinterface(
      ClassName("io.grpc.stub", "AbstractStub")
        .parameterizedBy(ClassName("", "${service.name}Stub"))
    )
    .addFunction(
      FunSpec.constructorBuilder()
        .addModifiers(KModifier.INTERNAL)
        .addParameter("channel", ClassName("io.grpc", "Channel"))
        .callSuperConstructor("channel")
        .build()
    )
    .addFunction(
      FunSpec.constructorBuilder()
        .addModifiers(KModifier.INTERNAL)
        .addParameter("channel", ClassName("io.grpc", "Channel"))
        .addParameter("callOptions", ClassName("io.grpc", "CallOptions"))
        .callSuperConstructor("channel", "callOptions")
        .build()
    )
    .addFunction(
      FunSpec.builder("build")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("channel", ClassName("io.grpc", "Channel"))
        .addParameter("callOptions", ClassName("io.grpc", "CallOptions"))
        .addStatement("return ${service.name}Stub(channel, callOptions)")
        .build()
    )

  private fun addStubRpcCalls(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val codeBlock = when {
        rpc.requestStreaming ->
          CodeBlock.of(
            "return %M(channel.newCall(get${rpc.name}Method(), callOptions), response)",
            MemberName(ClassName("io.grpc.stub", "ClientCalls"), clientCallType(rpc))
          )
        else ->
          CodeBlock.of(
            "%M(channel.newCall(get${rpc.name}Method(), callOptions), request, response)",
            MemberName(ClassName("io.grpc.stub", "ClientCalls"), clientCallType(rpc))
          )
      }
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .apply { addImplBaseRpcSignature(this, rpc) }
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }

  private fun clientCallType(rpc: Rpc): String = when {
    rpc.requestStreaming && rpc.responseStreaming -> "asyncBidiStreamingCall"
    rpc.requestStreaming -> "asyncClientStreamingCall"
    rpc.responseStreaming -> "asyncServerStreamingCall"
    else -> "asyncUnaryCall"
  }
}
