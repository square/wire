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
import com.squareup.wire.schema.Service
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.channels.Channel

object BindableAdapterGenerator {

  internal fun addBindableAdapter(
    generator: ClassNameGenerator,
    builder: TypeSpec.Builder,
    service: Service,
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    val serviceClassName = generator.classNameFor(service.type)
    val implBaseClassName = ClassName(
      serviceClassName.packageName,
      "${service.name}WireGrpc",
      "${service.name}ImplBase"
    )
    return builder
      .addType(
        TypeSpec.classBuilder("BindableAdapter")
          .superclass(implBaseClassName)
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .apply { if (!options.suspendingCalls) {
                // non suspending calls need an executor for the streaming calls
                // suspending calls use CoroutineContext instead
                this.addParameter(
                  name = "streamExecutor",
                  type = ExecutorService::class
                )
              } }

              .apply { addRpcConstructorParameters(generator, this, service, options) }
              .build()
          )
          .apply { if (!options.suspendingCalls) {
            this.addProperty(
              PropertySpec.builder("streamExecutor", ExecutorService::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("streamExecutor")
                .build()
            )
          } }
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
    options: KotlinGrpcGenerator.Companion.Options
  ): FunSpec.Builder {
    val serviceSuffix = if (options.suspendingCalls) {
      SUSPENDING_SERVER_SUFFIX
    } else {
      BLOCKING_SERVER_SUFFIX
    }
    if (options.singleMethodServices) {
      service.rpcs.forEach { rpc ->
        builder.addParameter(
          name = rpc.name,
          type = LambdaTypeName.get(
            returnType = ClassName(
              generator.classNameFor(service.type).packageName,
              "${service.name}${rpc.name}${serviceSuffix}"
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
            "${service.name}${serviceSuffix}"
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
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    val serviceSuffix = if (options.suspendingCalls) {
      SUSPENDING_SERVER_SUFFIX
    } else {
      BLOCKING_SERVER_SUFFIX
    }
    if (options.singleMethodServices) {
      service.rpcs.forEach { rpc ->
        builder.addProperty(
          PropertySpec.builder(
            name = rpc.name,
            type = LambdaTypeName.get(
              returnType = ClassName(
                generator.classNameFor(service.type).packageName,
                "${service.name}${rpc.name}${serviceSuffix}"
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
              "${service.name}${serviceSuffix}"
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
    options: KotlinGrpcGenerator.Companion.Options
  ): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val serviceProviderName = if (options.singleMethodServices) {
        rpc.name
      } else {
        SERVICE_CONSTRUCTOR_ARGUMENT
      }

      val codeBlock = if (options.suspendingCalls) {
        when {
          !rpc.requestStreaming && !rpc.responseStreaming -> CodeBlock.of(
            "return ${serviceProviderName}().${rpc.name}(request)"
          )
          !rpc.requestStreaming -> CodeBlock.of(
            "return %T.serverStream(context, request, %L()::%L)", FlowAdapter::class, serviceProviderName, rpc.name
          )
          !rpc.responseStreaming -> CodeBlock.of(
            "return %T.clientStream(context, request, %L()::%L)", FlowAdapter::class, serviceProviderName, rpc.name
          )
          else -> CodeBlock.of(
            "return %T.bidiStream(context, request, %L()::%L)", FlowAdapter::class, serviceProviderName, rpc.name
          )
        }
      } else {
        val responseAdapter = ClassName("com.squareup.wire.kotlin.grpcserver", "MessageSinkAdapter")
        when {
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
            responseAdapter
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
            responseAdapter
          )

          else -> CodeBlock.of(
            """
          |response.onNext(${serviceProviderName}().${rpc.name}(request))
          |response.onCompleted()
          |""".trimMargin()
          )
        }
      }
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .addModifiers(KModifier.OVERRIDE)
          .apply { addImplBaseRpcSignature(generator, this, rpc, options) }
          .apply { if (options.suspendingCalls && !rpc.responseStreaming) { addModifiers(KModifier.SUSPEND) } }
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }

  private const val SERVICE_CONSTRUCTOR_ARGUMENT = "service"
  private const val BLOCKING_SERVER_SUFFIX = "BlockingServer"
  private const val SUSPENDING_SERVER_SUFFIX = "Server"
}
