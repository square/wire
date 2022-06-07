plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  kotlin {
    rpcCallStyle = "suspending"
    rpcRole = "client"
    singleMethodServices = false
  }
}

dependencies {
  implementation(libs.wire.grpcClient)
}
