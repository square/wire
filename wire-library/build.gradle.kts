import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(libs.pluginz.kotlinSerialization)
    classpath(libs.pluginz.shadow)
    classpath(libs.pluginz.japicmp)
    classpath(libs.protobuf.gradlePlugin)
    classpath(libs.animalSniffer.gradle)
    // https://github.com/melix/japicmp-gradle-plugin/issues/36
    classpath("com.google.guava:guava:28.2-jre")
    classpath(libs.vanniktechPublishPlugin)
    classpath(libs.dokka.gradlePlugin)
    classpath(libs.dokka.core)
    classpath(libs.pluginz.spotless)
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

apply(plugin = "com.vanniktech.maven.publish.base")

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    mavenCentral()
    google()
  }
}

rootProject.plugins.withType<NodeJsRootPlugin> {
  // 16+ required for Apple Silicon support
  // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
  rootProject.the<NodeJsRootExtension>().nodeVersion = "18.0.0"
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
      toggleOffOn()
    }
  }

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

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8"
      // Disable optimized callable references. See https://youtrack.jetbrains.com/issue/KT-37435
      freeCompilerArgs += "-Xno-optimized-callable-references"
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    options.encoding = Charsets.UTF_8.toString()
  }

  tasks.withType<Test> {
    testLogging {
      events(STARTED, PASSED, SKIPPED, FAILED)
      exceptionFormat = TestExceptionFormat.FULL
      showStandardStreams = false
    }
  }

  if (!(project.name.endsWith("-swift") || project.name.endsWith("-bom"))) {
    apply(plugin = "checkstyle")

    afterEvaluate {
      configure<CheckstyleExtension> {
        toolVersion = "7.7"
        sourceSets = listOf(project.extensions.getByType<SourceSetContainer>()["main"])
      }
    }
  }
}

allprojects {
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

  tasks.withType<Jar>().configureEach {
    if (name == "jar") {
      manifest {
        attributes("Automatic-Module-Name" to project.name)
      }
    }
  }

  tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
      reportUndocumented.set(false)
      skipDeprecated.set(true)
      jdkVersion.set(8)
      perPackageOption {
        matchingRegex.set("com\\.squareup\\.wire\\.internal.*")
        suppress.set(true)
      }
    }
    if (name == "dokkaGfm") {
      outputDirectory.set(project.file("${project.rootDir}/docs/3.x"))
    }
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
      repositories {
        maven {
          name = "test"
          setUrl("file://${project.rootProject.buildDir}/localMaven")
        }
        /**
         * Want to push to an internal repository for testing?
         * Set the following properties in ~/.gradle/gradle.properties.
         *
         * internalUrl=YOUR_INTERNAL_URL
         * internalUsername=YOUR_USERNAME
         * internalPassword=YOUR_PASSWORD
         */
        val internalUrl = providers.gradleProperty("internalUrl")
        if (internalUrl.isPresent) {
          maven {
            name = "internal"
            setUrl(internalUrl)
            credentials(PasswordCredentials::class)
          }
        }
      }
    }

    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.S01)
      val inMemoryKey = project.findProperty("signingInMemoryKey") as String?
      if (!inMemoryKey.isNullOrEmpty()) {
        signAllPublications()
      }
      pom {
        description.set("gRPC and protocol buffers for Android, Kotlin, and Java.")
        name.set(project.name)
        url.set("https://github.com/square/wire/")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
        developers {
          developer {
            id.set("square")
            name.set("Square, Inc.")
          }
        }
        scm {
          url.set("https://github.com/square/wire/")
          connection.set("scm:git:https://github.com/square/wire.git")
          developerConnection.set("scm:git:ssh://git@github.com/square/wire.git")
        }
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
