buildscript {
  dependencies {
    classpath("com.squareup.wire:wire-gradle-plugin:${properties["wireVersion"]}")
    classpath(libs.pluginz.kotlin)
  }

  repositories {
    maven {
      setUrl(File(rootDir, "../../../../../build/localMaven").toURI().toString())
    }
    mavenCentral()
    google()
  }
}

plugins {
  id("com.squareup.wire").version("${properties["wireVersion"]}")
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

val genProtosDir = "$buildDir/generated/proto"

tasks.register("genProto") {
  val outFile = project.file("$genProtosDir/period.proto")
  outputs.file(outFile)
  doLast {
    outFile.writeText("""
      syntax = "proto2";

      enum Period {
        CRETACEOUS = 1;
      }
    """)
  }
}

tasks.matching { it.name == "generateMainProtos" }.configureEach {
  dependsOn("genProto")
}

wire {
  sourcePath {
    srcDir("src/main/proto/")
  }

  sourcePath {
    srcDir(genProtosDir)
  }

  kotlin {
  }
}
