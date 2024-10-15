plugins {
  kotlin("multiplatform")
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {
    // Required by MavenPublishBaseExtension even though we do not have Java sources.
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
  if (System.getProperty("knative", "true").toBoolean()) {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.okio.core)
        api(projects.wireSchema)
        implementation(libs.okio.fakefilesystem)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations)
        implementation(libs.kotlin.test)
        implementation(projects.wireTestUtils)
        implementation(libs.assertk)
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
