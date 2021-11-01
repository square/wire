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
    srcJar(deps.protobuf.java)
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
  protobuf(deps.wire.schema)
  implementation(deps.wire.grpcClient)
  implementation(deps.okio.core)
  implementation(deps.protobuf.java)
  testImplementation(deps.wire.compiler)
  testImplementation(deps.wire.gsonSupport)
  testImplementation(deps.wire.moshiAdapter)
  testImplementation(deps.assertj)
  testImplementation(deps.junit)
  testImplementation(deps.protobuf.javaUtil)
  testImplementation(deps.wire.testUtils)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}
