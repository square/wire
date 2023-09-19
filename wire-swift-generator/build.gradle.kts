
plugins {
  id("java-library")
  kotlin("jvm")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

dependencies {
  api(libs.swiftpoet)
  api(projects.wireSchema)
}
