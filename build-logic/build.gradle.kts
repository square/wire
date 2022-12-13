import org.jetbrains.kotlin.gradle.dsl.KotlinCompile;

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.animalSniffer.gradle)
    classpath(libs.dokka.core)
    classpath(libs.dokka.gradlePlugin)
    classpath(libs.pluginz.buildConfig)
    classpath(libs.pluginz.spotless)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    // TODO(Benoit) Use to create version file
    // buildConfig-plugin = "com.github.gmazzo:gradle-buildconfig-plugin:3.1.0"
    // classpath libs.buildConfig.plugin
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
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

  // Prefer to get dependency versions from BOMs.
  configurations.all {
    val configuration = this
    configuration.dependencies.all {
      val bom = when (group) {
        "com.squareup.okio" -> libs.okio.bom.get()
        "com.squareup.okhttp3" -> libs.okhttp.bom.get()
        else -> return@all
      }
      configuration.dependencies.add(project.dependencies.platform(bom))
    }
  }
}
