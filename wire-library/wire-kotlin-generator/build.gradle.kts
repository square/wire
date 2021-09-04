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
    attributes("Automatic-Module-Name" to "wire-kotlin-generator")
  }
}

dependencies {
  api(project(":wire-schema"))
  implementation(project(":wire-profiles"))
  implementation(project(":wire-runtime"))
  implementation(project(":wire-grpc-client"))
  implementation(project(":wire-grpc-server-generator"))
  implementation(deps.okio.jvm)
  api(deps.kotlinpoet)
  implementation(deps.guava)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.truth)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
