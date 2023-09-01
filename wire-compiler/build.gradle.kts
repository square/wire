import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  // TODO(Benoit)  Re-enable dokka when it works again. Probably related to https://github.com/Kotlin/dokka/issues/2977
  // id("org.jetbrains.dokka")
  id("com.github.johnrengelman.shadow").apply(false)
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.github.johnrengelman.shadow")
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
  implementation(libs.kotlin.serialization)
  implementation(libs.kaml)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(projects.wireTestUtils)
}

if (project.rootProject.name == "wire") {
  val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("jar-with-dependencies")
  }

  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Javadoc(), sourcesJar = true)
    )
  }
}
