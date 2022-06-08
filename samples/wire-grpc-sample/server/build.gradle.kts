plugins {
  application
  kotlin("jvm")
  id("com.squareup.wire")
}

application {
  mainClassName = "com.squareup.wire.whiteboard.MiskGrpcServerKt"
}

wire {
  sourcePath {
    srcDir("$rootDir/samples/wire-grpc-sample/protos/src/main/proto")
  }
  kotlin {
    rpcCallStyle = "blocking"
    rpcRole = "server"
    singleMethodServices = true
  }
}

dependencies {
  implementation(project(":samples:wire-grpc-sample:protos"))
  implementation(libs.wire.runtime)
  implementation(libs.misk)
}
