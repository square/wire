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
}

wire {
  protoLibrary = true

  kotlin {
  }
}
