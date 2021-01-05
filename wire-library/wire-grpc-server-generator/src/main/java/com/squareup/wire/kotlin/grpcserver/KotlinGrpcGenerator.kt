package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.kotlin.grpcserver.BlockingStub.addBlockingStub
import com.squareup.wire.kotlin.grpcserver.ImplBase.addImplBase
import com.squareup.wire.kotlin.grpcserver.LegacyAdapter.addLegacyAdapter
import com.squareup.wire.kotlin.grpcserver.MethodDescriptor.addMethodDescriptor
import com.squareup.wire.kotlin.grpcserver.ServiceDescriptor.addServiceDescriptor
import com.squareup.wire.kotlin.grpcserver.Stub.addStub
import com.squareup.wire.schema.Service

class KotlinGrpcGenerator {
  fun generateGrpcServer(service: Service): Pair<ClassName, TypeSpec> {
    val typeName = "${service.name}WireGrpc"
    return ClassName(
      packageName = service.type.enclosingTypeOrPackage ?: "",
      simpleNames = listOf(typeName)
    ) to TypeSpec.objectBuilder(typeName)
      .apply {
        addServiceDescriptor(this, service)
        service.rpcs.forEach { addMethodDescriptor(this, service, it) }
        addImplBase(this, service)
        addLegacyAdapter(this, service)
        addStub(this, service)
        addBlockingStub(this, service)
      }
      .build()
  }
}
