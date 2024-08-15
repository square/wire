plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  kotlin {
    includes = listOf("squareup.wire.unrecognized_constant.*")
    out = "src/main/kotlin"
    buildersOnly = true
    enumMode = "sealed_class"
  }

  kotlin {
    includes = listOf(
      "squareup.wire.buildersonly.*",
      "squareup.wire.alltypes.*",
    )
    out = "src/main/kotlin"
    buildersOnly = true
  }

  kotlin {
    includes = listOf("squareup.wire.boxedoneof.*")
    out = "src/main/kotlin"
    javaInterop = true
    boxOneOfsMinSize = 1
  }

  java {
    includes = listOf("squareup.java.*")
    out = "src/main/java"
  }

  kotlin {
    out = "src/main/kotlin"
  }
}

tasks.getByName("spotlessJava").dependsOn("generateMainProtos")
tasks.getByName("spotlessKotlin").dependsOn("generateMainProtos")
tasks.getByName("spotlessSwift").dependsOn("generateMainProtos")
