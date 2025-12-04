plugins {
  kotlin("jvm")
}

dependencies {
  testImplementation(projects.wireRuntime)
  testImplementation(projects.wireGsonSupport)
  testImplementation(libs.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.truth)
  testImplementation(projects.wireTestUtils)
  testImplementation(projects.wireTestProtoGeneration.jsonJava)
  testImplementation(projects.wireTestProtoGeneration.jsonJavaCompact)
  testImplementation(projects.wireTestProtoGeneration.jsonKotlin)
  testImplementation(projects.wireTestProtoGeneration.jsonKotlinInterop)
}
