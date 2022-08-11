plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.github.johnrengelman.shadow")
  id("org.jetbrains.dokka")
}

application {
  mainClassName = "com.squareup.wire.WireDescriptor"
}

dependencies {
  api(projects.wireSchema)
  api(libs.protobuf.java)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(projects.wireTestUtils)
}
