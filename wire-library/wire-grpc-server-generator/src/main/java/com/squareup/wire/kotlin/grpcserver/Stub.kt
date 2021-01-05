package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.wire.kotlin.grpcserver.ImplBase.addImplBaseRpcSignature
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls

object Stub {
  internal fun addStub(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder = builder
    .addFunction(
      FunSpec.builder("newStub")
        .addParameter("channel", Channel::class)
        .returns(ClassName("", "${service.name}Stub"))
        .addCode("return %T(channel)", ClassName("", "${service.name}Stub"))
        .build()
    )
    .addType(
      TypeSpec.classBuilder("${service.name}Stub")
        .apply {
          addAbstractStubConstructor(this, service)
          addStubRpcCalls(this, service)
        }
        .build()
    )

  internal fun addAbstractStubConstructor(builder: TypeSpec.Builder, service: Service) = builder
    // Really this is a superclass, just want to add secondary constructors.
    .addSuperinterface(
      AbstractStub::class.asTypeName()
        // TODO refer to its own class
        .parameterizedBy(ClassName("", "${service.name}Stub"))
    )
    .addFunction(
      FunSpec.constructorBuilder()
        .addModifiers(KModifier.INTERNAL)
        .addParameter("channel", Channel::class)
        .callSuperConstructor("channel")
        .build()
    )
    .addFunction(
      FunSpec.constructorBuilder()
        .addModifiers(KModifier.INTERNAL)
        .addParameter("channel", Channel::class)
        .addParameter("callOptions", CallOptions::class)
        .callSuperConstructor("channel", "callOptions")
        .build()
    )
    .addFunction(
      FunSpec.builder("build")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("channel", Channel::class)
        .addParameter("callOptions", CallOptions::class)
        .addStatement("return ${service.name}Stub(channel, callOptions)")
        .build()
    )

  private fun addStubRpcCalls(builder: TypeSpec.Builder, service: Service): TypeSpec.Builder {
    service.rpcs.forEach { rpc ->
      val codeBlock = when {
        rpc.requestStreaming ->
          CodeBlock.of(
            "return %M(channel.newCall(get${rpc.name}Method(), callOptions), response)",
            MemberName(ClientCalls::class.asClassName(), clientCallType(rpc))
          )
        else ->
          CodeBlock.of(
            "%M(channel.newCall(get${rpc.name}Method(), callOptions), request, response)",
            MemberName(ClientCalls::class.asClassName(), clientCallType(rpc))
          )
      }
      builder.addFunction(
        FunSpec.builder(rpc.name)
          .apply { addImplBaseRpcSignature(this, rpc) }
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
