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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.kotlin.grpcserver.BlockingStubGenerator.addBlockingStub
import com.squareup.wire.kotlin.grpcserver.ImplBaseGenerator.addImplBase
import com.squareup.wire.kotlin.grpcserver.LegacyAdapterGenerator.addLegacyAdapter
import com.squareup.wire.kotlin.grpcserver.MethodDescriptorGenerator.addMethodDescriptor
import com.squareup.wire.kotlin.grpcserver.ServiceDescriptorGenerator.addServiceDescriptor
import com.squareup.wire.kotlin.grpcserver.StubGenerator.addStub
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Service

class KotlinGrpcGenerator(private val typeToKotlinName: Map<ProtoType, TypeName>) {
  fun generateGrpcServer(service: Service): Pair<ClassName, TypeSpec> {
    val classNameGenerator = ClassNameGenerator(typeToKotlinName)
    val grpcClassName = classNameGenerator.classNameFor(service.type, "WireGrpc")
    val builder = TypeSpec.objectBuilder(grpcClassName)

    addServiceDescriptor(builder, service)
    service.rpcs.forEach { rpc -> addMethodDescriptor(classNameGenerator, builder, service, rpc) }
    addImplBase(classNameGenerator, builder, service)
    addLegacyAdapter(classNameGenerator, builder, service)
    addStub(classNameGenerator, builder, service)
    addBlockingStub(classNameGenerator, builder, service)

    return grpcClassName to builder.build()
  }
}
