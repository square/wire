/*
 * Copyright (C) 2013 Square, Inc.
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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.size
import com.squareup.wire.kotlin.EnumMode
import com.squareup.wire.kotlin.RpcCallStyle
import com.squareup.wire.kotlin.RpcRole
import com.squareup.wire.schema.CustomTarget
import com.squareup.wire.schema.EmptyWireLogger
import com.squareup.wire.schema.JavaTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SwiftTarget
import com.squareup.wire.schema.WireRun
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test

class WireCompilerTest {
  private val fileSystem = FakeFileSystem()

  @Test
  fun compileExecutes() {
    val outputDirectory = "/out".toPath()

    fileSystem.appendingSink("baz.proto".toPath()).buffer().use {
      it.writeUtf8(
        """
          |syntax = "proto2";
          |
          |message Player {
          |  optional string name = 1;
          |}
          |
        """.trimMargin(),
      )
    }
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--proto_path=/",
      "--java_out=$outputDirectory",
    )
    wireCompiler.compile()

    assertThat(fileSystem.listRecursively("/out".toPath())).containsExactly(outputDirectory / "Player.java")
  }

  @Test
  fun default() {
    val wireCompiler = WireCompiler.forArgs(fileSystem, EmptyWireLogger(), "--java_out=out/")
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::sourcePath).isEmpty()
      prop(WireRun::protoPath).isEmpty()
      prop(WireRun::treeShakingRoots).containsExactly("*")
      prop(WireRun::treeShakingRubbish).isEmpty()
      prop(WireRun::moves).isEmpty()
      prop(WireRun::sinceVersion).isNull()
      prop(WireRun::untilVersion).isNull()
      prop(WireRun::onlyVersion).isNull()
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "out/",
        ),
      )
      prop(WireRun::modules).isEmpty()
      prop(WireRun::permitPackageCycles).isFalse()
      prop(WireRun::loadExhaustively).isFalse()
      prop(WireRun::escapeKotlinKeywords).isFalse()
      prop(WireRun::eventListeners).isEmpty()
      prop(WireRun::rejectUnusedRootsOrPrunes).isTrue()
      prop(WireRun::opaqueTypes).isEmpty()
    }
  }

  @Test // TODO(Benoit) Delete? Nobody wanna maintain such a test
  fun allFlags() {
    fileSystem.appendingSink("foo.proto".toPath())
    fileSystem.appendingSink("bar.proto".toPath())
    fileSystem.appendingSink("proto.include".toPath())
      .buffer().use {
        it.writeUtf8("foo.proto\n")
        it.writeUtf8("bar.proto\n")
      }
    fileSystem.appendingSink("proto_manifest.yaml".toPath())
      .buffer().use {
        it.writeUtf8(
          """
          |a: {}
          |b:
          |  dependencies:
          |   - a
          |
          """.trimMargin(),
        )
      }

    val wireCompiler = WireCompiler.forArgs(
      fileSystem, EmptyWireLogger(),
      "--proto_path=/",
      "--java_out=java_out",
      "--kotlin_out=kotlin_out",
      "--swift_out=swift_out",
      // "--custom_out=custom_out",
      // "--schema_handler_factory_class=schema_handler_factory_class",
      "--files=proto.include",
      "--includes=includes",
      "--excludes=excludes",
      "--experimental-module-manifest=proto_manifest.yaml",
      // "--event_listener_factory_class=event_listener_factory_class",
      // "--logger_factory_class=logger_factory_class",
      "--android",
      "--android-annotations",
      "--compact",
      "--skip_declared_options",
      "--skip_applied_options",
      "--permit_package_cycles",
      "--load_exhaustively",
      "--java_interop",
      "--dry_run",
      "--kotlin_box_oneofs_min_size=67",
      "--no_java_exclusive",
      "--no_kotlin_exclusive",
      "--no_swift_exclusive",
      "--kotlin_rpc_call_style=blocking",
      "--kotlin_rpc_role=server",
      "--kotlin_single_method_services",
      "--kotlin_name_suffix=kotlin_name_suffix",
      "--kotlin_builders_only",
      "--kotlin_escape_keywords",
      "--emit_proto_reader_32",
      "--kotlin_enum_mode=sealed_class",
      // "--custom_option=a,1",
      // "--custom_option=b,2",
      "--opaque_types=opaque_types",
      "--ignore_unused_roots_and_prunes",
      "--kotlin_explicit_streaming_calls",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::sourcePath).containsExactly(Location.get("foo.proto"), Location.get("bar.proto"))
      prop(WireRun::protoPath).containsExactly(Location.get("/"))
      prop(WireRun::treeShakingRoots).containsExactly("includes")
      prop(WireRun::treeShakingRubbish).containsExactly("excludes")
      // There's no flag to define type moves.
      prop(WireRun::moves).isEmpty()
      prop(WireRun::sinceVersion).isNull()
      prop(WireRun::untilVersion).isNull()
      prop(WireRun::onlyVersion).isNull()
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          includes = listOf("*"),
          excludes = listOf(),
          exclusive = false,
          outDirectory = "java_out",
          android = true,
          androidAnnotations = true,
          compact = true,
          emitDeclaredOptions = false,
          emitAppliedOptions = false,
          buildersOnly = false,
        ),
        KotlinTarget(
          includes = listOf("*"),
          excludes = listOf(),
          exclusive = false,
          outDirectory = "kotlin_out",
          android = true,
          javaInterop = true,
          emitDeclaredOptions = false,
          emitAppliedOptions = false,
          rpcCallStyle = RpcCallStyle.BLOCKING,
          rpcRole = RpcRole.SERVER,
          singleMethodServices = true,
          boxOneOfsMinSize = 67,
          nameSuffix = "kotlin_name_suffix",
          buildersOnly = true,
          escapeKotlinKeywords = true,
          enumMode = EnumMode.SEALED_CLASS,
          emitProtoReader32 = true,
          mutableTypes = false,
          explicitStreamingCalls = true,
        ),
        SwiftTarget(
          includes = listOf("*"),
          excludes = listOf(),
          exclusive = false,
          outDirectory = "swift_out",
        ),
      )
      prop(WireRun::modules).isEqualTo(
        mapOf("a" to WireRun.Module(), "b" to WireRun.Module(dependencies = setOf("a"))),
      )
      prop(WireRun::permitPackageCycles).isTrue()
      prop(WireRun::loadExhaustively).isTrue()
      prop(WireRun::escapeKotlinKeywords).isFalse()
      prop(WireRun::eventListeners).isEmpty()
      prop(WireRun::rejectUnusedRootsOrPrunes).isFalse()
      prop(WireRun::opaqueTypes).containsExactly("opaque_types")
    }
  }

  @Test
  fun protoLocations() {
    fileSystem.appendingSink("foo.proto".toPath())
    fileSystem.appendingSink("bar.proto".toPath())
    fileSystem.appendingSink("proto.include".toPath())
      .buffer().use {
        it.writeUtf8("foo.proto\n")
        it.writeUtf8("bar.proto\n")
      }
    fileSystem.appendingSink("baz.proto".toPath())

    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--proto_path=/",
      "--java_out=java_out",
      "--files=proto.include",
      "baz.proto",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::sourcePath).containsExactly(Location.get("foo.proto"), Location.get("bar.proto"), Location.get("baz.proto"))
      prop(WireRun::protoPath).containsExactly(Location.get("/"))
      prop(WireRun::treeShakingRoots).containsExactly("*")
      prop(WireRun::treeShakingRubbish).isEmpty()
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
    }
  }

  @Test
  fun treeShaking() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--kotlin_out=kotlin_out",
      "--includes=includes1,includes2,includes3",
      "--excludes=excludes1,excludes2",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::treeShakingRoots).containsExactly("includes1", "includes2", "includes3")
      prop(WireRun::treeShakingRubbish).containsExactly("excludes1", "excludes2")
      prop(WireRun::targets).containsExactly(
        KotlinTarget(
          outDirectory = "kotlin_out",
        ),
      )
    }
  }

  @Test
  fun manifest() {
    fileSystem.appendingSink("proto_manifest.yaml".toPath())
      .buffer().use {
        it.writeUtf8(
          """
          |a: {}
          |b:
          |  dependencies:
          |   - a
          |
          """.trimMargin(),
        )
      }

    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--java_out=java_out",
      "--experimental-module-manifest=proto_manifest.yaml",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
      prop(WireRun::modules).isEqualTo(
        mapOf("a" to WireRun.Module(), "b" to WireRun.Module(dependencies = setOf("a"))),
      )
    }
  }

  @Test
  fun loadExhaustively() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--java_out=java_out",
      "--load_exhaustively",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
      prop(WireRun::loadExhaustively).isTrue()
    }
  }

  @Test
  fun permitPackageCycles() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--java_out=java_out",
      "--permit_package_cycles",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
      prop(WireRun::permitPackageCycles).isTrue()
    }
  }

  @Test
  fun dryRun() {
    val outputDirectory = "/out".toPath()

    fileSystem.appendingSink("baz.proto".toPath()).buffer().use {
      it.writeUtf8(
        """
          |syntax = "proto2";
          |
          |message Player {
          |  optional string name = 1;
          |}
          |
        """.trimMargin(),
      )
    }
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--proto_path=/",
      "--java_out=$outputDirectory",
      "--dry_run",
    )
    wireCompiler.compile()

    // Dry run didn't write on disk.
    assertThat(fileSystem.listRecursively("/out".toPath())).isEmpty()
  }

  @Test
  fun opaqueTypes() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--java_out=java_out",
      "--opaque_types=opaque_types",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
      prop(WireRun::opaqueTypes).containsExactly("opaque_types")
    }
  }

  @Test
  fun ignoreUnusedRootsAndPrunes() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--java_out=java_out",
      "--ignore_unused_roots_and_prunes",
    )
    val wireRun = wireCompiler.createRun()
    assertThat(wireRun).all {
      prop(WireRun::targets).containsExactly(
        JavaTarget(
          outDirectory = "java_out",
        ),
      )
      prop(WireRun::rejectUnusedRootsOrPrunes).isFalse()
    }
  }

  @Test
  fun allTargetsAndAllOptions() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem, EmptyWireLogger(),
      "--proto_path=/",
      "--java_out=java_out",
      "--kotlin_out=kotlin_out",
      "--swift_out=swift_out",
      "--custom_out=custom_out",
      "--schema_handler_factory_class=schema_handler_factory_class",
      "--android",
      "--android-annotations",
      "--compact",
      "--skip_declared_options",
      "--skip_applied_options",
      "--permit_package_cycles",
      "--java_interop",
      "--kotlin_box_oneofs_min_size=67",
      "--no_java_exclusive",
      "--no_kotlin_exclusive",
      "--no_swift_exclusive",
      "--kotlin_rpc_call_style=blocking",
      "--kotlin_rpc_role=server",
      "--kotlin_single_method_services",
      "--kotlin_name_suffix=kotlin_name_suffix",
      "--kotlin_builders_only",
      "--kotlin_escape_keywords",
      "--emit_proto_reader_32",
      "--kotlin_enum_mode=sealed_class",
      "--custom_option=a,1",
      "--custom_option=b,2",
      "--kotlin_explicit_streaming_calls",
    )
    val wireRun = wireCompiler.createRun()
    val targets = wireRun.targets
    assertThat(targets.filterIsInstance<JavaTarget>().single()).isEqualTo(
      JavaTarget(
        includes = listOf("*"),
        excludes = listOf(),
        exclusive = false,
        outDirectory = "java_out",
        android = true,
        androidAnnotations = true,
        compact = true,
        emitDeclaredOptions = false,
        emitAppliedOptions = false,
        buildersOnly = false,
      ),
    )
    assertThat(targets.filterIsInstance<KotlinTarget>().single()).isEqualTo(
      KotlinTarget(
        includes = listOf("*"),
        excludes = listOf(),
        exclusive = false,
        outDirectory = "kotlin_out",
        android = true,
        javaInterop = true,
        emitDeclaredOptions = false,
        emitAppliedOptions = false,
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
        singleMethodServices = true,
        boxOneOfsMinSize = 67,
        nameSuffix = "kotlin_name_suffix",
        buildersOnly = true,
        escapeKotlinKeywords = true,
        enumMode = EnumMode.SEALED_CLASS,
        emitProtoReader32 = true,
        mutableTypes = false,
        explicitStreamingCalls = true,
      ),
    )
    assertThat(targets.filterIsInstance<SwiftTarget>().single()).isEqualTo(
      SwiftTarget(
        includes = listOf("*"),
        excludes = listOf(),
        exclusive = false,
        outDirectory = "swift_out",
      ),
    )
    val customTarget = targets.filterIsInstance<CustomTarget>().single()
    assertThat(customTarget).all {
      prop(CustomTarget::includes).containsExactly("*")
      prop(CustomTarget::excludes).isEmpty()
      prop(CustomTarget::exclusive).isTrue()
      prop(CustomTarget::outDirectory).isEqualTo("custom_out")
      prop(CustomTarget::options).containsOnly("a" to "1", "b" to "2")
      prop(CustomTarget::schemaHandlerFactory).isNotNull()
    }
    assertThat(targets.size).isEqualTo(4)
  }

  @Test
  fun customOutput() {
    val wireCompiler = WireCompiler.forArgs(
      fileSystem,
      EmptyWireLogger(),
      "--proto_path=/",
      "--custom_out=custom_out",
      "--schema_handler_factory_class=schema_handler_factory_class",
      "--custom_option=a,1",
      "--custom_option=b,2",
    )
    val wireRun = wireCompiler.createRun()
    val target = wireRun.targets.single() as CustomTarget
    assertThat(target.includes).containsExactly("*")
    assertThat(target.excludes).isEmpty()
    assertThat(target.exclusive).isTrue()
    assertThat(target.options).isEqualTo(mapOf("a" to "1", "b" to "2"))
    assertThat(target.schemaHandlerFactory).isNotNull()
  }
}
