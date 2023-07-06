plugins {
  application
  kotlin("jvm")
  id("com.squareup.wire")
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
  }
}

dependencies {
  implementation(projects.samples.wireGrpcSample.protos)
  implementation(projects.wireRuntime)
  implementation(libs.misk)
}
