import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

gradlePlugin {
  plugins {
    create("wire") {
      id = "com.squareup.wire"
      implementationClass = "com.squareup.wire.gradle.WirePlugin"
    }
  }
}

dependencies {
  implementation(project(":wire-compiler"))
  implementation(project(":wire-kotlin-generator"))
  implementation(deps.swiftpoet)

  compileOnly(gradleApi())
  implementation(deps.plugins.kotlin)
  compileOnly(deps.plugins.android)

  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}

sourceSets {
  val main by getting {
    java.srcDir("src/generated/kotlin")
  }
}

val generateVersion by tasks.creating {
  val outputDir = file("src/generated/kotlin")

  inputs.property("version", version)
  outputs.dir(outputDir)

  doLast {
    val versionFile = file("${outputDir}/com/squareup/wire/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText(
      """
      |// Generated file. Do not edit!
      |package com.squareup.wire
      |const val VERSION = "${project.version}"
      |""".trimMargin()
    )
  }
}

val compileKotlin by tasks.getting {
  dependsOn(generateVersion)
}

val installProtoJars by tasks.creating(Copy::class) {
  into("${rootDir.path}/build/localMaven")
  from("${projectDir.path}/src/test/libs")
}

tasks.withType<Test>().configureEach {
  dependsOn(installProtoJars)
  dependsOn(":wire-runtime:installLocally")
}

val test by tasks.getting(Test::class) {
  // Fixtures run in a separate JVM, routing properties from the VM running the build into test VM.
  systemProperty("kjs", System.getProperty("kjs"))
  systemProperty("knative", System.getProperty("knative"))
}

// The plugin marker artifacts don't conform to Maven Central's requirements. Disable publishing
// them until we fix that. https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers
tasks.withType<AbstractPublishToMaven>().configureEach {
  if (name == "publishWirePluginMarkerMavenPublicationToMavenCentralRepository") {
    enabled = false
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    GradlePlugin(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
