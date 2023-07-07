plugins {
  id("com.android.library")
  id("com.squareup.wire")
}

wire {
  java {
    android = true
  }
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
  namespace = "com.squareup.wire.android.lib.java"
}
