import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(deps.plugins.kotlin)
    classpath(deps.plugins.kotlinSerialization)
    classpath(deps.plugins.shadow)
    classpath(deps.plugins.japicmp)
    classpath(deps.plugins.mavenPublish)
    classpath(deps.animalSniffer.gradle)
    // https://github.com/melix/japicmp-gradle-plugin/issues/36
    classpath("com.google.guava:guava:28.2-jre")
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    mavenCentral()
    google()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://kotlin.bintray.com/kotlinx/")
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

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8"
      // Disable optimized callable references. See https://youtrack.jetbrains.com/issue/KT-37435
      freeCompilerArgs += "-Xno-optimized-callable-references"
      freeCompilerArgs += "-Xuse-experimental=okio.ExperimentalFileSystem"
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
  }

  if (!project.name.endsWith("-swift")) {
    // TODO this should use plugins.withId first rather than names
    if (project.name != "wire-runtime" &&
        project.name != "wire-kotlin-serialization" &&
        project.name != "wire-reflector" &&
        project.name != "wire-schema" &&
        project.name != "sample" &&
        project.name != "wire-codegen-sample" &&
        project.name != "japicmp" &&
        project.name != "wire-tests" &&
        project.name != "wire-grpc-client") {
      apply(plugin = "com.vanniktech.maven.publish")
    } else {
      tasks.create("uploadArchives") {
        // TODO(egorand): Find out why this became a problem.
        // No-op task to prevent Gradle from calling a real uploadArchives task which is added by an
        // unknown plugin (or Gradle itself) and fails for multiplatform projects.
      }
    }
    apply(plugin = "checkstyle")

    afterEvaluate {
      configure<CheckstyleExtension> {
        toolVersion = "7.7"
        sourceSets = listOf(project.extensions.getByType<SourceSetContainer>()["main"])
      }
    }
  }
}
