
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
  implementation(projects.wireGrpcServerGenerator)
  implementation(libs.okio.core)
  api(libs.kotlinpoet)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
}
