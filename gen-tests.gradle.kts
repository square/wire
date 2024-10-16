import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
    classpath(platform(libs.okio.bom))
    classpath(libs.okio.core)
  }
  repositories {
    mavenCentral()
  }
}

val fileSystem = FileSystem.SYSTEM
val protosDir = "${rootProject.rootDir}/wire-tests/src/commonTest/proto/java".toPath()
val PROTOS = fileSystem.listRecursively(protosDir)
  .filter { fileSystem.metadata(it).isRegularFile }
  .map { it.relativeTo(protosDir).toString() }
  .filter { it.endsWith(".proto") }
  .sorted()
  .toList()

val wire by configurations.creating {
  attributes {
    val platformAttr = Attribute.of("org.jetbrains.kotlin.platform.type", KotlinPlatformType::class.java)
    attribute(platformAttr, KotlinPlatformType.jvm)
  }
}

dependencies {
  wire(project("wire-compiler"))
}

// JAVA

val generateJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaTest/proto-java",
      "--excludes=squareup.options.*"
  ) + PROTOS
}

val generateKotlinClassAmongJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the Java test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--kotlin_out=wire-tests/src/jvmJavaTest/proto-kotlin",
    "custom_options2.proto"
  )
}

// NO OPTIONS

val generateNoOptionsTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes with no options from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaNoOptionsTest/proto-java",
      "--excludes=google.protobuf.*",
      "--excludes=squareup.options.*",
      "--excludes=com.squareup.wire.ModelEvaluation"
  ) + PROTOS
}

// COMPACT

val generateCompactTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates compact Java classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaCompactTest/proto-java",
      "--compact",
      "all_types.proto"
  )
}

// GSON

val generateGsonAdapterCompactJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Compact Java Tests"
  description = "Generates compat Java classes from the GsonAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--java_out=wire-gson-support/src/test/java",
          "--compact",
          "all_types_proto2.proto",
  )
}

val generateGsonAdapterJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Java Tests"
  description = "Generates Java classes from the GsonAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-gson-support/src/test/java",
          "collection_types.proto",
          "dinosaur_java.proto",
          "period_java.proto",
          "keyword_java.proto"
  )
}

val generateGsonAdapterInteropKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Java interop Kotlin Tests"
  description = "Generates Java interop Kotlin classes from the GsonAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-gson-support/src/test/java",
          "--java_interop",
          "dinosaur_java_interop_kotlin.proto",
          "period_java_interop_kotlin.proto",
          "keyword_kotlin.proto"
  )
}

val generateGsonAdapterKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Kotlin Tests"
  description = "Generates Kotlin classes from the GsonAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-gson-support/src/test/java",
          "all_structs.proto",
          "dinosaur_kotlin.proto",
          "period_kotlin.proto",
          "person_kotlin.proto",
          "getters_kotlin.proto",
          "pizza.proto",
  )
}

val generateGsonTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates Java and Kotlin classes that use Gson from the test protos"
  dependsOn(
    generateGsonAdapterCompactJavaTests,
    generateGsonAdapterJavaTests,
    generateGsonAdapterInteropKotlinTests,
    generateGsonAdapterKotlinTests
  )
}

// INCLUDES / EXCLUDES

val generateIncludesExcludesTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes with included and excluded protos from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaPrunedTest/proto-java",
      "--includes=squareup.protos.roots.A,squareup.protos.roots.H",
      "--excludes=squareup.protos.roots.B",
      "roots.proto"
  )
}

// ANDROID

val generateAndroidTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes for Android from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaAndroidTest/proto-java",
      "--android",
      "person.proto"
  )
}

// ANDROID COMPACT

val generateAndroidCompactTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates compact Java classes for Android from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/java",
      "--java_out=wire-tests/src/jvmJavaAndroidCompactTest/proto-java",
      "--android",
      "--compact",
      "person.proto"
  )
}

// OPAQUE TYPES

val generateOpaqueTypeTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java, Kotlin, and Swift code to demonstrate opaque types"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--java_out=wire-tests/src/jvmJavaTest/proto-java",
    "--no_java_exclusive",
    "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
    "--no_kotlin_exclusive",
    "--swift_out=wire-tests-swift/opaques",
    "--no_swift_exclusive",
    "--opaque_types=squareup.protos.opaque_types.OuterOpaqueType.InnerOpaqueType1",
    "opaque_types.proto"
  )
}

// KOTLIN

val generateKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
      "--kotlin_box_oneofs_min_size=8",
      "all_types.proto",
      "bool.proto",
      "boxed_oneofs.proto",
      "custom_options.proto",
      "deprecated.proto",
      "deprecated_enum.proto",
      "edge_cases.proto",
      "external_message.proto",
      "foreign.proto",
      "form.proto",
      "map.proto",
      "negative_value_enum.proto",
      "no_fields.proto",
      "one_of.proto",
      "optional_enum.proto",
      "option_redacted.proto",
      "packed_encoding.proto",
      "person.proto",
      "redacted_one_of.proto",
      "redacted_test.proto",
      "same_name_enum.proto",
      "simple_message.proto",
      "to_string.proto",
      "unknown_fields.proto",
      "uses_any.proto"
  )
}

val generateKotlinBuildersOnlyTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin builders only classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
    "--kotlin_builders_only",
    "redacted_test_builders_only.proto",
    "simple_message_builders_only.proto",
  )
}

val generateKotlinZipTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test jar"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/kotlin/protos.jar",
      "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
      "squareup/geology/period.proto",
      "squareup/dinosaurs/dinosaur.proto"
  )
}

val generateKotlinEscapeKeywordsTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from protos containing fields whose names clash with Kotlin keywords"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
      "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
      "--kotlin_escape_keywords",
      "keyword_kotlin.proto",
  )
}

// KOTLIN PROTO3

val generateProto3KotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test proto3s"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/proto3/kotlin",
          "--kotlin_out=wire-tests/src/commonTest/proto-kotlin",
          "all_types.proto",
          "person.proto"
  )
}

// Java PROTO3

val generateProto3JavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test proto3s"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/proto3/java",
          "--java_out=wire-tests/src/jvmJavaTest/proto-java",
          "all_types.proto",
          "person.proto"
  )
}

// KOTLIN SERVICES

val generateKotlinServicesTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--kotlin_out=wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "service_kotlin.proto",
    "service_without_package.proto",
  )
}

val generateKotlinServicesAllFlagsTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test protos with all flags"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--no_kotlin_exclusive",
    "--kotlin_rpc_call_style=blocking",
    "--kotlin_rpc_role=server",
    "--kotlin_single_method_services",
    "--kotlin_name_suffix=SomeSuffix",
    "--kotlin_builders_only",
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--kotlin_out=wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "service_kotlin_with_all_flags.proto",
  )
}

// KOTLIN EMIT_PROTO_READER_32

val generateProtoReader32KotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes that use emit_proto_reader_32"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--kotlin_out=wire-tests/src/jvmKotlinProtoReader32Test/proto-kotlin",
    "--emit_proto_reader_32",
    "all_types.proto",
  )
}

// KOTLIN ANDROID

val generateKotlinAndroidTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes for Android from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-tests/src/jvmKotlinAndroidTest/proto-kotlin",
      "--android",
      "person.proto"
  )
}

// KOTLIN JAVA INTEROP

val generateKotlinJavaInteropTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes with Java interop from the test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
      "--proto_path=wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
      "--java_interop",
      "--kotlin_box_oneofs_min_size=8",
      "all_types.proto",
      "deprecated.proto",
      "custom_options.proto",
      "easter.proto",
      "external_message.proto",
      "foreign.proto",
      "form.proto",
      "map.proto",
      "no_fields.proto",
      "one_of.proto",
      "option_redacted.proto",
      "packed_encoding.proto",
      "percents_in_kdoc.proto",
      "person.proto",
      "recursive_map.proto",
      "redacted_one_of.proto",
      "repeated.proto",
      "same_name_enum.proto",
      "simple_message.proto",
      "unknown_fields.proto",
      "uses_any.proto"
  )
}

// ONE JAVA CLASS WHICH WILL BE USING A KOTLIN OPTION

val generateJavaForKotlinJavaInteropTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates one Java class which will depend on a Kotlin generated annotation"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
    "--proto_path=wire-tests/src/commonTest/proto/java",
    "--java_out=wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "depend_on_kotlin_option.proto"
  )
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
    "--proto_path=wire-tests/src/commonTest/proto3/kotlin",
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
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
    "--swift_out=$swiftOut",
    "--experimental-module-manifest=wire-tests/src/commonTest/proto/kotlin/swift_modules_manifest.yaml",
    "swift_module_one.proto",
    "swift_module_two.proto",
    "swift_module_three.proto",
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
    "--proto_path=wire-tests/src/commonTest/proto/kotlin",
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

// GRPC
val grpcProtosDir = "${rootProject.rootDir}/wire-grpc-tests/src/test/proto".toPath()
val GRPC_PROTOS = fileSystem.listRecursively(grpcProtosDir)
  .filter { fileSystem.metadata(it).isRegularFile }
  .map { it.relativeTo(grpcProtosDir).toString() }
  .filter { !it.startsWith("kotlin") && it.endsWith(".proto") }
  .sorted()
  .toList()

val generateGrpcTests by tasks.creating(JavaExec::class) {
  group = "Generate gRPC Tests"
  description = "Generates Kotlin classes from the gRPC test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-grpc-tests/src/test/proto",
          "--kotlin_out=wire-grpc-tests/src/test/proto-grpc",
          "routeguide/RouteGuideProto.proto"
  ) + GRPC_PROTOS
}

// Moshi Adapter

val generateMoshiAdapterCompactJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate MoshiAdapter Compact Java Tests"
  description = "Generates compat Java classes from the MoshiAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--java_out=wire-moshi-adapter/src/test/java",
          "--compact",
          "all_types_proto2.proto"
  )
}

val generateMoshiAdapterJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate MoshiAdapter Java Tests"
  description = "Generates Java classes from the MoshiAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-moshi-adapter/src/test/java",
          "collection_types.proto",
          "person_java.proto",
          "dinosaur_java.proto",
          "period_java.proto",
          "keyword_java.proto"
  )
}

val generateMoshiAdapterInteropKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate MoshiAdapter Java interop Kotlin Tests"
  description = "Generates Java interop Kotlin classes from the MoshiAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-moshi-adapter/src/test/java",
          "--java_interop",
          "person_java_interop_kotlin.proto",
          "dinosaur_java_interop_kotlin.proto",
          "period_java_interop_kotlin.proto",
          "keyword_kotlin.proto"
  )
}

val generateMoshiAdapterKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate MoshiAdapter Kotlin Tests"
  description = "Generates Kotlin classes from the MoshiAdapter test protos"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-moshi-adapter/src/test/java",
          "all_structs.proto",
          "person_kotlin.proto",
          "dinosaur_kotlin.proto",
          "period_kotlin.proto",
          "getters_kotlin.proto",
          "pizza.proto",
  )
}

val generateMoshiTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates Java and Kotlin classes that use Moshi from the test protos"
  dependsOn(
    generateMoshiAdapterCompactJavaTests,
    generateMoshiAdapterJavaTests,
    generateMoshiAdapterInteropKotlinTests,
    generateMoshiAdapterKotlinTests
  )
}

// Shared Json tests.

val generateSharedJsonCompactJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Shared JSON Tests"
  description = "Generates compat Java classes for shared JSON tests"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-tests/src/jvmJsonJavaTest/proto-java",
          "--compact",
          "all32.proto",
          "all64.proto",
          "all_types_proto2.proto",
          "all_types_proto3_test_proto3_optional.proto",
          "all_structs.proto",
          "all_wrappers.proto",
          "camel_case.proto",
          "map_types.proto",
          "pizza.proto"
  )
}

val generateSharedJsonKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Shared JSON Tests"
  description = "Generates Kotlin classes for shared JSON tests"
  classpath = wire
  mainClass.set("com.squareup.wire.WireCompiler")
  args = listOf(
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-tests/src/jvmJsonKotlinTest/proto-kotlin",
          "--java_interop",
          "all32.proto",
          "all64.proto",
          "all_types_proto2.proto",
          "all_types_proto3_test_proto3_optional.proto",
          "all_structs.proto",
          "all_wrappers.proto",
          "camel_case.proto",
          "map_types.proto",
          "pizza.proto"
  )
}

val generateSharedJson by tasks.creating {
  group = "Generate Tests"
  description = "Generates Java and Kotlin classes for shared JSON tests"
  dependsOn(
    generateSharedJsonCompactJavaTests,
    generateSharedJsonKotlinTests
  )
}

val generateTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates all test classes"
  dependsOn(
    "wire-golden-files:generateProtos",
    generateJavaTests,
    generateCompactTests,
    generateNoOptionsTests,
    generateGsonTests,
    generateIncludesExcludesTests,
    generateAndroidTests,
    generateAndroidCompactTests,
    generateKotlinTests,
    generateKotlinBuildersOnlyTests,
    generateKotlinZipTests,
    generateKotlinEscapeKeywordsTests,
    generateKotlinServicesTests,
    generateKotlinServicesAllFlagsTests,
    generateKotlinAndroidTests,
    generateKotlinJavaInteropTests,
    generateJavaForKotlinJavaInteropTests,
    generateSwiftTests,
    generateGrpcTests,
    generateMoshiTests,
    generateProto3KotlinTests,
    generateProto3JavaTests,
    generateProtoReader32KotlinTests,
    generateSharedJson,
    generateOpaqueTypeTests,
  )
}

val cleanGeneratedTests by tasks.creating(Delete::class) {
  group = "Generate Tests"
  description = "Delete all generated tests"
  delete(
    "wire-grpc-tests/src/test/proto-grpc",
    "wire-gson-support/src/test/java/com/squareup/wire/protos",
    "wire-gson-support/src/test/java/squareup/proto2/keywords/",
    "wire-moshi-adapter/src/test/java/com/squareup/wire/protos",
    "wire-moshi-adapter/src/test/java/squareup/proto2/keywords/",
    "wire-tests/src/commonTest/proto-java",
    "wire-tests/src/commonTest/proto-kotlin",
    "wire-tests/src/jvmJavaAndroidCompactTest/proto-java",
    "wire-tests/src/jvmJavaAndroidTest/proto-java",
    "wire-tests/src/jvmJavaCompactTest/proto-java",
    "wire-tests/src/jvmJavaNoOptionsTest/proto-java",
    "wire-tests/src/jvmJavaPrunedTest/proto-java",
    "wire-tests/src/jvmJavaTest/proto-java",
    "wire-tests/src/jvmKotlinAndroidTest/proto-kotlin",
    "wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "wire-tests/src/jvmKotlinProtoReader32Test/proto-kotlin",
    "wire-tests/src/jvmJsonJavaTest/proto-java",
    "wire-tests/src/jvmJsonKotlinTest/proto-kotlin"
  )
}
