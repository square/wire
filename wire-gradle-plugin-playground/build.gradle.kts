import com.squareup.wire.schema.EventListener
import org.gradle.api.internal.file.FileOperations

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

class MyEventListenerFactory : EventListener.Factory {
  override fun create(): EventListener {
    return object: EventListener() {}
  }
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
    val outFile1 = outputDirectory.get().asFile.resolve("period.proto")
    outFile1.writeText(
      """
      syntax = "proto2";

      enum Period {
        CRETACEOUS = 1;
      }
    """
    )
    val outFile2 = outputDirectory.get().asFile.resolve("moment.proto")
    outFile2.writeText(
      """
      syntax = "proto2";

      enum Moment {
        VRAIMENT = 1;
      }
    """
    )
  }
}

val doItTask = tasks.register("do_it", ProtoWritingTask::class.java) {
  outputDirectory.set(layout.buildDirectory.dir("generated/proto/"))
}

wire {
  protoLibrary = true

  sourcePath {
    srcDir(doItTask)
    include("period.proto")
  }

  eventListenerFactory(MyEventListenerFactory())

  kotlin {
    singleMethodServices = true
    escapeKotlinKeywords = true
  }
}

dependencies {
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  implementation(projects.wireCompiler)
  implementation(projects.wireSchema)
  implementation(projects.wireGsonSupport)
  implementation(projects.wireMoshiAdapter)
  implementation(libs.assertk)
  implementation(libs.junit)
  implementation(libs.protobuf.javaUtil)
  implementation(projects.wireTestUtils)
}
