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
internal class GrpcurlProto2InteropTest {
  @Test
  fun `list services`() {
    val schema = loadSchema()
    // See golden test file https://github.com/juliaogris/reflect/blob/v0.0.7/testdata/proto2-base64/TestServicesCmd.base64
    val respBase64 = "Cgtsb2NhbGhvc3Q6MBIPCgtsb2NhbGhvc3Q6MDoAMjoKDAoKZWNobzIuRWNobwoqCihncnBjLnJlZmxlY3Rpb24udjFhbHBoYS5TZXJ2ZXJSZWZsZWN0aW9u=="
    val expectedResponse = ServerReflectionResponse.ADAPTER.decode(respBase64.decodeBase64()!!)
    assertThat(
      SchemaReflector(schema).process(
        ServerReflectionRequest(
          list_services = "",
          host = "localhost:0"
        )
      )
    ).isEqualTo(expectedResponse)
  }
  @Test
  @Ignore("disagreements over snake_case vs. camelCase for json_name")
  fun `file_containing_symbol`() {
    val schema = loadSchema()
    val respBase64 = "EhciFXJvdXRlZ3VpZGUuUm91dGVHdWlkZSLfBgrcBgoXcmd1aWRlL3JvdXRlZ3VpZGUucHJvdG8SCnJvdXRlZ3VpZGUiQQoFUG9pbnQSGgoIbGF0aXR1ZGUYASABKAVSCGxhdGl0dWRlEhwKCWxvbmdpdHVkZRgCIAEoBVIJbG9uZ2l0dWRlIlEKCVJlY3RhbmdsZRIhCgJsbxgBIAEoCzIRLnJvdXRlZ3VpZGUuUG9pbnRSAmxvEiEKAmhpGAIgASgLMhEucm91dGVndWlkZS5Qb2ludFICaGkiTAoHRmVhdHVyZRISCgRuYW1lGAEgASgJUgRuYW1lEi0KCGxvY2F0aW9uGAIgASgLMhEucm91dGVndWlkZS5Qb2ludFIIbG9jYXRpb24iVAoJUm91dGVOb3RlEi0KCGxvY2F0aW9uGAEgASgLMhEucm91dGVndWlkZS5Qb2ludFIIbG9jYXRpb24SGAoHbWVzc2FnZRgCIAEoCVIHbWVzc2FnZSKTAQoMUm91dGVTdW1tYXJ5Eh8KC3BvaW50X2NvdW50GAEgASgFUgpwb2ludENvdW50EiMKDWZlYXR1cmVfY291bnQYAiABKAVSDGZlYXR1cmVDb3VudBIaCghkaXN0YW5jZRgDIAEoBVIIZGlzdGFuY2USIQoMZWxhcHNlZF90aW1lGAQgASgFUgtlbGFwc2VkVGltZTK6AgoKUm91dGVHdWlkZRI0CgpHZXRGZWF0dXJlEhEucm91dGVndWlkZS5Qb2ludBoTLnJvdXRlZ3VpZGUuRmVhdHVyZRI7ChFHZXREZWZhdWx0RmVhdHVyZRIRLnJvdXRlZ3VpZGUuUG9pbnQaEy5yb3V0ZWd1aWRlLkZlYXR1cmUSPAoMTGlzdEZlYXR1cmVzEhUucm91dGVndWlkZS5SZWN0YW5nbGUaEy5yb3V0ZWd1aWRlLkZlYXR1cmUwARI8CgtSZWNvcmRSb3V0ZRIRLnJvdXRlZ3VpZGUuUG9pbnQaGC5yb3V0ZWd1aWRlLlJvdXRlU3VtbWFyeSgBEj0KCVJvdXRlQ2hhdBIVLnJvdXRlZ3VpZGUuUm91dGVOb3RlGhUucm91dGVndWlkZS5Sb3V0ZU5vdGUoATABQihaJmdpdGh1Yi5jb20vanVsaWFvZ3Jpcy9ndXBweS9wa2cvcmd1aWRl"
    val expectedResponse = ServerReflectionResponse.ADAPTER.decode(respBase64.decodeBase64()!!)
    val response = SchemaReflector(schema).process(
      ServerReflectionRequest(
        file_containing_symbol = "routeguide.RouteGuide"
      )
    )
    assertThat(response.fileDescriptors).isEqualTo(expectedResponse.fileDescriptors)
  }

  private val ServerReflectionResponse.fileDescriptors
    get() = file_descriptor_response!!.file_descriptor_proto.map {
      FileDescriptorProto.parseFrom(
        it.toByteArray()
      )
    }

  private fun loadSchema(): Schema {
    return SchemaLoader(FileSystem.SYSTEM)
      .apply {
        initRoots(
          // TODO(juliaogris): Can we derive dependencies transitively?
          sourcePath = listOf(
            Location.get("src/jvmMain/resources", "grpc/reflection/v1alpha/reflection.proto"),
            Location.get("src/jvmTest/proto", "echo2/echo2.proto"),
            Location.get("src/jvmTest/proto", "google/api/annotations.proto"),
            Location.get("src/jvmTest/proto", "google/api/http.proto"),
            Location.get("src/jvmTest/proto", "google/protobuf/any.proto"),
          ),
          protoPath = listOf()
        )
      }
      .loadSchema()
    }
  }

