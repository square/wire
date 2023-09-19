import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  api(libs.kotlinpoet)
  api(libs.protobuf.java)
  implementation(projects.wireGrpcServer)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
  testImplementation(libs.kotlin.jsr223)
}

sourceSets {
  val test by getting {
    java.srcDir("src/test/proto")
  }
}
