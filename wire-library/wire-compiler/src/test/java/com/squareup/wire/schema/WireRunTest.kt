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
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.testing.add
import com.squareup.wire.testing.find
import com.squareup.wire.testing.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
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
        targets = listOf(JavaTarget(outDirectory = "generated/java"))
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
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
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
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteClient.kt"))
        .contains(
            "class GrpcRouteClient(\n  private val client: GrpcClient\n) : RouteClient",
            "fun GetUpdatedBlue()"
        )
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
            KotlinTarget(
                outDirectory = "generated/kt",
                rpcCallStyle = RpcCallStyle.BLOCKING,
                rpcRole = RpcRole.SERVER
            )
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteBlockingServer.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteBlockingServer.kt"))
        .contains(
            "interface RouteBlockingServer : Service",
            "fun GetUpdatedRed")
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
            KotlinTarget(outDirectory = "generated/kt", singleMethodServices = true))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt",
        "generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt"))
        .contains("class GrpcRouteGetUpdatedBlueClient(\n  private val client: GrpcClient\n) : RouteGetUpdatedBlueClient")
        .doesNotContain("RouteGetUpdatedRedClient")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt"))
        .contains("class GrpcRouteGetUpdatedRedClient(\n  private val client: GrpcClient\n) : RouteGetUpdatedRedClient")
        .doesNotContain("RouteGetUpdatedBlueClient")
  }

  @Test
  fun protoOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(ProtoTarget(outDirectory = "generated/proto"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/proto/squareup/colors/blue.proto",
        "generated/proto/squareup/colors/red.proto")
    assertThat(fs.get("generated/proto/squareup/colors/blue.proto"))
        .contains("message Blue {")
    assertThat(fs.get("generated/proto/squareup/colors/red.proto"))
        .contains("message Red {")
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
            KotlinTarget(
                outDirectory = "generated/kt",
                includes = listOf("squareup.colors.Blue")),
            JavaTarget(
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
            JavaTarget(
                outDirectory = "generated/java",
                includes = listOf("squareup.colors.Blue")),
            KotlinTarget(
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
            KotlinTarget(
                outDirectory = "generated/kt",
                excludes = listOf("squareup.colors.Red")),
            JavaTarget(
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
            KotlinTarget(
                outDirectory = "generated/kt",
                excludes = listOf("squareup.colors.*")),
            JavaTarget(
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
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
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
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
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
            NullTarget(includes = listOf("squareup.colors.Red")),
            KotlinTarget(outDirectory = "generated/kt")
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
            JavaTarget(
                outDirectory = "generated/java",
                includes = listOf("squareup.polygons.Square")),
            KotlinTarget(
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
            JavaTarget(
                outDirectory = "generated/java"),
            KotlinTarget(
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

  @Test
  fun proto3Skipped() {
    writeBlueProto()
    fs.add("colors/src/main/proto/squareup/colors/red.proto", """
          |syntax = "proto3";
          |package squareup.colors;
          |message Red {
          |  string oval = 1;
          |}
          """.trimMargin())
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt")
  }

  @Test
  fun proto3ReadIfPreviewIsTurnedOn() {
    writeBlueProto()
    fs.add("colors/src/main/proto/squareup/colors/red.proto", """
          |syntax = "proto3";
          |package squareup.colors;
          |message Red {
          |  string oval = 1;
          |}
          """.trimMargin())
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
        proto3Preview = true
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/colors/Blue.kt",
        "generated/kt/squareup/colors/Red.kt")
  }

  /**
   * If Wire loaded (and therefore validated) members of dependencies this would fail with a
   * [SchemaException]. But we no longer do this to make Wire both faster and to eliminate the need
   * to place all transitive dependencies in the proto path.
   */
  @Test
  fun onlyDirectDependenciesOfSourcePathRequired() {
    writeBlueProto()
    fs.add("polygons/src/main/proto/squareup/polygons/triangle.proto", """
          |syntax = "proto2";
          |package squareup.polygons;
          |message Triangle {
          |  repeated squareup.geometry.Angle angles = 2; // No such type!
          |}
          """.trimMargin())

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(JavaTarget(outDirectory = "generated/java"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/java/squareup/colors/Blue.java")
    assertThat(fs.get("generated/java/squareup/colors/Blue.java"))
        .contains("public final class Blue extends Message")
  }

  @Test
  fun customOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(CustomTargetBeta(
            outDirectory = "generated/markdown",
            customHandlerClass = MarkdownHandler::class.qualifiedName!!
        ))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/markdown/squareup/colors/Blue.md",
        "generated/markdown/squareup/colors/Red.md")
    assertThat(fs.get("generated/markdown/squareup/colors/Blue.md")).isEqualTo("""
            |# Blue
            |
            |This is the color of the sky.
            |""".trimMargin())
    assertThat(fs.get("generated/markdown/squareup/colors/Red.md")).isEqualTo("""
            |# Red
            |
            |This is the color of the sky when the sky is lava.
            |""".trimMargin())
  }

  @Test
  fun noSuchClass() {
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(CustomTargetBeta(
            outDirectory = "generated/markdown",
            customHandlerClass = "foo"
        ))
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("Couldn't find CustomHandlerClass 'foo'")
    }
  }

  @Test
  fun noPublicConstructor() {
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(CustomTargetBeta(
            outDirectory = "generated/markdown",
            customHandlerClass = "java.lang.Void"
        ))
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("No public constructor on java.lang.Void")
    }
  }

  @Test
  fun classDoesNotImplementCustomHandlerInterface() {
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(CustomTargetBeta(
            outDirectory = "generated/markdown",
            customHandlerClass = "java.lang.Object"
        ))
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("java.lang.Object does not implement CustomHandlerBeta")
    }
  }

  private fun writeRedProto() {
    fs.add("colors/src/main/proto/squareup/colors/red.proto", """
          |syntax = "proto2";
          |package squareup.colors;
          |/** This is the color of the sky when the sky is lava. */
          |message Red {
          |  optional string oval = 1;
          |}
          """.trimMargin())
  }

  @Test
  fun includeSubTypes() {
    writeOrangeProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/colors/Orange.kt")
    assertThat(fs.get("generated/kt/squareup/colors/Orange.kt"))
        .contains("class Orange")
  }

  @Test
  fun includeSubTypesWithPruning() {
    writeOrangeProto()
    writeTriangleProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        treeShakingRoots = listOf("squareup.colors.*"),
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("generated")).containsExactly(
        "generated/kt/squareup/colors/Orange.kt")
    assertThat(fs.get("generated/kt/squareup/colors/Orange.kt"))
        .contains("class Orange")
  }

  private fun writeOrangeProto() {
    fs.add("colors/src/main/proto/squareup/colors/orange.proto", """
          |syntax = "proto2";
          |package squareup.colors;
          |import "squareup/polygons/triangle.proto";
          |message Orange {
          |  optional string circle = 1;
          |  optional squareup.polygons.Triangle.Type triangle = 2;
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
          |/** This is the color of the sky. */
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
          |  enum Type {
          |    EQUILATERAL = 1;
          |    ISOSCELES = 2;
          |    RIGHTANGLED = 3;
          |  }
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

