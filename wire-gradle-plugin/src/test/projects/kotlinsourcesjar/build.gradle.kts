plugins {
  id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin
  id("com.squareup.wire") version("$wireVersion")
}

wire {
  kotlin {
  }
}
