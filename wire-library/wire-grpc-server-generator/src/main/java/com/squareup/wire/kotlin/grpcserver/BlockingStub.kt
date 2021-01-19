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
import com.squareup.wire.kotlin.grpcserver.Stub.addAbstractStubConstructor
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service

object BlockingStub {
  internal fun addBlockingStub(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder =
    builder
      .addFunction(
        FunSpec.builder("newBlockingStub")
          .addParameter("channel", ClassName("io.grpc", "Channel"))
          .returns(ClassName("", "${service.name}BlockingStub"))
          .addCode("return %T(channel)", ClassName("", "${service.name}BlockingStub"))
          .build()
      )
      .addType(
        TypeSpec.classBuilder("${service.name}BlockingStub")
          .apply { addAbstractStubConstructor(this, service) }
          .addBlockingStubRpcCalls(service)
          .build()
      )

  private fun TypeSpec.Builder.addBlockingStubRpcCalls(service: Service): TypeSpec.Builder {
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
            .addBlockingStubSignature(rpc)
            .addCode(codeBlock)
            .build()
        )
      }
    return this
  }

  private fun FunSpec.Builder.addBlockingStubSignature(rpc: Rpc): FunSpec.Builder = this
    .addParameter("request", ClassName.bestGuess(rpc.requestType.toString()))
    .returns(
      if (rpc.requestStreaming)
        Iterator::class.asClassName()
          .parameterizedBy(ClassName.bestGuess(rpc.responseType.toString()))
      else ClassName.bestGuess(rpc.responseType.toString())
    )
}