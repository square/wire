import com.gradle.publish.PluginBundleExtension
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.MavenPublishBaseExtension

//
// // This module is included in two projects:
// // - In the root project where it's released as one of our artifacts
// // - In build-logic project where we can use it for the test-schema and samples.
// //
// // We only want to publish when it's being built in the root project.
// if (rootProject.name == 'wire') {
//   apply plugin: 'com.vanniktech.maven.publish'
//   apply plugin: 'org.jetbrains.dokka'
// }


plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish").version("0.18.0").apply(false)
  id("org.jetbrains.dokka").apply(false)
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.gradle.plugin-publish")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "com.vanniktech.maven.publish.base")
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

dependencies {
  implementation(projects.wireCompiler)
  implementation(projects.wireKotlinGenerator)
  implementation(libs.swiftpoet)

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
