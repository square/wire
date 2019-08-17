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
import com.squareup.wire.StringWireLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WireRunTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())
  private val logger = StringWireLogger()

  @Test
  fun javaOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(Target.JavaTarget(outDirectory = "generated/java"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/java/squareup/colors/Blue.java",
        "generated/java/squareup/colors/Red.java")
    assertThat(fs.get("generated/java/squareup/colors/Blue.java"))
        .contains("public final class Blue extends Message")
    assertThat(fs.get("generated/java/squareup/colors/Red.java"))
        .contains("public final class Red extends Message")
  }

  @Test
  fun ktOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(Target.KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/colors/Blue.kt",
        "generated/kt/squareup/colors/Red.kt")
    assertThat(fs.get("generated/kt/squareup/colors/Blue.kt"))
        .contains("class Blue")
    assertThat(fs.get("generated/kt/squareup/colors/Red.kt"))
        .contains("class Red")
  }

  @Test
  fun ktOnlyWithService() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    writeColorsRouteProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("routes/src/main/proto")),
        protoPath = listOf(Location.get("colors/src/main/proto"),
            Location.get("polygons/src/main/proto")),
        targets = listOf(Target.KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteClient.kt"))
        .contains("interface RouteClient : Service", "suspend fun GetUpdatedRed")
  }

  @Test
  fun ktOnlyWithBlockingService() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    writeColorsRouteProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("routes/src/main/proto")),
        protoPath = listOf(Location.get("colors/src/main/proto"),
            Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.KotlinTarget(outDirectory = "generated/kt", blockingServices = true))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteBlockingServer.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteBlockingServer.kt"))
        .contains("interface RouteBlockingServer : Service", "fun GetUpdatedRed")
        .doesNotContain("suspend fun GetUpdatedRed")
  }

  @Test
  fun ktOnlyWithServiceAsSingleMethod() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    writeColorsRouteProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("routes/src/main/proto")),
        protoPath = listOf(Location.get("colors/src/main/proto"),
            Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.KotlinTarget(outDirectory = "generated/kt", singleMethodServices = true))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt",
        "generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt"))
        .contains("interface RouteGetUpdatedBlueClient : Service")
        .doesNotContain("interface RouteGetUpdatedRedClient : Service")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt"))
        .contains("interface RouteGetUpdatedRedClient : Service")
        .doesNotContain("interface RouteGetUpdatedBlueClient : Service")
  }

  @Test
  fun ktThenJava() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.KotlinTarget(
                outDirectory = "generated/kt",
                includes = listOf("squareup.colors.Blue")),
            Target.JavaTarget(
                outDirectory = "generated/java")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt",
        "generated/java/squareup/colors/Red.java")
    assertThat(fs.get("generated/kt/squareup/colors/Blue.kt"))
        .contains("class Blue")
    assertThat(fs.get("generated/java/squareup/colors/Red.java"))
        .contains("public final class Red extends Message")
  }

  @Test
  fun javaThenKt() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.JavaTarget(
                outDirectory = "generated/java",
                includes = listOf("squareup.colors.Blue")),
            Target.KotlinTarget(
                outDirectory = "generated/kt")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/java/squareup/colors/Blue.java",
        "generated/kt/squareup/colors/Red.kt")
    assertThat(fs.get("generated/java/squareup/colors/Blue.java"))
        .contains("public final class Blue extends Message")
    assertThat(fs.get("generated/kt/squareup/colors/Red.kt"))
        .contains("class Red")
  }

  @Test
  fun excludesTypeName() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(
            Location.get("colors/src/main/proto"),
            Location.get("polygons/src/main/proto")
        ),
        targets = listOf(
            Target.KotlinTarget(
                outDirectory = "generated/kt",
                excludes = listOf("squareup.colors.Red")),
            Target.JavaTarget(
                outDirectory = "generated/java")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt",
        "generated/java/squareup/colors/Red.java",
        "generated/kt/squareup/polygons/Triangle.kt")
    assertThat(fs.get("generated/kt/squareup/colors/Blue.kt"))
        .contains("class Blue")
    assertThat(fs.get("generated/java/squareup/colors/Red.java"))
        .contains("public final class Red extends Message")
    assertThat(fs.get("generated/kt/squareup/polygons/Triangle.kt"))
        .contains("class Triangle")
  }

  @Test
  fun excludesWildcard() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(
            Location.get("colors/src/main/proto"),
            Location.get("polygons/src/main/proto")
        ),
        targets = listOf(
            Target.KotlinTarget(
                outDirectory = "generated/kt",
                excludes = listOf("squareup.colors.*")),
            Target.JavaTarget(
                outDirectory = "generated/java")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/java/squareup/colors/Blue.java",
        "generated/java/squareup/colors/Red.java",
        "generated/kt/squareup/polygons/Triangle.kt")
    assertThat(fs.get("generated/java/squareup/colors/Blue.java"))
        .contains("public final class Blue extends Message")
    assertThat(fs.get("generated/java/squareup/colors/Red.java"))
        .contains("public final class Red extends Message")
    assertThat(fs.get("generated/kt/squareup/polygons/Triangle.kt"))
        .contains("class Triangle")
  }

  @Test
  fun treeShakingRoots() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        treeShakingRoots = listOf("squareup.colors.Blue"),
        targets = listOf(Target.KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt")
  }

  @Test
  fun treeShakingRubbish() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        treeShakingRubbish = listOf("squareup.colors.Red"),
        targets = listOf(Target.KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt")
  }

  @Test
  fun nullTarget() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.NullTarget(includes = listOf("squareup.colors.Red")),
            Target.KotlinTarget(outDirectory = "generated/kt")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt")
  }

  @Test
  fun javaPackageForJvmLanguages() {
    writeSquareProto()
    writeRhombusProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.JavaTarget(
                outDirectory = "generated/java",
                includes = listOf("squareup.polygons.Square")),
            Target.KotlinTarget(
                outDirectory = "generated/kt")
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/java/com/squareup/polygons/Square.java",
        "generated/kt/com/squareup/polygons/Rhombus.kt")
  }

  @Test
  fun nonExclusiveTypeEmittedTwice() {
    writeSquareProto()
    writeRhombusProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(
            Target.JavaTarget(
                outDirectory = "generated/java"),
            Target.KotlinTarget(
                outDirectory = "generated/kt",
                exclusive = false,
                includes = listOf("squareup.polygons.Square"))
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/java/com/squareup/polygons/Square.java",
        "generated/java/com/squareup/polygons/Rhombus.java",
        "generated/kt/com/squareup/polygons/Square.kt")
  }

  private fun writeRedProto() {
    fs.add("colors/src/main/proto/squareup/colors/red.proto", """
          |syntax = "proto2";
          |package squareup.colors;
          |message Red {
          |  optional string oval = 1;
          |}
          """.trimMargin())
  }

  private fun writeColorsRouteProto() {
    fs.add("routes/src/main/proto/squareup/routes/route.proto", """
          |syntax = "proto2";
          |package squareup.routes;
          |import "squareup/colors/blue.proto";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |  rpc GetUpdatedBlue(squareup.colors.Blue) returns (squareup.colors.Blue) {}
          |}
          """.trimMargin())
  }

  private fun writeBlueProto() {
    fs.add("colors/src/main/proto/squareup/colors/blue.proto", """
          |syntax = "proto2";
          |package squareup.colors;
          |import "squareup/polygons/triangle.proto";
          |message Blue {
          |  optional string circle = 1;
          |  optional squareup.polygons.Triangle triangle = 2;
          |}
          """.trimMargin())
  }

  private fun writeTriangleProto() {
    fs.add("polygons/src/main/proto/squareup/polygons/triangle.proto", """
          |syntax = "proto2";
          |package squareup.polygons;
          |message Triangle {
          |  repeated double angles = 1;
          |}
          """.trimMargin())
  }

  private fun writeSquareProto() {
    fs.add("polygons/src/main/proto/squareup/polygons/square.proto", """
          |syntax = "proto2";
          |package squareup.polygons;
          |option java_package = "com.squareup.polygons";
          |message Square {
          |  optional double length = 1;
          |}
          """.trimMargin())
  }

  private fun writeRhombusProto() {
    fs.add("polygons/src/main/proto/squareup/polygons/rhombus.proto", """
        |syntax = "proto2";
        |package squareup.polygons;
        |option java_package = "com.squareup.polygons";
        |message Rhombus {
        |  optional double length = 1;
        |  optional double acute_angle = 2;
        |}
        """.trimMargin())
  }
}

