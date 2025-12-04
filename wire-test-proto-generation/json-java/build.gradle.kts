plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("../../wire-tests/fixtures/shared/proto/proto2")
    srcDir("../../wire-tests/fixtures/shared/proto/proto3")
    include(
      "collection_types.proto",
      "person_java.proto",
      "dinosaur_java.proto",
      "period_java.proto",
      "keyword_java.proto",
    )
  }
  java {
  }
}
