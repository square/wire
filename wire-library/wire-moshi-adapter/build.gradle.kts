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

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-moshi-adapter")
  }
}

val main by sourceSets.getting
configure<AnimalSnifferExtension> {
  sourceSets = listOf(main)
}

dependencies {
  implementation(project(":wire-runtime"))
  api(deps.moshi)
  api(deps.moshiKotlin)
  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.junit)
  testImplementation(deps.assertj)
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
