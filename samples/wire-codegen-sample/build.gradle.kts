plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.wire.runtime)
  implementation(projects.wire.schema)
  implementation(projects.wire.javaGenerator)
  implementation(projects.wire.compiler)
  implementation(libs.okio.core)
  implementation(libs.guava)
  implementation(libs.javapoet)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
}
