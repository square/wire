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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.buildSchema
import com.squareup.wire.kotlin.grpcserver.GoldenTestUtils.assertFileEquals
import com.squareup.wire.schema.addLocal
import kotlin.test.assertEquals
import okio.Path.Companion.toPath
import org.junit.Test

class StubTest {
  @Test
  fun addStub() {
    val schema = buildSchema { addLocal("src/test/proto/RouteGuideProto.proto".toPath()) }
    val service = schema.getService("routeguide.RouteGuide")

    val code = FileSpec.builder("routeguide", "RouteGuide")
      .addType(
        TypeSpec.classBuilder("RouteGuideWireGrpc")
          .apply {
            StubGenerator.addStub(
              generator = ClassNameGenerator(buildClassMap(schema, service!!)),
              builder = this,
              service,
              options = KotlinGrpcGenerator.Companion.Options(
                singleMethodServices = false,
                suspendingCalls = false
              )
            )
          }
          .build()
      )
      .build()

    assertFileEquals("Stub.kt", code)
  }

  @Test
  fun `generates stubs for suspended bidi streaming rpc`() {
    val code = stubCodeFor("test", "TestService", """
      syntax = "proto2";
      package test;

      message Test {}
      service TestService {
        rpc TestRPC(stream Test) returns (stream Test){}
      }
      """.trimMargin())
    assertEquals("""
      package test

      import io.grpc.CallOptions
      import io.grpc.Channel
      import io.grpc.kotlin.AbstractCoroutineStub
      import io.grpc.kotlin.ClientCalls.bidiStreamingRpc
      import kotlinx.coroutines.flow.Flow

      public class TestServiceWireGrpc {
        public fun newStub(channel: Channel): TestServiceStub = TestServiceStub(channel)

        public class TestServiceStub : AbstractCoroutineStub<TestServiceStub> {
          internal constructor(channel: Channel) : super(channel)

          internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

          override fun build(channel: Channel, callOptions: CallOptions): TestServiceStub =
              TestServiceStub(channel, callOptions)

          public suspend fun TestRPC(request: Flow<Test>): Flow<Test> = bidiStreamingRpc(channel,
              getTestRPCMethod(), request, callOptions)
        }
      }
    """.trimIndent().trim(), code)
  }

  @Test
  fun `generates stubs for suspended server streaming rpc`() {
    val code = stubCodeFor("test", "TestService", """
      syntax = "proto2";
      package test;

      message Test {}
      service TestService {
        rpc TestRPC(Test) returns (stream Test){}
      }
      """.trimMargin())
    assertEquals("""
      package test

      import io.grpc.CallOptions
      import io.grpc.Channel
      import io.grpc.kotlin.AbstractCoroutineStub
      import io.grpc.kotlin.ClientCalls.serverStreamingRpc
      import kotlinx.coroutines.flow.Flow

      public class TestServiceWireGrpc {
        public fun newStub(channel: Channel): TestServiceStub = TestServiceStub(channel)

        public class TestServiceStub : AbstractCoroutineStub<TestServiceStub> {
          internal constructor(channel: Channel) : super(channel)

          internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

          override fun build(channel: Channel, callOptions: CallOptions): TestServiceStub =
              TestServiceStub(channel, callOptions)

          public suspend fun TestRPC(request: Test): Flow<Test> = serverStreamingRpc(channel,
              getTestRPCMethod(), request, callOptions)
        }
      }
    """.trimIndent().trim(), code)
  }

  @Test
  fun `generates stubs for suspended client streaming rpc`() {
    val code = stubCodeFor("test", "TestService", """
      syntax = "proto2";
      package test;

      message Test {}
      service TestService {
        rpc TestRPC(stream Test) returns (Test){}
      }
      """.trimMargin())
    assertEquals("""
      package test

      import io.grpc.CallOptions
      import io.grpc.Channel
      import io.grpc.kotlin.AbstractCoroutineStub
      import io.grpc.kotlin.ClientCalls.clientStreamingRpc
      import kotlinx.coroutines.flow.Flow

      public class TestServiceWireGrpc {
        public fun newStub(channel: Channel): TestServiceStub = TestServiceStub(channel)

        public class TestServiceStub : AbstractCoroutineStub<TestServiceStub> {
          internal constructor(channel: Channel) : super(channel)

          internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

          override fun build(channel: Channel, callOptions: CallOptions): TestServiceStub =
              TestServiceStub(channel, callOptions)

          public suspend fun TestRPC(request: Flow<Test>): Test = clientStreamingRpc(channel,
              getTestRPCMethod(), request, callOptions)
        }
      }
    """.trimIndent().trim(), code)
  }

  private fun stubCodeFor(pkg: String, serviceName: String, schemaCode: String,
                              options: KotlinGrpcGenerator.Companion.Options = KotlinGrpcGenerator.Companion.Options(
                                singleMethodServices = false,
                                suspendingCalls = true
                              )): String {
    val schema = buildSchema { add("test.proto".toPath(), schemaCode) }
    val service = schema.getService("$pkg.$serviceName")!!
    val typeSpec = TypeSpec.classBuilder("${serviceName}WireGrpc")
    val nameGenerator = ClassNameGenerator(buildClassMap(schema, service))

    StubGenerator.addStub(nameGenerator, typeSpec, service, options)

    return FileSpec.builder(pkg, "test.kt")
      .addType(typeSpec.build())
      .build()
      .toString()
      .trim()
  }
}
