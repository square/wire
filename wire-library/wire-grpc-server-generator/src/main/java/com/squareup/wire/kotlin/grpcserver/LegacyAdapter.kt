package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.wire.kotlin.grpcserver.ImplBase.addImplBaseRpcSignature
import com.squareup.wire.schema.Service
import java.util.concurrent.ExecutorService

object LegacyAdapter {
  internal fun addLegacyAdapter(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder =
    builder
      .addType(
        TypeSpec.classBuilder("${service.name}ImplLegacyAdapter")
          .superclass(
            ClassName(
              "",
              "${service.name}ImplBase"
            )
          )
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter(
                name = "streamExecutor",
                type = ExecutorService::class
              )
              .apply { addRpcConstructorParameters(this, service) }
              .build()
          )
          .addProperty(
            PropertySpec.builder("streamExecutor", ExecutorService::class)
              .addModifiers(KModifier.PRIVATE)
              .initializer("streamExecutor")
              .build()
          )
          .apply {
            addRpcProperties(this, service)
            addRpcAdapterCodeBlocks(this, service)
          }
          .build()
      )

  private fun addRpcConstructorParameters(
    builder: FunSpec.Builder,
    service: Service
  ): FunSpec.Builder {
    service.rpcs.forEach { rpc ->
      builder.addParameter(
        name = rpc.name,
        type = LambdaTypeName.get(
          returnType = ClassName(
            service.type.enclosingTypeOrPackage!!,
            "${service.name}${rpc.name}BlockingServer"
          )
        )
      )
    }
    return builder
  }

  private fun addRpcProperties(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      builder.addProperty(
        PropertySpec.builder(
          name = rpc.name,
          type = LambdaTypeName.get(
            returnType = ClassName(
              service.type.enclosingTypeOrPackage!!,
              "${service.name}${rpc.name}BlockingServer"
            )
          )
        )
          .initializer(rpc.name)
          .addModifiers(KModifier.PRIVATE)
          .build()
      )
    }
    return builder
  }

  private fun addRpcAdapterCodeBlocks(
    builder: TypeSpec.Builder,
    service: Service
  ): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val codeBlock = when {
        rpc.requestStreaming && rpc.responseStreaming -> CodeBlock.of(
          """
          val requestStream = %T()
          streamExecutor.submit {
            ${rpc.name}().${rpc.name}(requestStream, %T(response))
          }
          return requestStream
        """.trimIndent(),
          MessageSourceAdapter::class.asTypeName()
            .parameterizedBy(ClassName.bestGuess(rpc.requestType.toString())),
          MessageSinkAdapter::class.asTypeName()
        )
        rpc.requestStreaming -> CodeBlock.of(
          """
          val requestStream = %T()
          streamExecutor.submit {
            response.onNext(${rpc.name}().${rpc.name}(requestStream))
            response.onCompleted()
          }
          return requestStream
          """.trimIndent(),
          MessageSourceAdapter::class.asTypeName()
            .parameterizedBy(ClassName.bestGuess(rpc.requestType.toString()))
        )
        rpc.responseStreaming -> CodeBlock.of(
          """
          ${rpc.name}().${rpc.name}(request, %T(response))
          """.trimIndent(),
          MessageSinkAdapter::class.asTypeName()
        )
        else -> CodeBlock.of(
          """
          response.onNext(${rpc.name}().${rpc.name}(request))
          response.onCompleted()
          """.trimIndent()
        )
      }
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .addModifiers(KModifier.OVERRIDE)
          .apply { addImplBaseRpcSignature(this, rpc) }
          .addCode(codeBlock)
          .build()
      )
    }
    return builder
  }
}
