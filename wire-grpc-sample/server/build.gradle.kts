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
    srcDir("$rootDir/wire-grpc-sample/protos/src/main/proto")
  }
  kotlin {
    rpcCallStyle = "blocking"
    rpcRole = "server"
    singleMethodServices = true
  }
}

dependencies {
  implementation(project(":wire-grpc-sample:protos"))
  implementation(deps.wire.runtime)
  implementation(deps.misk)
}
