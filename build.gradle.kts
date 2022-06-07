import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(deps.plugins.kotlin)
    classpath(deps.plugins.shadow)
    classpath(deps.wire.gradlePlugin)
    classpath(deps.plugins.japicmp)
    classpath(deps.animalSniffer.gradle)
    classpath(deps.plugins.android)
    classpath(deps.protobuf.gradlePlugin)
    classpath(deps.plugins.spotless)
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
        "com.squareup.okhttp3" -> deps.okhttp.bom
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
      ktlint(versions.ktlint).userData(kotlin.collections.mapOf("indent_size" to "2"))
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
