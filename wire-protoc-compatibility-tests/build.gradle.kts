import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.protobuf")
  id("com.squareup.wire")
}

protobuf {
  protoc {
    artifact = deps.protobuf.protoc
  }
}

wire {
  sourcePath {
    srcDir("src/main/proto")
  }

  sourcePath {
    srcJar(libs.protobuf.java)
    include("google/protobuf/descriptor.proto")
  }

  protoPath {
    srcJar("src/main/proto/protos.jar")
    include("squareup/geology/period.proto")
  }

  kotlin {
    javaInterop = true
    boxOneOfsMinSize = 5

    includes = listOf(
      "squareup.proto2.kotlin.*",
      "squareup.proto3.kotlin.*",
      "google.protobuf.*"
    )
  }

  java {
    includes = listOf(
      "squareup.proto2.java.*",
      "squareup.proto3.java.*"
    )
  }
}

sourceSets {
  val test by getting {
    java.srcDir("build/generated/source/proto/main/java")
  }
}

dependencies {
  protobuf(libs.wire.schema)
  implementation(libs.wire.grpcClient)
  implementation(libs.okio.core)
  implementation(libs.protobuf.java)
  testImplementation(libs.wire.compiler)
  testImplementation(libs.wire.gsonSupport)
  testImplementation(libs.wire.moshiAdapter)
  testImplementation(libs.assertj)
  testImplementation(libs.junit)
  testImplementation(libs.protobuf.javaUtil)
  testImplementation(libs.wire.testUtils)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}
