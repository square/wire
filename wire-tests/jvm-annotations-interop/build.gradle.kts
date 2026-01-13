plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

dependencies {
  implementation(libs.assertk)
  implementation(libs.jimfs)
  implementation(libs.kotlin.test.junit)
  implementation(libs.truth)
  implementation(projects.wireTestUtils)
}

wire {
  sourcePath {
    srcDir("../fixtures/proto/kotlin")
    include(
      "custom_options2.proto",
    )
  }
  sourcePath {
    srcDir("../fixtures/proto/java")
    include(
      "custom_options.proto",
      "foreign.proto",
      "option_redacted.proto",
    )
  }

  kotlin {
    includes = listOf("squareup.protos.custom_options.FooBar2")
  }

  java {
  }
}
