import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  application
  kotlin("jvm")
}

dependencies {
  api(deps.junit)
  api(project(":wire-compiler"))
  api(project(":wire-schema"))
  implementation(project(":wire-kotlin-generator"))
  implementation(project(":wire-java-generator"))
  implementation(project(":wire-swift-generator"))
  implementation(project(":wire-profiles"))
  implementation(deps.okio.core)
  implementation(deps.okio.fakefilesystem)
  testImplementation(deps.assertj)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(project(":wire-test-utils"))
}
