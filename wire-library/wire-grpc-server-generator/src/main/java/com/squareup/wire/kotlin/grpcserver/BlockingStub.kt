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
import io.grpc.Channel
import io.grpc.stub.ClientCalls

object BlockingStub {
  internal fun addBlockingStub(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder =
    builder
      .addFunction(
        FunSpec.builder("newBlockingStub")
          .addParameter("channel", Channel::class)
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
          """
          return %M(channel, get${rpc.name}Method(), callOptions, request)
        """.trimIndent(),
          MemberName(
            enclosingClassName = ClientCalls::class.asClassName(),
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