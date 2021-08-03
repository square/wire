import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.github.johnrengelman.shadow")
  id("internal-publishing")
}

application {
  mainClassName = "com.squareup.wire.WireCompiler"
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-compiler")
  }
}

dependencies {
  api(project(":wire-schema"))
  api(project(":wire-kotlin-generator"))
  implementation(project(":wire-java-generator"))
  implementation(project(":wire-swift-generator"))
  implementation(project(":wire-profiles"))
  implementation(deps.okio.jvm)
  implementation(deps.guava)
  implementation(deps.kotlin.serialization)
  implementation(deps.kaml)
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.okio.fakefilesystem)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(project(":wire-test-utils"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
  classifier = "jar-with-dependencies"
}

artifacts {
  archives(shadowJar)
}

// The `shadow` plugin internally applies the `distribution` plugin and
// automatically adds tasks to create respective tar and zip artifacts.
// https://github.com/johnrengelman/shadow/issues/347#issuecomment-424726972
// https://github.com/johnrengelman/shadow/commit/a824e4f6e4618785deb7f084c4a80ce1b78fc4fd
tasks.findByName("shadowDistTar")?.enabled = false
tasks.findByName("shadowDistZip")?.enabled = false
configurations["archives"].artifacts.removeAll {
  val file: File = it.file
  file.name.contains("tar") || file.name.contains("zip")
}
