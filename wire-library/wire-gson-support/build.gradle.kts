plugins {
  id("java-library")
  id("ru.vyarus.animalsniffer")
  kotlin("jvm")
  id("internal-publishing")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-gson-support")
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
}

dependencies {
  implementation(project(":wire-runtime"))
  api(deps.gson)
  api(deps.okio.jvm)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
  testImplementation(project(":wire-test-utils"))
}
