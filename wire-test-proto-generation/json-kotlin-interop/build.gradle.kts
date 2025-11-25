plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("../../wire-tests/fixtures/shared/proto/proto2")
    srcDir("../../wire-tests/fixtures/shared/proto/proto3")
    include(
      "dinosaur_java_interop_kotlin.proto",
      "keyword_kotlin.proto",
      "period_java_interop_kotlin.proto",
      "person_java_interop_kotlin.proto",
    )
  }
  kotlin {
    javaInterop = true
  }
}
