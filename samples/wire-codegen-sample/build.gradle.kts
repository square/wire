plugins {
  kotlin("jvm")
}

dependencies {
  implementation(libs.wire.runtime)
  implementation(libs.wire.schema)
  implementation(libs.wire.javaGenerator)
  implementation(libs.wire.compiler)
  implementation(libs.okio.core)
  implementation(deps.guava)
  implementation(deps.javapoet)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}
