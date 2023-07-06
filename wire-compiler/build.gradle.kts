import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.github.johnrengelman.shadow")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

application {
  mainClass.set("com.squareup.wire.WireCompiler")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireKotlinGenerator)
  implementation(projects.wireJavaGenerator)
  implementation(projects.wireSwiftGenerator)
  implementation(libs.okio.core)
  implementation(libs.okio.fakefilesystem)
  implementation(libs.guava)
  implementation(libs.kotlin.serialization)
  implementation(libs.kaml)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(projects.wireTestUtils)
}

val shadowJar by tasks.getting(ShadowJar::class) {
  archiveClassifier.set("jar-with-dependencies")
}

if (project.rootProject.name == "wire") {
  configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
      artifact(shadowJar)
    }
  }

  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
    )
  }
}

// The `shadow` plugin internally applies the `distribution` plugin and
// automatically adds tasks to create respective tar and zip artifacts.
// https://github.com/johnrengelman/shadow/issues/347#issuecomment-424726972
// https://github.com/johnrengelman/shadow/commit/a824e4f6e4618785deb7f084c4a80ce1b78fc4fd
tasks.findByName("shadowDistTar")?.enabled = false
tasks.findByName("shadowDistZip")?.enabled = false
configurations["archives"].artifacts.removeAll {
  val file: File = it.file
  file.name.contains("tar") || file.name.contains("zip")
}
