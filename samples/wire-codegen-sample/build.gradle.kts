plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wireRuntime)
  implementation(projects.wireSchema)
  implementation(projects.wireJavaGenerator)
  implementation(projects.wireCompiler)
  implementation(libs.okio.core)
  implementation(libs.guava)
  implementation(libs.javapoet)
  testImplementation(libs.junit)
  testImplementation(libs.assertk)
}
