import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

plugins {
  id("ru.vyarus.animalsniffer")
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

val main by sourceSets.getting
configure<AnimalSnifferExtension> {
  sourceSets = listOf(main)
}

dependencies {
  implementation(projects.wireRuntime)
  api(libs.gson)
  api(libs.okio.core)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(projects.wireTestUtils)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/test/java/com/squareup/wire/protos/**/*.kt",
      "src/test/java/com/squareup/wire/proto2/**/*.kt",
      "src/test/java/squareup/proto2/keywords/**/*.kt",
    )
  }
}
