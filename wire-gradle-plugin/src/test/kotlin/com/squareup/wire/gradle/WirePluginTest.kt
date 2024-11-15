/*
 * Copyright (C) 2023 Square, Inc.
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

package com.squareup.wire.gradle

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.exists
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isIn
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import com.squareup.wire.VERSION
import com.squareup.wire.testing.withPlatformSlashes
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WirePluginTest {

  @JvmField
  @Rule
  val tmpFolder = TemporaryFolder()

  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setUp() {
    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
      // Ensure individual tests are isolated and not reusing each other's previous outputs
      // by setting project dir and gradle home directly.
      .withProjectDir(tmpFolder.newFolder("project-dir"))
      .withArguments(
        "-g",
        tmpFolder.newFolder("gradle-home").absolutePath,
        "generateProtos",
        "--stacktrace",
        "--info",
        "--configuration-cache",
      )
      .withDebug(true)
  }

  @After
  fun clearOutputs() {
    // We clear outputs otherwise tests' tasks will be skip after their first execution.
    getOutputDirectories(File("src/test/projects")).forEach(::unsafeDelete)
  }

  @Test fun versionIsExposed() {
    assertThat(VERSION).isNotNull()
  }

  @Test
  fun missingPlugin() {
    val fixtureRoot = File("src/test/projects/missing-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output).contains("Wire Gradle plugin applied in project ':' but unable to find either the Java, Kotlin, or Android plugin")
  }

  @Test
  fun sourcePathDirDoesNotExistButProtoPathDoes() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-srcdir-with-protopath")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(result.output).contains("NO-SOURCE")
  }

  @Test
  fun sourcePathDirDoesNotExist() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")?.getOutcome())
      .isEqualTo(TaskOutcome.NO_SOURCE)
  }

  @Test
  fun sourcePathSrcDirDoesNotExist() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-srcdir")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(result.output).contains("NO-SOURCE")
  }

  @Ignore("Since bumping to Gradle8+, it doesn't seem to be working in tests but Benoît was able to make it work in the playground")
  @Test
  fun sourcePathBuildDir() {
    val fixtureRoot = File("src/test/projects/sourcepath-build-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(false).build() }
    assertThat(result.task(":copyProtos")).isNotNull()
    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(result.output).all {
      contains("Writing com.squareup.geology.Period")
      contains("src/test/projects/sourcepath-build-dir/build/generated/source/wire".withPlatformSlashes())
    }
  }

  @Test
  fun requireTarget() {
    val fixtureRoot = File("src/test/projects/require-target")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    val task = result.task(":generateProtos")
    assertThat(task).isNull()
    assertThat(result.output)
      .contains("At least one target must be provided for project")
  }

  @Test
  fun useDefaultSourcePath() {
    val fixtureRoot = File("src/test/projects/sourcepath-default")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun dryRun() {
    val fixtureRoot = File("src/test/projects/dry-run")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    // We didn't generate any file.
    assertThat(outputRoot.walk().toList().filter { it.isFile }).isEmpty()
  }

  @Test
  fun sourcePathWithoutSources() {
    val fixtureRoot = File("src/test/projects/sourcepath-no-sources")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    val task = result.task(":generateMainProtos")
    assertThat(task).isNotNull()
    assertThat(result.output).contains("no sources")
  }

  @Test
  fun sourcePathStringShouldNotBeRegularFile() {
    val fixtureRoot = File("src/test/projects/sourcepath-file")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
      .contains(
        """
        |Invalid path string: "src/main/proto/squareup/geology/period.proto".
        |For individual files, use the following syntax:
        |wire {
        |  sourcePath {
        |    srcDir("dirPath")
        |    include("relativePath")
        |  }
        |}
        """.trimMargin().withPlatformSlashes(),
      )
  }

  @Test
  fun sourcePathStringShouldNotBeUri() {
    val fixtureRoot = File("src/test/projects/sourcepath-uri")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
      .contains(
        """Cannot resolve external dependency http://www.squareup.com because no repositories are defined.""",
      )
  }

  @Test
  fun sourcePathDir() {
    val fixtureRoot = File("src/test/projects/sourcepath-dir")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourcePathMavenCoordinates() {
    val fixtureRoot = File("src/test/projects/sourcepath-maven")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Ignore("Probable ClassLoader problem which makes the test fails")
  @Test
  fun listener() {
    val fixtureRoot = File("src/test/projects/listener")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(result.output).all {
      contains("runStart")
      contains("loadSchemaStart")
      contains("loadSchemaSuccess")
      contains("treeShakeStart")
      contains("treeShakeEnd")
      contains("moveTypesStart")
      contains("moveTypesEnd")
      contains("schemaHandlersStart")
      contains("schemaHandlerStart")
      contains("schemaHandlerEnd")
      contains("schemaHandlersEnd")
      contains("runSuccess")
    }
  }

  @Test
  fun listenerNoSuchClass() {
    val fixtureRoot = File("src/test/projects/listener-no-such-class")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output)
      .contains("Couldn't find EventListenerClass 'NoSuchClass'")
  }

  @Test
  fun sourcePathMavenCoordinatesSingleFile() {
    val fixtureRoot = File("src/test/projects/sourcepath-maven-single-file")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceTreeOneSrcDirOneFile() {
    val fixtureRoot = File("src/test/projects/sourcetree-one-srcdir-one-file")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceTreeOneSrcDirMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcetree-one-srcdir-many-files")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun rejectUnused() {
    val fixtureRoot = File("src/test/projects/reject-unused")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateMainProtos")!!.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output)
      .contains(
        """
        |Unused element(s) in roots:
        |  squareup.dinosaurs.Dinosaur#height
        |  squareup.dinosaurs.Crustacean
        |Unused element(s) in prunes:
        |  squareup.mammals.Human
        """.trimMargin(),
      )
  }

  @Test
  fun sourceTreeMultipleSrcDirs() {
    val fixtureRoot = File("src/test/projects/sourcetree-many-srcdirs")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceJarLocalOneJarMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-many-files")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceJarLocalOneJarMultipleFilesIncludingNonProtos() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-nonproto-file")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceJarLocalOneJarSingleFile() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-single-file")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceJarMixedWithConflictingProtos() {
    val fixtureRoot = File("src/test/projects/sourcejar-mixed-conflicts")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
    assertThat(File(outputRoot, "com/excluded/Martian.java")).doesNotExist()
  }

  @Test
  fun sourceJarRemoteOneJarMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-many-files")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceJarRemoteWildcardIncludes() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-wildcards")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
    assertThat(File(outputRoot, "com/excluded/Martian.java")).doesNotExist()
  }

  @Test
  fun sourceJarRemoteOneJarWithProtoPath() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-protopath")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(true).build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun sourceJarRemoteViaVersionCatalog() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-version-catalog")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(true).build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun sourceZipLocalOneZipWithProtoPath() {
    val fixtureRoot = File("src/test/projects/sourcezip-local-protopath")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(true).build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun sourceAarLocalOneAarWithProtoPath() {
    val fixtureRoot = File("src/test/projects/sourceaar-local-protopath")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(true).build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun projectDependencyViaTypesafeAccessor() {
    val fixtureRoot = File("src/test/projects/project-dependencies-typesafe-accessor")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateMainProtos", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":dinosaurs:generateMainProtos")?.outcome)
      .isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
    val generatedProto1 = File(
      fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt",
    )
    val generatedProto2 = File(
      fixtureRoot,
      "geology/build/generated/source/wire/com/squareup/geology/Period.kt",
    )
    val generatedProto3 = File(
      fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/location/Continent.kt",
    )
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
    assertThat(generatedProto3).exists()

    val notExpected = File(
      fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/location/Planet.kt",
    )
    assertThat(notExpected).doesNotExist()

    ZipFile(File(fixtureRoot, "geology/build/libs/geology.jar")).use {
      assertThat(it.getEntry("squareup/geology/period.proto")).isNotNull()
    }
  }

  @Test
  fun protoPathMavenCoordinates() {
    val fixtureRoot = File("src/test/projects/protopath-maven")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dig.java")).exists()
  }

  @Test
  fun differentJavaOutputDir() {
    val fixtureRoot = File("src/test/projects/different-java-out")
    val outputRoot = File(fixtureRoot, "custom")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun differentKotlinOutputDir() {
    val fixtureRoot = File("src/test/projects/different-kotlin-out")
    val outputRoot = File(fixtureRoot, "custom")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
  }

  @Test
  fun differentProtoOutputDir() {
    val fixtureRoot = File("src/test/projects/different-proto-out")
    val outputRoot = File(fixtureRoot, "custom")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "squareup/geology/period.proto")).exists()
  }

  @Test
  fun kotlinTargetMissingKotlinPlugin() {
    val fixtureRoot = File("src/test/projects/missing-kotlin-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
      .contains(
        "Wire Gradle plugin applied in project ':' but no supported Kotlin plugin was found",
      )
  }

  @Test
  fun rootKeepsField() {
    val fixtureRoot = File("src/test/projects/field-root")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")).doesNotExist()
    val generatedProto =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource)).containsExactly("val name")
  }

  @Test
  fun multipleRoots() {
    val fixtureRoot = File("src/test/projects/field-roots")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")).doesNotExist()
    val generatedProto =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource))
      .containsExactly("val name", "val length_meters")
  }

  @Test
  fun pruneRemovesField() {
    val fixtureRoot = File("src/test/projects/field-prune")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    val actual = File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(actual).exists()
    assertThat(actual.readText())
      .doesNotContain("val name")
  }

  @Test
  fun multiplePrunes() {
    val fixtureRoot = File("src/test/projects/field-prunes")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    val actual = File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(actual).exists()
    assertThat(actual.readText())
      .doesNotContain("val name", "val length_meters")
  }

  @Test
  fun ruleKeepsField() {
    val fixtureRoot = File("src/test/projects/field-rule-root")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")).doesNotExist()
    val generatedProto =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource)).containsExactly("val name")
  }

  @Test
  fun ruleRemovesField() {
    val fixtureRoot = File("src/test/projects/field-rule-prune")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
    val actual = File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(actual).exists()
    assertThat(actual.readText()).doesNotContain("val name")
  }

  @Test
  fun javaProjectJavaProtos() {
    val fixtureRoot = File("src/test/projects/java-project-java-protos")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun javaProjectKotlinProtos() {
    val fixtureRoot = File("src/test/projects/java-project-kotlin-protos")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
  }

  @Test
  fun kotlinProjectJavaProtos() {
    val fixtureRoot = File("src/test/projects/kotlin-project-java-protos")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
  }

  @Test
  fun kotlinProjectKotlinProtos() {
    val fixtureRoot = File("src/test/projects/kotlin-project-kotlin-protos")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
  }

  @Test
  fun protoLibrary() {
    val fixtureRoot = File("src/test/projects/proto-library")

    gradleRunner.runFixture(fixtureRoot) {
      withArguments("jar", "--stacktrace", "--info").build()
    }

    ZipFile(File(fixtureRoot, "build/libs/proto-library.jar")).use {
      assertThat(it.getEntry("squareup/geology/period.proto")).isNotNull()
      assertThat(it.getEntry("squareup/dinosaurs/dinosaur.proto")).isNotNull()
    }
  }

  @Test
  fun sourceDirExclude() {
    val fixtureRoot = File("src/test/projects/sourcedir-exclude")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
    assertThat(File(outputRoot, "com/excluded/Martian.kt")).doesNotExist()
  }

  @Test
  fun sourceDirInclude() {
    val fixtureRoot = File("src/test/projects/sourcedir-include")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull()

    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
    assertThat(File(outputRoot, "com/excluded/Martian.kt")).doesNotExist()
  }

  @Test
  fun sourcePathAndProtoPathIntersect() {
    val fixtureRoot = File("src/test/projects/sourcepath-and-protopath-intersect")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun emitJavaThenEmitKotlin() {
    val fixtureRoot = File("src/test/projects/emit-java-then-emit-kotlin")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun doNotEmitWireRuntimeProtos() {
    val fixtureRoot = File("src/test/projects/do-not-emit-wire-runtime-protos")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    // We should generate Octagon only. Other proto files in this project are all the special ones
    // which Wire doesn't want to generate, google types and Wire extensions.
    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(File(outputRoot, "squareup/polygons/Octagon.java")).exists()
    assertThat(File(outputRoot, "google/protobuf/DescriptorProto.java")).doesNotExist()
  }

  @Test
  fun emitKotlinThenEmitJava() {
    val fixtureRoot = File("src/test/projects/emit-kotlin-then-emit-java")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).doesNotExist()
  }

  @Test
  fun emitKotlinAndEmitJava() {
    val fixtureRoot = File("src/test/projects/emit-kotlin-and-emit-java")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).doesNotExist()
  }

  @Test
  fun emitService() {
    val fixtureRoot = File("src/test/projects/emit-service")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).exists()
  }

  @Test
  fun dontEmitServiceIfRoleIsNone() {
    val fixtureRoot = File("src/test/projects/emit-service-none")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(result.output).doesNotContain("Service")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).doesNotExist()
  }

  @Test
  fun emitServiceTwoWays() {
    val fixtureRoot = File("src/test/projects/emit-service-two-ways")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt"))
      .exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceFightBlockingServer.kt"))
      .exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceBrawlBlockingServer.kt"))
      .exists()
  }

  @Test
  fun emitServiceWithSpecificSuffix() {
    val fixtureRoot = File("src/test/projects/emit-service-name-suffix")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }
    val task = result.task(":generateProtos")

    assertThat(task).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).exists()
  }

  /**
   * This test is symmetric with [protoPathMavenCoordinates] but it manipulates the configuration
   * directly. We expect this to be useful in cases where users want to make dependency resolution
   * non-transitive.
   */
  @Test
  fun customizeConfiguration() {
    val fixtureRoot = File("src/test/projects/customize-configuration")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dig.java")).exists()
  }

  /**
   * This test manipulates the tasks directly. We expect this to be useful in cases where users want to make
   * source file available to embedded dependancies.
   */
  @Test
  fun customizeTask() {
    val fixtureRoot = File("src/test/projects/customize-task")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull()
    assertThat(result.task(":helloWorld")).isNotNull()
    assertThat(result.output).contains("Hello, World!")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dig.java")).exists()
  }

  @Ignore("Probable ClassLoader problem which makes the test fails")
  @Test
  fun customOutput() {
    val fixtureRoot = File("src/test/projects/custom-output")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output)
      .contains(
        "custom handler is running!! " +
          "squareup.dinosaurs.Dinosaur, " +
          "squareup.geology.Period, true, " +
          "a=one, b=two, c=three",
      )
  }

  @Test
  fun customOutputNoSuchClass() {
    val fixtureRoot = File("src/test/projects/custom-output-no-such-class")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output)
      .contains("Couldn't find SchemaHandlerClass 'NoSuchClass'")
  }

  @Test
  fun sinceUntil() {
    val fixtureRoot = File("src/test/projects/since-until")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    val actual = File(outputRoot, "com/squareup/media/NewsFlash.kt")
    assertThat(actual).exists()
    assertThat(actual.readText()).all {
      contains("val tv")
      contains("val website")
      doesNotContain("val radio")
    }
  }

  @Test
  fun only() {
    val fixtureRoot = File("src/test/projects/only-version")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()
    val actual = File(outputRoot, "com/squareup/media/NewsFlash.kt")
    assertThat(actual).exists()
    assertThat(actual.readText()).all {
      contains("val tv")
      doesNotContain("val website")
      doesNotContain("val radio")
    }
  }

  @Test
  fun kotlinMultiplatform() {
    val fixtureRoot = File("src/test/projects/kotlin-multiplatform")
    val kmpJsEnabled = System.getProperty("kjs", "true")!!.toBoolean()
    val kmpNativeEnabled = System.getProperty("knative", "true")!!.toBoolean()

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments(
        "assemble",
        "--stacktrace",
        "-Dkjs=$kmpJsEnabled",
        "-Dknative=$kmpNativeEnabled",
        "--debug",
      ).build()
    }

    println(result.tasks.joinToString { it.toString() })
    assertThat(result.task(":generateCommonMainProtos")).isNotNull()
    assertThat(result.output).all {
      contains("Writing com.squareup.dinosaurs.Dinosaur")
      contains("Writing com.squareup.geology.Period")
      contains("src/test/projects/kotlin-multiplatform/build/generated/source/wire".withPlatformSlashes())
    }

    val generatedProto1 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val generatedProto2 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
  }

  private fun fieldsFromProtoSource(generatedProtoSource: String): List<String> {
    val protoFieldPattern = "@field:WireField.*?(val .*?):"
    val matchedFields = protoFieldPattern.toRegex(setOf(MULTILINE, DOT_MATCHES_ALL))
      .findAll(generatedProtoSource)
    return matchedFields
      .map { it.groupValues[1] }
      .toList()
  }

  @Test
  fun emitProto() {
    val fixtureRoot = File("src/test/projects/emit-proto")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).exists()
    assertThat(File(outputRoot, "squareup/geology/period.proto")).exists()
  }

  @Test
  fun emitProtoWithPrune() {
    val fixtureRoot = File("src/test/projects/emit-proto-with-prune")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).exists()
    assertThat(File(outputRoot, "squareup/geology/period.proto")).doesNotExist()
  }

  @Test
  fun emitProtoWithRoot() {
    val fixtureRoot = File("src/test/projects/emit-proto-with-root")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "squareup/geology/period.proto")).exists()
    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).doesNotExist()
  }

  @Test
  fun consecutiveRuns() {
    val fixtureRoot = File("src/test/projects/consecutive-runs")
    val outputRoot = File(fixtureRoot, "custom")

    val firstRun = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(firstRun.task(":generateMainProtos")).isNotNull()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()

    val secondRun = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(secondRun.task(":generateMainProtos")).isNotNull()
    assertThat(secondRun.output)
      .contains("Task :generateMainProtos UP-TO-DATE")
  }

  @Test
  fun moveMessage() {
    val fixtureRoot = File("src/test/projects/move-message")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto").readText())
      .contains("import \"squareup/geology/geology.proto\";")

    assertThat(File(outputRoot, "squareup/geology/geology.proto").readText())
      .contains("enum Period {")
  }

  @Test
  fun opaqueMessage() {
    val fixtureRoot = File("src/test/projects/opaque-message")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()

    assertThat(File(outputRoot, "cafe/cafe.proto").readText()).all {
      contains("repeated bytes shots")
      doesNotContain("repeated EspressoShot shots")
    }
  }

  @Test
  fun emitJavaOptions() {
    val fixtureRoot = File("src/test/projects/emit-java-options")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(outputRoot, "squareup/polygons/Octagon.java").readText())
      .contains("""@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")""")
  }

  @Test
  fun emitKotlinOptions() {
    val fixtureRoot = File("src/test/projects/emit-kotlin-options")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(outputRoot, "squareup/polygons/Octagon.kt").readText())
      .contains("""@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")""")
  }

  @Test
  fun emitOptionsWithIncludes() {
    val fixtureRoot = File("src/test/projects/emit-options-with-includes")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(outputRoot, "squareup/polygons/Octagon.kt").readText()).all {
      contains("""@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")""")
      contains("""@CustomerSupportUrlOption("https://en.wikipedia.org/wiki/Customer_support")""")
    }
    assertThat(File(outputRoot, "squareup/other_options/CustomerSupportUrlOption.kt"))
      .doesNotExist()
  }

  @Test
  fun emitOptionsWithoutConflicts() {
    val fixtureRoot = File("src/test/projects/emit-options-without-conflicts")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    assertThat(File(outputRoot, "squareup/polygons/Octagon.kt").readText()).all {
      contains(
        """@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")
          |public class Octagon(
        """.trimMargin(),
      )
      // Although we didn't generate the annotation, we still apply it!
      contains(
        """  @DocumentationUrlFieldOption("https://en.wikipedia.org/wiki/stop")
          |  @field:WireField(
        """.trimMargin(),
      )
    }
  }

  @Test
  fun kotlinEnumMode() {
    val fixtureRoot = File("src/test/projects/kotlin-enum-mode")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull()

    // Wire has been configured so that `Continent` should always be the opposite of the global
    // setting while `Period` and `Drink` match it.

    assertThat(File(outputRoot, "com/squareup/enum/geology/Period.kt").readText())
      .contains("enum class Period")
    assertThat(File(outputRoot, "com/squareup/enum/geology/Continent.kt").readText())
      .contains("sealed class Continent")
    assertThat(File(outputRoot, "com/squareup/enum/geology/Drink.kt").readText())
      .contains("enum class Drink")
    assertThat(File(outputRoot, "com/squareup/sealed/geology/Period.kt").readText())
      .contains("sealed class Period")
    assertThat(File(outputRoot, "com/squareup/sealed/geology/Continent.kt").readText())
      .contains("enum class Continent")
    assertThat(File(outputRoot, "com/squareup/sealed/geology/Drink.kt").readText())
      .contains("sealed class Drink")
  }

  @Test
  fun packageCycles() {
    val fixtureRoot = File("src/test/projects/package-cycles")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output).contains("packages form a cycle")
  }

  @Test
  fun packageCyclesPermitted() {
    val fixtureRoot = File("src/test/projects/package-cycles-permitted")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(File(outputRoot, "people/Employee.kt")).exists()
    assertThat(File(outputRoot, "people/OfficeManager.kt")).exists()
    assertThat(File(outputRoot, "locations/Office.kt")).exists()
    assertThat(File(outputRoot, "locations/Residence.kt")).exists()
  }

  @Test
  fun projectDependencies() {
    val fixtureRoot = File("src/test/projects/project-dependencies")
    val dinosaursOutputRoot = File(fixtureRoot, "dinosaurs/build/generated/source/wire")
    val geologyOutputRoot = File(fixtureRoot, "geology/build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateMainProtos", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":dinosaurs:generateMainProtos")?.outcome)
      .isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
    assertThat(File(dinosaursOutputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(geologyOutputRoot, "com/squareup/geology/Period.kt")).exists()
    assertThat(File(dinosaursOutputRoot, "com/squareup/location/Continent.kt")).exists()
    assertThat(File(dinosaursOutputRoot, "com/squareup/location/Planet.kt")).doesNotExist()

    ZipFile(File(fixtureRoot, "geology/build/libs/geology.jar")).use {
      assertThat(it.getEntry("squareup/geology/period.proto")).isNotNull()
    }
  }

  @Test
  fun cacheRelocation() {
    // Remove the build cache folder if it is leftover from a previous run
    val buildCacheDir = File("src/test/projects/.relocation-build-cache")
    if (buildCacheDir.exists()) {
      buildCacheDir.deleteRecursively()
    }
    assertThat(buildCacheDir.exists()).isFalse()

    val generatedProto = "build/generated/source/wire/com/squareup/geology/Period.kt"

    val fixtureRoot = File("src/test/projects/cache-relocation-1")
    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(fixtureRoot, generatedProto)).exists()

    // After the first project, the build cache should exist. It will get used for the second
    // project.
    assertThat(buildCacheDir.exists()).isTrue()

    val relocatedRoot = File("src/test/projects/cache-relocation-2")
    val relocatedResult = gradleRunner.runFixture(relocatedRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
    }

    assertThat(relocatedResult.task(":generateProtos")).isNotNull()
    assertThat(relocatedResult.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
    assertThat(relocatedResult.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(File(relocatedRoot, generatedProto)).exists()

    // Clean up on success; leave the dir on failure for easier debugging.
    buildCacheDir.deleteRecursively()
  }

  @Test
  fun cacheHappyPaths() {
    val buildCacheDir = File("src/test/projects/.cache-include-paths-build-cache")
    if (buildCacheDir.exists()) {
      assertThat(buildCacheDir.deleteRecursively()).isTrue()
    }

    val generatedPeriodProto = "build/generated/source/wire/com/squareup/geology/Period.kt"

    val fixtureRoot = File("src/test/projects/cache-include-paths-1")
    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--debug").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(fixtureRoot, generatedPeriodProto)).exists()

    // The task has been cached.
    assertThat(buildCacheDir.exists()).isTrue()

    val cachedResult = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--debug").build()
    }

    assertThat(cachedResult.task(":generateProtos")).isNotNull()
    assertThat(cachedResult.output).doesNotContain("Writing com.squareup.geology.Period")
    assertThat(cachedResult.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(cachedResult.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(File(fixtureRoot, generatedPeriodProto)).exists()

    buildCacheDir.deleteRecursively()
  }

  @Test
  fun cacheKeyIncludePaths() {
    val buildCacheDir = File("src/test/projects/.cache-include-paths-build-cache")
    if (buildCacheDir.exists()) {
      assertThat(buildCacheDir.deleteRecursively()).isTrue()
    }

    val generatedPeriodProto = "build/generated/source/wire/com/squareup/geology/Period.kt"
    val generatedDinosaurProto = "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt"

    val fixtureRoot = File("src/test/projects/cache-include-paths-1")
    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(fixtureRoot, generatedPeriodProto)).exists()
    assertThat(File(fixtureRoot, generatedDinosaurProto)).doesNotExist()

    // The task has been cached.
    assertThat(buildCacheDir.exists()).isTrue()

    // Even though the task is now cached, the configuration of the sourcePath has now changed. We
    // expect the new task to run again, without using the cache.
    val modifiedFixtureRoot = File("src/test/projects/cache-include-paths-2")
    val modifiedResult = gradleRunner.runFixture(modifiedFixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--debug").build()
    }

    assertThat(modifiedResult.task(":generateProtos")).isNotNull()
    assertThat(modifiedResult.output).contains("Writing com.squareup.geology.Period")
    assertThat(modifiedResult.output).contains("Writing com.squareup.dinosaurs.Dinosaur")
    assertThat(modifiedResult.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(modifiedResult.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(modifiedFixtureRoot, generatedPeriodProto)).exists()
    assertThat(File(modifiedFixtureRoot, generatedDinosaurProto)).exists()

    buildCacheDir.deleteRecursively()
  }

  @Test
  fun configurationCacheFailure() {
    val fixtureRoot = File("src/test/projects/configuration-cache-failure")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("clean", "generateMainProtos", "--stacktrace", "--info").build()
    }
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun kotlinSourcesJarHasSingleCopyOnly() {
    val fixtureRoot = File("src/test/projects/kotlinsourcesjar")

    gradleRunner.runFixture(fixtureRoot) {
      withArguments("clean", "kotlinSourcesJar", "--stacktrace", "--info").build()
    }

    ZipFile(File(fixtureRoot, "build/libs/kotlinsourcesjar-sources.jar")).use {
      assertThat(it.stream().filter { it.name.contains("Period.kt") }.count()).isEqualTo(1)
    }
  }

  @Test
  fun taskDependency() {
    val fixtureRoot = File("src/test/projects/task-dependency")
    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("clean", "generateMainProtos", "--stacktrace", "--info").build()
    }
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(outputRoot, "Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "Period.kt")).exists()
  }

  private fun GradleRunner.runFixture(
    root: File,
    action: GradleRunner.() -> BuildResult,
  ): BuildResult {
    var generatedSettings = false
    val settings = File(root, "settings.gradle")
    var generatedGradleProperties = false
    val gradleProperties = File(root, "gradle.properties")
    return try {
      if (!settings.exists()) {
        settings.createNewFile()
        generatedSettings = true
      }

      if (!gradleProperties.exists()) {
        val rootGradleProperties = File("../gradle.properties")
        if (!rootGradleProperties.exists()) {
          fail("Root gradle.properties doesn't exist at $rootGradleProperties.")
        }
        val versionName = rootGradleProperties.useLines { lines ->
          lines.firstOrNull { it.startsWith("VERSION_NAME") }
        }
        if (versionName == null) {
          fail("Root gradle.properties is missing the VERSION_NAME entry.")
        }
        gradleProperties.createNewFile()
        gradleProperties.writeText(versionName!!)
        generatedGradleProperties = true
      } else {
        gradleProperties.useLines { lines ->
          if (lines.none { it.startsWith("VERSION_NAME") }) {
            fail("Fixture's gradle.properties has to include the VERSION_NAME entry.")
          }
        }
      }

      withProjectDir(root).action()
    } finally {
      if (generatedSettings) settings.delete()
      if (generatedGradleProperties) gradleProperties.delete()
    }
  }

  companion object {
    private val OUTPUT_DIRECTORY_NAMES = arrayOf("build", "custom")

    private fun getOutputDirectories(root: File): List<File> {
      if (!root.isDirectory) return emptyList()
      if (root.isDirectory && root.name in OUTPUT_DIRECTORY_NAMES) return listOf(root)
      return root.listFiles()!!.flatMap { getOutputDirectories(it) }
    }

    // This follows symlink so don't use it at home.
    @Throws(IOException::class)
    fun unsafeDelete(f: File) {
      if (f.isDirectory) {
        for (c in f.listFiles()!!) unsafeDelete(c)
      }
      f.delete()
    }
  }
}

/**
 * Asserts the file does not exists.
 */
private fun Assert<File>.doesNotExist() = given { actual ->
  if (!actual.exists()) return
  expected("not to exist")
}
