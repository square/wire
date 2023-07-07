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
    srcProject(projects.samples.wireGrpcSample.protos)
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
