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
    attributes("Automatic-Module-Name" to "wire-grpc-server-generator")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-profiles"))
  implementation(project(":wire-runtime"))
  implementation(project(":wire-grpc-client"))
  implementation(deps.okio.jvm)
  api(deps.kotlinpoet)
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
