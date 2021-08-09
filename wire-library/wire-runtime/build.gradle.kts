plugins {
  kotlin("multiplatform")
  id("java-library")
  id("ru.vyarus.animalsniffer")
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
  if (kmpNativeEnabled) {
    iosX64()
    iosArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
  }
  sourceSets {
    all {
      languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
    val commonMain by getting {
      dependencies {
        api(deps.okio.multiplatform)
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
      val linuxX64Main by getting
      val macosX64Main by getting
      val iosX64Test by getting
      val iosArm64Test by getting
      val linuxX64Test by getting
      val macosX64Test by getting

      for (it in listOf(iosX64Main, iosArm64Main, linuxX64Main, macosX64Main)) {
        it.dependsOn(nativeMain)
      }

      for (it in listOf(iosX64Test, iosArm64Test, linuxX64Test, macosX64Test)) {
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

configure<PublishingExtension> {
  // Use default artifact name for the JVM target
  publications {
    val kotlinMultiplatform by getting(MavenPublication::class) {
      artifactId = "wire-runtime-multiplatform"
    }
    val jvm by getting(MavenPublication::class) {
      artifactId = "wire-runtime"
    }
  }
}

for (target in kotlin.targets.matching { it.platformType.name == "jvm" }) {
  val jar by tasks.getting(Jar::class) {
    manifest {
      attributes("Automatic-Module-Name" to "wire-runtime")
    }
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}
