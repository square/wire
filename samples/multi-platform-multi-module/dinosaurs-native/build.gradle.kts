plugins {
  kotlin("multiplatform")
  id("com.squareup.wire")
}

kotlin {
  val hostOs = System.getProperty("os.name")
  val isMingwX64 = hostOs.startsWith("Windows")
  val nativeTarget = when {
    hostOs == "Mac OS X" -> macosX64("native")
    hostOs == "Linux" -> linuxX64("native")
    isMingwX64 -> mingwX64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
  }

  nativeTarget.apply {
    binaries {
      executable {
        entryPoint = "main"
      }
    }
  }
  sourceSets {
    val commonMain by getting
    val nativeMain by getting
    val nativeTest by getting
  }
}

dependencies {
  protoPath(project(":samples:multi-platform-multi-module:geology-native"))
}

wire {
  sourcePath {
    srcDir("src/main/proto")
  }

  sourcePath {
    srcProject(":samples:multi-platform-multi-module:location-js")
    include("squareup/location/continent.proto")
  }

  kotlin {
  }
}