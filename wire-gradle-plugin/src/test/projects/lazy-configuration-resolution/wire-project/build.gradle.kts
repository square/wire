plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
  id("com.squareup.wire")
}

dependencies {
  protoSource(project(":dep-project"))
}

wire {
  kotlin {
  }
}
