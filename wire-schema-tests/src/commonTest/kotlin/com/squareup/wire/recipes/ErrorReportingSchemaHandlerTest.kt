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

import assertk.assertThat
import assertk.assertions.message
import assertk.assertions.startsWith
import com.squareup.wire.WireTestLogger
import com.squareup.wire.addFakeRuntimeProtos
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.ErrorCollector
import com.squareup.wire.schema.SchemaException
import com.squareup.wire.schema.SchemaHandler
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class ErrorReportingSchemaHandlerTest {
  @Test fun errorsWhenStartsWithA() {
    val schema = buildSchema {
      add(
        name = "a.proto".toPath(),
        protoFile = """
        |syntax = "proto2";
        |
        |message A {
        |  optional string acrobatic = 1;
        |  optional string biofidus = 2;
        |}
        """.trimMargin(),
      )
      add(
        name = "b.proto".toPath(),
        protoFile = """
        |syntax = "proto2";
        |
        |message B {
        |  optional string comment = 1;
        |  optional string dinosaur = 2;
        |}
        """.trimMargin(),
      )
      addFakeRuntimeProtos()
    }

    val errorCollector = ErrorCollector()
    val context = SchemaHandler.Context(
      fileSystem = FakeFileSystem(),
      outDirectory = "out".toPath(),
      logger = WireTestLogger(),
      errorCollector = errorCollector,
    )

    ErrorReportingSchemaHandler().handle(schema, context)
    val exception = assertFailsWith<SchemaException> {
      errorCollector.throwIfNonEmpty()
    }

    assertThat(exception.message!!).startsWith("field starts with 'a'")
  }
}
