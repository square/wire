plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  api(libs.okhttp.mockwebserver)
}
