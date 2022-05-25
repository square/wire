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
package com.squareup.wire.recipes

import com.squareup.wire.WireTestLogger
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.SchemaHandler
import kotlin.test.Test
import kotlin.test.assertEquals
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
                  """.trimMargin()
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
                  """.trimMargin()
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
                  """.trimMargin()
      )
    }

    val fileSystem = FakeFileSystem()
    val context = SchemaHandler.Context(
      fileSystem = fileSystem,
      outDirectory = "generated/markdown".toPath(),
      logger = WireTestLogger(),
      sourcePathPaths = setOf("squareup/colors/red.proto", "squareup/colors/blue.proto"),
    )
    MarkdownHandler().handle(schema, context)

    assertEquals(
      setOf(
        "generated/markdown/squareup/colors/Blue.md".withPlatformSlashes(),
        "generated/markdown/squareup/colors/Red.md".withPlatformSlashes(),
      ),
      fileSystem.findFiles("generated"),
    )
    assertEquals(
      """
        |# Blue
        |
        |This is the color of the sky.
        |""".trimMargin(),
      fileSystem.readUtf8("generated/markdown/squareup/colors/Blue.md"),
    )
    assertEquals(
      """
        |# Red
        |
        |This is the color of the sky when the sky is lava.
        |""".trimMargin(),
      fileSystem.readUtf8("generated/markdown/squareup/colors/Red.md"),
    )
  }
}

// TODO(Benoit) Some of this logic also lives in wire-test-utils. Let's find a way to share that.
private val slash = Path.DIRECTORY_SEPARATOR
private val otherSlash = if (slash == "/") "\\" else "/"

/**
 * This returns a string where all other slashes are replaced with the slash of the local platform.
 * On Windows, `/` will be replaced with `\`. On other platforms, `\` will be replaced with `/`.
 */
private fun String.withPlatformSlashes(): String {
  return replace(otherSlash, slash)
}

private fun FileSystem.readUtf8(pathString: String): String {
  read(pathString.toPath()) {
    return readUtf8()
  }
}

private fun FileSystem.findFiles(path: String): Set<String> {
  return listRecursively(path.withPlatformSlashes().toPath())
    .filter { !metadata(it).isDirectory }
    .map { it.toString() }
    .toSet()
}
