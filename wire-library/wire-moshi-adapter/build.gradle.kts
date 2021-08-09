import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  id("java-library")
  id("ru.vyarus.animalsniffer")
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-moshi-adapter")
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
}

dependencies {
  implementation(project(":wire-runtime"))
  api(deps.moshi)
  api(deps.moshiKotlin)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}

afterEvaluate {
  val dokka by tasks.getting(DokkaTask::class) {
    outputDirectory = "$rootDir/docs/3.x"
    outputFormat = "gfm"
  }
}
