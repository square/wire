import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE

plugins {
  kotlin("multiplatform")
  // TODO(Benoit)  Re-enable dokka when it works again. Probably related to https://github.com/Kotlin/dokka/issues/2977
  // id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

kotlin {
  jvm {
    withJava()
  }
  if (System.getProperty("kjs", "true").toBoolean()) {
    js(IR) {
      configure(listOf(compilations.getByName("main"), compilations.getByName("test"))) {
        tasks.getByName(compileKotlinTaskName) {
          kotlinOptions {
            moduleKind = "umd"
            sourceMap = true
            metaInfo = true
          }
        }
      }
      nodejs()
      browser()
    }
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.wireRuntime)
      }
    }
    val commonTest by getting {
    }
    val jvmMain by getting {
      dependencies {
        api(libs.okio.core)
        api(libs.guava.get().toString()) {
          attributes {
            // We help Gradle pick between the jre and android of Guava.
            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM))
          }
        }
        api(libs.javapoet)
        api(libs.kotlinpoet)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(projects.wireSchemaTests)
        implementation(projects.wireTestUtils)
        implementation(libs.assertj)
        implementation(libs.jimfs)
        implementation(libs.junit)
        implementation(libs.kotlin.test.junit)
        implementation(libs.protobuf.java)
        implementation(libs.okio.fakefilesystem)
      }
    }
    if (System.getProperty("kjs", "true").toBoolean()) {
      val jsTest by getting {
        dependencies {
          implementation(libs.kotlin.test.js)
        }
      }
    }
  }
}

if (project.rootProject.name == "wire") {
  configure<MavenPublishBaseExtension> {
    configure(
      KotlinMultiplatform(javadocJar = Javadoc())
    )
  }

  configure<SpotlessExtension> {
    kotlin {
      targetExclude(
        // Apache 2-licensed file from Apache.
        "src/jvmTest/kotlin/com/squareup/wire/schema/MavenVersionsTest.kt",
      )
    }
  }
}
