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

import com.squareup.wire.testing.add
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class SchemaLoaderTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }

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
    fs.add(
      "colors/src/main/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.add(
      "colors/src/main/proto/squareup/colors/red.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/oval.proto";
        |import "squareup/polygons/triangle.proto";
        |message Red {
        |}
        """.trimMargin()
    )
    fs.add(
      "polygons/src/main/proto/squareup/polygons/octagon.proto",
      """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Octagon {
        |}
        """.trimMargin()
    )
    fs.add(
      "polygons/src/main/proto/squareup/polygons/triangle.proto",
      """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Triangle {
        |}
        """.trimMargin()
    )
    fs.add(
      "lib/squareup/curves/circle.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin()
    )
    fs.add(
      "lib/squareup/curves/oval.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Oval {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(
        Location.get("colors/src/main/proto")
      ),
      protoPath = listOf(
        Location.get("polygons/src/main/proto"),
        Location.get("lib"),
      )
    )
    val sourcePathFiles = loader.loadSchema().protoFiles
    assertEquals(listOf(
      Location.get("google/protobuf/descriptor.proto"),
      Location.get("colors/src/main/proto", "squareup/colors/blue.proto"),
      Location.get("colors/src/main/proto", "squareup/colors/red.proto"),
    ),
      sourcePathFiles.map { it.location })
    assertEquals(
      Location.get("google/protobuf/descriptor.proto"),
      loader.load("google/protobuf/descriptor.proto").location
    )
    assertEquals(
      Location.get("lib", "squareup/curves/circle.proto"),
      loader.load("squareup/curves/circle.proto").location
    )
    assertEquals(
      Location.get("lib", "squareup/curves/oval.proto"),
      loader.load("squareup/curves/oval.proto").location
    )
    assertEquals(
      Location.get("polygons/src/main/proto", "squareup/polygons/triangle.proto"),
      loader.load("squareup/polygons/triangle.proto").location
    )
  }

  @Test
  fun noSourcesFound() {
    val loader = SchemaLoader(fs)
    loader.initRoots(sourcePath = listOf())
    val exception = assertFailsWith<SchemaException> {
      loader.loadSchema()
    }
    assertContains(exception.message!!, "no sources")
  }

  @Test
  fun packageDoesNotMatchFileSystemIsOkayWithBaseDirectory() {
    fs.add(
      "colors/src/main/proto/squareup/shapes/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("colors/src/main/proto", "squareup/shapes/blue.proto"))
    )
    val sourcePathFiles = loader.loadSchema().protoFiles
    assertEquals(listOf(
      "google/protobuf/descriptor.proto", "squareup/shapes/blue.proto"
    ), sourcePathFiles.map { it.location.path })
  }

  @Test
  fun packageDoesNotMatchFileSystemFailsWithoutBaseDirectory() {
    fs.add(
      "colors/src/main/proto/squareup/shapes/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("colors/src/main/proto/squareup/shapes/blue.proto"))
    )
    val exception = assertFailsWith<SchemaException> {
      loader.loadSchema()
    }
    assertContains(
      exception.message!!,
      "expected colors/src/main/proto/squareup/shapes/blue.proto " +
        "to have a path ending with squareup/colors/blue.proto"
    )
  }

  @Test
  fun protoPathSpecifiedWithBaseAndFile() {
    fs.add(
      "colors/src/main/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.add(
      "curves/src/main/proto/squareup/curves/circle.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("curves/src/main/proto", "squareup/curves/circle.proto"))
    )
    val sourcePathFiles = loader.loadSchema().protoFiles
    assertEquals(listOf(
      "google/protobuf/descriptor.proto",
      "squareup/colors/blue.proto"
    ), sourcePathFiles.map { it.location.path })
    assertEquals(
      Location.get("google/protobuf/descriptor.proto"),
      loader.load("google/protobuf/descriptor.proto").location
    )
    assertEquals(
      Location.get("curves/src/main/proto", "squareup/curves/circle.proto"),
      loader.load("squareup/curves/circle.proto").location
    )
  }

  @Test
  fun emptyPackagedProtoMessage() {
    fs.add(
      "address.proto",
      """
      |syntax = "proto3";
      |option java_package ="address";
      |
      |message Address {
      |  string street = 1;
      |  int32 zip = 2;
      |  string city = 3;
      |}""".trimMargin()
    )

    fs.add(
      "customer.proto",
      """
      |syntax = "proto3";
      |option java_package ="customer";
      |
      |import "address.proto";
      |
      |message Customer {
      |  string name = 1;
      |  Address address = 3;
      |}""".trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get(fs.workingDirectory.toString())),
      protoPath = listOf(Location.get(fs.workingDirectory.toString())),
    )
    val schema = loader.loadSchema()
    assertTrue(schema.getType(ProtoType.get("Address")) is MessageType)
    assertTrue(schema.getType(ProtoType.get("Customer")) is MessageType)
  }

  @Test
  fun symlinkDirectory() {
    if (!fs.allowSymlinks) return

    fs.add(
      "secret/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.createDirectories("colors/src/main".toPath())
    fs.createSymlink(
      "colors/src/main/proto".toPath(),
      "../../../secret/proto".toPath()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("colors/src/main/proto"))
    )
    val sourcePathFiles = loader.loadSchema().protoFiles
    assertEquals(listOf(
      Location.get("google/protobuf/descriptor.proto"),
      Location("colors/src/main/proto", "squareup/colors/blue.proto")
    ),
      sourcePathFiles.map { it.location })
  }

  @Test
  fun symlinkFile() {
    if (!fs.allowSymlinks) return

    fs.add(
      "secret/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.createDirectories("colors/src/main/proto/squareup/colors".toPath())
    fs.createSymlink(
      "colors/src/main/proto/squareup/colors/blue.proto".toPath(),
      "../../../../../../secret/proto/squareup/colors/blue.proto".toPath()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(Location.get("colors/src/main/proto"))
    )
    val sourcePathFiles = loader.loadSchema().protoFiles
    assertEquals(listOf(
      Location.get("google/protobuf/descriptor.proto"),
      Location("colors/src/main/proto", "squareup/colors/blue.proto")
    ),
      sourcePathFiles.map { it.location })
  }

  @Test
  fun importNotFound() {
    fs.add(
      "colors/src/main/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |import "squareup/polygons/rectangle.proto";
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.add(
      "polygons/src/main/proto/squareup/polygons/triangle.proto",
      """
        |syntax = "proto2";
        |package squareup.polygons;
        |message Triangle {
        |}
        """.trimMargin()
    )
    fs.add(
      "lib/squareup/curves/oval.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Oval {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(
        Location.get("colors/src/main/proto")
      ),
      protoPath = listOf(
        Location.get("polygons/src/main/proto"),
        Location.get("lib"),
      )
    )
    val exception = assertFailsWith<SchemaException> {
      loader.loadSchema()
    }
    assertContains(
      exception.message!!,
      """
        |unable to find squareup/curves/circle.proto
        |  searching 2 proto paths:
        |    polygons/src/main/proto
        |    lib
        |  for file colors/src/main/proto/squareup/colors/blue.proto
        |unable to find squareup/polygons/rectangle.proto
        |  searching 2 proto paths:
        |    polygons/src/main/proto
        |    lib
        |  for file colors/src/main/proto/squareup/colors/blue.proto
        """.trimMargin()
    )
  }

  @Test
  fun ambiguousImport() {
    fs.add(
      "colors/src/main/proto/squareup/colors/blue.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/curves/circle.proto";
        |message Blue {
        |}
        """.trimMargin()
    )
    fs.add(
      "polygons/src/main/proto/squareup/curves/circle.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin()
    )
    fs.add(
      "lib/squareup/curves/circle.proto",
      """
        |syntax = "proto2";
        |package squareup.curves;
        |message Circle {
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.initRoots(
      sourcePath = listOf(
        Location.get("colors/src/main/proto")
      ),
      protoPath = listOf(
        Location.get("polygons/src/main/proto"),
        Location.get("lib")
      )
    )
    val exception = assertFailsWith<SchemaException> {
      loader.loadSchema()
    }
    assertContains(
      exception.message!!,
      """
        |squareup/curves/circle.proto is ambiguous:
        |  lib/squareup/curves/circle.proto
        |  polygons/src/main/proto/squareup/curves/circle.proto
        """.trimMargin()
    )
  }

  @Test
  fun exhaustiveLoad() {
    fs.add(
      "colors/src/main/proto/squareup/colors/red.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/colors/orange.proto";
        |message Red {
        |  optional Orange orange = 1;
        |}
        """.trimMargin()
    )
    fs.add(
      "colors/src/main/proto/squareup/colors/orange.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |import "squareup/colors/yellow.proto";
        |message Orange {
        |  optional Yellow yellow = 1;
        |}
        """.trimMargin()
    )
    fs.add(
      "colors/src/main/proto/squareup/colors/yellow.proto",
      """
        |syntax = "proto2";
        |package squareup.colors;
        |// import "squareup/colors/green.proto";
        |message Yellow {
        |  // optional Green green = 1;
        |}
        """.trimMargin()
    )

    val loader = SchemaLoader(fs)
    loader.loadExhaustively = true
    loader.initRoots(
      sourcePath = listOf(
        Location.get("colors/src/main/proto", "squareup/colors/red.proto")
      ),
      protoPath = listOf(
        Location.get("colors/src/main/proto")
      )
    )
    val schema = loader.loadSchema()
    val redMessage = schema.getType("squareup.colors.Red") as MessageType
    assertEquals(
      ProtoType.get("squareup.colors.Orange"),
      redMessage.field("orange")!!.type
    )

    val orangeMessage = schema.getType("squareup.colors.Orange") as MessageType
    assertEquals(
      ProtoType.get("squareup.colors.Yellow"),
      orangeMessage.field("yellow")!!.type
    )

    val yellowMessage = schema.getType("squareup.colors.Yellow") as MessageType
    assertTrue(yellowMessage.fields.isEmpty())
  }
}
