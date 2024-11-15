import com.google.protobuf.gradle.protobuf
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.protobuf")
  id("com.squareup.wire")
}

protobuf {
  protoc {
    artifact = libs.protobuf.protoc.get().toString()
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
    buildersOnly = true
    boxOneOfsMinSize = 3

    includes = listOf(
      "squareup.proto2.kotlin.buildersonly.*",
    )
  }

  kotlin {
    javaInterop = true
    boxOneOfsMinSize = 5
    enumMode = "sealed_class"

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
  protobuf(projects.wireSchema)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  implementation(libs.protobuf.java)
  testImplementation(projects.wireCompiler)
  testImplementation(projects.wireGsonSupport)
  testImplementation(projects.wireMoshiAdapter)
  testImplementation(libs.assertk)
  testImplementation(libs.junit)
  testImplementation(libs.protobuf.javaUtil)
  testImplementation(projects.wireTestUtils)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}
