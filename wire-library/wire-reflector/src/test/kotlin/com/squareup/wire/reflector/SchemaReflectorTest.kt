/*
 * Copyright 2021 Square Inc.
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
package com.squareup.wire.reflector

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.RepoBuilder
import com.squareup.wire.schema.SchemaLoader
import grpc.reflection.v1alpha.ListServiceResponse
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import grpc.reflection.v1alpha.ServiceResponse
import okio.FileSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class SchemaReflectorTest {
    @Test
    fun `outputs a list of services`() {
        val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
        val schema = repoBuilder.schema()
        val request = ServerReflectionRequest(list_services = "*")
        assertThat(
                SchemaReflector(schema, includeDependencies = true).process(request)
        ).isEqualTo(
                ServerReflectionResponse(
                        original_request = request,
                        list_services_response = ListServiceResponse(
                                service = listOf(ServiceResponse(name = "routeguide.RouteGuide"))
                        )
                )
        )
    }

    @Test
    fun `gets a file descriptor for a filename`() {
        val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
        val schema = repoBuilder.schema()

        assertThat(
                SchemaReflector(schema, includeDependencies = true).process(
                        ServerReflectionRequest(
                                file_by_filename = "src/test/proto/RouteGuideProto.proto"
                        )
                )
        ).extracting { it.file_descriptor_response }.isNotNull
    }

    @Test
    fun `gets a file descriptor for a specific symbol`() {
        val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
        val schema = repoBuilder.schema()

        assertThat(
                SchemaReflector(schema, includeDependencies = true).process(
                        ServerReflectionRequest(
                                file_containing_symbol = "routeguide.RouteGuide"
                        )
                )
        ).extracting { it.file_descriptor_response }.isNotNull
    }

    @Test
    fun `transitive dependencies included`() {
        val repoBuilder = RepoBuilder()
                .add("/source/a.proto", """
                |import "b.proto";
                |message A {
                |  optional B b = 1;
                |}
                |""".trimMargin())
                .add("/source/b.proto", """
                |message B { }
                |""".trimMargin())
        val schema = repoBuilder.schema()

        val response = SchemaReflector(schema, includeDependencies = true)
                .process(ServerReflectionRequest(file_containing_symbol = ".A"))
        assertThat(response.file_descriptor_response!!.file_descriptor_proto.map {
            DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
        }.map { it.name }).containsExactly("a.proto", "b.proto")

        val responseB = SchemaReflector(schema, includeDependencies = true)
                .process(ServerReflectionRequest(file_containing_symbol = ".B"))
        assertThat(responseB.file_descriptor_response!!.file_descriptor_proto.map {
            DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
        }.map { it.name }).containsExactly("b.proto")
    }

    @Test
    fun `transitive dependencies included once`() {
        val repoBuilder = RepoBuilder()
                .add("/source/a.proto", """
                |import "b.proto";
                |import "c.proto";
                |message A {
                |  optional B b = 1;
                |  optional C c = 2;
                |}
                |""".trimMargin())
                .add("/source/b.proto", """
                |message B { }
                |""".trimMargin())
                .add("/source/c.proto", """
                |message C { }
                |""".trimMargin())
        val schema = repoBuilder.schema()

        val response = SchemaReflector(schema, includeDependencies = true)
                .process(ServerReflectionRequest(file_containing_symbol = ".A"))
        assertThat(response.file_descriptor_response!!.file_descriptor_proto.map {
            DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
        }.map { it.name }).containsExactly("a.proto", "b.proto", "c.proto")
    }

    @Test
    fun `transitive dependencies included ordered`() {
        val repoBuilder = RepoBuilder()
                .add("/source/a.proto", """
                |import "b.proto";
                |import "c.proto";
                |message A {
                |  optional B b = 1;
                |  optional C c = 2;
                |}
                |""".trimMargin())
                .add("/source/b.proto", """
                |import "c.proto";
                |message B {
                |  optional C c = 1;
                | }
                |""".trimMargin())
                .add("/source/c.proto", """
                |message C { }
                |""".trimMargin())
        val schema = repoBuilder.schema()

        val response = SchemaReflector(schema, includeDependencies = true)
                .process(ServerReflectionRequest(file_containing_symbol = ".A"))
        assertThat(response.file_descriptor_response!!.file_descriptor_proto.map {
            DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
        }.map { it.name }).containsExactly("a.proto", "b.proto", "c.proto")
    }

    @Test
    fun `transitive dependencies with public import`() {
        val repoBuilder = RepoBuilder()
                .add("/source/a.proto", """
                |import "b.proto";
                |message A {
                |  optional B b = 1;
                |}
                |""".trimMargin())
                .add("/source/b.proto", """
                |import public "b-new.proto";
                |""".trimMargin())
                .add("/source/b-new.proto", """
                |import "c.proto";
                |message B {
                |  optional C c = 1;
                | }
                |""".trimMargin())
                .add("/source/c.proto", """
                |message C { }
                |""".trimMargin())
        val schema = repoBuilder.schema()

        val response = SchemaReflector(schema, includeDependencies = true)
                .process(ServerReflectionRequest(file_containing_symbol = ".A"))
        assertThat(response.file_descriptor_response!!.file_descriptor_proto.map {
            DescriptorProtos.FileDescriptorProto.parseFrom(it.toByteArray())
        }.map { it.name }).containsExactly("a.proto", "b.proto", "b-new.proto", "c.proto")
    }

}
