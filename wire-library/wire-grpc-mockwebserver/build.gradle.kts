import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

plugins {
  id("ru.vyarus.animalsniffer")
  kotlin("jvm")
}

val main by sourceSets.getting
configure<AnimalSnifferExtension> {
  sourceSets = listOf(main)
}

dependencies {
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  api(libs.okhttp.mockwebserver)
}
