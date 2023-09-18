/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal.parser

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement.Kind
import kotlin.test.Test

class ServiceElementTest {
  internal var location = Location.get("file.proto")

  @Test
  fun emptyToSchema() {
    val service = ServiceElement(
      location = location,
      name = "Service",
    )
    val expected = "service Service {}\n"
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun singleToSchema() {
    val service = ServiceElement(
      location = location,
      name = "Service",
      rpcs = listOf(
        RpcElement(
          location = location,
          name = "Name",
          requestType = "RequestType",
          responseType = "ResponseType",
        ),
      ),
    )
    val expected = """
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |
    """.trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleRpcs() {
    val firstName = RpcElement(
      location = location,
      name = "FirstName",
      requestType = "RequestType",
      responseType = "ResponseType",
    )
    val lastName = RpcElement(
      location = location,
      name = "LastName",
      requestType = "RequestType",
      responseType = "ResponseType",
    )
    val service = ServiceElement(
      location = location,
      name = "Service",
      rpcs = listOf(firstName, lastName),
    )
    assertThat(service.rpcs).hasSize(2)
  }

  @Test
  fun singleWithOptionsToSchema() {
    val service = ServiceElement(
      location = location,
      name = "Service",
      options = listOf(OptionElement.create("foo", Kind.STRING, "bar")),
      rpcs = listOf(
        RpcElement(
          location = location,
          name = "Name",
          requestType = "RequestType",
          responseType = "ResponseType",
        ),
      ),
    )
    val expected = """
        |service Service {
        |  option foo = "bar";
        |
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |
    """.trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun addMultipleOptions() {
    val kitKat = OptionElement.create("kit", Kind.STRING, "kat")
    val fooBar = OptionElement.create("foo", Kind.STRING, "bar")
    val service = ServiceElement(
      location = location,
      name = "Service",
      options = listOf(kitKat, fooBar),
      rpcs = listOf(
        RpcElement(
          location = location,
          name = "Name",
          requestType = "RequestType",
          responseType = "ResponseType",
        ),
      ),
    )
    assertThat(service.options).hasSize(2)
  }

  @Test
  fun singleWithDocumentationToSchema() {
    val service = ServiceElement(
      location = location,
      name = "Service",
      documentation = "Hello",
      rpcs = listOf(
        RpcElement(
          location = location,
          name = "Name",
          requestType = "RequestType",
          responseType = "ResponseType",
        ),
      ),
    )
    val expected = """
        |// Hello
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |
    """.trimMargin()
    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun multipleToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      requestType = "RequestType",
      responseType = "ResponseType",
    )
    val service = ServiceElement(
      location = location,
      name = "Service",
      rpcs = listOf(rpc, rpc),
    )
    val expected = """
        |service Service {
        |  rpc Name (RequestType) returns (ResponseType);
        |  rpc Name (RequestType) returns (ResponseType);
        |}
        |
    """.trimMargin()

    assertThat(service.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      requestType = "RequestType",
      responseType = "ResponseType",
    )
    val expected = "rpc Name (RequestType) returns (ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithDocumentationToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      documentation = "Hello",
      requestType = "RequestType",
      responseType = "ResponseType",
    )
    val expected = """
        |// Hello
        |rpc Name (RequestType) returns (ResponseType);
        |
    """.trimMargin()
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithOptionsToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      requestType = "RequestType",
      responseType = "ResponseType",
      options = listOf(OptionElement.create("foo", Kind.STRING, "bar")),
    )

    val expected = """
        |rpc Name (RequestType) returns (ResponseType) {
        |  option foo = "bar";
        |};
        |
    """.trimMargin()
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithRequestStreamingToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      requestType = "RequestType",
      responseType = "ResponseType",
      requestStreaming = true,
    )
    val expected = "rpc Name (stream RequestType) returns (ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }

  @Test
  fun rpcWithResponseStreamingToSchema() {
    val rpc = RpcElement(
      location = location,
      name = "Name",
      requestType = "RequestType",
      responseType = "ResponseType",
      responseStreaming = true,
    )
    val expected = "rpc Name (RequestType) returns (stream ResponseType);\n"
    assertThat(rpc.toSchema()).isEqualTo(expected)
  }
}
