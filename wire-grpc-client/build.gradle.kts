import com.vanniktech.maven.publish.JavadocJar.Javadoc
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

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
        api(projects.wireRuntime)
        api(libs.okio.core)
        api(libs.kotlin.coroutines.core)
      }
    }
    val jvmMain by getting {
      dependencies {
        api(libs.okhttp.core)
      }
    }
    if (System.getProperty("knative", "true").toBoolean()) {
      val nativeMain by creating {
        dependsOn(commonMain)
      }
      val androidNativeArm32Main by getting
      val androidNativeArm64Main by getting
      val androidNativeX64Main by getting
      val androidNativeX86Main by getting
      val iosArm64Main by getting
      val iosSimulatorArm64Main by getting
      val iosX64Main by getting
      val linuxArm64Main by getting
      val linuxX64Main by getting
      val macosArm64Main by getting
      val macosX64Main by getting
      val mingwX64Main by getting
      val tvosArm64Main by getting
      val tvosSimulatorArm64Main by getting
      val tvosX64Main by getting
      val wasmMain by getting
      val watchosArm32Main by getting
      val watchosArm64Main by getting
      val watchosDeviceArm64Main by getting
      val watchosSimulatorArm64Main by getting
      val watchosX64Main by getting
      for (it in listOf(
        androidNativeArm32Main,
        androidNativeArm64Main,
        androidNativeX64Main,
        androidNativeX86Main,
        iosArm64Main,
        iosSimulatorArm64Main,
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

if (project.rootProject.name == "wire") {
  configure<MavenPublishBaseExtension> {
    configure(
      KotlinMultiplatform(javadocJar = Javadoc())
    )
  }
}
