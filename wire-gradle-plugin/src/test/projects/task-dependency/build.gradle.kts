import org.gradle.api.internal.file.FileOperations

plugins {
  id("com.squareup.wire") version "5.1.0-SNAPSHOT"
  id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin
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
