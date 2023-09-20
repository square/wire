import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish").version("1.2.1").apply(false)
}

// This module is included in two projects:
// - In the root project where it's released as one of our artifacts
// - In build-support project where we can use it for the test-schema and samples.
//
// We only want to publish when it's being built in the root project.
if (project.rootProject.name == "wire") {
  apply(plugin = "com.gradle.plugin-publish")
}

gradlePlugin {
  website.set("https://github.com/square/wire")
  vcsUrl.set("https://github.com/square/wire")
  description = "generate code from .proto files"
  plugins {
    create("wire") {
      id = "com.squareup.wire"
      displayName = "Wire"
      implementationClass = "com.squareup.wire.gradle.WirePlugin"
      description = "generate code from .proto files"
      tags.set(listOf("wire", "protobuf"))
    }
  }
}

dependencies {
  compileOnly(gradleApi())
  compileOnly(libs.pluginz.android)

  implementation(projects.wireCompiler)
  implementation(projects.wireKotlinGenerator)
  implementation(libs.swiftpoet)
  implementation(libs.pluginz.kotlin)

  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(projects.wireTestUtils)
}

val installProtoJars by tasks.creating(Copy::class) {
  into("${rootDir.path}/build/localMaven")
  from("${projectDir.path}/src/test/libs")
}

tasks.withType<Test>().configureEach {
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  dependsOn(installProtoJars)
  dependsOn(":wire-runtime:installLocally")
}

val test by tasks.getting(Test::class) {
  // Fixtures run in a separate JVM, routing properties from the VM running the build into test VM.
  systemProperty("kjs", System.getProperty("kjs"))
  systemProperty("knative", System.getProperty("knative"))
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
    topLevelConstants = true
  }

  packageName("com.squareup.wire")
  buildConfigField("String", "VERSION", "\"${project.version}\"")
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_11.toString()
  targetCompatibility = JavaVersion.VERSION_11.toString()
}

tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}
