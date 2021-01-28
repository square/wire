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
import com.squareup.wire.kotlin.grpcserver.ImplBaseGenerator.addImplBaseRpcSignature
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service

object StubGenerator {
  internal fun addStub(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service
  ): TypeSpec.Builder {
    val serviceClassName = generator.classNameFor(service.type)
    val stubClassName = ClassName(
      packageName = serviceClassName.packageName,
      "${serviceClassName.simpleName}WireGrpc", "${serviceClassName.simpleName}Stub"
    )
    return builder
      .addFunction(
        FunSpec.builder("newStub")
          .addParameter("channel", ClassName("io.grpc", "Channel"))
          .returns(stubClassName)
          .addCode("return %T(channel)", stubClassName)
          .build()
      )
      .addType(
        TypeSpec.classBuilder(stubClassName)
          .apply {
            addAbstractStubConstructor(generator, this, service)
            addStubRpcCalls(generator, this, service)
          }
          .build()
      )
  }

  internal fun addAbstractStubConstructor(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service
  ): TypeSpec.Builder {
    val serviceClassName = generator.classNameFor(service.type)
    val stubClassName = ClassName(
      packageName = serviceClassName.packageName,
      "${serviceClassName.simpleName}WireGrpc", "${serviceClassName.simpleName}Stub"
    )
    return builder
      // Really this is a superclass, just want to add secondary constructors.
      .addSuperinterface(
        ClassName("io.grpc.stub", "AbstractStub")
          .parameterizedBy(stubClassName)
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
  }

  private fun addStubRpcCalls(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service
  ): TypeSpec.Builder {
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
          .apply { addImplBaseRpcSignature(generator, this, rpc) }
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
