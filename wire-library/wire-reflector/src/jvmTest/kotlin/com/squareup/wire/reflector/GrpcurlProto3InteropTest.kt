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

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import okio.ByteString.Companion.decodeBase64
import okio.FileSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

// Reference golang reflection tests https://github.com/juliaogris/reflect
internal class GrpcurlProto3InteropTest {
  @Test
  fun `list services`() {
    val schema = loadSchema()
    // See golden test file https://github.com/juliaogris/reflect/blob/v0.0.7/testdata/proto3-base64/TestServicesCmd.base64
    val listRespBase64 = "Cgtsb2NhbGhvc3Q6MBIPCgtsb2NhbGhvc3Q6MDoAMjoKDAoKZWNobzMuRWNobwoqCihncnBjLnJlZmxlY3Rpb24udjFhbHBoYS5TZXJ2ZXJSZWZsZWN0aW9u"
    val expectedListResponse = ServerReflectionResponse.ADAPTER.decode(listRespBase64.decodeBase64()!!)
    assertThat(
      SchemaReflector(schema).process(
        ServerReflectionRequest(
          list_services = "",
          host = "localhost:0"
        )
      )
    ).isEqualTo(expectedListResponse)
  }

  val ECHO3_FILEDESCRIPTOR_RESPONSE = "Cgtsb2NhbGhvc3Q6MBIaCgtsb2NhbGhvc3Q6MCILZWNobzMuRWNobzM6HwgFEht1bmtub3duIHN5bWJvbDogZWNobzMuRWNobzM="
  @Test
  @Ignore("Fails with java.lang.IllegalStateException: field type not implemented: http at com.squareup.wire.schema.internal.SchemaEncoder.toJson(SchemaEncoder.kt:412)")
  fun `file_containing_symbol`() {
    val schema = loadSchema()
    val expectedResponse = ServerReflectionResponse.ADAPTER.decode(ECHO3_FILEDESCRIPTOR_RESPONSE.decodeBase64()!!)
    val response = SchemaReflector(schema).process(
      ServerReflectionRequest(
        file_containing_symbol = "echo3.Echo",
        host = "localhost:0"
      )
    )
    assertThat(response).isEqualTo(expectedResponse)
  }

  private val ServerReflectionResponse.fileDescriptors
    get() = file_descriptor_response!!.file_descriptor_proto.map {
      FileDescriptorProto.parseFrom(
        it.toByteArray()
      )
    }

}
private fun loadSchema(): Schema {
  return SchemaLoader(FileSystem.SYSTEM)
    .apply {
      initRoots(
        sourcePath = listOf(
          Location.get("src/jvmMain/resources", "grpc/reflection/v1alpha/reflection.proto"),
          Location.get("src/jvmTest/proto", "echo3/echo3.proto"),
          Location.get("src/jvmTest/proto", "google/api/annotations.proto"),
          Location.get("src/jvmTest/proto", "google/api/http.proto"),
          Location.get("src/jvmTest/proto", "google/protobuf/any.proto"),
        ),
        protoPath = listOf()
      )
    }
    .loadSchema()
}
