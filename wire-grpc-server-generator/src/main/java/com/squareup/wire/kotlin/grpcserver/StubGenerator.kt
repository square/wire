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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.wire.kotlin.grpcserver.ImplBaseGenerator.addImplBaseRpcSignature
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service

object StubGenerator {
  internal fun addStub(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    return if (options.suspendingCalls) {
      suspendedStubs(generator, service, builder, options)
    } else {
      asyncStubs(generator, service, builder, options)
    }
  }

  private fun suspendedStubs(
    generator: ClassNameGenerator,
    service: Service,
    builder: TypeSpec.Builder,
    options: KotlinGrpcGenerator.Companion.Options
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
            addAbstractStubConstructor(generator, this, service,
              ClassName("io.grpc.kotlin", "AbstractCoroutineStub"))
            addSuspendedStubRpcCalls(generator, this, service, options)
          }
          .build()
      )
  }

  private fun asyncStubs(
    generator: ClassNameGenerator,
    service: Service,
    builder: TypeSpec.Builder,
    options: KotlinGrpcGenerator.Companion.Options
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
            addAbstractStubConstructor(generator, this, service, ClassName("io.grpc.stub", "AbstractStub"))
            addStubRpcCalls(generator, this, service, options)
          }
          .build()
      )
  }

  internal fun addAbstractStubConstructor(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    superClass: ClassName
  ): TypeSpec.Builder {
    val serviceClassName = generator.classNameFor(service.type)
    val stubClassName = ClassName(
      packageName = serviceClassName.packageName,
      "${serviceClassName.simpleName}WireGrpc", "${serviceClassName.simpleName}Stub"
    )
    return builder
      // Really this is a superclass, just want to add secondary constructors.
      .addSuperinterface(superClass.parameterizedBy(stubClassName))
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
          .returns(ClassName("", "${service.name}Stub"))
          .build()
      )
  }

  private fun addStubRpcCalls(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: KotlinGrpcGenerator.Companion.Options
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
          .apply { addImplBaseRpcSignature(generator, this, rpc, options) }
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }

  private fun addSuspendedStubRpcCalls(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val codeBlock = CodeBlock.of(
        "return %M(channel, get${rpc.name}Method(), request, callOptions)",
        MemberName(ClassName("io.grpc.kotlin", "ClientCalls"), suspendedClientCallType(rpc))
      )
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .apply { addImplBaseRpcSignature(generator, this, rpc, options) }
          .addModifiers(KModifier.SUSPEND)
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }

  private fun suspendedClientCallType(rpc: Rpc): String = when {
    rpc.requestStreaming && rpc.responseStreaming -> "bidiStreamingRpc"
    rpc.requestStreaming -> "clientStreamingRpc"
    rpc.responseStreaming -> "serverStreamingRpc"
    else -> "unaryRpc"
  }

  private fun clientCallType(rpc: Rpc): String = when {
    rpc.requestStreaming && rpc.responseStreaming -> "asyncBidiStreamingCall"
    rpc.requestStreaming -> "asyncClientStreamingCall"
    rpc.responseStreaming -> "asyncServerStreamingCall"
    else -> "asyncUnaryCall"
  }
}
