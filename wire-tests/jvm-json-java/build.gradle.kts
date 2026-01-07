import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.kotlin

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

java {
  sourceSets {
    val test by getting {
      kotlin.srcDir("../wire-json-shared-kotlin-tests")
    }
  }
}

wire {
  sourcePath {
    srcDir("../fixtures/shared/proto/proto2")
    srcDir("../fixtures/shared/proto/proto3")
    include(
      "all32.proto",
      "all64.proto",
      "all_types_proto2.proto",
      "all_types_proto3_test_proto3_optional.proto",
      "all_structs.proto",
      "all_wrappers.proto",
      "camel_case.proto",
      "map_types.proto",
      "pizza.proto",
    )
  }
  java {
    compact = true
  }
}
