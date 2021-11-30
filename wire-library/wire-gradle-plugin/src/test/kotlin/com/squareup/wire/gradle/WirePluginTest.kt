@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire.gradle

import okio.Path
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

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

  @Test
  fun missingPlugin() {
    val fixtureRoot = File("src/test/projects/missing-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output).contains("Missing either the Java, Kotlin, or Android plugin")
  }

  @Test
  fun sourcePathDirDoesNotExist() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output).contains(
            """
            |Invalid path string: "src/main/proto".
            |For individual files, use the following syntax:
            |wire {
            |  sourcePath {
            |    srcDir 'dirPath'
            |    include 'relativePath'
            |  }
            |}
            """.trimMargin())
  }

  @Test
  fun sourcePathSrcDirDoesNotExist() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-srcdir")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output).contains("NO-SOURCE")
  }

  @Test
  fun sourcePathBuildDir() {
    val fixtureRoot = File("src/test/projects/sourcepath-build-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(result.task(":copyProtos")).isNotNull
    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
            .contains("Writing com.squareup.geology.Period")
            .contains("src/test/projects/sourcepath-build-dir/build/generated/source/wire".withPlatformSlashes())
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

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcepath-default/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun sourcePathWithoutSources() {
    val fixtureRoot = File("src/test/projects/sourcepath-no-sources")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    val task = result.task(":generateMainProtos")
    assertThat(task).isNotNull
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
            |    srcDir 'dirPath'
            |    include 'relativePath'
            |  }
            |}
            """.trimMargin()
        )
  }

  @Test
  fun sourcePathStringShouldNotBeUri() {
    val fixtureRoot = File("src/test/projects/sourcepath-uri")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
        .contains(
            """Invalid path string: "http://www.squareup.com". URL dependencies are not allowed."""
        )
  }

  @Test
  fun sourcePathDir() {
    val fixtureRoot = File("src/test/projects/sourcepath-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcepath-dir/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun sourcePathMavenCoordinates() {
    val fixtureRoot = File("src/test/projects/sourcepath-maven")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcepath-maven/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun sourcePathMavenCoordinatesSingleFile() {
    val fixtureRoot = File("src/test/projects/sourcepath-maven-single-file")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
  }

  @Test
  fun sourceTreeOneSrcDirOneFile() {
    val fixtureRoot = File("src/test/projects/sourcetree-one-srcdir-one-file")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcetree-one-srcdir-one-file/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun sourceTreeOneSrcDirMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcetree-one-srcdir-many-files")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcetree-one-srcdir-many-files/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceTreeMultipleSrcDirs() {
    val fixtureRoot = File("src/test/projects/sourcetree-many-srcdirs")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcetree-many-srcdirs/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun sourceJarLocalOneJarMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-many-files")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcejar-local-many-files/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceJarLocalOneJarMultipleFilesIncludingNonProtos() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-nonproto-file")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcejar-local-nonproto-file/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceJarLocalOneJarSingleFile() {
    val fixtureRoot = File("src/test/projects/sourcejar-local-single-file")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcejar-local-single-file/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceJarMixedWithConflictingProtos() {
    val fixtureRoot = File("src/test/projects/sourcejar-mixed-conflicts")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
      .contains("Writing com.squareup.dinosaurs.Dinosaur")
      .contains("Writing com.squareup.geology.Period")
      .doesNotContain("Writing com.excluded.Martian")
      .contains(
        "src/test/projects/sourcejar-mixed-conflicts/build/generated/source/wire".withPlatformSlashes()
      )
  }

  @Test
  fun sourceJarRemoteOneJarMultipleFiles() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-many-files")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcejar-remote-many-files/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceJarRemoteWildcardIncludes() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-wildcards")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .doesNotContain("Writing com.excluded.Martian")
        .contains(
            "src/test/projects/sourcejar-remote-wildcards/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun sourceJarRemoteOneJarWithProtoPath() {
    val fixtureRoot = File("src/test/projects/sourcejar-remote-protopath")

    val result = gradleRunner.runFixture(fixtureRoot) { withDebug(true).build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .doesNotContain("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcejar-remote-protopath/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun protoPathMavenCoordinates() {
    val fixtureRoot = File("src/test/projects/protopath-maven")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.dinosaurs.Dinosaur")
        .doesNotContain("Writing com.squareup.geology.Period")
        .contains("Writing com.squareup.dinosaurs.Dig")
        .contains("src/test/projects/protopath-maven/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun differentJavaOutputDir() {
    val fixtureRoot = File("src/test/projects/different-java-out")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/different-java-out/custom".withPlatformSlashes())
  }

  @Test
  fun differentKotlinOutputDir() {
    val fixtureRoot = File("src/test/projects/different-kotlin-out")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/different-kotlin-out/custom".withPlatformSlashes())
  }

  @Test
  fun differentProtoOutputDir() {
    val fixtureRoot = File("src/test/projects/different-proto-out")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing squareup/geology/period.proto")
        .contains("src/test/projects/different-proto-out/custom".withPlatformSlashes())
  }

  @Test
  fun kotlinTargetMissingKotlinPlugin() {
    val fixtureRoot = File("src/test/projects/missing-kotlin-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
        .contains(
            "Wire Gradle plugin applied in project ':' but no supported Kotlin plugin was found")
  }

  @Test
  fun rootKeepsField() {
    val fixtureRoot = File("src/test/projects/field-root")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource)).containsOnly("val name")
  }

  @Test
  fun multipleRoots() {
    val fixtureRoot = File("src/test/projects/field-roots")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource))
        .containsOnly("val name", "val length_meters")
  }

  @Test
  fun pruneRemovesField() {
    val fixtureRoot = File("src/test/projects/field-prune")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    assertThat(generatedProto.readText()).doesNotContain("val name")
  }

  @Test
  fun multiplePrunes() {
    val fixtureRoot = File("src/test/projects/field-prunes")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    assertThat(generatedProto.readText()).doesNotContain("val name", "val length_meters")
  }

  @Test
  fun ruleKeepsField() {
    val fixtureRoot = File("src/test/projects/field-rule-root")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    val generatedProtoSource = generatedProto.readText()
    assertThat(fieldsFromProtoSource(generatedProtoSource)).containsOnly("val name")
  }

  @Test
  fun ruleRemovesField() {
    val fixtureRoot = File("src/test/projects/field-rule-prune")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    assertThat(generatedProto).exists()

    assertThat(generatedProto.readText()).doesNotContain("val name")
  }

  @Test
  fun javaProjectJavaProtos() {
    val fixtureRoot = File("src/test/projects/java-project-java-protos")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/java-project-java-protos/build/generated/source/wire".withPlatformSlashes())

    val generatedProto1 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.java")
    val generatedProto2 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.java")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
  }

  @Test
  fun javaProjectKotlinProtos() {
    val fixtureRoot = File("src/test/projects/java-project-kotlin-protos")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/java-project-kotlin-protos/build/generated/source/wire".withPlatformSlashes())

    val generatedProto1 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val generatedProto2 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
  }

  @Test
  fun kotlinProjectJavaProtos() {
    val fixtureRoot = File("src/test/projects/kotlin-project-java-protos")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/kotlin-project-java-protos/build/generated/source/wire".withPlatformSlashes())

    val generatedProto1 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.java")
    val generatedProto2 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.java")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
  }

  @Test
  fun kotlinProjectKotlinProtos() {
    val fixtureRoot = File("src/test/projects/kotlin-project-kotlin-protos")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/kotlin-project-kotlin-protos/build/generated/source/wire".withPlatformSlashes())

    val generatedProto1 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val generatedProto2 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
  }

  @Test
  fun sourceDirExclude() {
    val fixtureRoot = File("src/test/projects/sourcedir-exclude")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
      .contains("Writing com.squareup.dinosaurs.Dinosaur")
      .contains("Writing com.squareup.geology.Period")
      .contains("src/test/projects/sourcedir-exclude/build/generated/source/wire".withPlatformSlashes())
    assertThat(result.output)
      .doesNotContain("Writing com.excluded.Martian")

    val includedFile1 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val includedFile2 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(includedFile1).exists()
    assertThat(includedFile2).exists()
    val excludedFile =
      File(fixtureRoot, "build/generated/source/wire/com/excluded/Martian.kt")
    assertThat(excludedFile).doesNotExist()
  }

  @Test
  fun sourceDirInclude() {
    val fixtureRoot = File("src/test/projects/sourcedir-include")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("run", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.output)
      .contains("Writing com.squareup.dinosaurs.Dinosaur")
      .contains("Writing com.squareup.geology.Period")
      .contains("src/test/projects/sourcedir-include/build/generated/source/wire".withPlatformSlashes())
    assertThat(result.output)
      .doesNotContain("Writing com.excluded.Martian")

    val includedFile1 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val includedFile2 =
      File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(includedFile1).exists()
    assertThat(includedFile2).exists()
    val excludedFile =
      File(fixtureRoot, "build/generated/source/wire/com/excluded/Martian.kt")
    assertThat(excludedFile).doesNotExist()
  }

  @Test
  fun sourcePathAndProtoPathIntersect() {
    val fixtureRoot = File("src/test/projects/sourcepath-and-protopath-intersect")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .doesNotContain("Writing com.squareup.geology.Period")
        .contains(
            "src/test/projects/sourcepath-and-protopath-intersect/build/generated/source/wire".withPlatformSlashes()
        )
  }

  @Test
  fun emitJavaThenEmitKotlin() {
    val fixtureRoot = File("src/test/projects/emit-java-then-emit-kotlin")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).doesNotExist()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).doesNotExist()
  }

  @Test
  fun doNotEmitWireRuntimeProtos() {
    val fixtureRoot = File("src/test/projects/do-not-emit-wire-runtime-protos")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    // We should generate Octagon only. Other proto files in this project are all the special ones
    // which Wire doesn't want to generate, google types and Wire extensions.
    val task = result.task(":generateProtos")
    assertThat(task).isNotNull()
    assertThat(result.output)
      .contains("Writing squareup.polygons.Octagon")
      .doesNotContain("google.protobuf")
  }

  @Test
  fun emitKotlinThenEmitJava() {
    val fixtureRoot = File("src/test/projects/emit-kotlin-then-emit-java")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
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
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/Dinosaur.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.java")).exists()
    assertThat(File(outputRoot, "com/squareup/geology/Period.kt")).doesNotExist()
  }

  @Test
  fun emitService() {
    val fixtureRoot = File("src/test/projects/emit-service")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
      .contains("Writing com.squareup.dinosaurs.GrpcBattleServiceClient")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).exists()
  }

  @Test
  fun dontEmitServiceIfRoleIsNone() {
    val fixtureRoot = File("src/test/projects/emit-service-none")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output).doesNotContain("Service")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).doesNotExist()
  }

  @Test
  fun emitServiceTwoWays() {
    val fixtureRoot = File("src/test/projects/emit-service-two-ways")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.BattleServiceFight")
        .contains("Writing com.squareup.dinosaurs.BattleServiceBrawl")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).exists()
    assertThat(
        File(outputRoot, "com/squareup/dinosaurs/BattleServiceFightBlockingServer.kt")).exists()
    assertThat(
        File(outputRoot, "com/squareup/dinosaurs/BattleServiceBrawlBlockingServer.kt")).exists()
  }

  @Test
  fun emitServiceWithSpecificSuffix() {
    val fixtureRoot = File("src/test/projects/emit-service-name-suffix")
    val result = gradleRunner.runFixture(fixtureRoot) { build() }
    val task = result.task(":generateProtos")

    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.BattleServiceClient")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
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

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .doesNotContain("Writing com.squareup.dinosaurs.Dinosaur")
        .doesNotContain("Writing com.squareup.geology.Period")
        .contains("Writing com.squareup.dinosaurs.Dig")
        .contains("src/test/projects/customize-configuration/build/generated/source/wire".withPlatformSlashes())
  }

  /**
   * This test manipulates the tasks directly. We expect this to be useful in cases where users want to make
   * source file available to embedded dependancies.
   */
  @Test
  fun customizeTask() {
    val fixtureRoot = File("src/test/projects/customize-task")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateMainProtos")).isNotNull
    assertThat(result.task(":helloWorld")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dig")
        .contains("Hello, World!")
        .contains("src/test/projects/customize-task/build/generated/source/wire".withPlatformSlashes())
  }

  @Test
  fun customOutput() {
    val fixtureRoot = File("src/test/projects/custom-output")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output)
        .contains("custom handler is running!!")
  }

  @Test
  fun customOutputNoSuchClass() {
    val fixtureRoot = File("src/test/projects/custom-output-no-such-class")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }
    assertThat(result.output)
        .contains("Couldn't find CustomHandlerClass 'NoSuchClass'")
  }

  @Test
  fun sinceUntil() {
    val fixtureRoot = File("src/test/projects/since-until")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output).contains("Writing com.squareup.media.NewsFlash")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/media/NewsFlash.kt")
    assertThat(generatedProto).exists()

    val newsFlash = generatedProto.readText()
    assertThat(newsFlash).contains("val tv")
    assertThat(newsFlash).contains("val website")
    assertThat(newsFlash).doesNotContain("val radio")
  }

  @Test
  fun only() {
    val fixtureRoot = File("src/test/projects/only-version")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output).contains("Writing com.squareup.media.NewsFlash")

    val generatedProto =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/media/NewsFlash.kt")
    assertThat(generatedProto).exists()

    val newsFlash = generatedProto.readText()
    assertThat(newsFlash).contains("val tv")
    assertThat(newsFlash).doesNotContain("val website")
    assertThat(newsFlash).doesNotContain("val radio")
  }

  @Test
  fun kotlinMultiplatform() {
    val fixtureRoot = File("src/test/projects/kotlin-multiplatform")
    val kmpJsEnabled = System.getProperty("kjs", "true")!!.toBoolean()
    val kmpNativeEnabled = System.getProperty("knative", "true")!!.toBoolean()

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments(
          "assemble", "--stacktrace", "-Dkjs=$kmpJsEnabled", "-Dknative=$kmpNativeEnabled", "--info"
      ).build()
    }

    println(result.tasks.joinToString { it.toString() })
    assertThat(result.task(":generateCommonMainProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/kotlin-multiplatform/build/generated/source/wire".withPlatformSlashes())

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

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing squareup/dinosaurs/dinosaur.proto")
        .contains("Writing squareup/geology/period.proto")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).exists()
    assertThat(File(outputRoot, "squareup/geology/period.proto")).exists()
  }

  @Test
  fun emitProtoWithPrune() {
    val fixtureRoot = File("src/test/projects/emit-proto-with-prune")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing squareup/dinosaurs/dinosaur.proto")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).exists()
    assertThat(File(outputRoot, "squareup/geology/period.proto")).doesNotExist()
  }

  @Test
  fun emitProtoWithRoot() {
    val fixtureRoot = File("src/test/projects/emit-proto-with-root")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing squareup/geology/period.proto")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "squareup/geology/period.proto")).exists()
    assertThat(File(outputRoot, "squareup/dinosaurs/dinosaur.proto")).doesNotExist()
  }

  @Test
  fun consecutiveRuns() {
    val fixtureRoot = File("src/test/projects/consecutive-runs")

    val firstRun = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(firstRun.task(":generateMainProtos")).isNotNull
    assertThat(firstRun.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/consecutive-runs/custom".withPlatformSlashes())

    val secondRun = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(secondRun.task(":generateMainProtos")).isNotNull
    assertThat(secondRun.output)
        .contains("Task :generateMainProtos UP-TO-DATE")
  }

  @Test
  fun moveMessage() {
    val fixtureRoot = File("src/test/projects/move-message")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    val task = result.task(":generateProtos")
    assertThat(task).isNotNull
    assertThat(result.output)
        .contains("Writing squareup/dinosaurs/dinosaur.proto")
        .contains("Writing squareup/geology/geology.proto")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")

    val dinosaurProto = File(outputRoot, "squareup/dinosaurs/dinosaur.proto").readText()
    assertThat(dinosaurProto).contains("import \"squareup/geology/geology.proto\";")

    val geologyProto = File(outputRoot, "squareup/geology/geology.proto").readText()
    assertThat(geologyProto).contains("enum Period {")
  }

  @Test
  fun emitJavaOptions() {
    val fixtureRoot = File("src/test/projects/emit-java-options")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output).contains("Writing squareup.options.DocumentationUrlOption")

    val generatedProto = File(
        fixtureRoot, "build/generated/source/wire/squareup/polygons/Octagon.java")
    val octagon = generatedProto.readText()
    assertThat(octagon)
        .contains("""@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")""")
  }

  @Test
  fun emitKotlinOptions() {
    val fixtureRoot = File("src/test/projects/emit-kotlin-options")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output).contains("Writing squareup.options.DocumentationUrlOption")

    val generatedProto = File(
        fixtureRoot, "build/generated/source/wire/squareup/polygons/Octagon.kt")
    val octagon = generatedProto.readText()
    assertThat(octagon)
        .contains("""@DocumentationUrlOption("https://en.wikipedia.org/wiki/Octagon")""")
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

    val result = gradleRunner.runFixture(fixtureRoot) { build() }
    assertThat(result.output)
        .contains("Writing people.Employee")
        .contains("Writing people.OfficeManager")
        .contains("Writing locations.Office")
        .contains("Writing locations.Residence")
  }

  @Test
  fun projectDependencies() {
    val fixtureRoot = File("src/test/projects/project-dependencies")

    val result = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateMainProtos", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":dinosaurs:generateMainProtos")?.outcome)
      .isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
    val generatedProto1 = File(fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val generatedProto2 = File(fixtureRoot,
      "geology/build/generated/source/wire/com/squareup/geology/Period.kt")
    val generatedProto3 = File(fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/location/Continent.kt")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
    assertThat(generatedProto3).exists()

    val notExpected = File(fixtureRoot,
      "dinosaurs/build/generated/source/wire/com/squareup/location/Planet.kt")
    assertThat(notExpected).doesNotExist()

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
    assertThat(result.output).contains("Writing com.squareup.geology.Period")
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
    assertThat(relocatedResult.output).doesNotContain("Writing com.squareup.geology.Period")
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
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull()
    assertThat(result.output).contains("Writing com.squareup.geology.Period")
    assertThat(result.task(":generateMainProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateProtos")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(File(fixtureRoot, generatedPeriodProto)).exists()

    // The task has been cached.
    assertThat(buildCacheDir.exists()).isTrue()

    val cachedResult = gradleRunner.runFixture(fixtureRoot) {
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
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
    assertThat(result.output).contains("Writing com.squareup.geology.Period")
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
      withArguments("generateProtos", "--build-cache", "--stacktrace", "--info").build()
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

  private fun GradleRunner.runFixture(
    root: File,
    action: GradleRunner.() -> BuildResult
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
    @Throws(IOException::class) fun unsafeDelete(f: File) {
      if (f.isDirectory) {
        for (c in f.listFiles()!!) unsafeDelete(c)
      }
      f.delete()
    }

    // TODO(Benoit) The same code lives in `wire-test-utils`, find why we cannot use the code there
    // instead of duplicating this.
    private val slash = Path.DIRECTORY_SEPARATOR
    private val otherSlash = if (slash == "/") "\\" else "/"
    /**
     * This returns a string where all other slashes are replaced with the slash of the local platform.
     * On Windows, `/` will be replaced with `\`. On other platforms, `\` will be replaced with `/`.
     */
    private fun String.withPlatformSlashes(): String {
      return replace(otherSlash, slash)
    }
  }
}
