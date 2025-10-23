import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.pluginz.dokka)
    classpath(libs.pluginz.buildConfig)
    classpath(libs.pluginz.spotless)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    classpath(libs.pluginz.buildConfig)
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  kotlin("jvm") version "2.2.21"
}

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  compileOnly(libs.kotlin.gradleApi)
  implementation(libs.pluginz.android)
  implementation(libs.pluginz.binaryCompatibilityValidator)
  implementation(libs.pluginz.kotlin)
  implementation(libs.vanniktechPublishPlugin)
  implementation(libs.pluginz.dokka)
  implementation(libs.kotlin.serialization)
  implementation(libs.pluginz.buildConfig)
  implementation(libs.pluginz.spotless)
  implementation(libs.pluginz.kotlinSerialization)
  implementation(libs.pluginz.shadow)
  implementation(libs.pluginz.buildConfig)

  // Expose the generated version catalog API to the plugin.
  implementation(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
  plugins {
    create("wireBuild") {
      id = "com.squareup.wire.build"
      displayName = "Wire Build plugin"
      description = "Gradle plugin for Wire build things"
      implementationClass = "com.squareup.wire.buildsupport.WireBuildPlugin"
    }
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }

  plugins.withId("java") {
    configure<JavaPluginExtension> {
      withSourcesJar()
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
  }

  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
      freeCompilerArgs.add("-Xjvm-default=all")
    }
  }
}
