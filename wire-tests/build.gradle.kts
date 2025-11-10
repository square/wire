import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.kotlin.dsl.sourceSets
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
  applyDefaultHierarchyTemplate()
  registerJavaTest("javaTest") {
    java.srcDir("src/jvmJavaTest/proto-java")
    java.srcDir("src/jvmJavaTest/proto-kotlin")
  }

  registerJavaTest("javaNoOptionsTest") {
    java.srcDir("src/jvmJavaNoOptionsTest/proto-java")
  }

  registerJavaTest("javaCompactTest") {
    java.srcDir("src/jvmJavaCompactTest/proto-java")
  }

  registerJavaTest("javaPrunedTest") {
    java.srcDir("src/jvmJavaPrunedTest/proto-java")
  }

  registerJavaTest("javaAndroidTest") {
    java.srcDir("src/jvmJavaAndroidTest/proto-java")
  }

  registerJavaTest("javaAndroidCompactTest") {
    java.srcDir("src/jvmJavaAndroidCompactTest/proto-java")
  }

  registerJavaTest("jsonJavaTest") {
    java.srcDir("src/jvmJsonTest/kotlin")
    java.srcDir("src/jvmJsonJavaTest/proto-java")
  }

  registerJavaTest("jsonKotlinTest") {
    java.srcDir("src/jvmJsonTest/kotlin")
    java.srcDir("src/jvmJsonKotlinTest/proto-kotlin")
  }

  jvm {
    registerKotlinTest("kotlinInteropTest") {
      kotlin.srcDir("src/jvmKotlinInteropTest/proto-kotlin")
      dependencies {
        implementation(projects.wireRuntime)
        implementation(libs.kotlin.test.junit)
        implementation(libs.assertk)
        implementation(libs.kotlin.reflect)
      }
    }
    registerKotlinTest("kotlinAndroidTest") {
        kotlin.srcDir("src/jvmKotlinAndroidTest/proto-kotlin")
        dependencies {
          implementation(projects.wireRuntime)
          compileOnly(libs.android)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertk)
      }
    }
    val kotlinProtoReader32Test by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinProtoReader32Test/proto-kotlin")
        dependencies {
          implementation(projects.wireRuntime)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertk)
          implementation(libs.kotlin.reflect)
        }
      }
      val jvmProtoReader32Test by tasks.registering(Test::class) {
        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
        testClassesDirs = output.classesDirs
      }
      val jvmTest by tasks.getting {
        dependsOn(jvmProtoReader32Test)
      }
    }
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

val jvmTest by tasks.getting

fun KotlinJvmTarget.registerKotlinTest(
  name: String,
  configureSourceSet: KotlinSourceSet.() -> kotlin.Unit,
) {
  val kotlinInteropTest = compilations.create(name) {
    defaultSourceSet {
      configureSourceSet()
    }
    val jvmKotlinInteropTest by tasks.registering(Test::class) {
      classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
      testClassesDirs = output.classesDirs
    }
    val jvmTest by tasks.getting {
      dependsOn(jvmKotlinInteropTest)
      dependencies {
        implementation(projects.wireGrpcClient)
      }
    }
  }
}

fun KotlinMultiplatformExtension.registerJavaTest(
  name: String, // Like jvmJavaTest
  configureSourceSet: SourceSet.() -> Unit,
) {
  sourceSets {
    jvm {  }
  }
  project.sourceSets {
    val sourceSet: SourceSet = create(name) {
      configureSourceSet()
    }
    val taskProvider = tasks.register(name, Test::class) {
      classpath = sourceSet.runtimeClasspath
      testClassesDirs = sourceSet.output.classesDirs
    }
    jvmTest.dependsOn(taskProvider)
  }
}

sourceSets {
  val jvmTest by getting {
    java.srcDir("src/jvmTest/proto-java")
  }
}

dependencies {
  add("javaTestImplementation", projects.wireRuntime)
  add("javaNoOptionsTestImplementation", projects.wireRuntime)
  add("javaCompactTestImplementation", projects.wireRuntime)
  add("javaPrunedTestImplementation", projects.wireRuntime)
  add("javaAndroidTestImplementation", projects.wireRuntime)
  // add("javaAndroidCompactTestImplementation", projects.wireRuntime)
  add("jsonJavaTestImplementation", projects.wireRuntime)
  add("jsonKotlinTestImplementation", projects.wireRuntime)

  add("javaAndroidTestCompileOnly", libs.android)
  // add("javaAndroidCompactTestCompileOnly", libs.android)
  add("javaAndroidTestCompileOnly", libs.androidx.annotations)
  // add("javaAndroidCompactTestCompileOnly", libs.androidx.annotations)

  add("javaTestImplementation", libs.kotlin.test.junit)
  add("javaTestImplementation", libs.assertk)
  add("javaTestImplementation", libs.jimfs)
  add("javaTestImplementation", libs.truth)
  add("jsonJavaTestImplementation", projects.wireMoshiAdapter)
  add("jsonJavaTestImplementation", projects.wireGsonSupport)
  add("jsonJavaTestImplementation", projects.wireTestUtils)
  add("jsonJavaTestImplementation", libs.kotlin.test.junit)
  add("jsonJavaTestImplementation", libs.assertk)
  add("jsonJavaTestImplementation", libs.jimfs)
  add("jsonJavaTestImplementation", libs.truth)
  add("jsonKotlinTestImplementation", projects.wireMoshiAdapter)
  add("jsonKotlinTestImplementation", projects.wireGsonSupport)
  add("jsonKotlinTestImplementation", projects.wireTestUtils)
  add("jsonKotlinTestImplementation", libs.kotlin.test.junit)
  add("jsonKotlinTestImplementation", libs.assertk)
  add("jsonKotlinTestImplementation", libs.jimfs)
  add("jsonKotlinTestImplementation", libs.truth)

  // add("jvmJavaTestImplementation", projects.wireMoshiAdapter)
  // add("jvmJavaTestImplementation", libs.kotlin.reflect)
  // add("jvmJavaTestImplementation", libs.moshi)
  // add("jvmJavaTestImplementation", libs.moshiKotlin)
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
