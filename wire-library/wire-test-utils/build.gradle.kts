plugins {
  id("java-library")
  kotlin("jvm")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-test-utils")
  }
}

dependencies {
  api(deps.moshi)
  api(project(":wire-runtime"))
  api(project(":wire-schema"))
  implementation(project(":wire-compiler"))
  implementation(project(":wire-java-generator"))
  implementation(project(":wire-kotlin-generator"))
  implementation(project(":wire-profiles"))
  implementation(deps.assertj)
  implementation(deps.guava)
  implementation(deps.jimfs)
  implementation(deps.junit)
  implementation(deps.okio.jvm)
  implementation(deps.okio.fakefilesystem)
}
