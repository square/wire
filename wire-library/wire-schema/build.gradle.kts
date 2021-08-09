plugins {
  kotlin("multiplatform")
  id("internal-publishing")
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

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-schema")
  }
}

configure<PublishingExtension> {
  // Use default artifact name for the JVM target
  publications {
    val kotlinMultiplatform by getting(MavenPublication::class) {
      artifactId = "wire-schema-multiplatform"
    }
    val jvm by getting(MavenPublication::class) {
      artifactId = "wire-schema"
    }
  }
}
