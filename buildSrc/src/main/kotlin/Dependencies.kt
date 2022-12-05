// If false - JS targets will not be configured in multiplatform projects.
val kmpJsEnabled = System.getProperty("kjs", "true").toBoolean()

// If false - Native targets will not be configured in multiplatform projects.
val kmpNativeEnabled = System.getProperty("knative", "true").toBoolean()

object versions {
  val grpc = "1.44.1"
  val protobuf = "3.19.4"
}

object deps {
  object grpc {
    // TODO(Benoit) this cannot be passed to the `protobuf` Gradle plugin via versionCatalogs.
    //  See https://github.com/google/protobuf-gradle-plugin/issues/563
    val genJava = "io.grpc:protoc-gen-grpc-java:${versions.grpc}"
  }

  object protobuf {
    // TODO(Benoit) this cannot be passed to the `protobuf` Gradle plugin via versionCatalogs.
    //  See https://github.com/google/protobuf-gradle-plugin/issues/563
    val protoc = "com.google.protobuf:protoc:${versions.protobuf}"
  }
}
