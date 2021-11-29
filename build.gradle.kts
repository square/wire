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
    classpath(deps.plugins.jmh)
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
    jcenter()
  }
}

subprojects {
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
