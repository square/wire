import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-runtime"))
  implementation(libs.okio.core)
  implementation(libs.guava)
  api(libs.javapoet)
  compileOnly(libs.jsr305)
  testImplementation(project(":wire-test-utils"))
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.jimfs)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
