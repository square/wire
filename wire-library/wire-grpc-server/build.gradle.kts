plugins {
  id("java-library")
  kotlin("jvm")
  id("internal-publishing")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-grpc-server")
  }
}

dependencies {
  implementation(project(":wire-runtime"))
  // io.grpc.stub relies on guava-android. This module relies on a -jre version of guava.
  implementation(deps.grpc.stub) {
    exclude(group = "com.google.guava", module = "guava")
  }
  implementation("com.google.guava:guava:21.0")
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
