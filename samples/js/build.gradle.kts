plugins {
  kotlin("js")
  id("com.squareup.wire")
}

repositories {
  mavenCentral()
}

kotlin {
  js(IR) {
    binaries.executable()
    browser {
      commonWebpackConfig {
        cssSupport {
          enabled.set(true)
        }
      }
    }
    nodejs {
    }
    outputModuleName = "wire-js-module"
  }
}

wire {
  protoLibrary = true

  kotlin {
    buildersOnly = true
  }
}


buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.squareup.wire:wire-gradle-plugin")
  }
}
