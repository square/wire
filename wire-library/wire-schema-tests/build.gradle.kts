import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

// TODO(Benoit) this module can be multiplatform.

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  api(deps.junit)
  api(project(":wire-compiler"))
  api(project(":wire-schema"))
  implementation(project(":wire-kotlin-generator"))
  implementation(project(":wire-java-generator"))
  implementation(project(":wire-swift-generator"))
  implementation(deps.okio.core)
  implementation(deps.okio.fakefilesystem)
  testImplementation(deps.assertj)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(project(":wire-test-utils"))
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
