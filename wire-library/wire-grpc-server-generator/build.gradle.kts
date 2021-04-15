plugins {
  id("java-library")
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-grpc-server-generator")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-profiles"))
  implementation(project(":wire-runtime"))
  implementation(project(":wire-grpc-client"))
  implementation(deps.okio.jvm)
  api(deps.kotlinpoet)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.truth)
  testImplementation(deps.assertj)
}

sourceSets {
  val test by getting {
    java.srcDir("src/test/proto")
  }
}
