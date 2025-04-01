import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-gradle-plugin")
  `kotlin-dsl`
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  compileOnly(kotlin("gradle-plugin-api"))
  compileOnly(project(":wire-binary-compatibility-kotlin-plugin"))
  implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
  plugins {
    create("wireBinaryCompatibility") {
      id = "com.squareup.wire.binarycompatibility"
      displayName = "Wire Binary Compatibility"
      description = "Rewrites Wire callsites to be resilient to API changes"
      implementationClass = "com.squareup.wire.binarycompatibility.gradle.WireBinaryCompatibility"
    }
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    GradlePlugin(
      javadocJar = JavadocJar.Empty()
    )
  )
}
