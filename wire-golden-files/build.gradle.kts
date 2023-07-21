plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

dependencies {
  implementation(projects.wireGrpcClient)
}

wire {
  kotlin {
    includes = listOf("squareup.wire.service.*")
    out = "src/main/kotlin"
    exclusive = false
    rpcRole = "client"
    rpcCallStyle = "suspending"
  }

  kotlin {
    includes = listOf("squareup.wire.service.*")
    out = "src/main/kotlin"
    exclusive = true
    rpcRole = "server"
    rpcCallStyle = "blocking"
  }

  kotlin {
    includes = listOf("squareup.wire.buildersonly.*")
    out = "src/main/kotlin"
    buildersOnly = true
  }

  kotlin {
    includes = listOf("squareup.wire.boxedoneof.*")
    out = "src/main/kotlin"
    javaInterop = true
    boxOneOfsMinSize = 1
  }

  kotlin {
    out = "src/main/kotlin"
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}
