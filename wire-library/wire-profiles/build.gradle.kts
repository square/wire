plugins {
  id("java-library")
  kotlin("jvm")
  id("internal-publishing")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-profiles")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-runtime"))
  implementation(deps.okio.jvm)
  implementation(deps.guava)
  api(deps.javapoet)
  api(deps.kotlinpoet)
  compileOnly(deps.jsr305)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.junit)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.jimfs)
}
