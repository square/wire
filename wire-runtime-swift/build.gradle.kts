import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("swift-library")
  id("xctest")
  id("binary-compatibility-validator")
}

library {
  sourceCompatibility = SwiftVersion.SWIFT5
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
  wire(projects.wireCompiler)
}

val generateSwiftProtos by tasks.creating(JavaExec::class) {
  val swiftOut = "src/main/swift/wellknowntypes"
  val protoPath = "../wire-schema/src/jvmMain/resources/"

  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  val includedTypes = listOf("Duration", "Timestamp").map {
    "google.protobuf.$it"
  }

  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=$protoPath",
    "--swift_out=$swiftOut",
    "--includes=${includedTypes.joinToString(separator = ",")}",
    "--excludes=google.protobuf.*"
  )

  // TODO(kcianfarini) this is a workaround for https://github.com/square/wire/issues/1928
  doLast {
    val wellKnownTypesDir = "wire-runtime-swift/src/main/swift/wellknowntypes/"
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

val generateSampleProtos by tasks.creating(JavaExec::class) {
  val swiftOut = "src/test/swift/sample"
  val protoPath = "../samples/simple-sample/src/main/proto"

  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=$protoPath",
    "--swift_out=$swiftOut"
  )
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
  mainClass.set("com.squareup.wire.WireCompiler")
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

  tasks.named("generateTestProtos").configure {
    dependsOn(generateSampleProtos)
  }
  tasks.named("compileTestSwift").configure {
    dependsOn(generateTestProtos)
  }

  tasks.withType(SwiftCompile::class).all {
    // Include the ${DEVELOPER_DIR}/usr/lib as we also need to use libXCTestSwiftSupport.dylib as of
    // Xcode 12.5:
    // https://forums.swift.org/t/why-xcode-12-5b-cannot-find-xctassertequal-in-scope-in-playground/44411
    val developerDir = compilerArgs.get().extractDeveloperDir() ?: return@all
    compilerArgs.add("-I$developerDir/usr/lib")
  }

  tasks.withType(LinkMachOBundle::class).all {
    // Include the ${DEVELOPER_DIR}/usr/lib as we also need to use libXCTestSwiftSupport.dylib as of
    // Xcode 12.5:
    // https://forums.swift.org/t/why-xcode-12-5b-cannot-find-xctassertequal-in-scope-in-playground/44411
    val developerDir = linkerArgs.get().extractDeveloperDir() ?: return@all
    linkerArgs.add("-L$developerDir/usr/lib")
  }
}

fun List<String>.extractDeveloperDir(): String? = this
  .firstOrNull {
    it.startsWith("-F") && it.endsWith("/Developer/Library/Frameworks")
  }
  ?.removePrefix("-F")
  ?.removeSuffix("/Library/Frameworks")

configure<SpotlessExtension> {
  format("Swift") {
    targetExclude(
      "src/main/swift/wellknowntypes/*.swift",
      "src/test/swift/gen/*.swift",
      "src/test/swift/sample/*.swift",
    )
  }
}
