import com.gradle.publish.PluginBundleExtension
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// This module is included in two projects:
// - In the root project where it's released as one of our artifacts
// - In build-logic project where we can use it for the test-schema and samples.
//
// We only want to publish when it's being built in the root project.

plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish").version("0.18.0").apply(false)
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.gradle.plugin-publish")
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

gradlePlugin {
  plugins {
    create("wire") {
      id = "com.squareup.wire"
      displayName = "Wire"
      implementationClass = "com.squareup.wire.gradle.WirePlugin"
      description = "generate code from .proto files"
    }
  }
}

if (project.rootProject.name == "wire") {
  configure<PluginBundleExtension> {
    website = "https://github.com/square/wire"
    vcsUrl = "https://github.com/square/wire"
    description = "generate code from .proto files"

    (plugins) {
      "wire" {
        displayName = "Wire"
        tags = listOf("wire", "protobuf")
      }
    }
  }
}

dependencies {
  implementation(gradleKotlinDsl())

  implementation(projects.wireCompiler)
  implementation(projects.wireKotlinGenerator)
  implementation(libs.swiftpoet)
  implementation(libs.okio.fakefilesystem)

  compileOnly(gradleApi())

  implementation(libs.pluginz.kotlin)
  compileOnly(libs.pluginz.android)

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

if (project.rootProject.name == "wire") {
  configure<MavenPublishBaseExtension> {
    configure(
      GradlePlugin(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
    )
  }
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
