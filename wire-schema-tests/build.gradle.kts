plugins {
  kotlin("multiplatform")
}

kotlin {
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
    if (System.getProperty("knative", "true").toBoolean()) {
      val nativeMain by creating {
        dependsOn(commonMain)
      }
      val nativeTest by creating {
        dependsOn(commonTest)
      }

      val iosX64Main by getting
      val iosArm64Main by getting
      val iosSimulatorArm64Main by getting
      val linuxX64Main by getting
      val macosX64Main by getting
      val macosArm64Main by getting
      val mingwX64Main by getting
      val tvosX64Main by getting
      val tvosArm64Main by getting
      val tvosSimulatorArm64Main by getting
      val iosX64Test by getting
      val iosArm64Test by getting
      val iosSimulatorArm64Test by getting
      val linuxX64Test by getting
      val macosX64Test by getting
      val macosArm64Test by getting
      val mingwX64Test by getting
      val tvosX64Test by getting
      val tvosArm64Test by getting
      val tvosSimulatorArm64Test by getting

      for (it in listOf(iosX64Main, iosArm64Main, iosSimulatorArm64Main, linuxX64Main, macosX64Main, macosArm64Main, mingwX64Main, tvosX64Main, tvosArm64Main, tvosSimulatorArm64Main)) {
        it.dependsOn(nativeMain)
      }

      for (it in listOf(iosX64Test, iosArm64Test, iosSimulatorArm64Test, linuxX64Test, macosX64Test, macosArm64Test, mingwX64Test, tvosX64Test, tvosArm64Test, tvosSimulatorArm64Test)) {
        it.dependsOn(nativeTest)
      }
    }
  }
}
