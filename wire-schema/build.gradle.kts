import com.diffplug.gradle.spotless.SpotlessExtension
import okio.ByteString
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source
import okio.use
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm().withJava()
  if (System.getProperty("kjs", "true").toBoolean()) {
    js(IR) {
      configure(listOf(compilations.getByName("main"), compilations.getByName("test"))) {
        tasks.getByName(compileKotlinTaskName) {
          kotlinOptions {
            moduleKind = "umd"
            sourceMap = true
            metaInfo = true
          }
        }
      }
      nodejs()
      browser()
    }
  }
  if (System.getProperty("knative", "true").toBoolean()) {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.wireRuntime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(projects.wireSchemaTests)
        implementation(libs.okio.fakefilesystem)
        implementation(projects.wireTestUtils)
      }
    }
    val jvmMain by getting {
      dependencies {
        api(libs.okio.core)
        api(libs.guava)
        api(libs.javapoet)
        api(libs.kotlinpoet)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(libs.assertj)
        implementation(libs.jimfs)
        implementation(libs.protobuf.java)
      }
    }
    if (System.getProperty("kjs", "true").toBoolean()) {
      val jsTest by getting {
        dependencies {
          implementation(libs.kotlin.test.js)
        }
      }
    }
    if (System.getProperty("knative", "true").toBoolean()) {
      val nativeMain by creating {
        dependsOn(commonMain)
      }
      val nativeTest by creating {
        dependsOn(commonTest)
      }

      val iosX64Main by getting
      val iosArm64Main by getting
      val iosSimulatorArm64Main by getting
      val linuxX64Main by getting
      val macosX64Main by getting
      val macosArm64Main by getting
      val mingwX64Main by getting
      val tvosX64Main by getting
      val tvosArm64Main by getting
      val tvosSimulatorArm64Main by getting
      val iosX64Test by getting
      val iosArm64Test by getting
      val iosSimulatorArm64Test by getting
      val linuxX64Test by getting
      val macosX64Test by getting
      val macosArm64Test by getting
      val mingwX64Test by getting
      val tvosX64Test by getting
      val tvosArm64Test by getting
      val tvosSimulatorArm64Test by getting

      for (it in listOf(
        iosX64Main,
        iosArm64Main,
        iosSimulatorArm64Main,
        linuxX64Main,
        macosX64Main,
        macosArm64Main,
        mingwX64Main,
        tvosX64Main,
        tvosArm64Main,
        tvosSimulatorArm64Main,
      )) {
        it.dependsOn(nativeMain)
      }

      for (it in listOf(
        iosX64Test,
        iosArm64Test,
        iosSimulatorArm64Test,
        linuxX64Test,
        macosX64Test,
        macosArm64Test,
        mingwX64Test,
        tvosX64Test,
        tvosArm64Test,
        tvosSimulatorArm64Test,
      )) {
        it.dependsOn(nativeTest)
      }
    }
  }
}

if (project.rootProject.name == "wire") {
  configure<SpotlessExtension> {
    kotlin {
      targetExclude(
        // Apache 2-licensed file from Apache.
        "src/commonTest/kotlin/com/squareup/wire/schema/MavenVersionsTest.kt",
      )
    }
  }
}

with (CoreLoaderTasks) {
  installCoreLoaderTasks()
}


/*
 * When we add Wire's `.proto` files like `google/descriptor.proto` as an element on the class path,
 * we risk colliding with other `.jar` files that do the same, including other versions of Wire
 * and with `protobuf-java.jar`.
 *
 * Mitigate this by putting Wire's copy of these protos in a directory named by the hash of its
 * content.
 *
 * Also generate a single `.kt` file with this hash, so we can look it up later.
 */
fun Project.installCoreLoaderTasks() {
  val copyTask = tasks.create("coreLoaderCopy", CoreLoaderTasks.CopyTask::class)
  copyTask.input.set(File(projectDir, "src/coreLoader"))
  copyTask.output.set(layout.buildDirectory.dir("coreLoader/protos"))

  val kotlinTask = tasks.create("coreLoaderKotlin", CoreLoaderTasks.KotlinTask::class)
  kotlinTask.input.set(File(projectDir, "src/coreLoader"))
  kotlinTask.output.set(layout.buildDirectory.dir("coreLoader/kotlin"))

  plugins.withType<KotlinBasePlugin> {
    kotlinExtension.sourceSets.configureEach {
      if (this.name == "jvmMain") {
        resources.srcDir(copyTask)
        kotlin.srcDir(kotlinTask)
      }
    }
  }
}

internal object CoreLoaderTasks {
  abstract class CopyTask : DefaultTask() {
    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun execute() {
      val inputDirectory = input.get()
      val outputDirectory = output.get().asFile
      outputDirectory.deleteRecursively()

      val directoryName = inputDirectory.coreLoaderResourceDirectory()
      val outputDirectoryPlusHash = File(outputDirectory, directoryName)

      for (sourceFile in inputDirectory.asFileTree.files) {
        val relativeSourceFile = sourceFile.relativeTo(inputDirectory.asFile).path
        val targetFile = File(outputDirectoryPlusHash, relativeSourceFile)
        targetFile.parentFile.mkdirs()
        sourceFile.copyTo(targetFile)
      }
    }
  }

  abstract class KotlinTask : DefaultTask() {
    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun execute() {
      val inputDirectory = input.get()
      val directoryName = inputDirectory.coreLoaderResourceDirectory()

      val outputDirectory = output.get().asFile
      outputDirectory.deleteRecursively()

      val ktFile = File(outputDirectory, "com/squareup/wire/schema/CoreLoaderResourceDirectory.kt")
      ktFile.parentFile.mkdirs()
      ktFile.writeText(
        """
      |// Generated by CoreLoaderKotlinTask. Do not edit.
      |package com.squareup.wire.schema
      |
      |internal const val coreLoaderResourceDirectory = "${directoryName}"
      |""".trimMargin()
      )
    }
  }

  fun Directory.coreLoaderResourceDirectory(): String {
    return "CoreLoader-${contentSha256().substring(0, 5).hex()}"
  }

  fun Directory.contentSha256(): ByteString {
    val hashingSink = HashingSink.sha256(blackholeSink())
    hashingSink.buffer().use {
      for (file in asFileTree.files.sortedBy { it.invariantSeparatorsPath }) {
        file.source().use { fileSource ->
          it.writeAll(fileSource)
        }
      }
    }
    return hashingSink.hash
  }
}
