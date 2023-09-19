/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.recipes

import com.squareup.wire.WireTestLogger
import com.squareup.wire.addFakeRuntimeProtos
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.SchemaHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.BufferedSource
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class LogToFileHandlerTest {
  @Test fun loggingTypes() {
    val schema = buildSchema {
      add(
        name = "test/message.proto".toPath(),
        protoFile = """
            |syntax = "proto2";
            |
            |package test;
            |
            |message Request {}
            |message Response {
            |  optional string result = 1;
            |}
        """.trimMargin(),
      )
      add(
        name = "test/service.proto".toPath(),
        protoFile = """
            |syntax = "proto2";
            |
            |package test;
            |
            |import "test/message.proto";
            |
            |service MyService {
            |  rpc fetch(test.Request) returns(test.Response) {};
            |}
        """.trimMargin(),
      )
      addFakeRuntimeProtos()
    }

    val context = SchemaHandler.Context(
      fileSystem = FakeFileSystem(),
      outDirectory = "/".toPath(),
      logger = WireTestLogger(),
      sourcePathPaths = setOf("test/message.proto", "test/service.proto"),
    )
    LogToFileHandler().handle(schema, context)

    val content = context.fileSystem.read("log.txt".toPath(), BufferedSource::readUtf8)
    val expected = """
        |Generating type: test.Request
        |Generating type: test.Response
        |Generating service: test.MyService
        |
    """.trimMargin()
    assertEquals(expected, content)
  }
}
