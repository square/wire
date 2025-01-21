/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.wire.schema

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.prop
import com.squareup.wire.StringWireLogger
import com.squareup.wire.WireLogger
import com.squareup.wire.WireLogger.Companion.NONE
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.schema.WireRun.Module
import com.squareup.wire.schema.internal.TypeMover.Move
import com.squareup.wire.testing.add
import com.squareup.wire.testing.containsExactlyInAnyOrderAsRelativePaths
import com.squareup.wire.testing.findFiles
import com.squareup.wire.testing.readUtf8
import kotlin.test.assertFailsWith
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assert.fail
import org.junit.Test

class WireRunTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }
  private val logger = StringWireLogger()

  @Test
  fun javaOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(JavaTarget(outDirectory = "generated/java")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Blue.java",
      "generated/java/squareup/colors/Red.java",
    )
    assertThat(fs.readUtf8("generated/java/squareup/colors/Blue.java"))
      .contains("public final class Blue extends Message")
    assertThat(fs.readUtf8("generated/java/squareup/colors/Red.java"))
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
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
      "generated/kt/squareup/colors/Red.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .contains("class Blue")
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Red.kt"))
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
      protoPath = listOf(
        Location.get("colors/src/main/proto"),
        Location.get("polygons/src/main/proto"),
      ),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/routes/RouteClient.kt",
      "generated/kt/squareup/routes/GrpcRouteClient.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/routes/RouteClient.kt"))
      .contains(
        "interface RouteClient : Service",
        "fun GetUpdatedBlue()",
      )
    assertThat(fs.readUtf8("generated/kt/squareup/routes/GrpcRouteClient.kt"))
      .contains(
        "class GrpcRouteClient(\n  private val client: GrpcClient,\n) : RouteClient",
        "override fun GetUpdatedBlue()",
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
      protoPath = listOf(
        Location.get("colors/src/main/proto"),
        Location.get("polygons/src/main/proto"),
      ),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          rpcCallStyle = RpcCallStyle.BLOCKING,
          rpcRole = RpcRole.SERVER,
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/routes/RouteBlockingServer.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/routes/RouteBlockingServer.kt")).all {
      contains(
        "interface RouteBlockingServer : Service",
        "fun GetUpdatedRed",
      )
      doesNotContain("suspend fun GetUpdatedRed")
    }
  }

  @Test
  fun ktOnlyWithServiceAsSingleMethod() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    writeColorsRouteProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("routes/src/main/proto")),
      protoPath = listOf(
        Location.get("colors/src/main/proto"),
        Location.get("polygons/src/main/proto"),
      ),
      targets = listOf(
        KotlinTarget(outDirectory = "generated/kt", singleMethodServices = true),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt",
      "generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt",
      "generated/kt/squareup/routes/GrpcRouteGetUpdatedBlueClient.kt",
      "generated/kt/squareup/routes/GrpcRouteGetUpdatedRedClient.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/routes/RouteGetUpdatedBlueClient.kt"))
      .contains("interface RouteGetUpdatedBlueClient : Service")
    assertThat(fs.readUtf8("generated/kt/squareup/routes/RouteGetUpdatedRedClient.kt"))
      .contains("interface RouteGetUpdatedRedClient : Service")
    assertThat(fs.readUtf8("generated/kt/squareup/routes/GrpcRouteGetUpdatedBlueClient.kt")).all {
      contains(
        "class GrpcRouteGetUpdatedBlueClient(\n  private val client: GrpcClient,\n) : RouteGetUpdatedBlueClient",
      )
      doesNotContain("RouteGetUpdatedRedClient")
    }
    assertThat(fs.readUtf8("generated/kt/squareup/routes/GrpcRouteGetUpdatedRedClient.kt")).all {
      contains(
        "class GrpcRouteGetUpdatedRedClient(\n  private val client: GrpcClient,\n) : RouteGetUpdatedRedClient",
      )
      doesNotContain("RouteGetUpdatedBlueClient")
    }
  }

  @Test
  fun protoOnly() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(ProtoTarget(outDirectory = "generated/proto")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/proto/squareup/colors/blue.proto",
      "generated/proto/squareup/colors/red.proto",
    )
    assertThat(fs.readUtf8("generated/proto/squareup/colors/blue.proto"))
      .contains("message Blue {")
    assertThat(fs.readUtf8("generated/proto/squareup/colors/red.proto"))
      .contains("message Red {")
  }

  @Test
  fun protoTargetNeverEmitsGoogleProtobufDescriptor() {
    writeSquareProto()
    writeMinimalGoogleProtobufProtos()
    writeMinimalWireProtos()

    val wireRun = WireRun(
      sourcePath = listOf(
        Location.get("polygons/src/main/proto"),
        Location.get("google/src/main/proto"),
        Location.get("wire/src/main/proto"),
      ),
      targets = listOf(ProtoTarget(outDirectory = "generated/proto")),
    )
    wireRun.execute(fs, logger)

    // We're happy if google.protobuf.descriptor isn't here.
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/proto/squareup/polygons/square.proto",
    )
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
          includes = listOf("squareup.colors.Blue"),
        ),
        JavaTarget(
          outDirectory = "generated/java",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Red.java",
      "generated/kt/squareup/colors/Blue.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .contains("class Blue")
    assertThat(fs.readUtf8("generated/java/squareup/colors/Red.java"))
      .contains("public final class Red extends Message")
  }

  @Test
  fun opaqueBeforeGeneratingKtThenJava() {
    writeBlueProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      opaqueTypes = listOf("squareup.polygons.Triangle"),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt")).all {
      contains("class Blue")
      // The type `Triangle` has been opaqued.
      contains("public val triangle: ByteString? = null")
    }
  }

  @Test
  fun noSuchClassEventListener() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newEventListenerFactory("foo").create()
      },
    ).hasMessage("Couldn't find EventListenerClass 'foo'")
  }

  @Test
  fun noPublicConstructorEventListener() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newEventListenerFactory("java.lang.Void").create()
      },
    ).hasMessage("No public constructor on java.lang.Void")
  }

  @Test
  fun classDoesNotImplementEventListenerInterface() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newEventListenerFactory("java.lang.Object").create()
      },
    ).hasMessage("java.lang.Object does not implement EventListener.Factory")
  }

  @Test
  fun myEventListenerSuccess() {
    val listenerA = MyEventListener()
    val listenerB = MyEventListener()
    val listeners = listOf(listenerA, listenerB)

    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    val wireRun = WireRun(
      treeShakingRubbish = listOf("not.existing"),
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          includes = listOf("squareup.colors.Blue"),
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
        ),
      ),
      eventListeners = listeners,
      rejectUnusedRootsOrPrunes = false,
    )
    wireRun.execute(fs, logger)

    listeners.forEach { listener ->
      assertThat(listener.takeLog()).isEqualTo("runStart")
      assertThat(listener.takeLog()).isEqualTo("loadSchemaStart")
      assertThat(listener.takeLog()).isEqualTo("loadSchemaSuccess")
      assertThat(listener.takeLog()).isEqualTo("treeShakeStart")
      assertThat(listener.takeLog()).isEqualTo("treeShakeEnd")
      assertThat(listener.takeLog()).isEqualTo("moveTypesStart")
      assertThat(listener.takeLog()).isEqualTo("moveTypesEnd")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlersStart")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlerStart")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlerEnd")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlerStart")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlerEnd")
      assertThat(listener.takeLog()).isEqualTo("schemaHandlersEnd")
      assertThat(listener.takeLog()).isEqualTo("runSuccess")
      listener.assertAllLogsAreConsumed()
    }
  }

  class MyEventListener : EventListener() {
    private val logs = ArrayDeque<String>()

    fun takeLog() = logs.removeFirst()

    fun assertAllLogsAreConsumed() {
      if (logs.isNotEmpty()) {
        throw AssertionError("Unconsumed logs: ${logs.joinToString(", ")}")
      }
    }

    override fun runStart(wireRun: WireRun) {
      logs.add("runStart")
    }

    override fun runSuccess(wireRun: WireRun) {
      logs.add("runSuccess")
    }

    override fun runFailed(errors: List<String>) {
      logs.add("runFailed")
    }

    override fun loadSchemaStart() {
      logs.add("loadSchemaStart")
    }

    override fun loadSchemaSuccess(schema: Schema) {
      logs.add("loadSchemaSuccess")
    }

    override fun treeShakeStart(
      schema: Schema,
      pruningRules: PruningRules,
    ) {
      logs.add("treeShakeStart")
    }

    override fun treeShakeEnd(
      refactoredSchema: Schema,
      pruningRules: PruningRules,
    ) {
      logs.add("treeShakeEnd")
    }

    override fun moveTypesStart(
      schema: Schema,
      moves: List<Move>,
    ) {
      logs.add("moveTypesStart")
    }

    override fun moveTypesEnd(
      refactoredSchema: Schema,
      moves: List<Move>,
    ) {
      logs.add("moveTypesEnd")
    }

    override fun schemaHandlersStart() {
      logs.add("schemaHandlersStart")
    }

    override fun schemaHandlersEnd() {
      logs.add("schemaHandlersEnd")
    }

    override fun schemaHandlerStart(
      schemaHandler: SchemaHandler,
      emittingRules: EmittingRules,
    ) {
      logs.add("schemaHandlerStart")
    }

    override fun schemaHandlerEnd(
      schemaHandler: SchemaHandler,
      emittingRules: EmittingRules,
    ) {
      logs.add("schemaHandlerEnd")
    }

    class Factory : EventListener.Factory {
      override fun create(): EventListener = MyEventListener()
    }
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
          includes = listOf("squareup.colors.Blue"),
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Blue.java",
      "generated/kt/squareup/colors/Red.kt",
    )
    assertThat(fs.readUtf8("generated/java/squareup/colors/Blue.java"))
      .contains("public final class Blue extends Message")
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Red.kt"))
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
        Location.get("polygons/src/main/proto"),
      ),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          excludes = listOf("squareup.colors.Red"),
        ),
        JavaTarget(
          outDirectory = "generated/java",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
      "generated/java/squareup/colors/Red.java",
      "generated/kt/squareup/polygons/Triangle.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .contains("class Blue")
    assertThat(fs.readUtf8("generated/java/squareup/colors/Red.java"))
      .contains("public final class Red extends Message")
    assertThat(fs.readUtf8("generated/kt/squareup/polygons/Triangle.kt"))
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
        Location.get("polygons/src/main/proto"),
      ),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          excludes = listOf("squareup.colors.*"),
        ),
        JavaTarget(
          outDirectory = "generated/java",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Blue.java",
      "generated/java/squareup/colors/Red.java",
      "generated/kt/squareup/polygons/Triangle.kt",
    )
    assertThat(fs.readUtf8("generated/java/squareup/colors/Blue.java"))
      .contains("public final class Blue extends Message")
    assertThat(fs.readUtf8("generated/java/squareup/colors/Red.java"))
      .contains("public final class Red extends Message")
    assertThat(fs.readUtf8("generated/kt/squareup/polygons/Triangle.kt"))
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
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
    )
  }

  @Test
  fun unusedTreeShakingRoots() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      treeShakingRoots = listOf(
        "squareup.colors.Blue",
        "squareup.colors.Purple",
        "squareup.colors.Color#name",
      ),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
        "Unused element(s) in roots:\n" +
          "  squareup.colors.Purple\n" +
          "  squareup.colors.Color#name",
      )
    }
  }

  @Test
  fun unusedTreeShakingPrunes() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      treeShakingRoots = listOf("squareup.colors.Blue"),
      treeShakingRubbish = listOf("squareup.colors.Purple", "squareup.colors.Color#name"),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
        "Unused element(s) in prunes:\n" +
          "  squareup.colors.Purple\n" +
          "  squareup.colors.Color#name",
      )
    }
  }

  @Test
  fun unusedTreeShakingRootsAndPrunes() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      treeShakingRoots = listOf("squareup.colors.Blue", "squareup.colors.Green"),
      treeShakingRubbish = listOf("squareup.colors.Purple", "squareup.colors.Color#name"),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
        "Unused element(s) in roots:\n" +
          "  squareup.colors.Green\n" +
          "Unused element(s) in prunes:\n" +
          "  squareup.colors.Purple\n" +
          "  squareup.colors.Color#name",
      )
    }
  }

  /** Confirm we can disable the `rejectUnusedRootsOrPrunes` check. */
  @Test
  fun permitUnusedRootsOrPrunes() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      treeShakingRoots = listOf("squareup.colors.Blue", "squareup.colors.Green"),
      treeShakingRubbish = listOf("squareup.colors.Purple", "squareup.colors.Color#name"),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
      rejectUnusedRootsOrPrunes = false,
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
    )
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
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
    )
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
          includes = listOf("squareup.polygons.Square"),
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/com/squareup/polygons/Square.java",
      "generated/kt/com/squareup/polygons/Rhombus.kt",
    )
  }

  @Test
  fun nonExclusiveTypeEmittedTwice() {
    writeSquareProto()
    writeRhombusProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(
        JavaTarget(
          outDirectory = "generated/java",
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
          exclusive = false,
          includes = listOf("squareup.polygons.Square"),
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/com/squareup/polygons/Square.java",
      "generated/java/com/squareup/polygons/Rhombus.java",
      "generated/kt/com/squareup/polygons/Square.kt",
    )
  }

  @Test
  fun proto3ReadAlways() {
    writeBlueProto()
    fs.add(
      "colors/src/main/proto/squareup/colors/red.proto",
      """
          |syntax = "proto3";
          |package squareup.colors;
          |message Red {
          |  string oval = 1;
          |}
      """.trimMargin(),
    )
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
      "generated/kt/squareup/colors/Red.kt",
    )
  }

  /**
   * If Wire loaded (and therefore validated) members of dependencies this would fail with a
   * [SchemaException]. But we no longer do this to make Wire both faster and to eliminate the need
   * to place all transitive dependencies in the proto path.
   */
  @Test
  fun onlyDirectDependenciesOfSourcePathRequired() {
    writeBlueProto()
    fs.add(
      "polygons/src/main/proto/squareup/polygons/triangle.proto",
      """
          |syntax = "proto2";
          |package squareup.polygons;
          |message Triangle {
          |  repeated squareup.geometry.Angle angles = 2; // No such type!
          |}
      """.trimMargin(),
    )

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(JavaTarget(outDirectory = "generated/java")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Blue.java",
    )
    assertThat(fs.readUtf8("generated/java/squareup/colors/Blue.java"))
      .contains("public final class Blue extends Message")
  }

  @Test
  fun optionsOnlyValidatedForPathFiles() {
    writeBlueProto()
    fs.add(
      "polygons/src/main/proto/squareup/polygons/triangle.proto",
      """
          |syntax = "proto2";
          |package squareup.polygons;
          |option (unicorn) = true; // No such option!
          |message Triangle {
          |}
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(JavaTarget(outDirectory = "generated/java")),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/colors/Blue.java",
    )
    assertThat(fs.readUtf8("generated/java/squareup/colors/Blue.java"))
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
      targets = listOf(
        CustomTarget(
          outDirectory = "generated/markdown",
          schemaHandlerFactory = MarkdownHandlerFactory(),
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/markdown/squareup/colors/Blue.md",
      "generated/markdown/squareup/colors/Red.md",
    )
    assertThat(fs.readUtf8("generated/markdown/squareup/colors/Blue.md")).isEqualTo(
      """
            |# Blue
            |
            |This is the color of the sky.
            |
      """.trimMargin(),
    )
    assertThat(fs.readUtf8("generated/markdown/squareup/colors/Red.md")).isEqualTo(
      """
            |# Red
            |
            |This is the color of the sky when the sky is lava.
            |
      """.trimMargin(),
    )
  }

  object NullSchemaHandler : SchemaHandler() {
    override fun handle(schema: Schema, context: Context) {
    }
    override fun handle(type: Type, context: Context) = null
    override fun handle(service: Service, context: Context) = listOf<Path>()
    override fun handle(extend: Extend, field: Field, context: Context) = null

    object Factory : SchemaHandler.Factory {
      override fun create(
        includes: List<String>,
        excludes: List<String>,
        exclusive: Boolean,
        outDirectory: String,
        options: Map<String, String>,
      ): SchemaHandler = NullSchemaHandler
    }
  }

  /**
   * We had a bug where custom handlers that don't need [SchemaHandler.Context.emittingRules], would
   * trigger an annoying warning like this:
   *
   * ```
   * Unused includes in targets:
   *   *
   * ```
   *
   * The '*' here is the default includes rule, which isn't really the user's fault.
   */
  @Test
  fun noUnusedIncludesWarningOnStar() {
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(
        CustomTarget(
          outDirectory = "generated/out",
          schemaHandlerFactory = NullSchemaHandler.Factory,
        ),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(logger.log).isEmpty()
  }

  @Test
  fun loadExhaustively() {
    writeBlueProto()
    writeRedProto()
    writeTriangleProto()

    lateinit var handledSchema: Schema

    class CustomSchemaHandler : SchemaHandler() {

      override fun handle(schema: Schema, context: Context) {
        handledSchema = schema
        super.handle(schema, context)
      }

      override fun handle(type: Type, context: Context): Path? = null

      override fun handle(service: Service, context: Context): List<Path> = emptyList()

      override fun handle(extend: Extend, field: Field, context: Context): Path? = null
    }

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      loadExhaustively = true,
      targets = listOf(
        CustomTarget(
          outDirectory = "generated/out",
          schemaHandlerFactory = object : SchemaHandler.Factory {
            override fun create(
              includes: List<String>,
              excludes: List<String>,
              exclusive: Boolean,
              outDirectory: String,
              options: Map<String, String>,
            ): SchemaHandler = CustomSchemaHandler()
          },
        ),
      ),
    )

    wireRun.execute(fs, logger)

    // These fields would not be present by default, but are linked when using `loadExhaustively`.
    assertThat(handledSchema)
      .transform("Square type") { it.getType("squareup.polygons.Triangle") as MessageType }
      .prop(MessageType::fields)
      .isNotEmpty()
  }

  @Test
  fun noSuchClass() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        callCustomHandler(newSchemaHandler("foo"))
      },
    ).hasMessage("Couldn't find SchemaHandlerClass 'foo'")
  }

  @Test
  fun noPublicConstructor() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        callCustomHandler(newSchemaHandler("java.lang.Void"))
      },
    ).hasMessage("No public constructor on java.lang.Void")
  }

  @Test
  fun classDoesNotImplementCustomHandlerInterface() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        callCustomHandler(newSchemaHandler("java.lang.Object"))
      },
    ).hasMessage("java.lang.Object does not implement SchemaHandler.Factory")
  }

  class ErrorReportingCustomHandler : SchemaHandler.Factory {
    override fun create(
      includes: List<String>,
      excludes: List<String>,
      exclusive: Boolean,
      outDirectory: String,
      options: Map<String, String>,
    ): SchemaHandler {
      return object : SchemaHandler() {
        override fun handle(type: Type, context: SchemaHandler.Context): Path? {
          val errorCollector = context.errorCollector
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

        override fun handle(service: Service, context: SchemaHandler.Context): List<Path> = listOf()

        override fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path? =
          null
      }
    }
  }

  @Test
  fun errorReportingCustomHandler() {
    val customHandler = newSchemaHandler(
      "${WireRunTest::class.qualifiedName}${"$"}ErrorReportingCustomHandler",
    )

    assertThat(
      assertFailsWith<SchemaException> {
        callCustomHandler(customHandler)
      },
    ).hasMessage(
      """
        |field starts with 'a'
        |  for field angles (polygons/src/main/proto/squareup/polygons/triangle.proto:4:3)
      """.trimMargin(),
    )
  }

  private fun callCustomHandler(schemaHandlerFactory: SchemaHandler.Factory) {
    writeTriangleProto()
    val schemaLoader = SchemaLoader(fs)
    schemaLoader.initRoots(listOf(Location.get("polygons/src/main/proto")))
    val schema = schemaLoader.loadSchema()
    val errorCollector = ErrorCollector()
    schemaHandlerFactory
      .create(
        includes = listOf(),
        excludes = listOf(),
        exclusive = true,
        outDirectory = "",
        options = mapOf(),
      )
      .handle(
        schema = schema,
        context = SchemaHandler.Context(
          fileSystem = fs,
          outDirectory = "out".toPath(),
          logger = EmptyWireLogger(),
          errorCollector = errorCollector,
          claimedPaths = ClaimedPaths(),
        ),
      )

    errorCollector.throwIfNonEmpty()
  }

  private fun writeRedProto() {
    fs.add(
      "colors/src/main/proto/squareup/colors/red.proto",
      """
          |syntax = "proto2";
          |package squareup.colors;
          |/** This is the color of the sky when the sky is lava. */
          |message Red {
          |  optional string oval = 1;
          |}
      """.trimMargin(),
    )
  }

  @Test
  fun includeSubTypes() {
    writeOrangeProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Orange.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Orange.kt"))
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
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Orange.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Orange.kt"))
      .contains("class Orange")
  }

  @Test
  fun partitionAcrossFiles() {
    fs.add(
      "protos/one.proto",
      """
      |syntax = "proto2";
      |message A {}
      |message B {}
      |
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("protos")),
      targets = listOf(JavaTarget(outDirectory = "gen")),
      modules = mapOf(
        "a" to Module(
          pruningRules = PruningRules.Builder()
            .prune("B")
            .build(),
        ),
        "b" to Module(dependencies = setOf("a")),
      ),
    )
    wireRun.execute(fs, logger)

    assertThat(fs.findFiles("gen/a")).containsExactlyInAnyOrderAsRelativePaths("gen/a/A.java")
    assertThat(fs.findFiles("gen/b")).containsExactlyInAnyOrderAsRelativePaths("gen/b/B.java")
  }

  @Test
  fun partitionWithOptionsIsNotLinkedTwice() {
    // This test exercises a bug where stub replacement would cause options to get linked twice
    // which would then fail as a duplicate.

    fs.add(
      "protos/one.proto",
      """
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
      |
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("protos")),
      targets = listOf(JavaTarget(outDirectory = "gen")),
      modules = mapOf(
        "a" to Module(
          pruningRules = PruningRules.Builder()
            .prune("example.B")
            .build(),
        ),
        "b" to Module(
          dependencies = setOf("a"),
        ),
      ),
    )
    wireRun.execute(fs, logger)

    // TODO(jwilson): fix modules to treat extension fields as first-class objects.
    assertThat(fs.findFiles("gen/a")).containsExactlyInAnyOrderAsRelativePaths(
      "gen/a/example/A.java",
      "gen/a/example/MapsToOption.java",
      "gen/a/example/TypeOption.java",
    )
    assertThat(fs.findFiles("gen/b")).containsExactlyInAnyOrderAsRelativePaths(
      "gen/b/example/B.java",
      "gen/b/example/MapsToOption.java",
      "gen/b/example/TypeOption.java",
    )
  }

  @Test fun crashWhenTypeGenerationConflicts() {
    fs.add(
      "protos/one/au.proto",
      """
          |package one;
          |option java_package = "same.package";
          |message Owner {}
          |
      """.trimMargin(),
    )
    fs.add(
      "protos/two/jp.proto",
      """
          |package two;
          |option java_package = "same.package";
          |message Owner {}
          |
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("protos")),
      targets = listOf(JavaTarget(outDirectory = "generated/java")),
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
        "Same file generated/java/same/package/Owner.java is getting generated by different messages:\n" +
          "  Owner at protos/one/au.proto:3:1\n" +
          "  Owner at protos/two/jp.proto:3:1",
      )
    }
  }

  @Test fun optionNameIsPreciseToAvoidConflict_java() {
    fs.add(
      "protos/zero/zero.proto",
      """
          |package zero;
          |option java_package = "same.package";
          |import "google/protobuf/descriptor.proto";
          |extend google.protobuf.FieldOptions {
          |  optional string documentation_url = 60001;
          |}
          |extend google.protobuf.MessageOptions {
          |  optional string documentation_url = 60002;
          |}
          |
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("protos")),
      targets = listOf(
        JavaTarget(
          outDirectory = "generated/java",
          emitDeclaredOptions = true,
          emitAppliedOptions = true,
        ),
      ),
    )

    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/same/package/DocumentationUrlMessageOption.java",
      "generated/java/same/package/DocumentationUrlOption.java",
    )
  }

  @Test fun optionNameIsPreciseToAvoidConflict_kotlin() {
    fs.add(
      "protos/zero/zero.proto",
      """
          |package zero;
          |option java_package = "same.package";
          |import "google/protobuf/descriptor.proto";
          |extend google.protobuf.FieldOptions {
          |  optional string documentation_url = 60001;
          |}
          |extend google.protobuf.MessageOptions {
          |  optional string documentation_url = 60002;
          |}
          |
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("protos")),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kotlin",
          emitDeclaredOptions = true,
          emitAppliedOptions = true,
        ),
      ),
    )

    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kotlin/same/package/DocumentationUrlMessageOption.kt",
      "generated/kotlin/same/package/DocumentationUrlOption.kt",
    )
  }

  @Test fun javaDoesNotClaimServices() {
    writeRedProto()
    fs.add(
      "routes/src/main/proto/squareup/routes1/route.proto",
      """
          |syntax = "proto2";
          |package squareup.routes;
          |option java_package = "same.package";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |}
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("routes/src/main/proto")),
      protoPath = listOf(Location.get("colors/src/main/proto")),
      targets = listOf(
        JavaTarget(outDirectory = "generate/java"),
        KotlinTarget(outDirectory = "generated/kt"),
      ),
    )

    wireRun.execute(fs, logger)
    // We had a bug where the Java target, even though it doesn't generate them, would claim the
    // services and prevent the Kotlin target to generate them. Asserting that we have services
    // generated in Kotlin confirms the fix.
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/same/package/GrpcRouteClient.kt",
      "generated/kt/same/package/RouteClient.kt",
    )
  }

  @Test fun crashWhenServiceGenerationConflicts() {
    writeRedProto()
    fs.add(
      "routes/src/main/proto/squareup/routes1/route.proto",
      """
          |syntax = "proto2";
          |package squareup.routes;
          |option java_package = "same.package";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |}
      """.trimMargin(),
    )
    fs.add(
      "routes/src/main/proto/squareup/routes2/route.proto",
      """
          |syntax = "proto2";
          |package squareup.routes;
          |option java_package = "same.package";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |}
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("routes/src/main/proto")),
      protoPath = listOf(Location.get("colors/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt", exclusive = false)),
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessage(
        "Same file generated/kt/same/package/RouteClient.kt is getting generated by different services:\n" +
          "  Route at routes/src/main/proto/squareup/routes1/route.proto:5:1\n" +
          "  Route at routes/src/main/proto/squareup/routes2/route.proto:5:1",
      )
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
          "three" to Module(dependencies = setOf("one")),
        ),
      ).execute(fs, NONE)
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessage(
        """
        |ERROR: Modules contain dependency cycle(s):
        | - [one, two, three]
        |
        """.trimMargin(),
      )
    }
  }

  @Test fun crashOnPackageCycle() {
    fs.add(
      "source-path/people/employee.proto",
      """
        |syntax = "proto2";
        |import "locations/office.proto";
        |import "locations/residence.proto";
        |package people;
        |message Employee {
        |  optional locations.Office office = 1;
        |  optional locations.Residence residence = 2;
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/locations/office.proto",
      """
        |syntax = "proto2";
        |import "people/office_manager.proto";
        |package locations;
        |message Office {
        |  optional people.OfficeManager office_manager = 1;
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/locations/residence.proto",
      """
        |syntax = "proto2";
        |package locations;
        |message Residence {
        |}
      """.trimMargin(),
    )
    fs.add(
      "source-path/people/office_manager.proto",
      """
        |syntax = "proto2";
        |package people;
        |message OfficeManager {
        |}
      """.trimMargin(),
    )

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("source-path")),
      targets = emptyList(),
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (e: SchemaException) {
      assertThat(e).hasMessage(
        """
        |packages form a cycle:
        |  locations imports people
        |    locations/office.proto:
        |      import "people/office_manager.proto";
        |  people imports locations
        |    people/employee.proto:
        |      import "locations/office.proto";
        |      import "locations/residence.proto";
        """.trimMargin(),
      )
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
          exclusive = false,
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
          emitDeclaredOptions = true,
          exclusive = false,
        ),
      ),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/options/DocumentationUrlOption.java",
      "generated/kt/squareup/options/DocumentationUrlOption.kt",
    )
    assertThat(fs.readUtf8("generated/java/squareup/options/DocumentationUrlOption.java"))
      .contains("public @interface DocumentationUrlOption")
    assertThat(fs.readUtf8("generated/kt/squareup/options/DocumentationUrlOption.kt"))
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
          exclusive = false,
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
          emitDeclaredOptions = false,
          exclusive = false,
        ),
      ),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths()
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
          exclusive = false,
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
          emitAppliedOptions = true,
          exclusive = false,
        ),
      ),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/polygons/Octagon.java",
      "generated/kt/squareup/polygons/Octagon.kt",
    )
    assertThat(fs.readUtf8("generated/java/squareup/polygons/Octagon.java"))
      .contains("@DocumentationUrlOption(\"https://en.wikipedia.org/wiki/Octagon\")")
    assertThat(fs.readUtf8("generated/kt/squareup/polygons/Octagon.kt"))
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
          exclusive = false,
        ),
        KotlinTarget(
          outDirectory = "generated/kt",
          emitAppliedOptions = false,
          exclusive = false,
        ),
      ),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/java/squareup/polygons/Octagon.java",
      "generated/kt/squareup/polygons/Octagon.kt",
    )
    assertThat(fs.readUtf8("generated/java/squareup/polygons/Octagon.java"))
      .doesNotContain("@DocumentationUrl")
    assertThat(fs.readUtf8("generated/kt/squareup/polygons/Octagon.kt"))
      .doesNotContain("@DocumentationUrl")
  }

  @Test
  fun importNotFoundIncludesReferencingFile() {
    writeBlueProto()
    writeSquareProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )

    try {
      wireRun.execute(fs, logger)
      fail()
    } catch (expected: SchemaException) {
      assertThat(expected).hasMessage(
        """
          |unable to find squareup/polygons/triangle.proto
          |  searching 1 proto paths:
          |    polygons/src/main/proto
          |  for file colors/src/main/proto/squareup/colors/blue.proto
          |unable to resolve squareup.polygons.Triangle
          |  for field triangle (colors/src/main/proto/squareup/colors/blue.proto:7:3)
          |  in message squareup.colors.Blue (colors/src/main/proto/squareup/colors/blue.proto:5:1)
        """.trimMargin(),
      )
    }
  }

  /** We had a bug where extension fields names needed to be globally unique. */
  @Test
  fun conflictingExtends() {
    writeSquareProto()
    writeTriangleProto()
    fs.add(
      "polygons/src/main/proto/squareup/polygons/conflicting_extends.proto",
      """
        |syntax = "proto2";
        |package squareup.options;
        |import "squareup/polygons/square.proto";
        |import "squareup/polygons/triangle.proto";
        |
        |extend squareup.polygons.Square {
        |  optional string documentation_url = 22201;
        |}
        |
        |extend squareup.polygons.Triangle {
        |  optional string documentation_url = 22202;
        |}
      """.trimMargin(),
    )
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/com/squareup/polygons/Square.kt",
      "generated/kt/squareup/polygons/Triangle.kt",
    )
    assertThat(fs.readUtf8("generated/kt/com/squareup/polygons/Square.kt"))
      .contains("public val documentation_url: String")
    assertThat(fs.readUtf8("generated/kt/squareup/polygons/Triangle.kt"))
      .contains("public val documentation_url: String")
  }

  @Test
  fun emitProtoReader32() {
    writeBlueProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          emitProtoReader32 = true,
        ),
      ),
    )

    wireRun.execute(fs, logger)
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .contains("override fun decode(reader: ProtoReader32): Blue")
  }

  @Test
  fun skipProtoReader32() {
    writeBlueProto()
    writeTriangleProto()

    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(
        KotlinTarget(
          outDirectory = "generated/kt",
          emitProtoReader32 = false,
        ),
      ),
    )

    wireRun.execute(fs, logger)
    println(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .doesNotContain("ProtoReader32")
  }

  private fun writeOrangeProto() {
    fs.add(
      "colors/src/main/proto/squareup/colors/orange.proto",
      """
          |syntax = "proto2";
          |package squareup.colors;
          |import "squareup/polygons/triangle.proto";
          |message Orange {
          |  optional string circle = 1;
          |  optional squareup.polygons.Triangle.Type triangle = 2;
          |}
      """.trimMargin(),
    )
  }

  private fun writeColorsRouteProto() {
    fs.add(
      "routes/src/main/proto/squareup/routes/route.proto",
      """
          |syntax = "proto2";
          |package squareup.routes;
          |import "squareup/colors/blue.proto";
          |import "squareup/colors/red.proto";
          |service Route {
          |  rpc GetUpdatedRed(squareup.colors.Red) returns (squareup.colors.Red) {}
          |  rpc GetUpdatedBlue(squareup.colors.Blue) returns (squareup.colors.Blue) {}
          |}
      """.trimMargin(),
    )
  }

  private fun writeBlueProto() {
    fs.add(
      "colors/src/main/proto/squareup/colors/blue.proto",
      """
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
  }

  private fun writeTriangleProto() {
    fs.add(
      "polygons/src/main/proto/squareup/polygons/triangle.proto",
      """
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
  }

  private fun writeSquareProto() {
    fs.add(
      "polygons/src/main/proto/squareup/polygons/square.proto",
      """
          |syntax = "proto2";
          |package squareup.polygons;
          |option java_package = "com.squareup.polygons";
          |message Square {
          |  optional double length = 1;
          |}
      """.trimMargin(),
    )
  }

  private fun writeRhombusProto() {
    fs.add(
      "polygons/src/main/proto/squareup/polygons/rhombus.proto",
      """
        |syntax = "proto2";
        |package squareup.polygons;
        |option java_package = "com.squareup.polygons";
        |message Rhombus {
        |  optional double length = 1;
        |  optional double acute_angle = 2;
        |}
      """.trimMargin(),
    )
  }

  private fun writeDocumentationProto() {
    fs.add(
      "docs/src/main/proto/squareup/options/documentation.proto",
      """
        |syntax = "proto2";
        |package squareup.options;
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  optional string documentation_url = 22200;
        |}
      """.trimMargin(),
    )
  }

  private fun writeOctagonProto() {
    fs.add(
      "polygons/src/main/proto/squareup/polygons/octagon.proto",
      """
        |syntax = "proto2";
        |package squareup.polygons;
        |import "squareup/options/documentation.proto";
        |
        |message Octagon {
        |  option (options.documentation_url) = "https://en.wikipedia.org/wiki/Octagon";
        |  optional bool stop = 1;
        |}
      """.trimMargin(),
    )
  }

  private fun writeMinimalGoogleProtobufProtos() {
    fs.add(
      "google/src/main/proto/google/protobuf/descriptor.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message descriptor {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/any.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Any {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/duration.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Duration {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/empty.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Empty {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/struct.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Struct {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/timestamp.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Timestamp {}
      """.trimMargin(),
    )
    fs.add(
      "google/src/main/proto/google/protobuf/wrappers.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message Wrappers {}
      """.trimMargin(),
    )
  }

  private fun writeMinimalWireProtos() {
    fs.add(
      "wire/src/main/proto/wire/extensions.proto",
      """
        |syntax = "proto2";
        |package google.protobuf;
        |message extensions {}
      """.trimMargin(),
    )
  }

  @Test fun noSuchClassLogger() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newLoggerFactory("foo").create()
      },
    ).hasMessage("Couldn't find LoggerClass 'foo'")
  }

  @Test fun noPublicConstructorLogger() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newLoggerFactory("java.lang.Void").create()
      },
    ).hasMessage("No public constructor on java.lang.Void")
  }

  @Test fun classDoesNotImplementWireLoggerInterface() {
    assertThat(
      assertFailsWith<IllegalArgumentException> {
        newLoggerFactory("java.lang.Object").create()
      },
    ).hasMessage("java.lang.Object does not implement WireLogger.Factory")
  }

  @Test fun customLogger() {
    val logger = CustomLogger()

    writeBlueProto()
    writeRedProto()
    writeTriangleProto()
    val wireRun = WireRun(
      sourcePath = listOf(Location.get("colors/src/main/proto")),
      protoPath = listOf(Location.get("polygons/src/main/proto")),
      targets = listOf(KotlinTarget(outDirectory = "generated/kt")),
    )
    wireRun.execute(fs, logger)
    assertThat(fs.findFiles("generated")).containsExactlyInAnyOrderAsRelativePaths(
      "generated/kt/squareup/colors/Blue.kt",
      "generated/kt/squareup/colors/Red.kt",
    )
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Blue.kt"))
      .contains("class Blue")
    assertThat(fs.readUtf8("generated/kt/squareup/colors/Red.kt"))
      .contains("class Red")

    assertThat(logger.takeLog())
      .isEqualTo("artifactHandled: generated/kt, squareup.colors.Blue, Kotlin")
    assertThat(logger.takeLog())
      .isEqualTo("artifactHandled: generated/kt, squareup.colors.Red, Kotlin")

    logger.assertAllEventsAreConsumed()
  }

  class CustomLogger : WireLogger {
    private val logs = ArrayDeque<String>()

    fun takeLog() = logs.removeFirst()

    fun assertAllEventsAreConsumed() {
      if (logs.isNotEmpty()) {
        throw AssertionError("Unconsumed logs: ${logs.joinToString("")}")
      }
    }

    override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
      logs.add("artifactHandled: $outputPath, $qualifiedName, $targetName")
    }

    override fun artifactSkipped(type: ProtoType, targetName: String) {
      logs.add("artifactSkipped: $type, $targetName")
    }

    override fun unusedRoots(unusedRoots: Set<String>) {
      logs.add("unusedRoots: $unusedRoots")
    }

    override fun unusedPrunes(unusedPrunes: Set<String>) {
      logs.add("unusedPrunes: $unusedPrunes")
    }

    override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {
      logs.add("unusedIncludesInTarget: $unusedIncludes")
    }

    override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {
      logs.add("unusedExcludesInTarget: $unusedExcludes")
    }
  }
}
