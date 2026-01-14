import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
  }
  repositories {
    mavenCentral()
  }
}

val wire by configurations.creating {
  attributes {
    val platformAttr = Attribute.of("org.jetbrains.kotlin.platform.type", KotlinPlatformType::class.java)
    attribute(platformAttr, KotlinPlatformType.jvm)
  }
}

dependencies {
  wire(project("wire-compiler"))
}

// SWIFT

val generateSwiftProto3Tests by tasks.creating(JavaExec::class) {
  val swiftOut = "wire-tests-proto3-swift/src/main/swift/"
  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/fixtures/proto3/kotlin",
    "--swift_out=$swiftOut",
    "contains_duration.proto",
    "contains_timestamp.proto"
  )
}

val generateSwiftProto2ManifestTests by tasks.creating(JavaExec::class) {
  val swiftOut = "wire-tests-swift/manifest"

  group = "Generate Tests"
  description = "Generates Swift classes from the test protos using a manifest"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/fixtures/proto/kotlin",
    "--swift_out=$swiftOut",
    "--experimental-module-manifest=wire-tests/fixtures/proto/kotlin/swift_modules_manifest.yaml",
    "swift_module_one.proto",
    "swift_module_two.proto",
    "swift_module_three.proto",
    "swift_module_address.proto",
    "swift_module_location.proto",
  )
}

val generateSwiftProto2Tests by tasks.creating(JavaExec::class) {
  val swiftOut = "wire-tests-swift/no-manifest/src/main/swift"
  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/fixtures/proto/kotlin",
    "--swift_out=$swiftOut",
    "all_types.proto",
    "custom_options.proto",
    "deprecated.proto",
    "deprecated_enum.proto",
    "external_message.proto",
    "foreign.proto",
    "form.proto",
    "map.proto",
    "negative_value_enum.proto",
    "no_fields.proto",
    "one_of.proto",
    "option_redacted.proto",
    "optional_enum.proto",
    "packed_encoding.proto",
    "percents_in_kdoc.proto",
    "person.proto",
    "redacted_one_of.proto",
    "recursive_map.proto",
    "same_name_enum.proto",
    "swift_edge_cases.proto",
    "to_string.proto",
    "unknown_fields.proto"
  )
}

val generateSwiftTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  if (project.properties.get("swift") != "false") {
    dependsOn(
      generateSwiftProto2ManifestTests,
      generateSwiftProto2Tests,
      generateSwiftProto3Tests,
      ":wire-runtime-swift:generateTestProtos"
    )
  }
}

val generateTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates Swift test classes"
  dependsOn(
    generateSwiftTests,
  )
}
