plugins {
  id("java-library")
  kotlin("jvm")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(libs.okio.core)
  implementation(libs.guava)
  api(libs.javapoet)
  compileOnly(libs.jsr305)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.jimfs)
}
