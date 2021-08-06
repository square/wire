plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-descriptorgen-sample")
  }
}

dependencies {
  implementation(deps.wire.descriptor)
  implementation(project(":samples:wire-descriptorgen-sample:protos"))

  testImplementation(deps.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.protobuf.java)
}

