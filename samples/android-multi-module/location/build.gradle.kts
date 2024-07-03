plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.squareup.wire")
  id("maven-publish")
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
  namespace = "com.squareup.wire.android.app.multi.location"

  publishing {
    singleVariant("debug") {
      withSourcesJar()
    }
  }
}

publishing {
  publications {
    create("debug", MavenPublication::class.java) {
      groupId = "com.my-company"
      artifactId = "my-library"
      version = "1.0"

      afterEvaluate {
        from(components.getByName("debug"))
      }
    }
  }
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
