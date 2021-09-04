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
    attributes("Automatic-Module-Name" to "wire-grpc-server")
  }
}

dependencies {
  implementation(project(":wire-runtime"))
  // io.grpc.stub relies on guava-android. This module relies on a -jre version of guava.
  implementation(deps.grpc.stub) {
    exclude(group = "com.google.guava", module = "guava")
  }
  implementation("com.google.guava:guava:21.0")
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.truth)
  testImplementation(deps.assertj)
}

sourceSets {
  val test by getting {
    java.srcDir("src/test/proto")
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
