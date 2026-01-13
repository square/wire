plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

dependencies {
  implementation(libs.assertk)
  implementation(libs.jimfs)
  implementation(libs.kotlin.test.junit)
  implementation(libs.truth)
  implementation(projects.wireGsonSupport)
  implementation(projects.wireMoshiAdapter)
  implementation(projects.wireTestUtils)
}

wire {
  sourcePath {
    srcDir("../fixtures/proto/kotlin")
    include(
      "all_types.proto",
    )
  }

  kotlin {
    emitProtoReader32 = true
  }
}
