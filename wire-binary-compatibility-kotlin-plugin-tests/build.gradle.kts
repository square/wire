plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
}

dependencies {
  testImplementation(project(":wire-binary-compatibility-kotlin-plugin"))
  testImplementation(kotlin("compiler-embeddable"))
  testImplementation(kotlin("test-junit"))
  testImplementation(libs.assertk)
  testImplementation(libs.kotlin.compile.testing)
}
