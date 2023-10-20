plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wireRuntime)
  api(libs.moshi)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.assertj)
  testImplementation(libs.junit)
  testImplementation(libs.moshiKotlin)
}
