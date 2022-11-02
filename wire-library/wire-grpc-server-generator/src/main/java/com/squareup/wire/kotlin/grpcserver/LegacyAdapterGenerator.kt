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
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.kotlin.grpcserver.ImplBaseGenerator.addImplBaseRpcSignature
import com.squareup.wire.schema.Service
import java.util.concurrent.ExecutorService

object LegacyAdapterGenerator {

  data class Options(val singleMethodServices: Boolean)

  internal fun addLegacyAdapter(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: Options
  ): TypeSpec.Builder {
    val serviceClassName = generator.classNameFor(service.type)
    val implBaseClassName = ClassName(
      serviceClassName.packageName,
      "${service.name}WireGrpc",
      "${service.name}ImplBase"
    )
    return builder
      .addType(
        TypeSpec.classBuilder(generator.classNameFor(service.type, "ImplLegacyAdapter"))
          .superclass(implBaseClassName)
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter(
                name = "streamExecutor",
                type = ExecutorService::class
              )
              .apply { addRpcConstructorParameters(generator, this, service, options) }
              .build()
          )
          .addProperty(
            PropertySpec.builder("streamExecutor", ExecutorService::class)
              .addModifiers(KModifier.PRIVATE)
              .initializer("streamExecutor")
              .build()
          )
          .apply {
            addRpcProperties(generator, this, service, options)
            addRpcAdapterCodeBlocks(generator, this, service, options)
          }
          .build()
      )
  }

  private fun addRpcConstructorParameters(
    generator: ClassNameGenerator,
    builder: FunSpec.Builder,
    service: Service,
    options: Options
  ): FunSpec.Builder {
    if (options.singleMethodServices) {
      service.rpcs.forEach { rpc ->
        builder.addParameter(
          name = rpc.name,
          type = LambdaTypeName.get(
            returnType = ClassName(
              generator.classNameFor(service.type).packageName,
              "${service.name}${rpc.name}${BLOCKING_SERVER_SUFFIX}"
            )
          )
        )
      }
    } else {
      builder.addParameter(
        name = SERVICE_CONSTRUCTOR_ARGUMENT,
        type = LambdaTypeName.get(
          returnType = ClassName(
            generator.classNameFor(service.type).packageName,
            "${service.name}${BLOCKING_SERVER_SUFFIX}"
          )
        )
      )
    }
    return builder
  }

  private fun addRpcProperties(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: Options
  ): TypeSpec.Builder {
    if (options.singleMethodServices) {
      service.rpcs.forEach { rpc ->
        builder.addProperty(
          PropertySpec.builder(
            name = rpc.name,
            type = LambdaTypeName.get(
              returnType = ClassName(
                generator.classNameFor(service.type).packageName,
                "${service.name}${rpc.name}${BLOCKING_SERVER_SUFFIX}"
              )
            )
          )
            .initializer(rpc.name)
            .addModifiers(KModifier.PRIVATE)
            .build()
        )
      }
    } else {
      builder.addProperty(
        PropertySpec.builder(
          name = SERVICE_CONSTRUCTOR_ARGUMENT,
          type = LambdaTypeName.get(
            returnType = ClassName(
              generator.classNameFor(service.type).packageName,
              "${service.name}${BLOCKING_SERVER_SUFFIX}"
            )
          )
        )
          .initializer(SERVICE_CONSTRUCTOR_ARGUMENT)
          .addModifiers(KModifier.PRIVATE)
          .build()
      )
    }
    return builder
  }

  private fun addRpcAdapterCodeBlocks(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: Options
  ): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val serviceProviderName = if (options.singleMethodServices) { rpc.name } else { SERVICE_CONSTRUCTOR_ARGUMENT }

      val codeBlock = when {
        rpc.requestStreaming && rpc.responseStreaming -> CodeBlock.of(
          """
          |val requestStream = %T()
          |streamExecutor.submit {
          |  ${serviceProviderName}().${rpc.name}(requestStream, %T(response))
          |}
          |return requestStream
          |""".trimMargin(),
          ClassName("com.squareup.wire.kotlin.grpcserver", "MessageSourceAdapter")
            .parameterizedBy(generator.classNameFor(rpc.requestType!!)),
          ClassName("com.squareup.wire.kotlin.grpcserver", "MessageSinkAdapter")
        )
        rpc.requestStreaming -> CodeBlock.of(
          """
          |val requestStream = %T()
          |streamExecutor.submit {
          |  response.onNext(${serviceProviderName}().${rpc.name}(requestStream))
          |  response.onCompleted()
          |}
          |return requestStream
          |""".trimMargin(),
          ClassName("com.squareup.wire.kotlin.grpcserver", "MessageSourceAdapter")
            .parameterizedBy(generator.classNameFor(rpc.requestType!!))
        )
        rpc.responseStreaming -> CodeBlock.of(
          """
          |${serviceProviderName}().${rpc.name}(request, %T(response))
          |""".trimMargin(),
          ClassName("com.squareup.wire.kotlin.grpcserver", "MessageSinkAdapter")
        )
        else -> CodeBlock.of(
          """
          |response.onNext(${serviceProviderName}().${rpc.name}(request))
          |response.onCompleted()
          |""".trimMargin()
        )
      }
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .addModifiers(KModifier.OVERRIDE)
          .apply { addImplBaseRpcSignature(generator, this, rpc) }
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }

  private const val SERVICE_CONSTRUCTOR_ARGUMENT = "service"
  private const val BLOCKING_SERVER_SUFFIX = "BlockingServer"
}
