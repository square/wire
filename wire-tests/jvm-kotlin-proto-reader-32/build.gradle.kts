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
    srcDir("../fixtures/shared/proto/proto2")
    srcDir("../fixtures/shared/proto/proto3")
    include(
      "all_types.proto",
      "edge_cases.proto",
      "empty_packed.proto",
      "map.proto",
      "one_of.proto",
      "oneof_merge_boxed.proto",
      "oneof_merge_sealed.proto",
      "option_redacted.proto",
      "redacted_map.proto",
      "use_array.proto",
      "use_array_redacted.proto",
      "use_array_proto2.proto",
    )
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.oneof_merge_boxed.*")
    oneofMode = "boxed"
    emitProtoReader32 = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.oneof_merge_sealed.*")
    oneofMode = "sealed_class"
    emitProtoReader32 = true
  }

  kotlin {
    emitProtoReader32 = true
  }
}
