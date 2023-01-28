import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("org.jetbrains.dokka").apply(false)
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "com.vanniktech.maven.publish.base")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(libs.okio.core)
  implementation(libs.guava)
  api(libs.javapoet)
  compileOnly(libs.jsr305)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.jimfs)
}

if (project.rootProject.name == "wire") {
  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
    )
  }
}
