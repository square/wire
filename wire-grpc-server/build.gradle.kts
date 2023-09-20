
plugins {
  id("java-library")
  kotlin("jvm")
}



dependencies {
  implementation(projects.wireRuntime)
  // io.grpc.stub relies on guava-android. This module relies on a -jre version of guava.
  implementation(libs.grpc.stub) {
    exclude(group = "com.google.guava", module = "guava")
  }
  implementation(libs.checker.qual)
  implementation(libs.guava)
  implementation(libs.kotlin.coroutines.core)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
}

sourceSets {
  val test by getting {
    java.srcDir("src/test/proto")
  }
}
