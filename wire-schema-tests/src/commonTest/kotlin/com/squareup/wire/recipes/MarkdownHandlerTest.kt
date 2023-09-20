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
import assertk.assertions.isEqualTo
import com.squareup.wire.WireTestLogger
import com.squareup.wire.addFakeRuntimeProtos
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.testing.containsExactlyInAnyOrderAsRelativePaths
import com.squareup.wire.testing.findFiles
import com.squareup.wire.testing.readUtf8
import kotlin.test.Test
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class MarkdownHandlerTest {
  @Test fun markdownHandlerGenerates() {
    val schema = buildSchema {
      add(
        name = "squareup/colors/red.proto".toPath(),
        protoFile = """
                  |syntax = "proto2";
                  |package squareup.colors;
                  |/** This is the color of the sky when the sky is lava. */
                  |message Red {
                  |  optional string oval = 1;
                  |}
        """.trimMargin(),
      )
      add(
        name = "squareup/colors/blue.proto".toPath(),
        protoFile = """
                  |syntax = "proto2";
                  |package squareup.colors;
                  |import "squareup/polygons/triangle.proto";
                  |/** This is the color of the sky. */
                  |message Blue {
                  |  optional string circle = 1;
                  |  optional squareup.polygons.Triangle triangle = 2;
                  |}
        """.trimMargin(),
      )
      add(
        name = "squareup/polygons/triangle.proto".toPath(),
        protoFile = """
                  |syntax = "proto2";
                  |package squareup.polygons;
                  |message Triangle {
                  |  repeated double angles = 1;
                  |  enum Type {
                  |    EQUILATERAL = 1;
                  |    ISOSCELES = 2;
                  |    RIGHTANGLED = 3;
                  |  }
                  |}
        """.trimMargin(),
      )
      addFakeRuntimeProtos()
    }

    val fileSystem = FakeFileSystem()
    val context = SchemaHandler.Context(
      fileSystem = fileSystem,
      outDirectory = "generated/markdown".toPath(),
      logger = WireTestLogger(),
      sourcePathPaths = setOf("squareup/colors/red.proto", "squareup/colors/blue.proto"),
    )
    MarkdownHandler().handle(schema, context)

    assertThat(fileSystem.findFiles("generated"))
      .containsExactlyInAnyOrderAsRelativePaths(
        "generated/markdown/squareup/colors/Blue.md",
        "generated/markdown/squareup/colors/Red.md",
      )
    assertThat(fileSystem.readUtf8("generated/markdown/squareup/colors/Blue.md"))
      .isEqualTo(
        """
            |# Blue
            |
            |This is the color of the sky.
            |
        """.trimMargin(),
      )
    assertThat(fileSystem.readUtf8("generated/markdown/squareup/colors/Red.md"))
      .isEqualTo(
        """
            |# Red
            |
            |This is the color of the sky when the sky is lava.
            |
        """.trimMargin(),
      )
  }
}
