/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.sample

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import okio.buffer
import okio.sink
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ServiceGeneratorTest {
  @get:Rule
  var temporaryFolder = TemporaryFolder()

  @Test @Throws(IOException::class)
  fun service() {
    val schema = schema(
        mapOf("sample.proto" to """
              |syntax = "proto2";
              |package squareup.wire.sample;
              |
              |message SampleMessage {
              |  repeated string array = 1;
              |}
              |
              |message SampleRequest {
              |  optional string name = 1;
              |  optional SampleMessage sample_message = 2;
              |}
              |
              |message SampleResponse {
              |  optional int32 age = 1;
              |}
              |
              |// This is it. A really fantastic service interface.
              |service SampleApi {
              |  // Call this RPC. You'll be glad you did!
              |  rpc FirstRpc (SampleRequest) returns (SampleResponse);
              |  rpc OtherOne (SampleRequest) returns (SampleResponse);
              |}
              |""".trimMargin()
        )
    )
    val service = schema.getService("squareup.wire.sample.SampleApi")
    val javaGenerator = JavaGenerator.get(schema)
    val generator = ServiceGenerator(javaGenerator)
    val typeSpec = generator.api(service)
    assertThat(toString(typeSpec))
        .isEqualTo(
            """
              |package squareup.wire.sample;
              |
              |/**
              | * This is it. A really fantastic service interface.
              | */
              |public interface SampleApi {
              |  /**
              |   * Call this RPC. You'll be glad you did!
              |   */
              |  SampleResponse FirstRpc(SampleRequest request);
              |
              |  SampleResponse OtherOne(SampleRequest request);
              |}
              |""".trimMargin()
        )
  }

  @Throws(IOException::class)
  private fun schema(fileToProto: Map<String, String>): Schema {
    val schemaLoader = SchemaLoader()
    schemaLoader.addSource(temporaryFolder.root)
    for (entry in fileToProto.entries) {
      val file = File(temporaryFolder.root, entry.key)
      file.parentFile
          .mkdirs()
      file.sink()
          .buffer()
          .use { out -> out.writeUtf8(entry.value) }
      schemaLoader.addProto(entry.key)
    }
    return schemaLoader.load()
  }

  @Throws(IOException::class)
  private fun toString(typeSpec: TypeSpec): String {
    val result = StringBuilder()
    val javaFile = JavaFile.builder("squareup.wire.sample", typeSpec).build()
    javaFile.writeTo(result)
    return result.toString()
  }
}