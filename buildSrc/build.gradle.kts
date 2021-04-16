import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
  `kotlin-dsl`
}

repositories {
  jcenter()
}

kotlin.sourceSets {
  val main by getting {
    kotlin.srcDir("../wire-library/buildSrc/src/main/kotlin")
  }
}

// Without this we see "java.lang.NoClassDefFoundError: kotlin/KotlinNothingValueException"
// https://issuetracker.google.com/issues/166468915#comment15
dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.0")
}
