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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.Service

object ServiceDescriptorGenerator {
  internal fun addServiceDescriptor(
    builder: TypeSpec.Builder,
    service: Service
  ) = builder
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
        type = ClassName("io.grpc", "ServiceDescriptor").copy(nullable = true),
        modifiers = listOf(KModifier.PRIVATE),
      )
        .addAnnotation(Volatile::class)
        .initializer("null")
        .mutable(true)
        .build()
    )
    .addFunction(
      FunSpec.builder("getServiceDescriptor")
        .returns(ClassName("io.grpc", "ServiceDescriptor").copy(nullable = true))
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
      .addStatement(
        "result = %M(SERVICE_NAME)",
        MemberName(
          enclosingClassName = ClassName("io.grpc", "ServiceDescriptor"),
          simpleName = "newBuilder"
        )
      )
    service.rpcs.forEach { builder.addStatement(".addMethod(get${it.name}Method())") }
    builder
      .addStatement(".build()")
      .addStatement("serviceDescriptor = result")
      .endControlFlow()
    return builder.build()
  }
}
