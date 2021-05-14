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
          implementation(deps.kotlin.test.junit)
          implementation(deps.assertj)
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
          compileOnly(deps.android)
          implementation(deps.kotlin.test.junit)
          implementation(deps.assertj)
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
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":wire-runtime"))
        api(project(":wire-grpc-client"))
      }
    }
    val commonTest by getting {
      kotlin.srcDir("src/commonTest/proto-kotlin")
      dependencies {
        implementation(deps.kotlin.test.common)
        implementation(deps.kotlin.test.annotations)
      }
    }
    val jvmTest by getting {
      kotlin.srcDir("src/jvmTest/proto-kotlin")
      kotlin.srcDir("src/jvmTest/proto-java")
      dependencies {
        implementation(project(":wire-test-utils"))
        implementation(deps.assertj)
        implementation(deps.kotlin.test.junit)
        implementation(deps.jimfs)
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
      val darwinTest by creating {
        dependsOn(commonTest)
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
      for (it in listOf(iosX64Test, iosArm64Test, macosX64Test)) {
        it.dependsOn(darwinTest)
      }
    }
    all {
      languageSettings.useExperimentalAnnotation("kotlin.Experimental")
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
      add("javaTestImplementation", project(":wire-runtime"))
      add("javaNoOptionsTestImplementation", project(":wire-runtime"))
      add("javaCompactTestImplementation", project(":wire-runtime"))
      add("javaPrunedTestImplementation", project(":wire-runtime"))
      add("javaAndroidTestImplementation", project(":wire-runtime"))
      add("javaAndroidCompactTestImplementation", project(":wire-runtime"))
      add("jsonJavaTestImplementation", project(":wire-runtime"))
      add("jsonKotlinTestImplementation", project(":wire-runtime"))

      add("javaAndroidTestCompileOnly", deps.android)
      add("javaAndroidCompactTestCompileOnly", deps.android)
      add("javaAndroidTestCompileOnly", deps.androidx.annotations)
      add("javaAndroidCompactTestCompileOnly", deps.androidx.annotations)

      add("javaTestImplementation", deps.kotlin.test.junit)
      add("javaTestImplementation", deps.assertj)
      add("javaTestImplementation", deps.jimfs)
      add("jsonJavaTestImplementation", project(":wire-moshi-adapter"))
      add("jsonJavaTestImplementation", project(":wire-gson-support"))
      add("jsonJavaTestImplementation", project(":wire-test-utils"))
      add("jsonJavaTestImplementation", deps.kotlin.test.junit)
      add("jsonJavaTestImplementation", deps.assertj)
      add("jsonJavaTestImplementation", deps.jimfs)
      add("jsonKotlinTestImplementation", project(":wire-moshi-adapter"))
      add("jsonKotlinTestImplementation", project(":wire-gson-support"))
      add("jsonKotlinTestImplementation", project(":wire-test-utils"))
      add("jsonKotlinTestImplementation", deps.kotlin.test.junit)
      add("jsonKotlinTestImplementation", deps.assertj)
      add("jsonKotlinTestImplementation", deps.jimfs)

      add("jvmJavaTestImplementation", project(":wire-moshi-adapter"))
      add("jvmJavaTestImplementation", deps.kotlin.reflect)
      add("jvmJavaTestImplementation", deps.moshi)
      add("jvmJavaTestImplementation", deps.moshiKotlin)
    }
  }

  val jar by tasks.getting(Jar::class) {
    manifest {
      attributes("Automatic-Module-Name" to "wire-tests")
    }
  }
}
