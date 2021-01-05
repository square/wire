package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.wire.schema.Service
import io.grpc.ServiceDescriptor

object ServiceDescriptor {
  internal fun addServiceDescriptor(builder: TypeSpec.Builder, service: Service) = builder
    .addProperty(
      PropertySpec.builder(
        name = "SERVICE_NAME",
        type = String::class,
        modifiers = emptyList()
      )
        .initializer("\"${service.type}\"")
        .build()
    )
    .addProperty(
      PropertySpec.builder(
        name = "serviceDescriptor",
        type = ServiceDescriptor::class.asTypeName().copy(nullable = true),
        modifiers = listOf(KModifier.PRIVATE),
      )
        .addAnnotation(Volatile::class)
        .initializer("null")
        .mutable(true)
        .build()
    )
    .addFunction(
      FunSpec.builder("getServiceDescriptor")
        .returns(ServiceDescriptor::class.asTypeName().copy(nullable = true))
        .addCode(serviceDescriptorCodeBlock(service))
        .build()
    )

  private fun serviceDescriptorCodeBlock(service: Service): CodeBlock {
    val grpcType = "${service.name}WireGrpc"
    val builder = CodeBlock.builder()
      .addStatement("var result = serviceDescriptor")
      .beginControlFlow("if (result == null)")
      .beginControlFlow("synchronized($grpcType::class)")
      .add(resultAssignerCodeBlock(service))
      .endControlFlow()
      .endControlFlow()
      .addStatement("return result")
    return builder.build()
  }

  private fun resultAssignerCodeBlock(service: Service): CodeBlock {
    val builder = CodeBlock.builder()
      .addStatement("result = serviceDescriptor")
      .beginControlFlow("if (result == null)")
      .addStatement("result = %M(SERVICE_NAME)",
        MemberName(
          enclosingClassName = ServiceDescriptor::class.asClassName(),
          simpleName = "newBuilder"
        ))
    service.rpcs.forEach { builder.addStatement(".addMethod(get${it.name}Method())") }
    builder
      .addStatement(".build()")
      .addStatement("serviceDescriptor = result")
      .endControlFlow()
    return builder.build()
  }
}