plugins {
  kotlin("jvm")
  id("com.squareup.wire")
  application
}

application {
  mainClass.set("com.squareup.wire.whiteboard.MiskGrpcServerKt")
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
  implementation(projects.samples.wireGrpcSample.protos)
  implementation(projects.wireGrpcServer)
  implementation(projects.wireRuntime)
  implementation(libs.grpc.netty)
  implementation(libs.grpc.stub)
  implementation(libs.grpc.protobuf)
}
