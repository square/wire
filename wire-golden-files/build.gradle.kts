plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  kotlin {
    includes = listOf("squareup.wire.buildersonly.*")
    out = "src/main/kotlin"
    buildersOnly = true
  }
}
