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
  namespace = "com.squareup.wire.android.app.multi.dinosaurs"
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintLayout)
  implementation(libs.androidMaterial)
  testImplementation(libs.junit)
}

wire {
  sourcePath {
    srcDir("src/main/proto")
  }

  sourcePath {
    srcProject(":samples:android-multi-module:location")
    include("squareup/location/continent.proto")
  }

  kotlin {
    out = "src/main/kotlin"
  }
}
