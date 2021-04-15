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
    all {
      languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
    val commonMain by getting {
      dependencies {
        implementation(deps.okio.multiplatform)
        implementation(deps.kotlin.serializationJson)
      }
    }
  }
}

afterEvaluate {
  val installLocally by tasks.creating {
    dependsOn("publishKotlinMultiplatformPublicationToTestRepository")
    dependsOn("publishMetadataPublicationToTestRepository")
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

apply(from = "$rootDir/gradle/gradle-mvn-mpp-push.gradle")

configure<PublishingExtension> {
  // Use default artifact name for the JVM target
  publications {
    val kotlinMultiplatform by getting(MavenPublication::class) {
      artifactId = "wire-kotlin-serialization-multiplatform"
    }
    val jvm by getting(MavenPublication::class) {
      artifactId = "wire-kotlin-serialization"
    }
  }
}

for (target in kotlin.targets.matching { it.platformType.name == "jvm" }) {
  val jar by tasks.getting(Jar::class) {
    manifest {
      attributes("Automatic-Module-Name" to "wire-kotlin-serialization")
    }
  }
}

val main by sourceSets.getting
animalsniffer {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}
