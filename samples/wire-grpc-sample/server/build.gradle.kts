import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
  kotlin("jvm")
  id("com.squareup.wire")
}

application {
  mainClass.set("com.squareup.wire.whiteboard.MiskGrpcServerKt")
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_17.toString()
  targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs += "-Xjvm-default=all"
    // https://kotlinlang.org/docs/whatsnew13.html#progressive-mode
    freeCompilerArgs += "-progressive"
  }
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
