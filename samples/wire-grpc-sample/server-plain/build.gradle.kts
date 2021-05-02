plugins {
  kotlin("jvm")
  id("com.squareup.wire")
  application
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
    grpcServerCompatible = true
  }
}

dependencies {
  implementation(project(":samples:wire-grpc-sample:protos"))
  implementation(deps.wire.grpcServer)
  implementation(deps.wire.runtime)
  implementation(deps.grpc.netty)
  implementation(deps.grpc.stub)
}
