/*
 * Copyright (C) 2015 Square, Inc.
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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.reflector

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.squareup.wire.schema.RepoBuilder
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SchemaReflectorTest {
  val schema = RepoBuilder()
    .add("service.proto", """
            |import "request.proto";
            |import "response.proto";
            |service Service {
            |  rpc Call (Request) returns (Response);
            |}
            """.trimMargin())
    .add("request.proto", """
            |message Request {
            |}
            """.trimMargin())
    .add("response.proto", """
            |message Response {
            |}
            """.trimMargin())
    .schema()

  @Test
  fun happyPath() {
    val reflector = SchemaReflector(schema)
    val response = reflector.process(ServerReflectionRequest.Builder()
      .list_services("*")
      .build())
    assertThat(response.list_services_response!!.service.single().name).isEqualTo("Service")
  }

  @Test
  fun lookupSingleType() {
    val reflector = SchemaReflector(schema)
    val response = reflector.process(ServerReflectionRequest.Builder()
      .file_by_filename("service.proto")
      .build())
    val file = response.files[0]
    assertThat(file.serviceList.single().name).isEqualTo("Service")
    assertThat(file.serviceList.single().methodList.single().name).isEqualTo("Call")
  }

  private val ServerReflectionResponse.files: List<FileDescriptorProto>
    get() {
      val fileDescriptors = file_descriptor_response!!.file_descriptor_proto
      return fileDescriptors.map { FileDescriptorProto.parseFrom(it.toByteArray()) }
    }
}
