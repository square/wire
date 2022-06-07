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
  implementation(deps.wire.grpcClient)
  implementation(libs.okio.core)
  implementation(deps.wire.compiler)
  implementation(deps.wire.gsonSupport)
  implementation(deps.wire.moshiAdapter)
  implementation(deps.assertj)
  implementation(deps.junit)
  implementation(deps.protobuf.javaUtil)
  implementation(deps.wire.testUtils)
}
