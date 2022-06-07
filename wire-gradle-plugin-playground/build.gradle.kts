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
  implementation(libs.wire.grpcClient)
  implementation(libs.okio.core)
  implementation(libs.wire.compiler)
  implementation(libs.wire.gsonSupport)
  implementation(libs.wire.moshiAdapter)
  implementation(deps.assertj)
  implementation(deps.junit)
  implementation(deps.protobuf.javaUtil)
  implementation(libs.wire.testUtils)
}
