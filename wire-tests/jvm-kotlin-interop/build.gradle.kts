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
    srcDir("../fixtures/shared/proto/proto2")
    srcDir("../fixtures/shared/proto/proto3")
    include(
      "all_types.proto",
      "edge_cases.proto",
      "empty_packed.proto",
      "deprecated.proto",
      "custom_options.proto",
      "easter.proto",
      "external_message.proto",
      "foreign.proto",
      "flat_redacted_one_of.proto",
      "form.proto",
      "map.proto",
      "person.proto",
      "no_fields.proto",
      "one_of.proto",
      "oneof_merge_boxed.proto",
      "oneof_merge_sealed.proto",
      "oneof_merge_sealed_builders.proto",
      "option_redacted.proto",
      "packed_encoding.proto",
      "percents_in_kdoc.proto",
      "person_proto3.proto",
      "recursive_map.proto",
      "redacted_one_of.proto",
      "sealed_redacted_one_of.proto",
      "boxed_redacted_one_of.proto",
      "repeated.proto",
      "same_name_enum.proto",
      "simple_message.proto",
      "unknown_fields.proto",
      "uses_any.proto",
      "use_array_interop.proto",
      "use_array_redacted.proto",
      "use_array_proto2_interop.proto",
    )
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.edgecases.MergeChoiceBuildersOnly")
    buildersOnly = true
    javaInterop = true
  }

  kotlin {
    includes = listOf(
      "squareup.protos.kotlin.flat_redacted.FlatRedactedOneOfBuildersOnly",
      "squareup.protos.kotlin.flat_redacted.FlatRedactedSingleOneOfBuildersOnly",
    )
    buildersOnly = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.flat_redacted.FlatRedactedOneOfInteropBuildersOnly")
    buildersOnly = true
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.flat_redacted.FlatRedactedOneOfPlain")
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.boxed_redacted.BoxedRedactedOneOfBuildersOnly")
    oneofMode = "boxed"
    buildersOnly = true
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.boxed_redacted.*")
    oneofMode = "boxed"
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.sealed_redacted.SealedRedactedOneOfBuildersOnly")
    oneofMode = "sealed_class"
    buildersOnly = true
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.sealed_redacted.*")
    oneofMode = "sealed_class"
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.oneof_merge_boxed.*")
    oneofMode = "boxed"
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.oneof_merge_sealed.*")
    oneofMode = "sealed_class"
    javaInterop = true
  }

  kotlin {
    includes = listOf("squareup.protos.kotlin.oneof_merge_sealed_builders.*")
    oneofMode = "sealed_class"
    buildersOnly = true
    javaInterop = true
  }

  kotlin {
    boxOneOfsMinSize = 8
    javaInterop = true
  }
}
