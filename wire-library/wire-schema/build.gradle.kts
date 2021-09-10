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
        implementation(deps.okio.jvm)
        api(deps.guava)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(project(":wire-test-utils"))
        implementation(deps.assertj)
        implementation(deps.jimfs)
        implementation(deps.junit)
        implementation(deps.kotlin.test.junit)
        implementation(deps.protobuf.java)
      }
    }
    if (kmpJsEnabled) {
      val jsTest by getting {
        dependencies {
          implementation(deps.kotlin.test.js)
        }
      }
    }
  }
}

// https://github.com/vanniktech/gradle-maven-publish-plugin/issues/301
val metadataJar by tasks.getting(Jar::class)
configure<PublishingExtension> {
  publications.withType<MavenPublication>().named("kotlinMultiplatform").configure {
    artifact(metadataJar)
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = Dokka("dokkaGfm"))
  )
}
