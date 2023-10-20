
plugins {
  id("java-library")
  kotlin("jvm")
}



dependencies {
  api(libs.swiftpoet)
  api(projects.wireSchema)
}
