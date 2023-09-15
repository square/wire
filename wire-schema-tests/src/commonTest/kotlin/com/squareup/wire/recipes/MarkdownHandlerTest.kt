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

import assertk.Assert
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import com.squareup.wire.WireTestLogger
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.SchemaHandler
import kotlin.test.Test
import okio.FileSystem
import okio.Path
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
      // We manually add fake runtime protos to please Wire when running on a non-JVM platforms.
      // This isn't required if the code is to run on the JVM only.
      add("google/protobuf/descriptor.proto".toPath(), "")
      add("wire/extensions.proto".toPath(), "")
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

  // TODO(Benoit) Move all that into a MPP test-utils module.
  companion object {
    private fun FileSystem.readUtf8(pathString: String): String {
      read(pathString.toPath()) {
        return readUtf8()
      }
    }

    // We return an Iterable instead of a Set to please ambiguous APIs for Assertj.
    private fun FileSystem.findFiles(path: String): Iterable<String> {
      return listRecursively(path.withPlatformSlashes().toPath())
        .filter { !metadata(it).isDirectory }
        .map { it.toString() }
        .toSet()
    }

    private val slash = Path.DIRECTORY_SEPARATOR
    private val otherSlash = if (slash == "/") "\\" else "/"

    /**
     * This returns a string where all other slashes are replaced with the slash of the local platform.
     * On Windows, `/` will be replaced with `\`. On other platforms, `\` will be replaced with `/`.
     */
    private fun String.withPlatformSlashes(): String {
      return replace(otherSlash, slash)
    }

    /**
     * This asserts that [this] contains exactly in any order all [values] regardless of the slash they
     * may contain. This is useful to write one assertion which can be run on both macOS and Windows.
     */
    private fun Assert<Iterable<String>>.containsExactlyInAnyOrderAsRelativePaths(vararg values: String) {
      @Suppress("NAME_SHADOWING")
      val values = values.map { it.withPlatformSlashes() }
      return containsExactlyInAnyOrder(*values.toTypedArray())
    } }
}
