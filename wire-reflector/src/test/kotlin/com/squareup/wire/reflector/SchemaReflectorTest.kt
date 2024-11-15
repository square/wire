/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire.reflector

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.google.protobuf.DescriptorProtos
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.addLocal
import grpc.reflection.v1alpha.ListServiceResponse
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import grpc.reflection.v1alpha.ServiceResponse
import okio.Path.Companion.toPath
import org.junit.Test

internal class SchemaReflectorTest {
  @Test
  fun `outputs a list of services`() {
    val schema = buildSchema {
      addLocal("src/test/proto/RouteGuideProto.proto".toPath())
    }
    val request = ServerReflectionRequest(list_services = "*")
    assertThat(
      SchemaReflector(schema, includeDependencies = true).process(request),
    ).isEqualTo(
      ServerReflectionResponse(
        original_request = request,
        list_services_response = ListServiceResponse(
          service = listOf(ServiceResponse(name = "routeguide.RouteGuide")),
        ),
      ),
    )
  }

  @Test
  fun `gets a file descriptor for a filename`() {
    val schema = buildSchema {
      addLocal("src/test/proto/RouteGuideProto.proto".toPath())
    }

    assertThat(
      SchemaReflector(schema, includeDependencies = true).process(
        ServerReflectionRequest(
          file_by_filename = "src/test/proto/RouteGuideProto.proto",
        ),
      ),
    ).prop(ServerReflectionResponse::file_descriptor_response).isNotNull()
  }

  @Test
  fun `gets a file descriptor for a specific symbol`() {
    val schema = buildSchema {
      addLocal("src/test/proto/RouteGuideProto.proto".toPath())
    }

    assertThat(
      SchemaReflector(schema, includeDependencies = true).process(
        ServerReflectionRequest(
          file_containing_symbol = "routeguide.RouteGuide",
        ),
      ),
    ).prop(ServerReflectionResponse::file_descriptor_response).isNotNull()
  }

  @Test
  fun `transitive dependencies included`() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |import "b.proto";
        |message A {
        |  optional B b = 1;
        |}
        |
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |message B { }
        |
        """.trimMargin(),
      )
    }

    val response = SchemaReflector(schema, includeDependencies = true)
      .process(ServerReflectionRequest(file_containing_symbol = ".A"))
    assertThat(
      response.file_descriptor_response!!.file_descriptor_proto.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
      }.map { it.name },
    ).containsExactly("a.proto", "b.proto")

    val responseB = SchemaReflector(schema, includeDependencies = true)
      .process(ServerReflectionRequest(file_containing_symbol = ".B"))
    assertThat(
      responseB.file_descriptor_response!!.file_descriptor_proto.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
      }.map { it.name },
    ).containsExactly("b.proto")
  }

  @Test
  fun `transitive dependencies included once`() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |import "b.proto";
        |import "c.proto";
        |message A {
        |  optional B b = 1;
        |  optional C c = 2;
        |}
        |
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |message B { }
        |
        """.trimMargin(),
      )
      add(
        "c.proto".toPath(),
        """
        |message C { }
        |
        """.trimMargin(),
      )
    }

    val response = SchemaReflector(schema, includeDependencies = true)
      .process(ServerReflectionRequest(file_containing_symbol = ".A"))
    assertThat(
      response.file_descriptor_response!!.file_descriptor_proto.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
      }.map { it.name },
    ).containsExactly("a.proto", "b.proto", "c.proto")
  }

  @Test
  fun `transitive dependencies included ordered`() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |import "b.proto";
        |import "c.proto";
        |message A {
        |  optional B b = 1;
        |  optional C c = 2;
        |}
        |
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |import "c.proto";
        |message B {
        |  optional C c = 1;
        | }
        |
        """.trimMargin(),
      )
      add(
        "c.proto".toPath(),
        """
        |message C { }
        |
        """.trimMargin(),
      )
    }

    val response = SchemaReflector(schema, includeDependencies = true)
      .process(ServerReflectionRequest(file_containing_symbol = ".A"))
    assertThat(
      response.file_descriptor_response!!.file_descriptor_proto.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
      }.map { it.name },
    ).containsExactly("a.proto", "b.proto", "c.proto")
  }

  @Test
  fun `transitive dependencies with public import`() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |import "b.proto";
        |message A {
        |  optional B b = 1;
        |}
        |
        """.trimMargin(),
      )
      add(
        "b.proto".toPath(),
        """
        |import public "b-new.proto";
        |
        """.trimMargin(),
      )
      add(
        "b-new.proto".toPath(),
        """
        |import "c.proto";
        |message B {
        |  optional C c = 1;
        | }
        |
        """.trimMargin(),
      )
      add(
        "c.proto".toPath(),
        """
        |message C { }
        |
        """.trimMargin(),
      )
    }

    val response = SchemaReflector(schema, includeDependencies = true)
      .process(ServerReflectionRequest(file_containing_symbol = ".A"))
    assertThat(
      response.file_descriptor_response!!.file_descriptor_proto.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
      }.map { it.name },
    ).containsExactly("a.proto", "b.proto", "b-new.proto", "c.proto")
  }
}
