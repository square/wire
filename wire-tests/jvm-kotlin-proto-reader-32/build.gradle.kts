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
      "edge_cases.proto",
      "empty_packed.proto",
      "map.proto",
      "one_of.proto",
      "option_redacted.proto",
      "redacted_map.proto",
    )
  }

  kotlin {
    emitProtoReader32 = true
  }
}
