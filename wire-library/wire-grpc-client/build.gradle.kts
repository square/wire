import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

plugins {
  kotlin("multiplatform")
  id("ru.vyarus.animalsniffer")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
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
configure<AnimalSnifferExtension> {
  sourceSets = listOf(main)
  ignore("com.squareup.wire.internal")
}

// https://github.com/vanniktech/gradle-maven-publish-plugin/issues/301
val metadataJar by tasks.getting(Jar::class)
configure<PublishingExtension> {
  publications.withType<MavenPublication>().named("kotlinMultiplatform").configure {
    artifact(metadataJar)
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = Dokka("dokkaGfm"))
  )
}
