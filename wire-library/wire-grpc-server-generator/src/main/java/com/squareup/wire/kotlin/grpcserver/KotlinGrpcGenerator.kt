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
