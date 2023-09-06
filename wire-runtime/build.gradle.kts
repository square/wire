import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  kotlin("multiplatform")
  id("com.github.gmazzo.buildconfig")
  // TODO(Benoit)  Re-enable dokka when it works again. Probably related to https://github.com/Kotlin/dokka/issues/2977
  // id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base").apply(false)
}

if (project.rootProject.name == "wire") {
  apply(plugin = "com.vanniktech.maven.publish.base")
  apply(plugin = "binary-compatibility-validator")
}

kotlin {
  jvm().withJava()
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
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    iosArm64()
    iosSimulatorArm64()
    iosSimulatorArm64()
    iosX64()
    linuxArm64()
    linuxX64() // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    macosArm64()
    macosX64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    wasm().nodejs()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.okio.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations)
      }
    }
    val jvmMain by getting {
      dependencies {
        compileOnly(libs.android)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(libs.assertj)
        implementation(libs.kotlin.test.junit)
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
      val darwinMain by creating {
        dependsOn(commonMain)
      }

      val androidNativeArm32Main by getting
      val androidNativeArm32Test by getting
      val androidNativeArm64Main by getting
      val androidNativeArm64Test by getting
      val androidNativeX64Main by getting
      val androidNativeX64Test by getting
      val androidNativeX86Main by getting
      val androidNativeX86Test by getting
      val iosArm64Main by getting
      val iosArm64Test by getting
      val iosSimulatorArm64Main by getting
      val iosSimulatorArm64Test by getting
      val iosX64Main by getting
      val iosX64Test by getting
      val linuxArm64Main by getting
      val linuxArm64Test by getting
      val linuxX64Main by getting
      val linuxX64Test by getting
      val macosArm64Main by getting
      val macosArm64Test by getting
      val macosX64Main by getting
      val macosX64Test by getting
      val mingwX64Main by getting
      val mingwX64Test by getting
      val tvosArm64Main by getting
      val tvosArm64Test by getting
      val tvosSimulatorArm64Main by getting
      val tvosSimulatorArm64Test by getting
      val tvosX64Main by getting
      val tvosX64Test by getting
      val wasmMain by getting
      val wasmTest by getting
      val watchosArm32Main by getting
      val watchosArm32Test by getting
      val watchosArm64Main by getting
      val watchosArm64Test by getting
      val watchosDeviceArm64Main by getting
      val watchosDeviceArm64Test by getting
      val watchosSimulatorArm64Main by getting
      val watchosSimulatorArm64Test by getting
      val watchosX64Main by getting
      val watchosX64Test by getting

      for (it in listOf(
        androidNativeArm32Main,
        androidNativeArm64Main,
        androidNativeX64Main,
        androidNativeX86Main,
        iosArm64Main,
        iosSimulatorArm64Main,
        iosX64Main,
        linuxArm64Main,
        linuxX64Main,
        macosArm64Main,
        macosX64Main,
        mingwX64Main,
        tvosArm64Main,
        tvosSimulatorArm64Main,
        tvosX64Main,
        wasmMain,
        watchosArm32Main,
        watchosArm64Main,
        watchosDeviceArm64Main,
        watchosSimulatorArm64Main,
        watchosX64Main,
      )) {
        it.dependsOn(nativeMain)
      }

      for (it in listOf(
        androidNativeArm32Test,
        androidNativeArm64Test,
        androidNativeX64Test,
        androidNativeX86Test,
        iosArm64Test,
        iosSimulatorArm64Test,
        iosX64Test,
        linuxArm64Test,
        linuxX64Test,
        macosArm64Test,
        macosX64Test,
        mingwX64Test,
        tvosArm64Test,
        tvosSimulatorArm64Test,
        tvosX64Test,
        wasmTest,
        watchosArm32Test,
        watchosArm64Test,
        watchosDeviceArm64Test,
        watchosSimulatorArm64Test,
        watchosX64Test,
      )) {
        it.dependsOn(nativeTest)
      }

      for (it in listOf(
        androidNativeArm32Main,
        androidNativeArm64Main,
        androidNativeX64Main,
        androidNativeX86Main,
        iosArm64Main,
        iosSimulatorArm64Main,
        iosX64Main,
        linuxArm64Main,
        linuxX64Main,
        macosArm64Main,
        macosX64Main,
        mingwX64Main,
        tvosArm64Main,
        tvosSimulatorArm64Main,
        tvosX64Main,
        wasmMain,
        watchosArm32Main,
        watchosArm64Main,
        watchosDeviceArm64Main,
        watchosSimulatorArm64Main,
        watchosX64Main,
        )) {
        it.dependsOn(darwinMain)
      }
    }
  }
}

afterEvaluate {
  val installLocally by tasks.creating {
    dependsOn("publishKotlinMultiplatformPublicationToTestRepository")
    dependsOn("publishJvmPublicationToTestRepository")
    if (System.getProperty("kjs", "true").toBoolean()) {
      dependsOn("publishJsPublicationToTestRepository")
    }
  }
}

// TODO(egorand): Remove when https://github.com/srs/gradle-node-plugin/issues/301 is fixed
repositories.whenObjectAdded {
  if (this is IvyArtifactRepository) {
    metadataSources {
      artifact()
    }
  }
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
    topLevelConstants = true
  }

  packageName("com.squareup.wire")
  buildConfigField("String", "VERSION", "\"${project.version}\"")
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
        // Google license for Protobuf.
        "src/commonMain/kotlin/com/squareup/wire/ProtoReader.kt",
        // Google license for R8.
        "src/commonMain/kotlin/com/squareup/wire/internal/MathMethods.kt",
        // Apache 2-licensed files from Jetbrains.
        "src/commonMain/kotlin/com/squareup/wire/internal/DoubleArrayList.kt",
        "src/commonMain/kotlin/com/squareup/wire/internal/FloatArrayList.kt",
        "src/commonMain/kotlin/com/squareup/wire/internal/IntArrayList.kt",
        "src/commonMain/kotlin/com/squareup/wire/internal/LongArrayList.kt",
      )
    }
  }
}
