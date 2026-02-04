plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

dependencies {
  implementation(libs.assertk)
  implementation(libs.jimfs)
  implementation(libs.kotlin.test.junit)
  implementation(libs.truth)
  implementation(projects.wireGrpcClient)
  implementation(projects.wireGsonSupport)
  implementation(projects.wireMoshiAdapter)
  implementation(projects.wireTestUtils)
}

wire {
  sourcePath {
    srcDir("../fixtures/proto/kotlin")
    include(
      "all_types.proto",
      "deprecated.proto",
      "custom_options.proto",
      "easter.proto",
      "external_message.proto",
      "foreign.proto",
      "form.proto",
      "large_field_message.proto",
      "map.proto",
      "person.proto",
      "no_fields.proto",
      "one_of.proto",
      "option_redacted.proto",
      "packed_encoding.proto",
      "percents_in_kdoc.proto",
      "person_proto3.proto",
      "recursive_map.proto",
      "redacted_one_of.proto",
      "repeated.proto",
      "same_name_enum.proto",
      "simple_message.proto",
      "unknown_fields.proto",
      "uses_any.proto",
    )
  }

  kotlin {
    boxOneOfsMinSize = 8
    javaInterop = true
  }
}
