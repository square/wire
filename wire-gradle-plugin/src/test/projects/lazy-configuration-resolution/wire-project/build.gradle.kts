buildscript {
  dependencies {
    classpath("com.squareup.wire:wire-gradle-plugin:${properties["wireVersion"]}")
  }

  repositories {
    maven {
      setUrl("file://${rootDir.absolutePath}/../../../../../build/localMaven")
    }
    mavenCentral()
    google()
  }
}

plugins {
  id("com.squareup.wire").version("${properties["wireVersion"]}")
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

dependencies {
  protoSource(project(":dep-project"))
}

wire {
  kotlin {
  }
}
