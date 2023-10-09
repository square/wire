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
  }

  sourceSets {
    val main by getting {
      dependencies {
        implementation(projects.wireGrpcClient)
      }
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
