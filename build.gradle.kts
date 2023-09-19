import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

buildscript {
  dependencies {
    classpath(libs.pluginz.dokka)
    classpath(libs.pluginz.android)
    classpath(libs.pluginz.binaryCompatibilityValidator)
    classpath(libs.pluginz.kotlin)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    classpath(libs.pluginz.spotless)
    classpath(libs.protobuf.gradlePlugin)
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.pluginz.buildConfig)
    classpath(libs.wire.gradlePlugin)
    classpath(libs.wire.buildGradlePlugin)
    classpath(libs.guava)
    classpath("org.ow2.asm:asm:9.6")
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

rootProject.plugins.withType(NodeJsRootPlugin::class) {
  // 16+ required for Apple Silicon support
  // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
  rootProject.extensions.getByType(NodeJsRootExtension::class).nodeVersion = "16.13.1"
}

apply(plugin = "com.vanniktech.maven.publish.base")
apply(plugin = "org.jetbrains.dokka")

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    mavenCentral()
    google()
  }
}

subprojects {
  // The `application` plugin internally applies the `distribution` plugin and
  // automatically adds tasks to create/publish tar and zip artifacts.
  // https://docs.gradle.org/current/userguide/application_plugin.html
  // https://docs.gradle.org/current/userguide/distribution_plugin.html#sec:publishing_distributions_upload
  plugins.withType(DistributionPlugin::class) {
    tasks.findByName("distTar")?.enabled = false
    tasks.findByName("distZip")?.enabled = false
    configurations["archives"].artifacts.removeAll {
      val file: File = it.file
      file.name.contains("tar") || file.name.contains("zip")
    }
  }
}

allprojects {
  tasks.withType<Jar>().configureEach {
    if (name == "jar") {
      manifest {
        attributes("Automatic-Module-Name" to project.name)
      }
    }
  }
}

tasks.register("publishPluginToGradlePortalIfRelease") {
  val VERSION_NAME: String by project
  // Snapshots cannot be released to the Gradle portal. And we don't want to release internal square
  // builds.
  if (VERSION_NAME.endsWith("-SNAPSHOT") || VERSION_NAME.contains("square")) return@register

  dependsOn(":wire-gradle-plugin:publishPlugins")
}

apply(from = "gen-tests.gradle.kts")
