import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("java-library")
}

kotlin {
  jvm {
    val kotlinInteropTest by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinInteropTest/proto-kotlin")
        dependencies {
          implementation(compilations["main"].compileDependencyFiles)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertj)
          implementation(libs.kotlin.reflect)
        }
      }
      val jvmKotlinInteropTest by tasks.creating(Test::class) {
        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
        testClassesDirs = output.classesDirs
      }
      val jvmTest by tasks.getting {
        dependsOn(jvmKotlinInteropTest)
      }
    }
    val kotlinAndroidTest by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinAndroidTest/proto-kotlin")
        dependencies {
          implementation(compilations["main"].compileDependencyFiles)
          compileOnly(libs.android)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertj)
        }
      }
      val jvmKotlinAndroidTest by tasks.creating(Test::class) {
        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
        testClassesDirs = output.classesDirs
      }
      val jvmTest by tasks.getting {
        dependsOn(jvmKotlinAndroidTest)
      }
    }
    // FIXME(egor): withJava() has to be declared after all custom compilations.
    // See https://youtrack.jetbrains.com/issue/KT-41506.
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
    }
  }
  if (kmpNativeEnabled) {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
    macosArm64()
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
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations)
      }
    }
    val jvmTest by getting {
      kotlin.srcDir("src/jvmTest/proto-kotlin")
      kotlin.srcDir("src/jvmTest/proto-java")
      dependencies {
        implementation(projects.wireTestUtils)
        implementation(libs.assertj)
        implementation(libs.kotlin.test.junit)
        implementation(libs.jimfs)
      }
    }
    if (kmpJsEnabled) {
      val jsTest by getting {
        dependencies {
          implementation(libs.kotlin.test.js)
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
      val darwinTest by creating {
        dependsOn(commonTest)
      }

      val iosX64Main by getting
      val iosArm64Main by getting
      val iosSimulatorArm64Main by getting
      val linuxX64Main by getting
      val macosX64Main by getting
      val macosArm64Main by getting
      val iosX64Test by getting
      val iosArm64Test by getting
      val iosSimulatorArm64Test by getting
      val linuxX64Test by getting
      val macosX64Test by getting
      val macosArm64Test by getting
      for (it in listOf(iosX64Main, iosArm64Main, iosSimulatorArm64Main, linuxX64Main, macosX64Main, macosArm64Main)) {
        it.dependsOn(nativeMain)
      }
      for (it in listOf(iosX64Test, iosArm64Test, iosSimulatorArm64Test, linuxX64Test, macosX64Test, macosArm64Test)) {
        it.dependsOn(nativeTest)
      }
      for (it in listOf(iosX64Test, iosArm64Test, macosX64Test, macosArm64Test)) {
        it.dependsOn(darwinTest)
      }
    }
    all {
      languageSettings.optIn("kotlin.Experimental")
    }
  }
}

val jvmTest by tasks.getting

for (target in kotlin.targets.matching { it.platformType.name == "jvm" }) {
  target.project.sourceSets {
    val test by getting {
      java.srcDir("src/jvmTest/proto-java")
    }

    val javaTest by creating {
      java.srcDir("src/jvmJavaTest/proto-java")
    }
    val jvmJavaTest by tasks.creating(Test::class) {
      classpath = javaTest.runtimeClasspath
      testClassesDirs = javaTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaTest)

    val javaNoOptionsTest by creating {
      java.srcDir("src/jvmJavaNoOptionsTest/proto-java")
    }
    val jvmJavaNoOptionsTest by tasks.creating(Test::class) {
      classpath = javaNoOptionsTest.runtimeClasspath
      testClassesDirs = javaNoOptionsTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaNoOptionsTest)

    val javaCompactTest by creating {
      java.srcDir("src/jvmJavaCompactTest/proto-java")
    }
    val jvmJavaCompactTest by tasks.creating(Test::class) {
      classpath = javaCompactTest.runtimeClasspath
      testClassesDirs = javaCompactTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaCompactTest)

    val javaPrunedTest by creating {
      java.srcDir("src/jvmJavaPrunedTest/proto-java")
    }
    val jvmJavaPrunedTest by tasks.creating(Test::class) {
      classpath = javaPrunedTest.runtimeClasspath
      testClassesDirs = javaPrunedTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaPrunedTest)

    val javaAndroidTest by creating {
      java.srcDir("src/jvmJavaAndroidTest/proto-java")
    }
    val jvmJavaAndroidTest by tasks.creating(Test::class) {
      classpath = javaAndroidTest.runtimeClasspath
      testClassesDirs = javaAndroidTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaAndroidTest)

    val javaAndroidCompactTest by creating {
      java.srcDir("src/jvmJavaAndroidCompactTest/proto-java")
    }
    val jvmJavaAndroidCompactTest by tasks.creating(Test::class) {
      classpath = javaAndroidCompactTest.runtimeClasspath
      testClassesDirs = javaAndroidCompactTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJavaAndroidCompactTest)

    val jsonJavaTest by creating {
      java.srcDir("src/jvmJsonTest/kotlin")
      java.srcDir("src/jvmJsonJavaTest/proto-java")
    }
    val jvmJsonJavaTest by tasks.creating(Test::class) {
      classpath = jsonJavaTest.runtimeClasspath
      testClassesDirs = jsonJavaTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJsonJavaTest)

    val jsonKotlinTest by creating {
      java.srcDir("src/jvmJsonTest/kotlin")
      java.srcDir("src/jvmJsonKotlinTest/proto-kotlin")
    }
    val jvmJsonKotlinTest by tasks.creating(Test::class) {
      classpath = jsonKotlinTest.runtimeClasspath
      testClassesDirs = jsonKotlinTest.output.classesDirs
    }
    jvmTest.dependsOn(jvmJsonKotlinTest)

    dependencies {
      add("javaTestImplementation", projects.wireRuntime)
      add("javaNoOptionsTestImplementation", projects.wireRuntime)
      add("javaCompactTestImplementation", projects.wireRuntime)
      add("javaPrunedTestImplementation", projects.wireRuntime)
      add("javaAndroidTestImplementation", projects.wireRuntime)
      add("javaAndroidCompactTestImplementation", projects.wireRuntime)
      add("jsonJavaTestImplementation", projects.wireRuntime)
      add("jsonKotlinTestImplementation", projects.wireRuntime)

      add("javaAndroidTestCompileOnly", libs.android)
      add("javaAndroidCompactTestCompileOnly", libs.android)
      add("javaAndroidTestCompileOnly", libs.androidx.annotations)
      add("javaAndroidCompactTestCompileOnly", libs.androidx.annotations)

      add("javaTestImplementation", libs.kotlin.test.junit)
      add("javaTestImplementation", libs.assertj)
      add("javaTestImplementation", libs.jimfs)
      add("jsonJavaTestImplementation", projects.wireMoshiAdapter)
      add("jsonJavaTestImplementation", projects.wireGsonSupport)
      add("jsonJavaTestImplementation", projects.wireTestUtils)
      add("jsonJavaTestImplementation", libs.kotlin.test.junit)
      add("jsonJavaTestImplementation", libs.assertj)
      add("jsonJavaTestImplementation", libs.jimfs)
      add("jsonKotlinTestImplementation", projects.wireMoshiAdapter)
      add("jsonKotlinTestImplementation", projects.wireGsonSupport)
      add("jsonKotlinTestImplementation", projects.wireTestUtils)
      add("jsonKotlinTestImplementation", libs.kotlin.test.junit)
      add("jsonKotlinTestImplementation", libs.assertj)
      add("jsonKotlinTestImplementation", libs.jimfs)

      add("jvmJavaTestImplementation", projects.wireMoshiAdapter)
      add("jvmJavaTestImplementation", libs.kotlin.reflect)
      add("jvmJavaTestImplementation", libs.moshi)
      add("jvmJavaTestImplementation", libs.moshiKotlin)
    }
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
      "src/jvmJsonJavaTest/proto-java/**/*.kt",
      "src/jvmJsonKotlinTest/proto-kotlin/**/*.kt",
      )
  }
}
