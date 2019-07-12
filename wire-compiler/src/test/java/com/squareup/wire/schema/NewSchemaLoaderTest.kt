/*
 * Copyright 2018 Square Inc.
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
package com.squareup.wire.schema

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class NewSchemaLoaderTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())

  @Test
  fun happyPath() {
    // Dependency graph:
    //   - blue
    //     - circle
    //       - triangle
    //   - red
    //     - oval
    //     - triangle
    // Note that the protoPath element octagon.proto is not imported!
    fs.add("colors/src/main/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin())
    fs.add("colors/src/main/proto/squareup/colors/red.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/oval.proto";
        |import "squareup/polygons/triangle.proto";
        |message Red {
        |}
        """.trimMargin())
    fs.add("polygons/src/main/proto/squareup/polygons/octagon.proto", """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Octagon {
        |}
        """.trimMargin())
    fs.add("polygons/src/main/proto/squareup/polygons/triangle.proto", """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Triangle {
        |}
        """.trimMargin())
    fs.addZip("lib/curves.zip",
        "squareup/curves/circle.proto" to """
        |syntax = "proto2";
        |package squareup.curves;
        |import "squareup/polygons/triangle.proto";
        |message Circle {
        |}
        """.trimMargin(),
        "squareup/curves/oval.proto" to """
        |syntax = "proto2";
        |package squareup.curves;
        |message Oval {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto"))
    val protoPath = listOf(Location.get("polygons/src/main/proto"), Location.get("lib/curves.zip"))
    val loader = NewSchemaLoader(fs, sourcePath, protoPath)
    val protoFiles = loader.use { it.load() }
    assertThat(protoFiles.map { it.location().path }).containsExactlyInAnyOrder(
        "squareup/colors/blue.proto",
        "squareup/colors/red.proto",
        "squareup/curves/circle.proto",
        "squareup/curves/oval.proto",
        "squareup/polygons/triangle.proto"
    )
    assertThat(loader.sourceLocationPaths).containsExactlyInAnyOrder(
        "squareup/colors/blue.proto",
        "squareup/colors/red.proto"
    )
  }

  @Test
  fun noSourcesFound() {
    val sourcePath = listOf<Location>()
    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs, sourcePath).use { it.load() }
    }
    assertThat(exception).hasMessage("no sources")
  }

  @Test
  fun packageDoesNotMatchFileSystemIsOkayWithBaseDirectory() {
    fs.add("colors/src/main/proto/squareup/shapes/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto", "squareup/shapes/blue.proto"))
    NewSchemaLoader(fs, sourcePath).use { it.load() }
  }

  @Test
  fun packageDoesNotMatchFileSystemFailsWithoutBaseDirectory() {
    fs.add("colors/src/main/proto/squareup/shapes/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto/squareup/shapes/blue.proto"))
    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs, sourcePath).use { it.load() }
    }
    assertThat(exception).hasMessage("expected colors/src/main/proto/squareup/shapes/blue.proto " +
        "to have a path ending with squareup/colors/blue.proto")
  }

  @Test
  fun protoPathSpecifiedWithBaseAndFile() {
    fs.add("colors/src/main/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin())
    fs.add("curves/src/main/proto/squareup/curves/circle.proto", """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto"))
    val protoPath = listOf(Location.get("curves/src/main/proto", "squareup/curves/circle.proto"))
    val loader = NewSchemaLoader(fs, sourcePath, protoPath)
    val protoFiles = loader.use { it.load() }
    assertThat(protoFiles.map { it.location().path }).containsExactlyInAnyOrder(
        "squareup/colors/blue.proto",
        "squareup/curves/circle.proto"
    )
  }

  @Test
  fun importNotFound() {
    fs.add("colors/src/main/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |import "squareup/polygons/rectangle.proto";
        |message Blue {
        |}
        """.trimMargin())
    fs.add("polygons/src/main/proto/squareup/polygons/triangle.proto", """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Triangle {
        |}
        """.trimMargin())
    fs.addZip("lib/curves.zip",
        "squareup/curves/oval.proto" to """
        |syntax = "proto2";
        |package squareup.curves;
        |message Oval {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto"))
    val protoPath = listOf(Location.get("polygons/src/main/proto"), Location.get("lib/curves.zip"))
    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs, sourcePath, protoPath).use { it.load() }
    }
    assertThat(exception).hasMessage("""
        |unable to resolve 2 imports:
        |  squareup/curves/circle.proto
        |  squareup/polygons/rectangle.proto
        |searching 2 proto paths:
        |  polygons/src/main/proto
        |  lib/curves.zip
        """.trimMargin())
  }

  @Test
  fun ambiguousImport() {
    fs.add("colors/src/main/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin())
    fs.add("polygons/src/main/proto/squareup/curves/circle.proto", """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin())
    fs.addZip("lib/curves.zip",
        "squareup/curves/circle.proto" to """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin())

    val sourcePath = listOf(Location.get("colors/src/main/proto"))
    val protoPath = listOf(Location.get("polygons/src/main/proto"), Location.get("lib/curves.zip"))
    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs, sourcePath, protoPath).use { it.load() }
    }
    assertThat(exception).hasMessage("""
        |squareup/curves/circle.proto is ambiguous:
        |  lib/curves.zip/squareup/curves/circle.proto
        |  polygons/src/main/proto/squareup/curves/circle.proto
        """.trimMargin())
  }
}