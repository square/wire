import com.diffplug.gradle.spotless.SpotlessExtension
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
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  api(libs.kotlinpoet)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
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

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/test/golden/*.kt",
    )
  }
}
