plugins {
  id("swift-library")
  id("xctest")
}

library {
  module.set("Wire")
}

val wire by configurations.creating {
  attributes {
    // Despite being a Swift module, we want JVM dependencies in this configuration.
    val gradleUsage = Attribute.of("org.gradle.usage", String::class.java)
    attribute(gradleUsage, "java-runtime")
  }
}

dependencies {
  wire(project(":wire-compiler"))
}

val generateSwiftProtos by tasks.creating(JavaExec::class) {
  val swiftOut = "src/main/swift/wellknowntypes"
  val protoPath = "../wire-schema/src/jvmMain/resources/"

  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  val includedTypes = listOf("Duration").map {
    "google.protobuf.$it"
  }

  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
    "--proto_path=$protoPath",
    "--swift_out=$swiftOut",
    "--includes=${includedTypes.joinToString(separator = ",")}",
    "--excludes=google.protobuf.*"
  )

  // TODO(kcianfarini) this is a workaround for https://github.com/square/wire/issues/1928
  doLast {
    val wellKnownTypesDir = "wire-library/wire-runtime-swift/src/main/swift/wellknowntypes/"
    val files = listOf(
      "EnumOptions.swift",
      "EnumValueOptions.swift",
      "FieldOptions.swift",
      "FileOptions.swift",
      "MessageOptions.swift",
      "MethodOptions.swift",
      "ServiceOptions.swift"
    )

    val actualFiles = files.map { wellKnownTypesDir + it }

    actualFiles.forEach { File(it).delete() }

    File(wellKnownTypesDir)
      .walk()
      .filter { it.extension == "swift" }
      .forEach { file ->
        val content = file.readText(Charsets.UTF_8)
        file.writeText(content.replace("import Wire\n", ""), Charsets.UTF_8)
      }
  }
}

val generateTestProtos by tasks.creating(JavaExec::class) {
  val swiftOut = "src/test/swift/gen"
  val protoPath = "src/test/proto"

  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
    "--proto_path=$protoPath",
    "--swift_out=$swiftOut"
  )
}

// TODO make finding test tasks more dynamic
afterEvaluate {
  val swiftTasks = listOf("compileDebugSwift", "compileReleaseSwift")
  for (task in swiftTasks) {
    tasks.named(task).configure {
      dependsOn(generateSwiftProtos)
    }
  }

  tasks.named("compileTestSwift").configure {
    dependsOn(generateTestProtos)
  }
}
