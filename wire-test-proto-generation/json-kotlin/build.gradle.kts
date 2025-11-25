plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("../../wire-tests/fixtures/shared/proto/proto2")
    srcDir("../../wire-tests/fixtures/shared/proto/proto3")
    include(
      "all_structs.proto",
      "dinosaur_kotlin.proto",
      "getters_kotlin.proto",
      "period_kotlin.proto",
      "person_kotlin.proto",
      "pizza.proto",
    )
  }
  kotlin {
  }
}
