plugins {
  id("java-library")
  kotlin("jvm")
}



dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  api(libs.kotlinpoet)
  api(libs.protobuf.java)
  implementation(projects.wireGrpcServer)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
  testImplementation(libs.kotlin.jsr223)
}

sourceSets {
  val test by getting {
    java.srcDir("src/test/proto")
  }
}
