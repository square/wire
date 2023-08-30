import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("jvm")
  // TODO(Benoit)  Re-enable dokka when it works again. Probably related to https://github.com/Kotlin/dokka/issues/2977
  // id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")

  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Javadoc(), sourcesJar = true)
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
