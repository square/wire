plugins {
  kotlin("jvm")
}

dependencies {
  implementation(libs.wire.runtime)
  implementation(libs.wire.schema)
  implementation(libs.wire.javaGenerator)
  implementation(libs.wire.compiler)
  implementation(libs.okio.core)
  implementation(libs.guava)
  implementation(libs.javapoet)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
}
