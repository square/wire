import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("java-library")
}

kotlin {
  applyDefaultHierarchyTemplate()
  jvm {
    val kotlinInteropTest by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinInteropTest/proto-kotlin")
        dependencies {
          implementation(projects.wireRuntime)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertk)
          implementation(libs.kotlin.reflect)
        }
      }
      val jvmKotlinInteropTest by tasks.creating(Test::class) {
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
    val kotlinAndroidTest by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinAndroidTest/proto-kotlin")
        dependencies {
          implementation(projects.wireRuntime)
          compileOnly(libs.android)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertj)
          implementation(libs.assertk)
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
    val kotlinProtoReader32Test by compilations.creating {
      defaultSourceSet {
        kotlin.srcDir("src/jvmKotlinProtoReader32Test/proto-kotlin")
        dependencies {
          implementation(projects.wireRuntime)
          implementation(libs.kotlin.test.junit)
          implementation(libs.assertj)
          implementation(libs.assertk)
          implementation(libs.kotlin.reflect)
        }
      }
      val jvmProtoReader32Test by tasks.creating(Test::class) {
        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
        testClassesDirs = output.classesDirs
      }
      val jvmTest by tasks.getting {
        dependsOn(jvmProtoReader32Test)
      }
    }
    // FIXME(egor): withJava() has to be declared after all custom compilations.
    // See https://youtrack.jetbrains.com/issue/KT-41506.
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
    if (System.getProperty("kjs", "true").toBoolean()) {
      val jsTest by getting {
        dependencies {
          implementation(libs.kotlin.test.js)
        }
      }
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
      java.srcDir("src/jvmJavaTest/proto-kotlin")
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
      add("javaTestImplementation", libs.assertk)
      add("javaTestImplementation", libs.jimfs)
      add("jsonJavaTestImplementation", projects.wireMoshiAdapter)
      add("jsonJavaTestImplementation", projects.wireGsonSupport)
      add("jsonJavaTestImplementation", projects.wireTestUtils)
      add("jsonJavaTestImplementation", libs.kotlin.test.junit)
      add("jsonJavaTestImplementation", libs.assertj)
      add("jsonJavaTestImplementation", libs.assertk)
      add("jsonJavaTestImplementation", libs.jimfs)
      add("jsonKotlinTestImplementation", projects.wireMoshiAdapter)
      add("jsonKotlinTestImplementation", projects.wireGsonSupport)
      add("jsonKotlinTestImplementation", projects.wireTestUtils)
      add("jsonKotlinTestImplementation", libs.kotlin.test.junit)
      add("jsonKotlinTestImplementation", libs.assertj)
      add("jsonKotlinTestImplementation", libs.assertk)
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
