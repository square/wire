plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wireRuntime)
  api(libs.gson)
  api(libs.okio.core)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.assertk)
  testImplementation(projects.wireTestUtils)
}
