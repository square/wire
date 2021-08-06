plugins {
  kotlin("jvm")
  id("internal-publishing")
}

dependencies {
  implementation(deps.okio.jvm)
  implementation(deps.protobuf.java)
  implementation(project(":wire-schema"))
  implementation("io.github.classgraph:classgraph:4.8.115")

  testImplementation(deps.wire.gsonSupport)
  testImplementation(deps.wire.moshiAdapter)
  testImplementation(deps.assertj)
  testImplementation(deps.junit)
  testImplementation(deps.protobuf.javaUtil)
  testImplementation(deps.wire.testUtils)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-descriptor")
  }
}
