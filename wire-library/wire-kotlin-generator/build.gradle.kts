plugins {
  id("java-library")
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-kotlin-generator")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-profiles"))
  implementation(project(":wire-runtime"))
  implementation(project(":wire-grpc-client"))
  implementation(project(":wire-grpc-server-generator"))
  implementation(deps.okio.jvm)
  api(deps.kotlinpoet)
  implementation(deps.guava)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.truth)
}
