plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("src/main/proto")
  }

  kotlin {
  }
}

dependencies {
  implementation(projects.wire.grpcClient)
  implementation(libs.okio.core)
  implementation(projects.wire.compiler)
  implementation(projects.wire.gsonSupport)
  implementation(projects.wire.moshiAdapter)
  implementation(libs.assertj)
  implementation(libs.junit)
  implementation(libs.protobuf.javaUtil)
  implementation(projects.wire.testUtils)
}
