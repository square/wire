import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
  dependencies {
    classpath(deps.protobuf.gradlePlugin)
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
      artifact = deps.grpc.genJava
    }
  }

  protoc {
    artifact = deps.protobuf.protoc
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
  implementation(deps.wire.runtime)
  implementation(deps.wire.grpcClient)
  implementation(deps.wire.grpcMockWebServer)
  implementation(deps.okio.jvm)
  if (JavaVersion.current().isJava9Compatible()) {
    // Workaround for @javax.annotation.Generated
    // see: https://github.com/grpc/grpc-java/issues/3633
    implementation("javax.annotation:javax.annotation-api:1.3.1")
  }
  compileOnly(deps.android)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.grpc.netty)
  testImplementation(deps.grpc.protobuf)
  testImplementation(deps.grpc.stub)
}

val test by tasks.getting(Test::class) {
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}
