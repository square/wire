plugins {
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-codegen-sample")
  }
}

dependencies {
  implementation(deps.wire.runtime)
  implementation(deps.wire.schema)
  implementation(deps.wire.javaGenerator)
  implementation(deps.wire.compiler)
  implementation(deps.okio.jvm)
  implementation(deps.guava)
  implementation(deps.javapoet)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}
