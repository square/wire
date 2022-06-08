import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(libs.pluginz.shadow)
    classpath(libs.wire.gradlePlugin)
    classpath(libs.pluginz.japicmp)
    classpath(libs.animalSniffer.gradle)
    classpath(libs.pluginz.android)
    classpath(libs.protobuf.gradlePlugin)
    classpath(libs.pluginz.spotless)
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

subprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    setEnforceCheck(false)
    kotlin {
      target("**/*.kt")
      ktlint(libs.versions.ktlint.get()).userData(kotlin.collections.mapOf("indent_size" to "2"))
      trimTrailingWhitespace()
      endWithNewline()
    }
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    options.encoding = Charsets.UTF_8.toString()
  }
}

apply(from = "gen-tests.gradle.kts")
