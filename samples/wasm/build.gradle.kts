plugins {
  kotlin("multiplatform")
  id("com.squareup.wire")
}

repositories {
  mavenCentral()
}

kotlin {
  if (System.getProperty("kwasm", "true").toBoolean()) {
    wasmJs {
      browser()
    }
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
