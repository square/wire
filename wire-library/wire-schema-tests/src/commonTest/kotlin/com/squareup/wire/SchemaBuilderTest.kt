/*
 * Copyright 2022 Block Inc.
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
package com.squareup.wire

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SchemaException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Path.Companion.toPath

class SchemaBuilderTest {
  @Test fun emptySchema() {
    val exception = assertFailsWith<SchemaException> {
      buildSchema {}
    }
    assertEquals("no sources", exception.message)
  }

  @Test fun sourcePathOnly() {
    val schema = buildSchema {
      add(
        "example1.proto".toPath(),
        """
          |syntax = "proto2";
          |
          |message A {
          |  optional B b = 1;
          |}
          |message B {
          |  optional C c = 1;
          |}
          |message C {
          |}
          |""".trimMargin()
      )
      add(
        "example2.proto".toPath(),
        """
          |syntax = "proto2";
          |
          |message D {
          |}
          |""".trimMargin()
      )
    }
    assertEquals(
      listOf(
        Location.get("/source", "example1.proto"),
        Location.get("/source", "example2.proto"),
        Location.get("google/protobuf/descriptor.proto"),
      ),
      schema.protoFiles.map { it.location },
    )
  }
}
