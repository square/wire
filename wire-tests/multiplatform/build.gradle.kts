import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.squareup.wire")
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {}
  if (System.getProperty("kjs", "true").toBoolean()) {
    js(IR) {
      configure(listOf(compilations.getByName("main"), compilations.getByName("test"))) {
        tasks.named<Kotlin2JsCompile>(compileKotlinTaskName).configure {
          compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_UMD)
            sourceMap.set(true)
          }
        }
      }
      nodejs()
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
        api(projects.wireRuntime)
        api(projects.wireGrpcClient)
      }
    }
    val commonTest by getting {
      kotlin.srcDir("src/commonTest/proto-kotlin")
      dependencies {
        implementation(libs.assertk)
        implementation(libs.kotlin.test.annotations)
      }
    }
    val jvmTest by getting {
      kotlin.srcDir("src/jvmTest/proto-kotlin")
      kotlin.srcDir("src/jvmTest/proto-java")
      dependencies {
        implementation(projects.wireTestUtils)
        implementation(libs.assertk)
        implementation(libs.jimfs)
        implementation(libs.kotlin.test.junit)
        implementation(libs.truth)
      }
    }
    if (System.getProperty("kjs", "true").toBoolean()) {
      val jsTest by getting {
        dependencies {
          implementation(libs.assertk)
          implementation(libs.kotlin.test.js)
        }
      }
    }
  }
}

wire {
  sourcePath {
    srcDir("src/commonTest/proto3/kotlin")
  }
  sourcePath {
    srcJar("src/commonTest/proto/kotlin/protos.jar")
    include(
      "squareup/geology/period.proto",
      "squareup/dinosaurs/dinosaur.proto",
    )
  }
  sourcePath {
    srcDir("src/commonTest/proto/kotlin")
    include(
      "all_types.proto",
      "bool.proto",
      "boxed_oneofs.proto",
      "custom_options.proto",
      "deprecated.proto",
      "deprecated_enum.proto",
      "edge_cases.proto",
      "external_message.proto",
      "foreign.proto",
      "form.proto",
      "map.proto",
      "negative_value_enum.proto",
      "no_fields.proto",
      "one_of.proto",
      "optional_enum.proto",
      "option_redacted.proto",
      "packed_encoding.proto",
      "person.proto",
      "redacted_one_of.proto",
      "redacted_test.proto",
      "same_name_enum.proto",
      "simple_message.proto",
      "to_string.proto",
      "unknown_fields.proto",
      "uses_any.proto",
      "redacted_test_builders_only.proto",
      "simple_message_builders_only.proto",
      )
  }
  kotlin {
    buildersOnly = true
    exclusive = true
    includes = listOf(
      "squareup.protos.kotlin.simple.buildersonly.SimpleMessageBuildersOnly",
      "squareup.protos.kotlin.redacted_test.buildersonly.*",
      )
  }
  kotlin {
    boxOneOfsMinSize = 8
    exclusive = false
    excludes = listOf(
      "squareup.protos.kotlin.simple.buildersonly.SimpleMessageBuildersOnly",
      "squareup.protos.kotlin.redacted_test.buildersonly.*",
    )
  }
}
