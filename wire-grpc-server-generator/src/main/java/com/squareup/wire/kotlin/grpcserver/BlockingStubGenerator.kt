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
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.kotlin.grpcserver.StubGenerator.addAbstractStubConstructor
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service

object BlockingStubGenerator {
  internal fun addBlockingStub(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    if (!options.suspendingCalls) {
      val serviceClassName = generator.classNameFor(service.type)
      val stubClassName = ClassName(
        packageName = serviceClassName.packageName,
        "${serviceClassName.simpleName}WireGrpc",
        "${serviceClassName.simpleName}BlockingStub"
      )
      return builder
        .addFunction(
          FunSpec.builder("newBlockingStub")
            .addParameter("channel", ClassName("io.grpc", "Channel"))
            .returns(stubClassName)
            .addCode("return %T(channel)", stubClassName)
            .build()
        )
        .addType(
          TypeSpec.classBuilder(stubClassName)
            .apply { addAbstractStubConstructor(generator, this, service,
              ClassName("io.grpc.stub", "AbstractStub")) }
            .addBlockingStubRpcCalls(generator, service)
            .build()
        )
    } else {
      return builder
    }
  }

  private fun TypeSpec.Builder.addBlockingStubRpcCalls(generator: ClassNameGenerator, service: Service): TypeSpec.Builder {
    service.rpcs
      .filter { !it.responseStreaming }
      .forEach { rpc ->
        val codeBlock = CodeBlock.of(
          "return %M(channel, get${rpc.name}Method(), callOptions, request)",
          MemberName(
            enclosingClassName = ClassName("io.grpc.stub", "ClientCalls"),
            simpleName = if (rpc.requestStreaming) "blockingServerStreamingCall"
            else "blockingUnaryCall"
          )
        )
        this.addFunction(
          FunSpec.builder(rpc.name)
            .addBlockingStubSignature(generator, rpc)
            .addCode(codeBlock)
            .build()
        )
      }
    return this
  }

  private fun FunSpec.Builder.addBlockingStubSignature(generator: ClassNameGenerator, rpc: Rpc): FunSpec.Builder = this
    .addParameter("request", ClassName.bestGuess(generator.classNameFor(rpc.requestType!!).toString()))
    .returns(
      if (rpc.requestStreaming)
        Iterator::class.asClassName()
          .parameterizedBy(generator.classNameFor(rpc.responseType!!))
      else generator.classNameFor(rpc.responseType!!)
    )
}
