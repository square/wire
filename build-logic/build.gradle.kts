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

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

repositories {
  mavenCentral()
}

dependencies {
  add("compileOnly", kotlin("gradle-plugin"))
  add("compileOnly", kotlin("gradle-plugin-api"))
  implementation(libs.pluginz.kotlin)
}

gradlePlugin {
  plugins {
    create("com.squareup.wire.build.logic") {
      id = "com.squareup.wire.build.logic"
      implementationClass = "BuildLogic"
    }
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
      jvmTarget = "11"
      // Disable optimized callable references. See https://youtrack.jetbrains.com/issue/KT-37435
      freeCompilerArgs += "-Xno-optimized-callable-references"
      freeCompilerArgs += "-Xjvm-default=all"
      // https://kotlinlang.org/docs/whatsnew13.html#progressive-mode
      freeCompilerArgs += "-progressive"
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
    options.encoding = Charsets.UTF_8.toString()
  }
}
