import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.tasks.JavadocJar

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
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
    val jvmMain by getting {
      dependencies {
        api(libs.okio.core)
        api(libs.guava)
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
      KotlinMultiplatform(javadocJar = Dokka("dokkaGfm"))
    )
  }
}

// Workaround for https://github.com/Kotlin/dokka/issues/1833
tasks.withType<JavadocJar>().configureEach {
  dependsOn(project.tasks.getByPath(":wire-runtime:dokkaGfm"))
}
