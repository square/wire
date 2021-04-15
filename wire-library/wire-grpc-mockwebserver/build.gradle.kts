plugins {
  id("java-library")
  id("ru.vyarus.animalsniffer")
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-grpc-mockwebserver")
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
}

dependencies {
  implementation(project(":wire-runtime"))
  implementation(project(":wire-grpc-client"))
  implementation(deps.okio.jvm)
  api(deps.mockwebserver)
}
