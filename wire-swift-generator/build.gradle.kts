
plugins {
  id("java-library")
  kotlin("jvm")
}



dependencies {
  api(libs.swiftpoet)
  api(projects.wireSchema)

  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.jimfs)
}
