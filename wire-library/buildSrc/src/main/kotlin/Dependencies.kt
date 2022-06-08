// If false - JS targets will not be configured in multiplatform projects.
val kmpJsEnabled = System.getProperty("kjs", "true").toBoolean()

// If false - Native targets will not be configured in multiplatform projects.
val kmpNativeEnabled = System.getProperty("knative", "true").toBoolean()

object versions {
  val android = "4.1.1.4"
  val animalSniffer = "1.16"
  val animalSnifferGradle = "1.5.0"
  val assertj = "3.11.0"
  val coroutines = "1.5.2"
  val dokka = "1.5.30"
  val grpc = "1.44.1"
  val gson = "2.9.0"
  val guava = "31.1-jre"
  val javapoet = "1.13.0"
  val jimfs = "1.0"
  val jmh = "1.34"
  val jsr305 = "3.0.2"
  val junit = "4.13.2"
  val kotlin = "1.6.10"
  val kotlinpoet = "1.10.2"
  val ktlint = "0.42.1"
  val moshi = "1.13.0"
  val okhttp = "4.9.3"
  val protobuf = "3.19.4"
  val protobufGradlePlugin = "0.8.18"
}

object deps {
  object plugins {
    val android = "com.android.tools.build:gradle:7.1.2"
    val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
    val kotlinSerialization = "org.jetbrains.kotlin:kotlin-serialization:${versions.kotlin}"
    val shadow = "com.github.jengelman.gradle.plugins:shadow:4.0.1"
    val japicmp = "me.champeau.gradle:japicmp-gradle-plugin:0.3.1"
    val spotless = "com.diffplug.spotless:spotless-plugin-gradle:6.3.0"
  }

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
