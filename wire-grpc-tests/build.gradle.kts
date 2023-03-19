import com.diffplug.gradle.spotless.SpotlessExtension
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
  dependencies {
    classpath(libs.protobuf.gradlePlugin)
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("java-library")
  kotlin("jvm")
  id("ru.vyarus.animalsniffer")
  id("com.google.protobuf")
}

protobuf {
  plugins {
    id("grpc") {
      artifact = libs.grpc.genJava.get().toString()
    }
  }

  protoc {
    artifact = libs.protobuf.protoc.get().toString()
  }

  generateProtoTasks {
    ofSourceSet("test").forEach {
      it.plugins {
        // Apply the "grpc" plugin whose spec is defined above, without
        // options.  Note the braces cannot be omitted, otherwise the
        // plugin will not be added. This is because of the implicit way
        // NamedDomainObjectContainer binds the methods.
        id("grpc") {}
      }
    }
  }
}

sourceSets {
  val test by getting {
    java.srcDir("build/generated/source/proto/test/grpc")
    java.srcDir("build/generated/source/proto/test/java")
    java.srcDir("src/test/proto-grpc")
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}

dependencies {
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(projects.wireGrpcMockwebserver)
  implementation(libs.okio.core)
  if (JavaVersion.current().isJava9Compatible()) {
    // Workaround for @javax.annotation.Generated
    // see: https://github.com/grpc/grpc-java/issues/3633
    implementation("javax.annotation:javax.annotation-api:1.3.1")
  }
  compileOnly(libs.android)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.grpc.netty)
  testImplementation(libs.grpc.protobuf)
  testImplementation(libs.grpc.stub)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/test/proto-grpc/**/*.kt",
    )
  }
}
