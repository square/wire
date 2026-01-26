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
package com.squareup.wire.buildsupport

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.io.File
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// I cannot get the wireBuild extension to work in projects included via `includeBuild` within
// `build-support`, so doing it here for now :sad:
private val PROJECT_TO_PUBLISH = listOf(
  "wire-bom",
  "wire-compiler",
  "wire-gradle-plugin",
  "wire-grpc-client",
  "wire-grpc-mockwebserver",
  "wire-gson-support",
  "wire-java-generator",
  "wire-kotlin-generator",
  "wire-moshi-adapter",
  "wire-reflector",
  "wire-runtime",
  "wire-runtime-swift",
  "wire-schema",
  "wire-schema-tests",
  "wire-swift-generator",
)

@Suppress("unused") // Invoked reflectively by Gradle.
class WireBuildPlugin : Plugin<Project> {
  private lateinit var libs: LibrariesForLibs

  override fun apply(target: Project) {
    libs = target.extensions.getByName("libs") as LibrariesForLibs

    target.extensions.add(
      WireBuildExtension::class.java,
      "wireBuild",
      WireBuildExtensionImpl(target),
    )

    target.configureCommonSpotless()
    target.configureCommonTesting()
    target.configureCommonAndroid()
    target.configureCommonKotlin()
    target.configureCommonDistribution()
    target.configureCommonJarManifest()

    if (target.name in PROJECT_TO_PUBLISH) {
      target.extensions.getByType(WireBuildExtension::class.java).publishing()
    }

    if (target.name == "wire-gradle-plugin") {
      target.publishToPluginPortalIfRelease()
    }
  }

  private fun Project.publishToPluginPortalIfRelease() {
    // Note that we create the task in all cases for it'll be executed on CI every time.
    tasks.register("publishPluginToGradlePortalIfRelease") {
      // Snapshots cannot be released to the Gradle portal. And we don't want to release internal
      // square builds.
      val version = version.toString()
      if (version.endsWith("-SNAPSHOT") || "square" in version) return@register
      dependsOn(":wire-gradle-plugin:publishPlugins")
    }
  }

  private fun Project.configureCommonSpotless() {
    plugins.apply("com.diffplug.spotless")
    val spotless = extensions.getByName("spotless") as SpotlessExtension
    val licenseHeaderFile = rootProject.file("gradle/license-header.txt")
    spotless.apply {
      // The nested build-support Gradle project contains Java sources. Use our root project to
      // target its sources rather than duplicating the Spotless setup in multiple places.
      java {
        if (path == ":") {
          target("build-support/settings/src/**/*.java")
        } else {
          target("src/**/*.java")
          // Avoid 'build' folders within test fixture projects which may contain generated sources.
          targetExclude("src/test/projects/**")
        }
        targetExcludeIfContentContains("// Code generated by Wire protocol buffer compiler")
        googleJavaFormat(libs.googleJavaFormat.get().version)
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn()
        lineEndings = LineEnding.UNIX
        licenseHeaderFile(licenseHeaderFile)
      }

      kotlin {
        // The nested build-support Gradle project contains Kotlin sources. Use our root project to
        // target its sources rather than duplicating the Spotless setup in multiple places.
        if (path == ":") {
          target("build-support/src/**/*.kt")
        } else {
          target("src/**/*.kt")
          targetExcludeIfContentContains("// Code generated by Wire protocol buffer compiler")
          // Avoid 'build' folders within test fixture projects which may contain generated sources.
          targetExclude("src/test/projects/**")
        }
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn()
        lineEndings = LineEnding.UNIX
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))
        licenseHeaderFile(licenseHeaderFile)
      }
      if (path != ":") {
        format("Swift") {
          target("**/*.swift")
          targetExcludeIfContentContains("// Code generated by Wire protocol buffer compiler")
          lineEndings = LineEnding.UNIX
          licenseHeaderFile(licenseHeaderFile, "(@propertyWrapper|public |import |enum )")
        }
      }
    }
  }

  private fun Project.configureCommonTesting() {
    tasks.withType(AbstractTestTask::class.java).configureEach {
      testLogging {
        if (System.getenv("CI") == "true") {
          events = setOf(FAILED, SKIPPED, PASSED)
        }
        exceptionFormat = FULL
        showStandardStreams = false
      }
    }
  }

  private fun Project.configureCommonAndroid() {
    plugins.withId("com.android.base") {
      project.extensions.getByType(CommonExtension::class.java).apply {
        compileSdk = 36
        compileOptions.apply {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        defaultConfig.minSdk { version = release(28) }

        if (this is ApplicationExtension) {
          defaultConfig.apply {
            if (project.name.contains("app")) {
              applicationId = "$group.${project.name}".replace(oldChar = '-', newChar = '.')
            }
            targetSdk = 33
            versionCode = 1
            versionName = "1.0"
          }
        }

        lint.apply {
          checkDependencies = true
          checkReleaseBuilds = false // Full lint runs as part of 'build' task.
        }
      }
    }
  }

  private fun Project.configureCommonKotlin() {
    tasks.withType(KotlinCompilationTask::class.java).configureEach {
      compilerOptions {
        freeCompilerArgs.addAll(
          "-progressive", // https://kotlinlang.org/docs/whatsnew13.html#progressive-mode
          "-Xexpect-actual-classes",
        )
      }
    }

    val javaVersion = JavaVersion.VERSION_1_8
    tasks.withType(KotlinJvmCompile::class.java).configureEach {
      compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        freeCompilerArgs.add("-Xjvm-default=all")
      }
    }
    // Kotlin requires the Java compatibility matches.
    tasks.withType(JavaCompile::class.java).configureEach {
      sourceCompatibility = javaVersion.toString()
      targetCompatibility = javaVersion.toString()
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
      val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

      // Opt-in everything.
      kotlin.sourceSets.configureEach {
        languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
      }
    }
  }

  private fun Project.configureCommonDistribution() {
    // The `application` plugin internally applies the `distribution` plugin and
    // automatically adds tasks to create/publish tar and zip artifacts.
    // https://docs.gradle.org/current/userguide/application_plugin.html
    // https://docs.gradle.org/current/userguide/distribution_plugin.html#sec:publishing_distributions_upload
    plugins.withType(DistributionPlugin::class) {
      tasks.findByName("distTar")?.enabled = false
      tasks.findByName("distZip")?.enabled = false
      configurations["archives"].artifacts.removeAll {
        val file: File = it.file
        file.name.contains("tar") || file.name.contains("zip")
      }
    }
  }

  private fun Project.configureCommonJarManifest() {
    tasks.withType<Jar>().configureEach {
      if (name == "jar") {
        manifest {
          attributes("Automatic-Module-Name" to project.name)
        }
      }
    }
  }
}

private class WireBuildExtensionImpl(private val project: Project) : WireBuildExtension {
  @OptIn(ExperimentalBCVApi::class)
  override fun publishing() {
    project.plugins.apply("com.vanniktech.maven.publish")
    project.plugins.apply("org.jetbrains.dokka")
    project.plugins.apply("binary-compatibility-validator")

    val publishing = project.extensions.getByName("publishing") as PublishingExtension
    publishing.apply {
      repositories {
        maven {
          name = "LocalMaven"
          url = project.rootProject.layout.buildDirectory.dir("localMaven").get().asFile.toURI()
        }
        maven {
          name = "test"
          url = project.rootProject.layout.buildDirectory.dir("localMaven").get().asFile.toURI()
        }

        // Want to push to an internal repository for testing?
        // Set the following properties in ~/.gradle/gradle.properties.
        //
        // internalUrl=YOUR_INTERNAL_URL
        // internalUsername=YOUR_USERNAME
        // internalPassword=YOUR_PASSWORD
        //
        // Then run the following command to publish a new internal release:
        //
        // ./gradlew publishAllPublicationsToInternalRepository -DRELEASE_SIGNING_ENABLED=false
        val internalUrl = project.providers.gradleProperty("internalUrl")
        if (internalUrl.isPresent) {
          maven {
            name = "internal"
            setUrl(internalUrl)
            credentials {
              username = project.providers.gradleProperty("internalUsername").get()
              password = project.providers.gradleProperty("internalPassword").get()
            }
          }
        }
      }
    }

    val mavenPublishing = project.extensions.getByName("mavenPublishing") as MavenPublishBaseExtension
    mavenPublishing.apply {
      // The Gradle plugin publish plugin configures `wire-gradle-plugin` for us, and we don't need
      // to configure `wire-bom`.
      if (!project.isWireGradlePlugin && !project.isWireBom) {
        // TODO(Benoit) Fix, this is failing with
        //  `SoftwareComponent with name 'java' not found.`
        // configure(KotlinJvm(javadocJar = Dokka("dokkaHtml"), sourcesJar = true))
      }

      publishToMavenCentral(automaticRelease = true)
      val inMemoryKey = project.findProperty("signingInMemoryKey") as String?
      if (!inMemoryKey.isNullOrEmpty()) {
        signAllPublications()
      }

      pom {
        name.set(project.name)
        description.set("gRPC and protocol buffers for Android, Kotlin, and Java.")
        inceptionYear.set("2017")
        url.set("https://github.com/square/wire/")

        licenses {
          license {
            name.set("Apache-2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
            distribution.set("repo")
          }
        }

        developers {
          developer {
            id.set("cashapp")
            name.set("CashApp")
            url.set("https://github.com/cashapp")
          }
        }

        scm {
          url.set("https://github.com/square/wire/")
          connection.set("scm:git:https://github.com/square/wire.git")
          developerConnection.set("scm:git:ssh://git@github.com/square/wire.git")
        }
      }
    }

    if (project.isWireBom) return

    project.tasks.withType(DokkaTask::class.java).configureEach {
      outputDirectory.set(project.file("${project.rootDir}/docs/3.x/${project.name}"))
      dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipDeprecated.set(true)
        jdkVersion.set(8)
        perPackageOption {
          matchingRegex.set("com\\.squareup\\.wire.*\\.internal.*")
          suppress.set(true)
        }
        // Document generated code.
        suppressGeneratedFiles.set(false)
      }
    }

    project.extensions.getByType(ApiValidationExtension::class.java).apply {
      ignoredPackages += "grpc.reflection.v1alpha"
      ignoredPackages += "com.squareup.wire.gradle.internal"
      ignoredPackages += "com.squareup.wire.internal"
      ignoredPackages += "com.squareup.wire.schema.internal"
    }
  }

  override fun createKotlinJvmTestTask(
    taskName: String,
    block: KotlinSourceSet.() -> Unit,
  ) {
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
      val kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

      val compilationName = "${taskName}Sources"
      val compilation = kotlin.jvm().compilations.create(compilationName).apply {
        defaultSourceSet {
          block()
        }
      }

      val task = project.tasks.register(taskName, Test::class) {
        classpath = compilation.compileDependencyFiles + compilation.runtimeDependencyFiles + compilation.output.allOutputs
        testClassesDirs = compilation.output.classesDirs
      }

      val jvmTest = project.tasks["jvmTest"]
      jvmTest.dependsOn(task)
    }
  }

  override fun createJavaTestTask(
    taskName: String,
    block: JavaTestTaskScope.() -> Unit,
  ) {
    val sourceSets = project.extensions.getByType<SourceSetContainer>()

    val sourceSetName = "${taskName}Sources"
    val sourceSet = sourceSets.create(sourceSetName)

    val task = project.tasks.register(taskName, Test::class) {
      classpath = sourceSet.runtimeClasspath
      testClassesDirs = sourceSet.output.classesDirs
    }

    val jvmTest = project.tasks["jvmTest"]
    jvmTest.dependsOn(task)

    val javaTestTaskScope = object : JavaTestTaskScope {
      override val sourceSet: SourceSet
        get() = sourceSet

      override fun implementation(dependencyNotation: Any) {
        project.dependencies {
          add("${sourceSetName}Implementation", dependencyNotation)
        }
      }

      override fun compileOnly(dependencyNotation: Any) {
        project.dependencies {
          add("${sourceSetName}CompileOnly", dependencyNotation)
        }
      }
    }

    javaTestTaskScope.block()
  }

  private val Project.isWireGradlePlugin
    get() = name.contains("wire-gradle-plugin")

  private val Project.isWireBom
    get() = name.contains("wire-bom")
}
