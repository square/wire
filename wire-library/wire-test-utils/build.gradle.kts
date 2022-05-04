import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("multiplatform")
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
        api(deps.okio.core)
        api(project(":wire-runtime"))
        api(project(":wire-schema"))
        api(project(":wire-schema-tests"))
      }
    }
    val jvmMain by getting {
      dependencies {
        api(deps.moshi)
        api(deps.protobuf.java)
        api(deps.assertj)
      }
    }
  }
}
