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

import com.squareup.wire.Message
import com.squareup.wire.StringWireLogger
import com.squareup.wire.WireLogger
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.schema.Target.SchemaHandler
import com.squareup.wire.schema.WireRun.Module
import com.squareup.wire.testing.add
import com.squareup.wire.testing.find
import com.squareup.wire.testing.get
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test

class WireRunTest {
  private val fs = FakeFileSystem()
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

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/routes/RouteClient.kt",
        "generated/kt/squareup/routes/GrpcRouteClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteClient.kt"))
        .contains(
            "interface RouteClient : Service",
            "fun GetUpdatedBlue()"
        )
    assertThat(fs.get("generated/kt/squareup/routes/GrpcRouteClient.kt"))
        .contains(
            "class GrpcRouteClient(\n  private val client: GrpcClient\n) : RouteClient",
            "override fun GetUpdatedBlue()"
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

    assertThat(fs.find("generated")).containsExactlyInAnyOrder(
        "generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt",
        "generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt",
        "generated/kt/squareup/routes/GrpcRouteGetUpdatedBlueClient.kt",
        "generated/kt/squareup/routes/GrpcRouteGetUpdatedRedClient.kt")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt"))
        .contains("interface RouteGetUpdatedBlueClient : Service")
    assertThat(fs.get("generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt"))
        .contains("interface RouteGetUpdatedRedClient : Service")
    assertThat(fs.get("generated/kt/squareup/routes/GrpcRouteGetUpdatedBlueClient.kt"))
        .contains(
            "class GrpcRouteGetUpdatedBlueClient(\n  private val client: GrpcClient\n) : RouteGetUpdatedBlueClient")
        .doesNotContain("RouteGetUpdatedRedClient")
    assertThat(fs.get("generated/kt/squareup/routes/GrpcRouteGetUpdatedRedClient.kt"))
        .contains(
            "class GrpcRouteGetUpdatedRedClient(\n  private val client: GrpcClient\n) : RouteGetUpdatedRedClient")
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
  fun proto3ReadAlways() {
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
  fun optionsOnlyValidatedForPathFiles() {
    writeBlueProto()
    fs.add("polygons/src/main/proto/squareup/polygons/triangle.proto", """
          |syntax = "proto2";
          |package squareup.polygons;
          |option (unicorn) = true; // No such option!
          |message Triangle {
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
            customHandler = MarkdownHandler()
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
    assertThat(assertFailsWith<IllegalArgumentException> {
      callCustomHandler(newCustomHandler("foo"))
    }).hasMessage("Couldn't find CustomHandlerClass 'foo'")
  }

  @Test
  fun noPublicConstructor() {
    assertThat(assertFailsWith<IllegalArgumentException> {
      callCustomHandler(newCustomHandler("java.lang.Void"))
    }).hasMessage("No public constructor on java.lang.Void")
  }

  @Test
  fun classDoesNotImplementCustomHandlerInterface() {
    assertThat(assertFailsWith<IllegalArgumentException> {
      callCustomHandler(newCustomHandler("java.lang.Object"))
    }).hasMessage("java.lang.Object does not implement CustomHandlerBeta")
  }

  class NotSerializableCustomHandler : CustomHandlerBeta {
    override fun newHandler(
      schema: Schema,
      fs: FileSystem,
      outDirectory: String,
      logger: WireLogger,
      profileLoader: ProfileLoader
    ): SchemaHandler {
      throw Exception("hello! this was not serialized")
    }
  }

  /** Confirm that custom handlers don't have to be serialized. */
  @Test
  fun newCustomHandlerIsSerializableEvenIfTargetClassIsNot() {
    val customHandlerA = newCustomHandler(
      "${WireRunTest::class.qualifiedName}${"$"}NotSerializableCustomHandler"
    )
    val customHandlerB = reserialize(customHandlerA)

    assertThat(assertFailsWith<Exception> {
      callCustomHandler(customHandlerB)
    }).hasMessage("hello! this was not serialized")
  }

  class ErrorReportingCustomHandler : CustomHandlerBeta {
    override fun newHandler(
      schema: Schema,
      fs: FileSystem,
      outDirectory: String,
      logger: WireLogger,
      profileLoader: ProfileLoader
    ): SchemaHandler {
      error("unexpected call")
    }

    override fun newHandler(
      schema: Schema,
      fs: FileSystem,
      outDirectory: String,
      logger: WireLogger,
      profileLoader: ProfileLoader,
      errorCollector: ErrorCollector,
    ): SchemaHandler {
      return object : SchemaHandler {
        override fun handle(type: Type): Path? {
          if ("descriptor.proto" in type.location.path) return null // Don't report errors on built-in stuff.
          if (type is MessageType) {
            for (field in type.fields) {
              if (field.name.startsWith("a")) {
                errorCollector.at(field) += "field starts with 'a'"
              }
            }
          }
          return null
        }

        override fun handle(service: Service): List<Path> = listOf()

        override fun handle(extend: Extend, field: Field): Path? = null
      }
    }
  }

  @Test
  fun errorReportingCustomHandler() {
    val customHandler = newCustomHandler(
      "${WireRunTest::class.qualifiedName}${"$"}ErrorReportingCustomHandler"
    )

    assertThat(assertFailsWith<SchemaException> {
      callCustomHandler(customHandler)
    }).hasMessage(
      """
      |field starts with 'a'
      |  for field angles (polygons/src/main/proto/squareup/polygons/triangle.proto:4:3)
      """.trimMargin()
    )
  }

  private fun <T> reserialize(value: T): T {
    val buffer = Buffer()
    ObjectOutputStream(buffer.outputStream()).use {
      it.writeObject(value)
    }
    return ObjectInputStream(buffer.inputStream()).use {
      it.readObject() as T
    }
  }

  private fun callCustomHandler(customHandler: CustomHandlerBeta) {
    writeTriangleProto()
    val schemaLoader = SchemaLoader(fs)
    schemaLoader.initRoots(listOf(Location.get("polygons/src/main/proto")))
    val schema = schemaLoader.loadSchema()
    val errorCollector = ErrorCollector()
    val schemaHandler = customHandler.newHandler(
            schema, fs, "out", StringWireLogger(), schemaLoader, errorCollector
    )
    for (type in schema.types) {
      schemaHandler.handle(schema.getType(type)!!)
    }

    errorCollector.throwIfNonEmpty()
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

  @Test
  fun partitionAcrossFiles() {
    fs.add("protos/one.proto", """
      |syntax = "proto2";
      |message A {}
      |message B {}
      |""".trimMargin())
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("protos")),
        targets = listOf(JavaTarget(outDirectory = "gen")),
        modules = mapOf(
            "a" to Module(pruningRules = PruningRules.Builder()
                .prune("B")
                .build()),
            "b" to Module(dependencies = setOf("a"))
        )
    )
    wireRun.execute(fs, logger)

    assertThat(fs.find("gen/a")).containsExactly("gen/a/A.java")
    assertThat(fs.find("gen/b")).containsExactly("gen/b/B.java")
  }

  @Test
  fun partitionWithOptionsIsNotLinkedTwice() {
    // This test exercises a bug where stub replacement would cause options to get linked twice
    // which would then fail as a duplicate.

    fs.add("protos/one.proto", """
      |syntax = "proto2";
      |package example;
      |
      |import 'google/protobuf/descriptor.proto';
      |
      |extend google.protobuf.MessageOptions {
      |  optional string type = 12000 [(maps_to) = 'test'];
      |}
      |extend google.protobuf.FieldOptions {
      |  optional string maps_to = 123301;
      |}
      |
      |message A {
      |}
      |message B {
      |}
      |""".trimMargin())
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("protos")),
        targets = listOf(JavaTarget(outDirectory = "gen")),
        modules = mapOf(
            "a" to Module(
                pruningRules = PruningRules.Builder()
                    .prune("example.B")
                    .build()
            ),
            "b" to Module(
                dependencies = setOf("a")
            )
        )
    )
    wireRun.execute(fs, logger)

    // TODO(jwilson): fix modules to treat extension fields as first-class objects.
    assertThat(fs.find("gen/a")).containsExactly(
        "gen/a/example/A.java",
        "gen/a/example/MapsToOption.java",
        "gen/a/example/TypeOption.java")
    assertThat(fs.find("gen/b")).containsExactly(
        "gen/b/example/B.java",
        "gen/b/example/MapsToOption.java",
        "gen/b/example/TypeOption.java")
  }

  @Test fun crashWhenTypeGenerationConflicts() {
    fs.add("protos/one/au.proto", """
          |package one;
          |option java_package = "same.package";
          |message Owner {}
          |""".trimMargin())
    fs.add("protos/two/jp.proto", """
          |package two;
          |option java_package = "same.package";
          |message Owner {}
          |""".trimMargin())
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("protos")),
        targets = listOf(JavaTarget(outDirectory = "generated/java"))
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Same type is getting generated by different messages:\n" +
          "  Owner at protos/one/au.proto:3:1\n" +
          "  Owner at protos/two/jp.proto:3:1")
    }
  }

  @Test fun crashWhenServiceGenerationConflicts() {
    writeRedProto()
    fs.add("routes/src/main/proto/squareup/routes1/route.proto", """
          |syntax = "proto2";
          |package squareup.routes;
          |option java_package = "same.package";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |}
          """.trimMargin())
    fs.add("routes/src/main/proto/squareup/routes2/route.proto", """
          |syntax = "proto2";
          |package squareup.routes;
          |option java_package = "same.package";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |}
          """.trimMargin())
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("routes/src/main/proto")),
        protoPath = listOf(Location.get("colors/src/main/proto")),
        targets = listOf(KotlinTarget(outDirectory = "generated/kt"))
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage("Same file is getting generated by different services:\n" +
          "  Route at routes/src/main/proto/squareup/routes1/route.proto:5:1\n" +
          "  Route at routes/src/main/proto/squareup/routes2/route.proto:5:1")
    }
  }

  @Test fun crashOnDependencyCycle() {
    try {
      WireRun(
          sourcePath = emptyList(),
          targets = emptyList(),
          modules = mapOf(
              "one" to Module(dependencies = setOf("two")),
              "two" to Module(dependencies = setOf("three")),
              "three" to Module(dependencies = setOf("one"))
          )
      )
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessage("""
        |ERROR: Modules contain dependency cycle(s):
        | - [one, two, three]
        |""".trimMargin())
    }
  }

  @Test fun crashOnPackageCycle() {
    fs.add("source-path/people/employee.proto", """
        |syntax = "proto2";
        |import "locations/office.proto";
        |import "locations/residence.proto";
        |package people;
        |message Employee {
        |  optional locations.Office office = 1;
        |  optional locations.Residence residence = 2;
        |}
        """.trimMargin())
    fs.add("source-path/locations/office.proto", """
        |syntax = "proto2";
        |import "people/office_manager.proto";
        |package locations;
        |message Office {
        |  optional people.OfficeManager office_manager = 1;
        |}
        """.trimMargin())
    fs.add("source-path/locations/residence.proto", """
        |syntax = "proto2";
        |package locations;
        |message Residence {
        |}
        """.trimMargin())
    fs.add("source-path/people/office_manager.proto", """
        |syntax = "proto2";
        |package people;
        |message OfficeManager {
        |}
        """.trimMargin())

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("source-path")),
        targets = emptyList(),
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (e: SchemaException) {
      assertThat(e).hasMessage("""
        |packages form a cycle:
        |  locations imports people
        |    locations/office.proto:
        |      import "people/office_manager.proto";
        |  people imports locations
        |    people/employee.proto:
        |      import "locations/office.proto";
        |      import "locations/residence.proto";
        """.trimMargin())
    }
  }

  @Test
  fun emitDeclaredOptions() {
    writeDocumentationProto()
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("docs/src/main/proto")),
        targets = listOf(
            JavaTarget(
                outDirectory = "generated/java",
                emitDeclaredOptions = true,
                exclusive = false
            ),
            KotlinTarget(
                outDirectory = "generated/kt",
                emitDeclaredOptions = true,
                exclusive = false
            )
        )
    )
    wireRun.execute(fs, logger)
    assertThat(fs.find("generated")).containsExactly(
        "generated/java/squareup/options/DocumentationUrlOption.java",
        "generated/kt/squareup/options/DocumentationUrlOption.kt")
    assertThat(fs.get("generated/java/squareup/options/DocumentationUrlOption.java"))
        .contains("public @interface DocumentationUrlOption")
    assertThat(fs.get("generated/kt/squareup/options/DocumentationUrlOption.kt"))
        .contains("annotation class DocumentationUrlOption")
  }

  @Test
  fun skipDeclaredOptions() {
    writeDocumentationProto()
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("docs/src/main/proto")),
        targets = listOf(
            JavaTarget(
                outDirectory = "generated/java",
                emitDeclaredOptions = false,
                exclusive = false
            ),
            KotlinTarget(
                outDirectory = "generated/kt",
                emitDeclaredOptions = false,
                exclusive = false
            ))
    )
    wireRun.execute(fs, logger)
    assertThat(fs.find("generated")).isEmpty()
  }

  @Test
  fun emitAppliedOptions() {
    writeDocumentationProto()
    writeOctagonProto()
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        protoPath = listOf(Location.get("docs/src/main/proto")),
        targets = listOf(
            JavaTarget(
                outDirectory = "generated/java",
                emitAppliedOptions = true,
                exclusive = false
            ),
            KotlinTarget(
                outDirectory = "generated/kt",
                emitAppliedOptions = true,
                exclusive = false
            )
        )
    )
    wireRun.execute(fs, logger)
    assertThat(fs.find("generated")).containsExactly(
        "generated/java/squareup/polygons/Octagon.java",
        "generated/kt/squareup/polygons/Octagon.kt")
    assertThat(fs.get("generated/java/squareup/polygons/Octagon.java"))
        .contains("@DocumentationUrlOption(\"https://en.wikipedia.org/wiki/Octagon\")")
    assertThat(fs.get("generated/kt/squareup/polygons/Octagon.kt"))
        .contains("@DocumentationUrlOption(\"https://en.wikipedia.org/wiki/Octagon\")")
  }

  @Test
  fun skipAppliedOptions() {
    writeDocumentationProto()
    writeOctagonProto()
    val wireRun = WireRun(
        sourcePath = listOf(Location.get("polygons/src/main/proto")),
        protoPath = listOf(Location.get("docs/src/main/proto")),
        targets = listOf(
            JavaTarget(
                outDirectory = "generated/java",
                emitAppliedOptions = false,
                exclusive = false
            ),
            KotlinTarget(
                outDirectory = "generated/kt",
                emitAppliedOptions = false,
                exclusive = false
            )
        )
    )
    wireRun.execute(fs, logger)
    assertThat(fs.find("generated")).containsExactly(
        "generated/java/squareup/polygons/Octagon.java",
        "generated/kt/squareup/polygons/Octagon.kt")
    assertThat(fs.get("generated/java/squareup/polygons/Octagon.java"))
        .doesNotContain("@DocumentationUrl")
    assertThat(fs.get("generated/kt/squareup/polygons/Octagon.kt"))
        .doesNotContain("@DocumentationUrl")
  }

  @Test
  fun importNotFoundIncludesReferencingFile() {
    writeBlueProto()
    writeSquareProto()

    val wireRun = WireRun(
        sourcePath = listOf(Location.get("colors/src/main/proto")),
        protoPath = listOf(Location.get("polygons/src/main/proto")),
        targets = listOf(NullTarget())
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage("""
          |unable to find squareup/polygons/triangle.proto
          |  searching 1 proto paths:
          |    polygons/src/main/proto
          |  for file colors/src/main/proto/squareup/colors/blue.proto
          |unable to resolve squareup.polygons.Triangle
          |  for field triangle (colors/src/main/proto/squareup/colors/blue.proto:7:3)
          |  in message squareup.colors.Blue (colors/src/main/proto/squareup/colors/blue.proto:5:1)
          """.trimMargin())
    }
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

  private fun writeDocumentationProto() {
    fs.add("docs/src/main/proto/squareup/options/documentation.proto", """
        |syntax = "proto2";
        |package squareup.options;
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  optional string documentation_url = 22200;
        |}
        """.trimMargin())
  }

  private fun writeOctagonProto() {
    fs.add("polygons/src/main/proto/squareup/polygons/octagon.proto", """
        |syntax = "proto2";
        |package squareup.polygons;
        |import "squareup/options/documentation.proto";
        |
        |message Octagon {
        |  option (documentation_url) = "https://en.wikipedia.org/wiki/Octagon";
        |  optional bool stop = 1;
        |}
        """.trimMargin())
  }
}

