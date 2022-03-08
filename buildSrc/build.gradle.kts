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
