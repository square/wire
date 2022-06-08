import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

kotlin {
  jvm {
    withJava()
  }
  if (kmpJsEnabled) {
    js {
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
      // TODO(jwilson): fix Okio for JS to support browser() by polyfilling OS.
      // browser()
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":wire-runtime"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(libs.okio.core)
        api(libs.guava)
        api(libs.javapoet)
        api(libs.kotlinpoet)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(project(":wire-schema-tests"))
        implementation(project(":wire-test-utils"))
        implementation(libs.assertj)
        implementation(libs.jimfs)
        implementation(libs.junit)
        implementation(libs.kotlin.test.junit)
        implementation(libs.protobuf.java)
        implementation(libs.okio.fakefilesystem)
      }
    }
    if (kmpJsEnabled) {
      val jsTest by getting {
        dependencies {
          implementation(libs.kotlin.test.js)
        }
      }
    }
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = Dokka("dokkaGfm"))
  )
}
