import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.github.johnrengelman.shadow").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.github.johnrengelman.shadow")

  val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("jar-with-dependencies")
  }
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
