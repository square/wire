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
  api(libs.junit)
  api(projects.wireCompiler)
  api(projects.wireSchema)
  implementation(projects.wireKotlinGenerator)
  implementation(projects.wireJavaGenerator)
  implementation(projects.wireSwiftGenerator)
  implementation(libs.okio.core)
  implementation(libs.okio.fakefilesystem)
  testImplementation(libs.assertj)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(projects.wireTestUtils)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
