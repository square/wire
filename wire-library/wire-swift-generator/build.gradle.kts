plugins {
  id("java-library")
  kotlin("jvm")
  id("internal-publishing")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-swift-generator")
  }
}

dependencies {
  api(deps.swiftpoet)
  api(project(":wire-schema"))
}
