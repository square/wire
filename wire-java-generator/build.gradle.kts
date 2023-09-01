import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
  id("java-library")
  kotlin("jvm")
  // TODO(Benoit)  Re-enable dokka when it works again. Probably related to https://github.com/Kotlin/dokka/issues/2977
  // id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(libs.okio.core)
  implementation(libs.guava){
    attributes {
      // We help Gradle pick between the jre and android of Guava.
      attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM))
    }
  }
  api(libs.javapoet)
  compileOnly(libs.jsr305)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.jimfs)
}

if (project.rootProject.name == "wire") {
  configure<MavenPublishBaseExtension> {
    configure(
      KotlinJvm(javadocJar = Javadoc(), sourcesJar = true)
    )
  }
}
