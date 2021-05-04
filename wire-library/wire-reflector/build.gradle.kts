plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm {
    withJava()
  }

  sourceSets {
    val jvmMain by getting {
      kotlin.srcDir("$buildDir/wire")
      dependencies {
        api(project(":wire-compiler"))
        api(project(":wire-grpc-client"))
        api(project(":wire-runtime"))
        api(project(":wire-schema"))
        implementation(deps.okio.jvm)
        api(deps.guava)
        implementation("io.grpc:grpc-protobuf:1.21.0")
        implementation("com.google.protobuf:protoc:3.6.1")
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(project(":wire-test-utils"))
        implementation(deps.junit)
        implementation(deps.kotlin.test.junit)
        implementation(deps.assertj)
        implementation(deps.jimfs)
      }
    }
  }
}

val generateReflectionProtosClasspath by configurations.creating

dependencies {
  generateReflectionProtosClasspath(project(":wire-compiler"))
}

val generateReflectionProtos by tasks.creating(JavaExec::class) {
  main = "com.squareup.wire.WireCompiler"
  classpath = generateReflectionProtosClasspath
  args(
    "--proto_path=$projectDir/src/jvmMain/resources",
    "--kotlin_out=$buildDir/wire",
    "grpc/reflection/v1alpha/reflection.proto"
  )
}

val compileKotlinJvm by tasks.getting {
  dependsOn(generateReflectionProtos)
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-reflector")
  }
}

apply(from = "$rootDir/gradle/gradle-mvn-mpp-push.gradle")

configure<PublishingExtension> {
  // Use default artifact name for the JVM target
  publications {
    val kotlinMultiplatform by getting(MavenPublication::class) {
      artifactId = "wire-reflector-multiplatform"
    }
    val jvm by getting(MavenPublication::class) {
      artifactId = "wire-reflector"
    }
  }
}
