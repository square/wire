plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  java {
    includes = listOf("squareup.wire.alltypes.proto3.*")
    out = "src/main/java"
  }

  kotlin {
    includes = listOf("squareup.wire.mutable.*")
    out = "src/main/kotlin"
    mutableTypes = true
  }

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

  opaque("squareup.protos.opaque_types.OuterOpaqueType.InnerOpaqueType1")
  kotlin {
    includes = listOf("squareup.protos.opaque_types.*")
    out = "src/main/kotlin"
  }

  kotlin {
    out = "src/main/kotlin"
  }
}

tasks.getByName("spotlessJava").dependsOn("generateMainProtos")
tasks.getByName("spotlessKotlin").dependsOn("generateMainProtos")
tasks.getByName("spotlessSwift").dependsOn("generateMainProtos")
