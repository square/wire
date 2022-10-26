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
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.addLocal
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

internal class KotlinGrpcGeneratorTest {
  @Test
  fun fullFile() {
    val schema = buildSchema { addLocal("src/test/proto/RouteGuideProto.proto".toPath()) }
    val service = schema.getService("routeguide.RouteGuide")

    val (_, typeSpec) = KotlinGrpcGenerator(buildClassMap(schema, service!!))
      .generateGrpcServer(service)
    val output = FileSpec.get("routeguide", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/RouteGuideWireGrpc.kt").source().buffer().readUtf8())
  }

  @Test
  fun `java_package option is respected`() {
    val schema = buildSchema { add(
      "service.proto".toPath(),
      """
      |syntax = "proto2";
      |
      |package foo;
      |option java_package = "com.foo.bar";
      |
      |message Request {}
      |message Response {}
      |
      |service FooService {
      |  rpc Call(Request) returns (Response) {}
      |}
      |""".trimMargin()
    ) }
    val service = schema.getService("foo.FooService")
    val (_, typeSpec) = KotlinGrpcGenerator(buildClassMap(schema, service!!)).generateGrpcServer(service)
    val output = FileSpec.get("com.foo.bar", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/JavaPackage.java").source().buffer().readUtf8())
  }
}
