import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  kotlin("multiplatform")
  id("com.github.gmazzo.buildconfig")
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {
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
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
  }

  if (System.getProperty("kwasm", "true").toBoolean()) {
    wasmJs {
      browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
      nodejs()
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.okio.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.assertk)
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
        implementation(libs.kotlin.test.junit)
      }
    }
    val nonJvmMain by creating {
      dependsOn(commonMain)
    }
    val nonJvmTest by creating {
      dependsOn(commonTest)
    }
    if (System.getProperty("knative", "true").toBoolean()) {
      val nativeMain by getting {
        dependsOn(nonJvmMain)
      }
      val nativeTest by getting {
        dependsOn(nonJvmTest)
      }
    }
    if (System.getProperty("kjs", "true").toBoolean()) {
      val jsMain by getting {
        dependsOn(nonJvmMain)
      }
      val jsTest by getting {
        dependsOn(nonJvmTest)
        dependencies {
          implementation(libs.kotlin.test.js)
        }
      }
    }
    if (System.getProperty("kwasm", "true").toBoolean()) {
      val wasmJsMain by getting {
        dependsOn(nonJvmMain)
      }
      val wasmJsTest by getting {
        dependsOn(nonJvmTest)
        dependencies {
          implementation(libs.kotlin.test.wasm.js)
        }
      }
      val wasmWasiMain by getting {
        dependsOn(nonJvmMain)
      }
      val wasmWasiTest by getting {
        dependsOn(nonJvmTest)
        dependencies {
          implementation(libs.kotlin.test)
        }
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

buildConfig {
  useKotlinOutput {
    internalVisibility = true
    topLevelConstants = true
  }

  packageName("com.squareup.wire")
  buildConfigField("String", "wireVersion", "\"${project.version}\"")
  // We keep it so as to not break consumers.
  buildConfigField("String", "VERSION", "\"${project.version}\"")
}

if (project.rootProject.name == "wire") {
  configure<SpotlessExtension> {
    kotlin {
      targetExclude(
        // Google license for Protobuf.
        "src/commonMain/kotlin/com/squareup/wire/ByteArrayProtoReader32.kt",
        "src/commonMain/kotlin/com/squareup/wire/ProtoReader.kt",
        "src/commonMain/kotlin/com/squareup/wire/ProtoReader32.kt",
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
