package com.squareup.wire.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

class WirePluginTest {
  private lateinit var gradleRunner: GradleRunner
  @Before
  fun setUp() {
    gradleRunner = GradleRunner.create()
        .withPluginClasspath()
        .withArguments("generateProtos", "--stacktrace")
  }

  @Test
  fun missingPlugin() {
    val fixtureRoot = File("src/test/projects/missing-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output).contains(
        """Either the Java or Kotlin plugin must be applied before the Wire Gradle plugin."""
    )
  }

  @Test
  fun sourcePathDirDoesNotExist() {
    val fixtureRoot = File("src/test/projects/sourcepath-nonexistent-dir")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output).contains(
        """Invalid path string: "src/main/proto". Path does not exist."""
    )
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
        .contains("src/test/projects/sourcepath-default/build/generated/source/wire")
  }

  @Test
  fun sourcePathWithoutSources() {
    val fixtureRoot = File("src/test/projects/sourcepath-no-sources")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    val task = result.task(":generateProtos")
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
        .contains("src/test/projects/sourcepath-dir/build/generated/source/wire")
  }

  @Test
  fun sourcePathMavenCoordinates() {
    val fixtureRoot = File("src/test/projects/sourcepath-maven")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/sourcepath-maven/build/generated/source/wire")
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
        .contains("src/test/projects/sourcetree-one-srcdir-one-file/build/generated/source/wire")
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
            "src/test/projects/sourcetree-one-srcdir-many-files/build/generated/source/wire"
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
        .contains("src/test/projects/sourcetree-many-srcdirs/build/generated/source/wire")
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
            "src/test/projects/sourcejar-local-many-files/build/generated/source/wire"
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
            "src/test/projects/sourcejar-local-single-file/build/generated/source/wire"
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
            "src/test/projects/sourcejar-remote-many-files/build/generated/source/wire"
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
            "src/test/projects/sourcejar-remote-protopath/build/generated/source/wire"
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
        .contains("src/test/projects/protopath-maven/build/generated/source/wire")
  }

  @Test
  fun differentJavaOutputDir() {
    val fixtureRoot = File("src/test/projects/different-java-out")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/different-java-out/custom")
  }

  @Test
  fun differentKotlinOutputDir() {
    val fixtureRoot = File("src/test/projects/different-kotlin-out")

    val result = gradleRunner.runFixture(fixtureRoot) { build() }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/different-kotlin-out/custom")
  }

  @Test
  fun kotlinTargetMissingKotlinPlugin() {
    val fixtureRoot = File("src/test/projects/missing-kotlin-plugin")

    val result = gradleRunner.runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":generateProtos")).isNull()
    assertThat(result.output)
        .contains("To generate Kotlin protos, please apply a Kotlin plugin.")
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
      withArguments("run", "--stacktrace").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/java-project-java-protos/build/generated/source/wire")

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
      withArguments("run", "--stacktrace").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/java-project-kotlin-protos/build/generated/source/wire")

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
      withArguments("run", "--stacktrace").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/kotlin-project-java-protos/build/generated/source/wire")

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
      withArguments("run", "--stacktrace").build()
    }

    assertThat(result.task(":generateProtos")).isNotNull
    assertThat(result.output)
        .contains("Writing com.squareup.dinosaurs.Dinosaur")
        .contains("Writing com.squareup.geology.Period")
        .contains("src/test/projects/kotlin-project-kotlin-protos/build/generated/source/wire")

    val generatedProto1 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/dinosaurs/Dinosaur.kt")
    val generatedProto2 =
        File(fixtureRoot, "build/generated/source/wire/com/squareup/geology/Period.kt")
    assertThat(generatedProto1).exists()
    assertThat(generatedProto2).exists()
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
            "src/test/projects/sourcepath-and-protopath-intersect/build/generated/source/wire"
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
        .contains("Writing com.squareup.dinosaurs.BattleService")

    val outputRoot = File(fixtureRoot, "build/generated/source/wire")
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceClient.kt")).exists()
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
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceFightBlockingServer.kt")).exists()
    assertThat(File(outputRoot, "com/squareup/dinosaurs/BattleServiceBrawlBlockingServer.kt")).exists()
  }

  private fun fieldsFromProtoSource(generatedProtoSource: String): List<String> {
    val protoFieldPattern = "@field:WireField.*?(val .*?):"
    val matchedFields = protoFieldPattern.toRegex(setOf(MULTILINE, DOT_MATCHES_ALL))
        .findAll(generatedProtoSource)
    return matchedFields
        .map { it.groupValues[1] }
        .toList()
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
}
