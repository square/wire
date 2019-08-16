/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire

import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.FileSystems
import java.util.ArrayList
import java.util.Collections

class WireCompilerTest {
  @Rule @JvmField val temp = TemporaryFolder()

  private var logger: StringWireLogger? = null
  private lateinit var testDir: File

  /** Returns all paths within `root`, and relative to `root`.  */
  private val paths: List<String>
    get() {
      val paths = mutableListOf<String>()
      getPathsRecursive(testDir.absoluteFile, "", paths)
      return paths
    }

  @Before fun setUp() {
    testDir = temp.root
  }

  @Test
  fun testFooBar() {
    val sources = arrayOf("foo.proto", "bar.proto")
    compileToJava(sources)

    val outputs = arrayOf(
        "com/squareup/foobar/protos/bar/Bar.java",
        "com/squareup/foobar/protos/foo/Foo.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testDifferentPackageFooBar() {
    val sources = arrayOf("differentpackage/foo.proto", "differentpackage/bar.proto")
    compileToJava(sources)

    val outputs = arrayOf(
        "com/squareup/differentpackage/protos/bar/Bar.java",
        "com/squareup/differentpackage/protos/foo/Foo.java")
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
        "com/squareup/wire/protos/foreign/ForeignMessage.java")
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
        "com/squareup/wire/protos/single_level/Foos.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testSameBasename() {
    val sources = arrayOf(
        "single_level.proto",
        "samebasename/single_level.proto")
    compileToJava(sources)

    val outputs = arrayOf(
        "com/squareup/wire/protos/single_level/Foo.java",
        "com/squareup/wire/protos/single_level/Foos.java",
        "com/squareup/wire/protos/single_level/Bar.java",
        "com/squareup/wire/protos/single_level/Bars.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testChildPackage() {
    val sources = arrayOf("child_pkg.proto")
    compileToJava(sources, "--named_files_only")

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
        "com/squareup/wire/protos/edgecases/Recursive.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testUnknownFields() {
    val sources = arrayOf("unknown_fields.proto")
    compileToJava(sources)

    val outputs = arrayOf(
        "com/squareup/wire/protos/unknownfields/VersionOne.java",
        "com/squareup/wire/protos/unknownfields/VersionTwo.java",
        "com/squareup/wire/protos/unknownfields/NestedVersionOne.java",
        "com/squareup/wire/protos/unknownfields/NestedVersionTwo.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testCustomOptions() {
    val sources = arrayOf("custom_options.proto", "option_redacted.proto")
    compileToJava(sources, "--named_files_only")

    val outputs = arrayOf(
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testCustomOptionsNoOptions() {
    val sources = arrayOf("custom_options.proto", "option_redacted.proto")
    compileToJava(sources, "--excludes=google.protobuf.*", "--named_files_only")

    val outputs = arrayOf(
        "com/squareup/wire/protos/custom_options/FooBar.java",
        "com/squareup/wire/protos/custom_options/MessageWithOptions.java")
    assertJavaOutputs(outputs, ".noOptions")
  }

  @Test
  fun testRedacted() {
    val sources = arrayOf("redacted_test.proto", "option_redacted.proto")
    compileToJava(sources)

    val outputs = arrayOf(
        "com/squareup/wire/protos/redacted/NotRedacted.java",
        "com/squareup/wire/protos/redacted/Redacted.java",
        "com/squareup/wire/protos/redacted/RedactedChild.java",
        "com/squareup/wire/protos/redacted/RedactedCycleA.java",
        "com/squareup/wire/protos/redacted/RedactedCycleB.java",
        "com/squareup/wire/protos/redacted/RedactedExtension.java",
        "com/squareup/wire/protos/redacted/RedactedRepeated.java",
        "com/squareup/wire/protos/redacted/RedactedRequired.java")
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
            "com/squareup/wire/protos/roots/K.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testExcludes() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources,
        "--includes=squareup.protos.roots.A",
        "--excludes=squareup.protos.roots.B")

    val outputs =
        arrayOf(
            "com/squareup/wire/protos/roots/A.java",
            "com/squareup/wire/protos/roots/D.java")
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
            "com/squareup/wire/protos/roots/D.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsB() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.B")

    val outputs = arrayOf(
        "com/squareup/wire/protos/roots/B.java",
        "com/squareup/wire/protos/roots/C.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsE() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.E")

    val outputs = arrayOf(
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/G.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testRootsH() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.H")

    val outputs = arrayOf(
        "com/squareup/wire/protos/roots/E.java",
        "com/squareup/wire/protos/roots/H.java")
    assertJavaOutputs(outputs, ".pruned")
  }

  @Test
  fun testRootsI() {
    val sources = arrayOf("roots.proto")
    compileToJava(sources, "--includes=squareup.protos.roots.I")

    val outputs = arrayOf(
        "com/squareup/wire/protos/roots/I.java",
        "com/squareup/wire/protos/roots/J.java",
        "com/squareup/wire/protos/roots/K.java")
    assertJavaOutputs(outputs)
  }

  @Test
  fun testDryRun() {
    val sources = arrayOf("service_root.proto")
    compileToJava(sources, "--includes=squareup.wire.protos.roots.TheService", "--dry_run",
        "--quiet")

    assertThat(logger!!.log).contains(
        testDir.absolutePath + " com.squareup.wire.protos.roots.TheRequest\n",
        testDir.absolutePath + " com.squareup.wire.protos.roots.TheResponse\n")
  }

  @Test
  fun serviceAreIgnoredForJava(){
    val sources = arrayOf("service_root.proto")
    compileToJava(sources, "--includes=squareup.wire.protos.roots.TheService")

    // TheService is not created.
    val outputs = arrayOf(
        "com/squareup/wire/protos/roots/TheResponse.java",
        "com/squareup/wire/protos/roots/TheRequest.java"
    )
    assertJavaOutputs(outputs)
  }

  @Test
  fun serviceInKotlin() {
    val sources = arrayOf("service_kotlin.proto")
    compileToKotlin(sources, "--includes=squareup.protos.kotlin.SomeService")

    val outputs = arrayOf(
        "com/squareup/wire/protos/kotlin/SomeServiceClient.kt",
        "com/squareup/wire/protos/kotlin/SomeResponse.kt",
        "com/squareup/wire/protos/kotlin/SomeRequest.kt"
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun serviceWithoutPackageInKotlin() {
    val sources = arrayOf("service_without_package.proto")
    compileToKotlin(sources, "--includes=NoPackageService")

    val outputs = arrayOf(
        "com/squareup/wire/protos/kotlin/NoPackageServiceClient.kt",
        "com/squareup/wire/protos/kotlin/NoPackageResponse.kt",
        "com/squareup/wire/protos/kotlin/NoPackageRequest.kt"
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
  fun testRedactedKotlin() {
    val sources = arrayOf("redacted_test.proto", "option_redacted.proto")
    compileToKotlin(sources)

    val outputs = arrayOf(
        "com/squareup/wire/protos/kotlin/redacted/NotRedacted.kt",
        "com/squareup/wire/protos/kotlin/redacted/Redacted.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedChild.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedCycleA.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedCycleB.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedRepeated.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedRequired.kt",
        "com/squareup/wire/protos/kotlin/redacted/RedactedExtension.kt"
    )
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testRedactedOneOfKotlin() {
    val sources = arrayOf("redacted_one_of.proto", "option_redacted.proto")
    compileToKotlin(sources)

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/redacted/RedactedOneOf.kt")
    assertKotlinOutputs(outputs)
  }

  @Test
  fun testRedactedOneOfJavaInteropKotlin() {
    val sources = arrayOf("redacted_one_of.proto", "option_redacted.proto")
    compileToKotlin(sources, "--java_interop")

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/redacted/RedactedOneOf.kt")
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
    compileToKotlin(sources)

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

    val outputs = arrayOf("com/squareup/wire/protos/kotlin/VeryLongProtoNameCausingBrokenLineBreaks.kt")
    assertKotlinOutputs(outputs)
  }

  private fun compileToJava(sources: Array<String>, vararg extraArgs: String) =
      invokeCompiler(TargetLanguage.JAVA, sources, *extraArgs)

  private fun compileToKotlin(sources: Array<String>, vararg extraArgs: String) =
      invokeCompiler(TargetLanguage.KOTLIN, sources, *extraArgs)

  private fun invokeCompiler(
    target: TargetLanguage,
    sources: Array<String>,
    vararg extraArgs: String
  ) {
    val args = ArrayList<String>()
    args.add(target.protoPathArg())
    args.add(target.outArg(testDir.absolutePath))
    Collections.addAll(args, *extraArgs)
    Collections.addAll(args, *sources)

    logger = StringWireLogger()
    val fs = FileSystems.getDefault()
    val compiler = WireCompiler.forArgs(fs, logger!!, *args.toTypedArray())
    compiler.compile()
  }

  private fun assertJavaOutputs(outputs: Array<String>, suffix: String = "") =
      assertOutputs(TargetLanguage.JAVA, outputs, suffix)

  private fun assertKotlinOutputs(outputs: Array<String>, suffix: String = "") =
      assertOutputs(TargetLanguage.KOTLIN, outputs, suffix)

  private fun assertOutputs(target: TargetLanguage, outputs: Array<String>, suffix: String = "") {
    val filesAfter = paths
    assertThat(filesAfter.size)
        .overridingErrorMessage(filesAfter.toString())
        .isEqualTo(outputs.size)

    for (output in outputs) {
      assertFilesMatch(target, testDir, output, suffix)
    }
  }

  private fun getPathsRecursive(base: File, path: String, paths: MutableList<String>) {
    val file = File(base, path)

    val children = file.list() ?: return

    for (child in children) {
      val childFile = File(file, child)
      if (childFile.isFile) {
        paths.add(path + child)
      } else {
        getPathsRecursive(base, "$path$child/", paths)
      }
    }
  }

  private fun assertFilesMatch(
    target: TargetLanguage,
    outputDir: File,
    path: String,
    suffix: String
  ) {
    // Compare against file with suffix if present
    val expectedFile = target.expectedFile(path, suffix)
    val actualFile = File(outputDir, path)
    assertFilesMatch(expectedFile, actualFile)
  }

  private fun assertFilesMatch(expectedFile: File, actualFile: File) {
    var expected = expectedFile.source().use { it.buffer().readUtf8() }
    var actual = actualFile.source().use { it.buffer().readUtf8() }

    // Normalize CRLF -> LF.
    expected = expected.replace("\r\n", "\n")
    actual = actual.replace("\r\n", "\n")
    assertThat(actual).isEqualTo(expected)
  }

  private enum class TargetLanguage {
    JAVA {
      override fun protoPathArg() = "--proto_path=../wire-tests/src/commonTest/proto"
      override fun outArg(testDirPath: String) = "--java_out=$testDirPath"
      override fun protoFolderSuffix() = "java"
    },
    KOTLIN {
      override fun protoPathArg() = "--proto_path=../wire-tests/src/commonTest/proto/kotlin"
      override fun outArg(testDirPath: String) = "--kotlin_out=$testDirPath"
      override fun protoFolderSuffix() = "kotlin"
    };

    internal abstract fun protoPathArg(): String
    internal abstract fun outArg(testDirPath: String): String
    internal abstract fun protoFolderSuffix(): String

    internal fun expectedFile(path: String, suffix: String): File {
      val protoFolder = "/proto-${protoFolderSuffix()}/"
      var expectedFile = File("../wire-tests/src/jvmTest$protoFolder$path$suffix")
      if (expectedFile.exists()) {
        println("Comparing against expected output ${expectedFile.name}")
      } else {
        expectedFile = File("../wire-tests/src/jvmTest$protoFolder$path")
      }
      return expectedFile
    }
  }
}
