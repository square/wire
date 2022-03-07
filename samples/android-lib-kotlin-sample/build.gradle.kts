plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.squareup.wire")
}

android {
  compileSdkVersion(30)

  defaultConfig {
    minSdkVersion(28)
    targetSdkVersion(30)
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

wire {
  kotlin {
    android = true
    javaInterop = true
  }
}

buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
  }
  dependencies {
    classpath(deps.plugins.android)
    classpath("com.squareup.wire:wire-gradle-plugin")
  }
}
