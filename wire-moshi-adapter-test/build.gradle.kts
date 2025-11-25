plugins {
  kotlin("jvm")
}

dependencies {
  testImplementation(projects.wireRuntime)
  testImplementation(projects.wireMoshiAdapter)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.assertk)
  testImplementation(libs.junit)
  testImplementation(libs.moshiKotlin)
  testImplementation(libs.truth)
  testImplementation(projects.wireTestProtoGeneration.jsonJava)
  testImplementation(projects.wireTestProtoGeneration.jsonJavaCompact)
  testImplementation(projects.wireTestProtoGeneration.jsonKotlin)
  testImplementation(projects.wireTestProtoGeneration.jsonKotlinInterop)
}
