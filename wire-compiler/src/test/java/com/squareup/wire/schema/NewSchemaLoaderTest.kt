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
import com.squareup.wire.testing.add
import com.squareup.wire.testing.addZip
import com.squareup.wire.testing.symlink
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

    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(
              Location.get("colors/src/main/proto")
          ),
          protoPath = listOf(
              Location.get("polygons/src/main/proto"),
              Location.get("lib/curves.zip")
          )
      )
      val sourcePathFiles = loader.loadSourcePathFiles()
      assertThat(sourcePathFiles.map { it.location() }).containsExactly(
          Location.get("colors/src/main/proto", "squareup/colors/blue.proto"),
          Location.get("colors/src/main/proto", "squareup/colors/red.proto")
      )
      assertThat(loader.load("google/protobuf/descriptor.proto").location())
          .isEqualTo(Location.get("google/protobuf/descriptor.proto"))
      assertThat(loader.load("squareup/curves/circle.proto").location())
          .isEqualTo(Location.get("lib/curves.zip", "squareup/curves/circle.proto"))
      assertThat(loader.load("squareup/curves/oval.proto").location())
          .isEqualTo(Location.get("lib/curves.zip", "squareup/curves/oval.proto"))
      assertThat(loader.load("squareup/polygons/triangle.proto").location())
          .isEqualTo(Location.get("polygons/src/main/proto", "squareup/polygons/triangle.proto"))
      loader.reportLoadingErrors()
    }
  }

  @Test
  fun noSourcesFound() {
    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs).use { loader ->
        loader.initRoots(sourcePath = listOf())
        loader.loadSourcePathFiles()
      }
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

    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("colors/src/main/proto", "squareup/shapes/blue.proto"))
      )
      val sourcePathFiles = loader.loadSourcePathFiles()
      assertThat(sourcePathFiles.map { it.location().path })
          .containsExactly("squareup/shapes/blue.proto")
    }
  }

  @Test
  fun packageDoesNotMatchFileSystemFailsWithoutBaseDirectory() {
    fs.add("colors/src/main/proto/squareup/shapes/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin())

    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs).use { loader ->
        loader.initRoots(
            sourcePath = listOf(Location.get("colors/src/main/proto/squareup/shapes/blue.proto"))
        )
        loader.loadSourcePathFiles()
      }
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

    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("colors/src/main/proto")),
          protoPath = listOf(Location.get("curves/src/main/proto", "squareup/curves/circle.proto"))
      )
      val sourcePathFiles = loader.loadSourcePathFiles()
      assertThat(sourcePathFiles.map { it.location().path }).containsExactlyInAnyOrder(
          "squareup/colors/blue.proto"
      )
      assertThat(loader.load("google/protobuf/descriptor.proto").location())
          .isEqualTo(Location.get("google/protobuf/descriptor.proto"))
      assertThat(loader.load("squareup/curves/circle.proto").location())
          .isEqualTo(Location.get("curves/src/main/proto", "squareup/curves/circle.proto"))
    }
  }

  @Test
  fun symlinkDirectory() {
    fs.add("secret/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin())
    fs.symlink(
        "colors/src/main/proto",
        "../../../secret/proto"
    )

    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("colors/src/main/proto"))
      )
      val sourcePathFiles = loader.loadSourcePathFiles()
      assertThat(sourcePathFiles.map { it.location() }).containsExactly(
          Location("colors/src/main/proto", "squareup/colors/blue.proto"))
    }
  }

  @Test
  fun symlinkFile() {
    fs.add("secret/proto/squareup/colors/blue.proto", """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin())
    fs.symlink(
        "colors/src/main/proto/squareup/colors/blue.proto",
        "../../../../../../secret/proto/squareup/colors/blue.proto"
    )

    NewSchemaLoader(fs).use { loader ->
      loader.initRoots(
          sourcePath = listOf(Location.get("colors/src/main/proto"))
      )
      val sourcePathFiles = loader.loadSourcePathFiles()
      assertThat(sourcePathFiles.map { it.location() }).containsExactlyInAnyOrder(
          Location("colors/src/main/proto", "squareup/colors/blue.proto")
      )
    }
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

    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs).use { loader ->
        loader.initRoots(
            sourcePath = listOf(
                Location.get("colors/src/main/proto")
            ),
            protoPath = listOf(
                Location.get("polygons/src/main/proto"),
                Location.get("lib/curves.zip")
            )
        )
        loader.loadSourcePathFiles()
        loader.load("squareup/curves/circle.proto")
        loader.load("squareup/polygons/rectangle.proto")
        loader.reportLoadingErrors()
      }
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

    val exception = assertFailsWith<IllegalArgumentException> {
      NewSchemaLoader(fs).use { loader ->
        loader.initRoots(
            sourcePath = listOf(
                Location.get("colors/src/main/proto")
            ),
            protoPath = listOf(
                Location.get("polygons/src/main/proto"),
                Location.get("lib/curves.zip")
            )
        )
        loader.loadSourcePathFiles()
        loader.load("squareup/curves/circle.proto")
        loader.reportLoadingErrors()
      }
    }
    assertThat(exception).hasMessage("""
        |squareup/curves/circle.proto is ambiguous:
        |  lib/curves.zip/squareup/curves/circle.proto
        |  polygons/src/main/proto/squareup/curves/circle.proto
        """.trimMargin())
  }
}
