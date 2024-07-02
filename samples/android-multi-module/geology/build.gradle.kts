plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.squareup.wire")
}

buildscript {
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath(libs.pluginz.android)
    classpath("com.squareup.wire:wire-gradle-plugin")
  }
}

android {
  namespace = "com.squareup.wire.android.app.multi.geology"
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintLayout)
  implementation(libs.androidMaterial)
  testImplementation(libs.junit)
}

wire {
  protoLibrary = true

  kotlin {
  }
}
