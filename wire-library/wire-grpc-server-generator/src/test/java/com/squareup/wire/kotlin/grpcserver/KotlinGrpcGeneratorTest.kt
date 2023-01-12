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
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.addLocal
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import javax.script.ScriptEngineManager

internal class KotlinGrpcGeneratorTest {
  @Test
  fun fullFile() {
    val path = "src/test/proto/RouteGuideProto.proto".toPath()
    val schema = buildSchema { addLocal(path) }
    val service = schema.getService("routeguide.RouteGuide")

    val (_, typeSpec) = KotlinGrpcGenerator(
      buildClassMap(schema, service!!),
      singleMethodServices = true,
      suspendingCalls = false
    ).generateGrpcServer(service, schema.protoFile(path), schema)
    val output = FileSpec.get("routeguide", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/RouteGuideWireGrpc.kt").source().buffer().readUtf8())
  }

  private val twoMethodSchema = """
      |syntax = "proto2";
      |
      |package foo;
      |option java_package = "com.foo.bar";
      |
      |message Request {}
      |message Response {}
      |
      |service FooService {
      |  rpc Call1(Request) returns (Response) {}
      |  rpc Call2(Request) returns (Response) {}
      |}
      |""".trimMargin()

  @Test
  fun `correctly generates singleMethodService = false adapters`() {
    val path = "service.proto".toPath()
    val schema = buildSchema { add(path, twoMethodSchema) }
    val service = schema.getService("foo.FooService")
    val (_, typeSpec) = KotlinGrpcGenerator(
      buildClassMap(schema, service!!),
      singleMethodServices = false,
      suspendingCalls = false
    ).generateGrpcServer(service, schema.protoFile(path), schema)
    val output = FileSpec.get("com.foo.bar", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/nonSingleMethodService.kt").source().buffer().readUtf8())
  }

  @Test
  fun `correctly generates singleMethodService = true adapters`() {
    val path = "service.proto".toPath()
    val schema = buildSchema { add(path, twoMethodSchema) }
    val service = schema.getService("foo.FooService")
    val (_, typeSpec) = KotlinGrpcGenerator(
      buildClassMap(schema, service!!),
      singleMethodServices = true,
      suspendingCalls = false
    ).generateGrpcServer(service, schema.protoFile(path), schema)
    val output = FileSpec.get("com.foo.bar", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/singleMethodService.kt").source().buffer().readUtf8())
  }
}
