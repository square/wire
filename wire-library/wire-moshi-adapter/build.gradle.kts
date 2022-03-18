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
  implementation(project(":wire-runtime"))
  api(deps.moshi)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.assertj)
  testImplementation(deps.junit)
  testImplementation(deps.moshiKotlin)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/test/java/com/squareup/wire/proto2/**/*.kt",
      "src/test/java/com/squareup/wire/protos/**/*.kt",
      "src/test/java/squareup/proto2/keywords/**/*.kt",
    )
  }
}
