plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

dependencies {
  implementation(libs.assertk)
  implementation(libs.kotlin.test.junit)
  implementation(projects.wireGsonSupport)
  implementation(projects.wireMoshiAdapter)
}

wire {
  java {
    includes = listOf("squareup.wire.json.oneof.java.*")
  }

  // Flat oneofs. Without `javaInterop` or `buildersOnly`, Wire uses a reflective builder.
  kotlin {
    includes = listOf("squareup.wire.json.oneof.kotlin.*")
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.kotlin_interop.*")
    javaInterop = true
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.kotlin_builders.*")
    buildersOnly = true
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.kotlin_interop_builders.*")
    javaInterop = true
    buildersOnly = true
  }

  kotlin {
    includes = listOf("squareup.wire.json.oneof.boxed.*")
    oneofMode = "boxed"
    javaInterop = true
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.boxed_builders.*")
    oneofMode = "boxed"
    buildersOnly = true
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.sealed_plain.*")
    oneofMode = "sealed_class"
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.sealed.*")
    oneofMode = "sealed_class"
    javaInterop = true
  }
  kotlin {
    includes = listOf("squareup.wire.json.oneof.sealed_builders.*")
    oneofMode = "sealed_class"
    buildersOnly = true
  }
}
