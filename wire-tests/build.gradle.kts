import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {
    // FIXME(egor): withJava() has to be declared after all custom compilations.
    // See https://youtrack.jetbrains.com/issue/KT-41506.
    withJava()
  }
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

wireBuild {
  createKotlinJvmTestTask("jvmKotlinInteropTest") {
    kotlin.srcDir("src/jvmKotlinInteropTest/proto-kotlin")
    dependencies {
      implementation(projects.wireRuntime)
      implementation(libs.kotlin.test.junit)
      implementation(libs.assertk)
      implementation(libs.kotlin.reflect)
      implementation(projects.wireGrpcClient)
    }
  }

  createKotlinJvmTestTask("jvmKotlinAndroidTest") {
    kotlin.srcDir("src/jvmKotlinAndroidTest/proto-kotlin")
    dependencies {
      implementation(projects.wireRuntime)
      implementation(libs.kotlin.test.junit)
      implementation(libs.assertk)
      compileOnly(libs.android)
    }
  }

  createKotlinJvmTestTask("jvmProtoReader32Test") {
    kotlin.srcDir("src/jvmKotlinProtoReader32Test/proto-kotlin")
    dependencies {
      implementation(projects.wireRuntime)
      implementation(libs.kotlin.test.junit)
      implementation(libs.assertk)
      implementation(libs.kotlin.reflect)
    }
  }

  createJavaTestTask("jvmJavaTest") {
    sourceSet.java.srcDir("src/jvmJavaTest/proto-java")
    sourceSet.java.srcDir("src/jvmJavaTest/proto-kotlin")
    implementation(libs.assertk)
    implementation(libs.jimfs)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.test.junit)
    implementation(libs.moshi)
    implementation(libs.moshiKotlin)
    implementation(libs.truth)
    implementation(projects.wireMoshiAdapter)
    implementation(projects.wireRuntime)
  }

  createJavaTestTask("jvmJavaNoOptionsTest") {
    sourceSet.java.srcDir("src/jvmJavaNoOptionsTest/proto-java")
    implementation(projects.wireRuntime)
  }

  createJavaTestTask("jvmJavaCompactTest") {
    sourceSet.java.srcDir("src/jvmJavaCompactTest/proto-java")
    implementation(projects.wireRuntime)
  }

  createJavaTestTask("jvmJavaPrunedTest") {
    sourceSet.java.srcDir("src/jvmJavaPrunedTest/proto-java")
    implementation(projects.wireRuntime)
  }

  createJavaTestTask("jvmJavaAndroidTest") {
    sourceSet.java.srcDir("src/jvmJavaAndroidTest/proto-java")
    implementation(projects.wireRuntime)
    compileOnly(libs.android)
    compileOnly(libs.androidx.annotations)
  }

  createJavaTestTask("jvmJavaAndroidCompactTest") {
    sourceSet.java.srcDir("src/jvmJavaAndroidCompactTest/proto-java")
    implementation(projects.wireRuntime)
    compileOnly(libs.android)
    compileOnly(libs.androidx.annotations)
  }

  createJavaTestTask("jvmJsonJavaTest") {
    sourceSet.java.srcDir("src/jvmJsonTest/kotlin")
    sourceSet.java.srcDir("src/jvmJsonJavaTest/proto-java")
    implementation(libs.assertk)
    implementation(libs.jimfs)
    implementation(libs.kotlin.test.junit)
    implementation(libs.truth)
    implementation(projects.wireGsonSupport)
    implementation(projects.wireMoshiAdapter)
    implementation(projects.wireRuntime)
    implementation(projects.wireTestUtils)
  }

  createJavaTestTask("jvmJsonKotlinTest") {
    sourceSet.java.srcDir("src/jvmJsonTest/kotlin")
    sourceSet.java.srcDir("src/jvmJsonKotlinTest/proto-kotlin")
    implementation(libs.assertk)
    implementation(libs.jimfs)
    implementation(libs.kotlin.test.junit)
    implementation(libs.truth)
    implementation(projects.wireGsonSupport)
    implementation(projects.wireMoshiAdapter)
    implementation(projects.wireRuntime)
    implementation(projects.wireTestUtils)
  }
}

configure<SpotlessExtension> {
  kotlin {
    targetExclude(
      "src/commonTest/proto-java/**/*.kt",
      "src/commonTest/proto-kotlin/**/*.kt",
      "src/jvmJavaAndroidCompactTest/proto-java/**/*.kt",
      "src/jvmJavaAndroidTest/proto-java/**/*.kt",
      "src/jvmJavaCompactTest/proto-java/**/*.kt",
      "src/jvmJavaNoOptionsTest/proto-java/**/*.kt",
      "src/jvmJavaPrunedTest/proto-java/**/*.kt",
      "src/jvmJavaTest/proto-java/**/*.kt",
      "src/jvmKotlinAndroidTest/proto-kotlin/**/*.kt",
      "src/jvmKotlinInteropTest/proto-kotlin/**/*.kt",
      "src/jvmKotlinProtoReader32Test/proto-kotlin/**/*.kt",
      "src/jvmJsonJavaTest/proto-java/**/*.kt",
      "src/jvmJsonKotlinTest/proto-kotlin/**/*.kt",
    )
  }
  java {
    targetExclude(
      "src/jvmJavaTest/proto-java/**/*.java",
      "src/jvmJavaNoOptionsTest/proto-java/**/*.java",
      "src/jvmJavaCompactTest/proto-java/**/*.java",
      "src/jvmJavaPrunedTest/proto-java/**/*.java",
      "src/jvmJavaAndroidTest/proto-java/**/*.java",
      "src/jvmJavaAndroidCompactTest/proto-java/**/*.java",
      "src/jvmJavaTest/proto-java/**/*.java",
      "src/jvmKotlinInteropTest/proto-kotlin/**/*.java",
      "src/jvmKotlinProtoReader32Test/proto-kotlin/**/*.java",
      "src/jvmJsonJavaTest/proto-java/**/*.java",
    )
  }
}
