import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

plugins {
  kotlin("multiplatform")
  id("ru.vyarus.animalsniffer")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

val versionWriterTaskProvider = tasks.register("writeVersion", VersionWriterTask::class)

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
  if (kmpNativeEnabled) {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
  }
  sourceSets {
    all {
      languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
    val commonMain by getting {
      kotlin.srcDir(versionWriterTaskProvider)
      dependencies {
        api(deps.okio.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(deps.kotlin.test.common)
        implementation(deps.kotlin.test.annotations)
      }
    }
    val jvmMain by getting {
      dependencies {
        compileOnly(deps.android)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(deps.assertj)
        implementation(deps.kotlin.test.junit)
      }
    }
    if (kmpJsEnabled) {
      val jsTest by getting {
        dependencies {
          implementation(deps.kotlin.test.js)
        }
      }
    }
    if (kmpNativeEnabled) {
      val nativeMain by creating {
        dependsOn(commonMain)
      }
      val nativeTest by creating {
        dependsOn(commonTest)
      }
      val darwinMain by creating {
        dependsOn(commonMain)
      }

      val iosX64Main by getting
      val iosArm64Main by getting
      val iosSimulatorArm64Main by getting
      val linuxX64Main by getting
      val macosX64Main by getting
      val iosX64Test by getting
      val iosArm64Test by getting
      val iosSimulatorArm64Test by getting
      val linuxX64Test by getting
      val macosX64Test by getting

      for (it in listOf(iosX64Main, iosArm64Main, iosSimulatorArm64Main, linuxX64Main, macosX64Main)) {
        it.dependsOn(nativeMain)
      }

      for (it in listOf(iosX64Test, iosArm64Test, iosSimulatorArm64Test, linuxX64Test, macosX64Test)) {
        it.dependsOn(nativeTest)
      }

      for (it in listOf(iosX64Main, iosArm64Main, macosX64Main)) {
        it.dependsOn(darwinMain)
      }
    }
  }
}

afterEvaluate {
  val installLocally by tasks.creating {
    dependsOn("publishKotlinMultiplatformPublicationToTestRepository")
    dependsOn("publishJvmPublicationToTestRepository")
    if (kmpJsEnabled) {
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

val main by sourceSets.getting
configure<AnimalSnifferExtension> {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = Dokka("dokkaGfm"))
  )
}
