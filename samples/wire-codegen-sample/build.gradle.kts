plugins {
  kotlin("jvm")
}

dependencies {
  implementation(deps.wire.runtime)
  implementation(deps.wire.schema)
  implementation(deps.wire.javaGenerator)
  implementation(deps.wire.compiler)
  implementation(deps.okio.core)
  implementation(deps.guava)
  implementation(deps.javapoet)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}
