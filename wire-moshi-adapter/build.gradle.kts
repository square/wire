plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wireRuntime)
  api(libs.moshi)
}
