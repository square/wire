plugins {
  id("com.squareup.wire") version("$wireVersion")
  id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin
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
