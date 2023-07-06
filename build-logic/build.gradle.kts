import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.dokka.core)
    classpath(libs.dokka.gradlePlugin)
    classpath(libs.pluginz.buildConfig)
    classpath(libs.pluginz.spotless)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    classpath(libs.pluginz.buildConfig)
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
  rootProject.extensions.getByType(NodeJsRootExtension::class).nodeVersion = "16.15.1"
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }

  plugins.withId("java") {
    configure<JavaPluginExtension> {
      withSourcesJar()
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs += "-Xjvm-default=all"
    }
  }
}
