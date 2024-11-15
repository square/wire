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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.size
import com.squareup.wire.schema.ProtoType
import java.util.Collections
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WireCompilerTest {
  @Rule
  @JvmField
  val temp = TemporaryFolder()
  private val fileSystem = FileSystem.SYSTEM

  private var logger: WireLogger? = null
  private lateinit var testDir: Path

  /** Returns all paths within `root`, and relative to `root`.  */
  private val paths: List<Path>
    get() {
      return fileSystem.listRecursively("/".toPath() / testDir)
        .filter { fileSystem.metadata(it).isRegularFile }
        .toList()
    }

  @Before
  fun setUp() {
    testDir = temp.root.toOkioPath()
  }

  @Test
  fun testFooBar() {
    val sources = arrayOf("foo.proto", "bar.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/foobar/protos/bar/Bar.java",
      "com/squareup/foobar/protos/foo/Foo.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testDifferentPackageFooBar() {
    val sources = arrayOf("differentpackage/foo.proto", "differentpackage/bar.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/differentpackage/protos/bar/Bar.java",
      "com/squareup/differentpackage/protos/foo/Foo.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testPerson() {
    val sources = arrayOf("person.proto")
    compileToJava(sources)

    val outputs = arrayOf("com/squareup/wire/protos/person/Person.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testPersonDryRun() {
    val sources = arrayOf("person.proto")

    val args = mutableListOf(
      TargetLanguage.JAVA.protoPathArg(),
      TargetLanguage.JAVA.outArg("/".toPath() / testDir),
      "--dry_run",
    )
    args.addAll(sources)

    val logs = mutableListOf<String>()
    val logger = object : WireLogger {
      override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
        logs.add("artifactHandled($qualifiedName, $targetName)")
      }
      override fun artifactSkipped(type: ProtoType, targetName: String) = Unit
      override fun unusedRoots(unusedRoots: Set<String>) = Unit
      override fun unusedPrunes(unusedPrunes: Set<String>) = Unit
      override fun unusedIncludesInTarget(unusedIncludes: Set<String>) = Unit
      override fun unusedExcludesInTarget(unusedExcludes: Set<String>) = Unit
    }
    val compiler = WireCompiler.forArgs(fileSystem, logger, *args.toTypedArray<String>())
    compiler.compile()

    // We assert nothing has been generated.
    assertJavaOutputs(arrayOf())
    // But we logged things because we're dry-running.
    assertThat(logs).containsExactly("artifactHandled(com.squareup.wire.protos.person.Person, Java)")
  }

  @Test
  fun runMultipleTargets() {
    val sources = arrayOf("person.proto")

    val args = mutableListOf(
      TargetLanguage.JAVA.protoPathArg(),
      TargetLanguage.JAVA.outArg("/".toPath() / testDir),
      TargetLanguage.KOTLIN.protoPathArg(),
      TargetLanguage.KOTLIN.outArg("/".toPath() / testDir),
      "--no_kotlin_exclusive",
      "--dry_run",
    )
    args.addAll(sources)

    val logs = mutableListOf<String>()
    val logger = object : WireLogger {
      override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
        logs.add("artifactHandled($qualifiedName, $targetName)")
      }
      override fun artifactSkipped(type: ProtoType, targetName: String) = Unit
      override fun unusedRoots(unusedRoots: Set<String>) = Unit
      override fun unusedPrunes(unusedPrunes: Set<String>) = Unit
      override fun unusedIncludesInTarget(unusedIncludes: Set<String>) = Unit
      override fun unusedExcludesInTarget(unusedExcludes: Set<String>) = Unit
    }
    val compiler = WireCompiler.forArgs(fileSystem, logger, *args.toTypedArray<String>())
    compiler.compile()

    // We assert nothing has been generated.
    assertJavaOutputs(arrayOf())
    assertKotlinOutputs(arrayOf())
    assertSwiftOutputs(arrayOf())
    // But we logged things because we're dry-running.
    assertThat(logs).containsExactly(
      "artifactHandled(com.squareup.wire.protos.person.Person, Kotlin)",
      "artifactHandled(com.squareup.wire.protos.person.Person, Java)",
    )
  }

  @Test
  fun notExclusiveTargets() {
    val sources = arrayOf("person.proto")

    val args = mutableListOf(
      TargetLanguage.JAVA.protoPathArg(),
      TargetLanguage.JAVA.outArg("/".toPath() / testDir),
      TargetLanguage.KOTLIN.protoPathArg(),
      TargetLanguage.KOTLIN.outArg("/".toPath() / testDir),
      TargetLanguage.SWIFT.protoPathArg(),
      TargetLanguage.SWIFT.outArg("/".toPath() / testDir),
      "--no_kotlin_exclusive",
      "--no_swift_exclusive",
      "--dry_run",
    )
    args.addAll(sources)

    val logs = mutableListOf<String>()
    val logger = object : WireLogger {
      override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
        logs.add("artifactHandled($qualifiedName, $targetName)")
      }
      override fun artifactSkipped(type: ProtoType, targetName: String) = Unit
      override fun unusedRoots(unusedRoots: Set<String>) = Unit
      override fun unusedPrunes(unusedPrunes: Set<String>) = Unit
      override fun unusedIncludesInTarget(unusedIncludes: Set<String>) = Unit
      override fun unusedExcludesInTarget(unusedExcludes: Set<String>) = Unit
    }
    val compiler = WireCompiler.forArgs(fileSystem, logger, *args.toTypedArray<String>())
    compiler.compile()

    // We assert nothing has been generated.
    assertJavaOutputs(arrayOf())
    assertKotlinOutputs(arrayOf())
    assertSwiftOutputs(arrayOf())
    // But we logged things because we're dry-running.
    assertThat(logs).containsExactly(
      "artifactHandled(com.squareup.wire.protos.person.Person, Kotlin)",
      "artifactHandled(.Person declared in person.proto, Swift)",
      "artifactHandled(com.squareup.wire.protos.person.Person, Java)",
    )
  }

  @Test
  fun testPersonAndroid() {
    val sources = arrayOf("person.proto")
    compileToJava(sources, "--android")

    val outputs = arrayOf("com/squareup/wire/protos/person/Person.java")
    assertJavaOutputs(outputs, ".android")
  }

  @Test
  fun testPersonCompact() {
    val sources = arrayOf("all_types.proto")
    compileToJava(sources, "--compact")

    val outputs = arrayOf("com/squareup/wire/protos/alltypes/AllTypes.java")
    assertJavaOutputs(outputs, ".compact")
  }

  @Test
  fun testSimple() {
    val sources = arrayOf("simple_message.proto", "external_message.proto", "foreign.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/simple/SimpleMessage.java",
      "com/squareup/wire/protos/simple/ExternalMessage.java",
      "com/squareup/wire/protos/foreign/ForeignEnum.java",
      "com/squareup/wire/protos/foreign/ForeignEnumValueOptionOption.java",
      "com/squareup/wire/protos/foreign/ForeignMessage.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testOneOf() {
    val sources = arrayOf("one_of.proto")
    compileToJava(sources)

    val outputs = arrayOf("com/squareup/wire/protos/oneof/OneOfMessage.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testSingleLevel() {
    val sources = arrayOf("single_level.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/single_level/Foo.java",
      "com/squareup/wire/protos/single_level/Foos.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testSameBasename() {
    val sources = arrayOf(
      "single_level.proto",
      "samebasename/single_level.proto",
    )
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/single_level/Foo.java",
      "com/squareup/wire/protos/single_level/Foos.java",
      "com/squareup/wire/protos/single_level/Bar.java",
      "com/squareup/wire/protos/single_level/Bars.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testChildPackage() {
    val sources = arrayOf("child_pkg.proto")
    compileToJava(sources)

    val outputs = arrayOf("com/squareup/wire/protos/ChildPackage.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testAllTypes() {
    val sources = arrayOf("all_types.proto")
    compileToJava(sources)

    val outputs = arrayOf("com/squareup/wire/protos/alltypes/AllTypes.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testEdgeCases() {
    val sources = arrayOf("edge_cases.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/edgecases/NoFields.java",
      "com/squareup/wire/protos/edgecases/OneField.java",
      "com/squareup/wire/protos/edgecases/OneBytesField.java",
      "com/squareup/wire/protos/edgecases/Recursive.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testUnknownFields() {
    val sources = arrayOf("unknown_fields.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/unknownfields/EnumVersionOne.java",
      "com/squareup/wire/protos/unknownfields/EnumVersionTwo.java",
      "com/squareup/wire/protos/unknownfields/VersionOne.java",
      "com/squareup/wire/protos/unknownfields/VersionTwo.java",
      "com/squareup/wire/protos/unknownfields/NestedVersionOne.java",
      "com/squareup/wire/protos/unknownfields/NestedVersionTwo.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testCustomOptions() {
    val sources = arrayOf("custom_options.proto", "option_redacted.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/custom_options/EnumOptionOption.java",
      "com/squareup/wire/protos/custom_options/EnumValueOptionOption.java",
      "com/squareup/wire/protos/custom_options/FooBar.java",
      "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
      "com/squareup/wire/protos/custom_options/MethodOptionOneOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionFiveOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionOneOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionSevenOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionSixOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionThreeOption.java",
      "com/squareup/wire/protos/custom_options/MyFieldOptionTwoOption.java",
      "com/squareup/wire/protos/custom_options/MyMessageOptionEightOption.java",
      "com/squareup/wire/protos/custom_options/MyMessageOptionFourOption.java",
      "com/squareup/wire/protos/custom_options/MyMessageOptionNineOption.java",
      "com/squareup/wire/protos/custom_options/MyMessageOptionSevenOption.java",
      "com/squareup/wire/protos/custom_options/MyMessageOptionTwoOption.java",
      "com/squareup/wire/protos/custom_options/RepeatedEnumValueOptionOneOption.java",
      "com/squareup/wire/protos/custom_options/RepeatedEnumValueOptionTwoOption.java",
      "com/squareup/wire/protos/custom_options/ServiceOptionOneOption.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testOpaqueTypes() {
    val sources = arrayOf("opaque_types.proto")
    compileToKotlin(sources, "--opaque_types=squareup.protos.opaque_types.OuterOpaqueType.InnerOpaqueType1")

    val outputs = arrayOf(
      "squareup/protos/opaque_types/OuterOpaqueType.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testCustomOptionsNoOptions() {
    val sources = arrayOf("custom_options.proto", "option_redacted.proto")
    compileToJava(sources, "--excludes=google.protobuf.*")

    val outputs = arrayOf(
      "com/squareup/wire/protos/custom_options/FooBar.java",
      "com/squareup/wire/protos/custom_options/MessageWithOptions.java",
    )
    assertJavaOutputs(outputs, ".noOptions")
  }

  @Test
  fun testRedacted() {
    val sources = arrayOf("redacted_test.proto", "option_redacted.proto")
    compileToJava(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/redacted/NotRedacted.java",
      "com/squareup/wire/protos/redacted/RedactedChild.java",
      "com/squareup/wire/protos/redacted/RedactedCycleA.java",
      "com/squareup/wire/protos/redacted/RedactedCycleB.java",
      "com/squareup/wire/protos/redacted/RedactedExtension.java",
      "com/squareup/wire/protos/redacted/RedactedFields.java",
      "com/squareup/wire/protos/redacted/RedactedRepeated.java",
      "com/squareup/wire/protos/redacted/RedactedRequired.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testNoRoots() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources)

    val outputs =
      arrayOf(
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/D.java",
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java",
        "com/squareup/wire/protos/roots/H.java",
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java",
      )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testExcludes() {
    val sources = arrayOf("roots.proto")
    compileToJava(
      sources,
      "--includes=squareup.protos.roots.A",
      "--excludes=squareup.protos.roots.B",
    )

    val outputs =
      arrayOf(
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/D.java",
      )
    assertJavaOutputs(outputs, ".pruned")
  }

  @Test
  fun testRootsA() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.A")

    val outputs =
      arrayOf(
        "com/squareup/wire/protos/roots/A.java",
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java",
        "com/squareup/wire/protos/roots/D.java",
      )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsB() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.B")

    val outputs = arrayOf(
      "com/squareup/wire/protos/roots/B.java",
      "com/squareup/wire/protos/roots/C.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsE() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.E")

    val outputs = arrayOf(
      "com/squareup/wire/protos/roots/E.java",
      "com/squareup/wire/protos/roots/G.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsH() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.H")

    val outputs = arrayOf(
      "com/squareup/wire/protos/roots/E.java",
      "com/squareup/wire/protos/roots/H.java",
    )
    assertJavaOutputs(outputs, ".pruned")
  }

  @Test
  fun testRootsI() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.I")

    val outputs = arrayOf(
      "com/squareup/wire/protos/roots/I.java",
      "com/squareup/wire/protos/roots/J.java",
      "com/squareup/wire/protos/roots/K.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun serviceAreIgnoredForJava() {
    val sources = arrayOf("service_root.proto")
    compileToJava(sources, "--includes=squareup.wire.protos.roots.TheService")

    // TheService is not created.
    val outputs = arrayOf(
      "com/squareup/wire/protos/roots/TheResponse.java",
      "com/squareup/wire/protos/roots/TheRequest.java",
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun serviceInKotlin() {
    val sources = arrayOf("service_kotlin.proto")
    compileToKotlin(sources, "--includes=squareup.protos.kotlin.SomeService")

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/services/GrpcSomeServiceClient.kt",
      "com/squareup/wire/protos/kotlin/services/SomeServiceClient.kt",
      "com/squareup/wire/protos/kotlin/services/SomeResponse.kt",
      "com/squareup/wire/protos/kotlin/services/SomeRequest.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun serviceWithoutPackageInKotlin() {
    val sources = arrayOf("service_without_package.proto")
    compileToKotlin(sources, "--includes=NoPackageService")

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/services/GrpcNoPackageServiceClient.kt",
      "com/squareup/wire/protos/kotlin/services/NoPackageServiceClient.kt",
      "com/squareup/wire/protos/kotlin/services/NoPackageResponse.kt",
      "com/squareup/wire/protos/kotlin/services/NoPackageRequest.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun noFiles() {
    val sources = arrayOf<String>()
    compileToJava(sources)

    assertThat(paths).isNotEmpty()
  }

  @Test
  fun testAllTypesKotlin() {
    val sources = arrayOf("all_types.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/alltypes/AllTypes.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun sourceInJar() {
    val sources = arrayOf("squareup/geology/period.proto", "squareup/dinosaurs/dinosaur.proto")
    val args = mutableListOf(
      "--proto_path=../wire-tests/src/commonTest/proto/kotlin/protos.jar",
      TargetLanguage.KOTLIN.outArg("/".toPath() / testDir),
    )
    Collections.addAll(args, *sources)
    logger = StringWireLogger()
    val compiler = WireCompiler.forArgs(fileSystem, logger!!, *args.toTypedArray())
    compiler.compile()

    val outputs = arrayOf(
      "com/squareup/geology/Period.kt",
      "com/squareup/dinosaurs/Dinosaur.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun sourceDependsOnJar() {
    // `dinosaur.proto` depends on `period.proto` which both are in `protos.jar`.
    val sources = arrayOf("squareup/dinosaurs/dinosaur.proto")
    val args = mutableListOf(
      "--proto_path=../wire-tests/src/commonTest/proto/kotlin/protos.jar",
      TargetLanguage.KOTLIN.outArg("/".toPath() / testDir),
    )
    Collections.addAll(args, *sources)
    logger = StringWireLogger()
    val compiler = WireCompiler.forArgs(fileSystem, logger!!, *args.toTypedArray())
    compiler.compile()

    val outputs = arrayOf("com/squareup/dinosaurs/Dinosaur.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testAllTypesJavaInteropKotlin() {
    val sources = arrayOf("all_types.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/alltypes/AllTypes.kt")
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testPersonKotlin() {
    val sources = arrayOf("person.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/person/Person.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testPersonAndroidKotlin() {
    val sources = arrayOf("person.proto")
    compileToKotlin(sources, "--android")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/person/Person.kt")
    assertKotlinOutputs(outputs, ".android")
  }

  @Test
  fun testPersonJavaInteropKotlin() {
    val sources = arrayOf("person.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/person/Person.kt")
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testOneOfKotlin() {
    val sources = arrayOf("one_of.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/OneOfMessage.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testOneOfKotlinJavaInterop() {
    val sources = arrayOf("one_of.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/OneOfMessage.kt")
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testDeprecatedKotlin() {
    val sources = arrayOf("deprecated.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/DeprecatedProto.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testDeprecatedJavaInteropKotlin() {
    val sources = arrayOf("deprecated.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/DeprecatedProto.kt")
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testPercentSignsInKDoc() {
    val sources = arrayOf("percents_in_kdoc.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/Percents.kt")
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testCustomOptionsKotlin() {
    val sources = arrayOf("custom_options.proto", "option_redacted.proto")
    compileToKotlin(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/custom_options/EnumOptionOption.kt",
      "com/squareup/wire/protos/custom_options/EnumValueOptionOption.kt",
      "com/squareup/wire/protos/custom_options/FooBar.kt",
      "com/squareup/wire/protos/custom_options/GrpcServiceWithOptionsClient.kt",
      "com/squareup/wire/protos/custom_options/MessageWithOptions.kt",
      "com/squareup/wire/protos/custom_options/MethodOptionOneOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionFiveOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionOneOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionSevenOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionSixOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionThreeOption.kt",
      "com/squareup/wire/protos/custom_options/MyFieldOptionTwoOption.kt",
      "com/squareup/wire/protos/custom_options/MyMessageOptionEightOption.kt",
      "com/squareup/wire/protos/custom_options/MyMessageOptionFourOption.kt",
      "com/squareup/wire/protos/custom_options/MyMessageOptionNineOption.kt",
      "com/squareup/wire/protos/custom_options/MyMessageOptionSevenOption.kt",
      "com/squareup/wire/protos/custom_options/MyMessageOptionTwoOption.kt",
      "com/squareup/wire/protos/custom_options/RepeatedEnumValueOptionOneOption.kt",
      "com/squareup/wire/protos/custom_options/RepeatedEnumValueOptionTwoOption.kt",
      "com/squareup/wire/protos/custom_options/ServiceOptionOneOption.kt",
      "com/squareup/wire/protos/custom_options/ServiceWithOptionsClient.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testRedactedKotlin() {
    val sources = arrayOf("redacted_test.proto", "option_redacted.proto")
    compileToKotlin(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/redacted/NotRedacted.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedFields.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedChild.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedCycleA.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedCycleB.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedRepeated.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedRequired.kt",
      "com/squareup/wire/protos/kotlin/redacted/RedactedExtension.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testRedactedOneOfKotlin() {
    val sources = arrayOf("redacted_one_of.proto", "option_redacted.proto")
    compileToKotlin(sources)

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/redacted/RedactedOneOf.kt",
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testRedactedOneOfJavaInteropKotlin() {
    val sources = arrayOf("redacted_one_of.proto", "option_redacted.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/redacted/RedactedOneOf.kt",
    )
    assertKotlinOutputs(outputs, ".java.interop")
  }

  @Test
  fun testDeprecatedEnumConstant() {
    val sources = arrayOf("deprecated_enum.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/DeprecatedEnum.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testFormOneOfKotlin() {
    val sources = arrayOf("form.proto")
    compileToKotlin(sources, "--kotlin_box_oneofs_min_size=8")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/Form.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testNoFieldsKotlin() {
    val sources = arrayOf("no_fields.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/NoFields.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testToStringKotlin() {
    val sources = arrayOf("to_string.proto")
    compileToKotlin(sources)

    val outputs =
      arrayOf("com/squareup/wire/protos/kotlin/VeryLongProtoNameCausingBrokenLineBreaks.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testWithAllKotlinFlags() {
    val sources = arrayOf("service_kotlin_with_all_flags.proto")
    compileToKotlin(
      sources,
      "--no_kotlin_exclusive",
      "--kotlin_rpc_call_style=blocking",
      "--kotlin_rpc_role=server",
      "--kotlin_single_method_services",
      "--kotlin_name_suffix=SomeSuffix",
      "--kotlin_builders_only",
    )

    val outputs = arrayOf(
      "com/squareup/wire/protos/kotlin/services/all_flags_on/SomeRequest.kt",
      "com/squareup/wire/protos/kotlin/services/all_flags_on/SomeResponse.kt",
      "com/squareup/wire/protos/kotlin/services/all_flags_on/SomeServiceSomeMethodSomeSuffix.kt",
    )
    assertKotlinOutputs(outputs)
  }

  private fun compileToJava(sources: Array<String>, vararg extraArgs: String) =
    invokeCompiler(TargetLanguage.JAVA, sources, *extraArgs)

  private fun compileToKotlin(sources: Array<String>, vararg extraArgs: String) =
    invokeCompiler(TargetLanguage.KOTLIN, sources, *extraArgs)

  private fun invokeCompiler(
    target: TargetLanguage,
    sources: Array<String>,
    vararg extraArgs: String,
  ) {
    val args = mutableListOf(
      target.protoPathArg(),
      target.outArg("/".toPath() / testDir),
    )
    Collections.addAll(args, *extraArgs)
    Collections.addAll(args, *sources)

    logger = StringWireLogger()
    val compiler = WireCompiler.forArgs(fileSystem, logger!!, *args.toTypedArray())
    compiler.compile()
  }

  private fun assertJavaOutputs(outputs: Array<String>, suffix: String = "") =
    assertOutputs(TargetLanguage.JAVA, outputs, suffix)

  private fun assertKotlinOutputs(outputs: Array<String>, suffix: String = "") =
    assertOutputs(TargetLanguage.KOTLIN, outputs, suffix)

  private fun assertSwiftOutputs(outputs: Array<String>, suffix: String = "") =
    assertOutputs(TargetLanguage.SWIFT, outputs, suffix)

  private fun assertOutputs(target: TargetLanguage, outputs: Array<String>, suffix: String = "") {
    val filesAfter = paths
    assertThat(filesAfter)
      .size()
      .isEqualTo(outputs.size)

    for (output in outputs) {
      assertFilesMatch(target, testDir, output, suffix)
    }
  }

  private fun assertFilesMatch(
    target: TargetLanguage,
    outputDir: Path,
    path: String,
    suffix: String,
  ) {
    // Compare against file with suffix if present.
    val expectedFile = target.expectedFile(path, suffix)
    val actualFile = outputDir / path
    assertFilesMatch(expectedFile, actualFile)
  }

  private fun assertFilesMatch(expectedFile: Path, actualFile: Path) {
    var expected = fileSystem.read(expectedFile) { readUtf8() }
    var actual = fileSystem.read(actualFile) { readUtf8() }

    // Normalize CRLF -> LF.
    expected = expected.replace("\r\n", "\n")
    actual = actual.replace("\r\n", "\n")
    assertThat(actual).isEqualTo(expected)
  }

  private enum class TargetLanguage {
    JAVA {
      override fun protoPathArg() = "--proto_path=../wire-tests/src/commonTest/proto/java"
      override fun outArg(testDirPath: Path) = "--java_out=$testDirPath"
      override fun protoFolderSuffix() = "java"
    },
    KOTLIN {
      override fun protoPathArg() = "--proto_path=../wire-tests/src/commonTest/proto/kotlin"
      override fun outArg(testDirPath: Path) = "--kotlin_out=$testDirPath"
      override fun protoFolderSuffix() = "kotlin"
    },
    SWIFT {
      override fun protoPathArg() = "--proto_path=../wire-tests/src/commonTest/proto/kotlin"
      override fun outArg(testDirPath: Path) = "--swift_out=$testDirPath"
      override fun protoFolderSuffix() = "swift"
    },
    ;

    abstract fun protoPathArg(): String
    abstract fun outArg(testDirPath: Path): String
    abstract fun protoFolderSuffix(): String

    fun expectedFile(path: String, suffix: String): Path {
      val sourceSet = when (val protoFolderSuffix = protoFolderSuffix()) {
        "kotlin" -> when (suffix) {
          "" -> if (path.contains("kotlin/services/")) "jvmKotlinInteropTest" else "commonTest"
          ".java.interop" -> "jvmKotlinInteropTest"
          ".android" -> "jvmKotlinAndroidTest"
          else -> throw AssertionError("Unknown suffix: $suffix")
        }

        "java" -> when (suffix) {
          "" -> "jvmJavaTest"
          ".noOptions" -> "jvmJavaNoOptionsTest"
          ".compact" -> "jvmJavaCompactTest"
          ".pruned" -> "jvmJavaPrunedTest"
          ".android" -> "jvmJavaAndroidTest"
          ".android.compact" -> "jvmJavaAndroidCompactTest"
          else -> throw AssertionError("Unknown suffix: $suffix")
        }

        else -> throw AssertionError("Unknown proto folder suffix: $protoFolderSuffix")
      }
      val expectedFile = "../wire-tests/src/$sourceSet/proto-${protoFolderSuffix()}/$path".toPath()
      return expectedFile.also {
        println("Comparing against expected output $name")
      }
    }
  }
}
