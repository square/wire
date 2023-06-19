import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")

  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
    )
  }
}

dependencies {
  implementation(projects.wireRuntime)
  api(libs.moshi)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.assertj)
  testImplementation(libs.junit)
  testImplementation(libs.moshiKotlin)
}

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/test/java/com/squareup/wire/proto2/**/*.kt",
      "src/test/java/com/squareup/wire/protos/**/*.kt",
      "src/test/java/squareup/proto2/keywords/**/*.kt",
    )
  }
}
