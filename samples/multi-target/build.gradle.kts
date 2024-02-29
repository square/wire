plugins {
  kotlin("multiplatform")
  id("com.squareup.wire")
}

repositories {
  mavenCentral()
}

wire {
  root("human.Person")

  kotlin {
    javaInterop = true
  }
}

kotlin {
  jvm { }

  js(IR) {
    nodejs()
  }

  iosArm64 {
    binaries.framework {
      baseName = "shared"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting
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
