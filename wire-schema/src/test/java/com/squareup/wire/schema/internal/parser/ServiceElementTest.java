/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.google.common.collect.ImmutableList
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ServiceElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val service = ServiceElement.builder(location)
        .name("Service")
        .build()
    val expected = "service Service {}\n"
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun singleToSchema() {
    val service = ServiceElement.builder(location)
        .name("Service")
        .rpcs(
            ImmutableList.of(
                RpcElement.builder(location)
                    .name("Name")
                    .requestType("RequestType")
                    .responseType("ResponseType")
                    .build()
            )
        )
        .build()
    val expected = """
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |""".trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleRpcs() {
    val firstName = RpcElement.builder(location)
        .name("FirstName")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build()
    val lastName = RpcElement.builder(location)
        .name("LastName")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build()
    val service = ServiceElement.builder(location)
        .name("Service")
        .rpcs(ImmutableList.of(firstName, lastName))
        .build()
    assertThat(service.rpcs()).hasSize(2)
  }

  @Test
  fun singleWithOptionsToSchema() {
    val service = ServiceElement.builder(location)
        .name("Service")
        .options(
            ImmutableList.of(
                OptionElement.create("foo", Kind.STRING, "bar")
            )
        )
        .rpcs(
            ImmutableList.of(
                RpcElement.builder(location)
                    .name("Name")
                    .requestType("RequestType")
                    .responseType("ResponseType")
                    .build()
            )
        )
        .build()
    val expected = """
        |service Service {
        |  option foo = "bar";
        |
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |""".trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val service = ServiceElement.builder(location)
        .name("Service")
        .options(ImmutableList.of(kitKat, fooBar))
        .rpcs(
            ImmutableList.of(
                RpcElement.builder(location)
                    .name("Name")
                    .requestType("RequestType")
                    .responseType("ResponseType")
                    .build()
            )
        )
        .build()
    assertThat(service.options()).hasSize(2)
  }

  @Test
  fun singleWithDocumentationToSchema() {
    val service = ServiceElement.builder(location)
        .name("Service")
        .documentation("Hello")
        .rpcs(
            ImmutableList.of(
                RpcElement.builder(location)
                    .name("Name")
                    .requestType("RequestType")
                    .responseType("ResponseType")
                    .build()
            )
        )
        .build()
    val expected = """
        |// Hello
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |""".trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun multipleToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build()
    val service = ServiceElement.builder(location)
        .name("Service")
        .rpcs(ImmutableList.of(rpc, rpc))
        .build()
    val expected = """
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |""".trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build()
    val expected = "rpc Name (RequestType) returns (ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithDocumentationToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .documentation("Hello")
        .requestType("RequestType")
        .responseType("ResponseType")
        .build()
    val expected = """
        |// Hello
        |rpc Name (RequestType) returns (ResponseType);
        |""".trimMargin()
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithOptionsToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .options(
            ImmutableList.of(
                OptionElement.create("foo", Kind.STRING, "bar")
            )
        )
        .build()
    val expected = """
        |rpc Name (RequestType) returns (ResponseType) {
        |  option foo = "bar";
        |};
        |""".trimMargin()
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithRequestStreamingToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .requestStreaming(true)
        .build()
    val expected = "rpc Name (stream RequestType) returns (ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithResponseStreamingToSchema() {
    val rpc = RpcElement.builder(location)
        .name("Name")
        .requestType("RequestType")
        .responseType("ResponseType")
        .responseStreaming(true)
        .build()
    val expected = "rpc Name (RequestType) returns (stream ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }
}
