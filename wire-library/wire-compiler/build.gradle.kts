import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  application
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.github.johnrengelman.shadow")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

application {
  mainClassName = "com.squareup.wire.WireCompiler"
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireKotlinGenerator)
  implementation(projects.wireJavaGenerator)
  implementation(projects.wireSwiftGenerator)
  implementation(libs.okio.core)
  implementation(libs.guava)
  implementation(libs.kotlin.serialization)
  implementation(libs.kaml)
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.okio.fakefilesystem)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(projects.wireTestUtils)
}

val shadowJar by tasks.getting(ShadowJar::class) {
  classifier = "jar-with-dependencies"
}

publishing {
  publications.withType<MavenPublication>().configureEach {
    artifact(shadowJar)
  }
}

tasks {
  // binary: https://www.baeldung.com/kotlin/gradle-executable-jar
  val binary = register<Jar>("binary") {
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
    archiveClassifier.set("standalone") // Naming the jar
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
      .map { if (it.isDirectory) it else zipTree(it) } +
      sourcesMain.output
    from(contents)
  }
  build {
    dependsOn(binary)
  }
}

// The `shadow` plugin internally applies the `distribution` plugin and
// automatically adds tasks to create respective tar and zip artifacts.
// https://github.com/johnrengelman/shadow/issues/347#issuecomment-424726972
// https://github.com/johnrengelman/shadow/commit/a824e4f6e4618785deb7f084c4a80ce1b78fc4fd
tasks.findByName("shadowDistTar")?.enabled = false
tasks.findByName("shadowDistZip")?.enabled = false
configurations["archives"].artifacts.removeAll {
  val file: File = it.file
  file.name.contains("tar") || file.name.contains("zip")
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
