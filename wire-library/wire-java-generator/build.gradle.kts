import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-java-generator")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-runtime"))
  implementation(project(":wire-profiles"))
  implementation(deps.okio.jvm)
  implementation(deps.guava)
  api(deps.javapoet)
  compileOnly(deps.jsr305)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.junit)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.jimfs)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
