plugins {
  id("com.squareup.wire")
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

val taskProvider = tasks.register("genProto") {
  val outFile = project.file("$buildDir/generated/proto/period.proto")
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

wire {
  sourcePath {
    srcDir("src/main/proto/")
  }

  sourcePath {
    srcDir(taskProvider)
  }

  kotlin {
  }
}
