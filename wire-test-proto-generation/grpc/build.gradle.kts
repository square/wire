plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  sourcePath {
    srcDir("../../wire-grpc-tests/src/test/proto")
    include(
      "routeguide/RouteGuideProto.proto"
    )
  }
  kotlin {
    explicitStreamingCalls = true
  }
}

dependencies {
  api(projects.wireGrpcClient)
}
