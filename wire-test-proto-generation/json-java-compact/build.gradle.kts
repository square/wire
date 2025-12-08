plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("../../wire-tests/fixtures/shared/proto/proto2")
    include(
      "all_types_proto2.proto",
    )
  }
  java {
    compact = true
  }
}
