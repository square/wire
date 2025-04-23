import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-gradle-plugin")
  `kotlin-dsl`
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  implementation(kotlin("gradle-plugin-api"))
  implementation(projects.wireBinaryCompatibilityKotlinPlugin)
  compileOnly(libs.kotlin.gradlePlugin)
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
  }
  val compilerPlugin = projects.wireBinaryCompatibilityKotlinPlugin
  val packageName = "com.squareup.wire.binarycompatibility.gradle"
  packageName(packageName)
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${packageName}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${compilerPlugin.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${compilerPlugin.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${compilerPlugin.version}\"")
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
