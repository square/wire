import org.gradle.api.internal.file.FileOperations

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

@CacheableTask
abstract class ProtoWritingTask @Inject constructor(
  objects: ObjectFactory,
  private val fileOperations: FileOperations,
) : DefaultTask() {

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generateWireFiles() {
    val outFile = outputDirectory.get().asFile.resolve("period.proto")
    outFile.writeText("""
      syntax = "proto2";

      enum Period {
        CRETACEOUS = 1;
      }
    """)
  }
}

val doItTask = tasks.register("do_it", ProtoWritingTask::class.java) {
  outputDirectory.set(layout.buildDirectory.dir("generated/proto/"))
}

wire {
  sourcePath {
    srcDir("src/main/proto/")
  }

  sourcePath {
    srcDir(doItTask)
  }

  kotlin {
  }
}
