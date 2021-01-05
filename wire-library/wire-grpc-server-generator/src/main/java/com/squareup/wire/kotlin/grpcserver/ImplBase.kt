package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import io.grpc.BindableService
import io.grpc.ServerServiceDefinition
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import java.io.InputStream

object ImplBase {
  internal fun addImplBase(builder: TypeSpec.Builder, service: Service) = builder
    .addType(
      TypeSpec.classBuilder("${service.name}ImplBase")
        .addModifiers(KModifier.ABSTRACT)
        .addSuperinterface(BindableService::class)
        .apply { addImplBaseBody(this, service) }
        .build()
    )

  internal fun addImplBaseRpcSignature(builder: FunSpec.Builder, rpc: Rpc): FunSpec.Builder {
    val requestType = ClassName.bestGuess(rpc.requestType.toString())
    val responseType = ClassName.bestGuess(rpc.responseType.toString())
    return when {
      rpc.requestStreaming -> builder
        .addParameter(
          "response", StreamObserver::class.asTypeName()
            .parameterizedBy(responseType)
        )
        .returns(
          StreamObserver::class.asTypeName()
            .parameterizedBy(requestType)
        )
      else -> builder
        .addParameter("request", requestType)
        .addParameter(
          "response", StreamObserver::class.asTypeName()
            .parameterizedBy(responseType)
        )
    }
  }

  private fun addImplBaseBody(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .addModifiers(KModifier.OPEN)
          .apply { addImplBaseRpcSignature(this, rpc) }
          .addCode(CodeBlock.of("TODO(\"not implemented\")"))
          .build()
      )
    }

    builder.addFunction(
      FunSpec.builder("bindService")
        .addModifiers(KModifier.OVERRIDE)
        .returns(ServerServiceDefinition::class)
        .addCode(bindServiceCodeBlock(service))
        .build()
    )

    service.rpcs
      .flatMap { listOf(it.requestType, it.responseType) }
      .distinct()
      .forEach {
        val className = ClassName.bestGuess(it.toString())
        builder.addType(
          TypeSpec.classBuilder("${it!!.simpleName}Marshaller")
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
        MemberName(ServerCalls::class.asTypeName(), bindServiceCallType(it))
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
