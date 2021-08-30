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
    val commonMain by getting {
      dependencies {
        api(project(":wire-runtime"))
        api(deps.okio.multiplatform)
        api(deps.kotlin.coroutines.core)
      }
    }
    val jvmMain by getting {
      dependencies {
        api(deps.okhttp)
      }
    }
    if (kmpNativeEnabled) {
      val nativeMain by creating {
        dependsOn(commonMain)
      }
      val iosX64Main by getting
      val iosArm64Main by getting
      val linuxX64Main by getting
      val macosX64Main by getting
      for (it in listOf(iosX64Main, iosArm64Main, linuxX64Main, macosX64Main)) {
        it.dependsOn(nativeMain)
      }
    }
  }
  targets.all {
    compilations.all {
      kotlinOptions {
        freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
        freeCompilerArgs += "-Xexperimental=com.squareup.wire.WireGrpcExperimental"
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

for (target in kotlin.targets.matching { it.platformType.name == "jvm" }) {
  val jar by tasks.getting(Jar::class) {
    manifest {
      attributes("Automatic-Module-Name" to "wire-grpc-client")
    }
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}

