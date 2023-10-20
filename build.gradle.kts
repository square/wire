import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

buildscript {
  dependencies {
    classpath(libs.pluginz.dokka)
    classpath(libs.pluginz.android)
    classpath(libs.pluginz.binaryCompatibilityValidator)
    classpath(libs.pluginz.kotlin)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    classpath(libs.pluginz.spotless)
    classpath(libs.protobuf.gradlePlugin)
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.pluginz.buildConfig)
    classpath(libs.guava)
    classpath(libs.asm)

    classpath("com.squareup.wire:wire-gradle-plugin")
    classpath("com.squareup.wire.build:gradle-plugin")
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    mavenCentral()
    google()
  }
}

apply(from = "gen-tests.gradle.kts")
