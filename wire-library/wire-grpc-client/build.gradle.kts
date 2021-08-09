import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  kotlin("multiplatform")
  id("java-library")
  id("ru.vyarus.animalsniffer")
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
      browser()
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

afterEvaluate {
  val dokka by tasks.getting(DokkaTask::class) {
    outputDirectory = "$rootDir/docs/3.x"
    outputFormat = "gfm"
  }
}

apply(from = "$rootDir/gradle/gradle-mvn-mpp-push.gradle")

configure<PublishingExtension> {
  // Use default artifact name for the JVM target
  publications {
    val kotlinMultiplatform by getting(MavenPublication::class) {
      artifactId = "wire-grpc-client-multiplatform"
    }
    val jvm by getting(MavenPublication::class) {
      artifactId = "wire-grpc-client"
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

