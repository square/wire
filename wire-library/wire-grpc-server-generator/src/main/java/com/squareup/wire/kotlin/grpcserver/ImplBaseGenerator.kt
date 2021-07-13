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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import java.io.InputStream
import java.lang.UnsupportedOperationException

object ImplBaseGenerator {
  internal fun addImplBase(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service
  ) = builder
    .addType(
      TypeSpec.classBuilder("${service.name}ImplBase")
        .addModifiers(KModifier.ABSTRACT)
        .addSuperinterface(ClassName("io.grpc", "BindableService"))
        .apply { addImplBaseBody(generator,this, service) }
        .build()
    )

  internal fun addImplBaseRpcSignature(
    generator: ClassNameGenerator,
    builder: FunSpec.Builder,
    rpc: Rpc
  ): FunSpec.Builder {
    val requestType = generator.classNameFor(rpc.requestType!!)
    val responseType = generator.classNameFor(rpc.responseType!!)
    return when {
      rpc.requestStreaming -> builder
        .addParameter(
          "response", ClassName("io.grpc.stub", "StreamObserver").parameterizedBy(responseType)
        )
        .returns(
          ClassName("io.grpc.stub", "StreamObserver").parameterizedBy(requestType)
        )
      else -> builder
        .addParameter("request", requestType)
        .addParameter(
          "response", ClassName("io.grpc.stub", "StreamObserver").parameterizedBy(responseType)
        )
        .returns(UNIT)
    }
  }

  private fun addImplBaseBody(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service
  ): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .addModifiers(KModifier.OPEN)
          .apply { addImplBaseRpcSignature(generator, this, rpc) }
          .addCode(CodeBlock.of("throw %T()", UnsupportedOperationException::class.java))
          .build()
      )
    }

    builder.addFunction(
      FunSpec.builder("bindService")
        .addModifiers(KModifier.OVERRIDE)
        .returns(ClassName("io.grpc", "ServerServiceDefinition"))
        .addCode(bindServiceCodeBlock(service))
        .build()
    )

    service.rpcs
      .flatMap { listOf(it.requestType, it.responseType) }
      .distinct()
      .forEach {
        val className = generator.classNameFor(it!!)
        builder.addType(
          TypeSpec.classBuilder("${it.simpleName}Marshaller")
            .addSuperinterface(
              ClassName("io.grpc", "MethodDescriptor")
                .nestedClass("Marshaller")
                .parameterizedBy(className)
            )
            .addFunction(
              FunSpec.builder("stream")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec(name = "value", type = className))
                .returns(InputStream::class)
                .addCode(CodeBlock.of("return %T.ADAPTER.encode(value).inputStream()", className))
                .build()
            )
            .addFunction(
              FunSpec.builder("parse")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("stream", InputStream::class)
                .returns(className)
                .addCode(CodeBlock.of("return %T.ADAPTER.decode(stream)", className))
                .build()
            )
            .build()
        )
      }

    return builder
  }

  private fun bindServiceCodeBlock(service: Service): CodeBlock {
    val codeBlock =
      CodeBlock.Builder().add("return ServerServiceDefinition.builder(getServiceDescriptor())")
    service.rpcs.forEach {
      codeBlock.add(
        """.addMethod(
          get${it.name}Method(),
          %M(this@${service.name}ImplBase::${it.name})
        )""".trimIndent(),
        MemberName(ClassName("io.grpc.stub", "ServerCalls"), bindServiceCallType(it))
      )
    }
    codeBlock.add(".build()")
    return codeBlock.build()
  }

  private fun bindServiceCallType(rpc: Rpc): String = when {
    rpc.requestStreaming && rpc.responseStreaming -> "asyncBidiStreamingCall"
    rpc.requestStreaming -> "asyncClientStreamingCall"
    rpc.responseStreaming -> "asyncServerStreamingCall"
    else -> "asyncUnaryCall"
  }
}
