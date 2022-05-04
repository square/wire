import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  api(project(":wire-schema"))
  api(deps.okio.core)
  implementation(deps.junit)
  implementation(deps.okio.fakefilesystem)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.assertj)
  testImplementation(deps.kotlin.test.junit)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
