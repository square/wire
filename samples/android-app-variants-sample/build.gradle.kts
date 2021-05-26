plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.squareup.wire")
}

android {
  compileSdkVersion(30)
  buildToolsVersion("30.0.3")

  defaultConfig {
    applicationId = "com.squareup.wire.android.app.variants"
    minSdkVersion(28)
    targetSdkVersion(30)
    versionCode = 1
    versionName = "1.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

wire {
  java {
    includes = listOf("com.squareup.wire.android.app.variants.CommonType")
  }
  kotlin {
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.2.0")
  implementation("androidx.constraintlayout:constraintlayout:2.0.4")
  implementation("com.google.android.material:material:1.3.0")
}

buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.6.3")
    classpath("com.squareup.wire:wire-gradle-plugin")
  }
}
