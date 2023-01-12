import okio.FileSystem
import okio.Path
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
val protosDir = "${rootProject.rootDir}/wire-library/wire-tests/src/commonTest/proto/java".toPath()
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

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
  wire(versionCatalog.findLibrary("wire-compiler").get())
}

// JAVA

val generateJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaTest/proto-java",
      "--excludes=squareup.options.*"
  ) + PROTOS
}

// NO OPTIONS

val generateNoOptionsTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Java classes with no options from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaNoOptionsTest/proto-java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaCompactTest/proto-java",
      "--compact",
      "all_types.proto"
  )
}

// GSON

val generateGsonAdapterCompactJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Compact Java Tests"
  description = "Generates compat Java classes from the GsonAdapter test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--java_out=wire-library/wire-gson-support/src/test/java",
          "--compact",
          "all_types_proto2.proto"
  )
}

val generateGsonAdapterJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate GsonAdapter Java Tests"
  description = "Generates Java classes from the GsonAdapter test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-library/wire-gson-support/src/test/java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-library/wire-gson-support/src/test/java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-library/wire-gson-support/src/test/java",
          "dinosaur_kotlin.proto",
          "period_kotlin.proto",
          "person_kotlin.proto",
          "getters_kotlin.proto"
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaPrunedTest/proto-java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaAndroidTest/proto-java",
      "--android",
      "person.proto"
  )
}

// ANDROID COMPACT

val generateAndroidCompactTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates compact Java classes for Android from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
      "--java_out=wire-library/wire-tests/src/jvmJavaAndroidCompactTest/proto-java",
      "--android",
      "--compact",
      "person.proto"
  )
}

// KOTLIN

val generateKotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-library/wire-tests/src/commonTest/proto-kotlin",
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

val generateKotlinZipTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test jar"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin/protos.jar",
      "--kotlin_out=wire-library/wire-tests/src/commonTest/proto-kotlin",
      "squareup/geology/period.proto",
      "squareup/dinosaurs/dinosaur.proto"
  )
}

// KOTLIN PROTO3

val generateProto3KotlinTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test proto3s"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/proto3/kotlin",
          "--kotlin_out=wire-library/wire-tests/src/commonTest/proto-kotlin",
          "all_types.proto",
          "person.proto"
  )
}

// Java PROTO3

val generateProto3JavaTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test proto3s"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/proto3/java",
          "--java_out=wire-library/wire-tests/src/jvmJavaTest/proto-java",
          "all_types.proto",
          "person.proto"
  )
}

// KOTLIN SERVICES

val generateKotlinServicesTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-library/wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
      "service_kotlin.proto",
      "service_without_package.proto"
  )
}

// KOTLIN ANDROID

val generateKotlinAndroidTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes for Android from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-library/wire-tests/src/jvmKotlinAndroidTest/proto-kotlin",
      "--android",
      "person.proto"
  )
}

// KOTLIN JAVA INTEROP

val generateKotlinJavaInteropTests by tasks.creating(JavaExec::class) {
  group = "Generate Tests"
  description = "Generates Kotlin classes with Java interop from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
      "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin",
      "--kotlin_out=wire-library/wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
      "--java_interop",
      "--kotlin_box_oneofs_min_size=8",
      "all_types.proto",
      "deprecated.proto",
      "custom_options.proto",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
    "--proto_path=wire-library/wire-tests/src/commonTest/proto/java",
    "--java_out=wire-library/wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "depend_on_kotlin_option.proto"
  )
}

// SWIFT

val generateSwiftProto3Tests by tasks.creating(JavaExec::class) {
  val swiftOut = "wire-library/wire-tests-proto3-swift/src/main/swift/"
  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
    "--proto_path=wire-library/wire-tests/src/commonTest/proto3/kotlin",
    "--swift_out=$swiftOut",
    "contains_duration.proto",
    "contains_timestamp.proto"
  )
}

val generateSwiftProto2Tests by tasks.creating(JavaExec::class) {
  val swiftOut = "wire-library/wire-tests-swift/src/main/swift/"
  doFirst {
    val outFile = file(swiftOut)
    outFile.deleteRecursively()
    outFile.mkdir()
  }

  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
    "--proto_path=wire-library/wire-tests/src/commonTest/proto/kotlin",
    "--swift_out=$swiftOut",
    "all_types.proto",
    "custom_options.proto",
    "deprecated.proto",
    "deprecated_enum.proto",
//      'edge_cases.proto',
    "external_message.proto",
    "foreign.proto",
    "form.proto",
//      'keyword.proto',
    "map.proto",
    "no_fields.proto",
    "one_of.proto",
    "option_redacted.proto",
    "optional_enum.proto",
    "packed_encoding.proto",
    "percents_in_kdoc.proto",
    "person.proto",
    "redacted_one_of.proto",
//      'redacted_test.proto',
    "recursive_map.proto",
    "same_name_enum.proto",
//      'simple_message.proto',
    "to_string.proto",
    "unknown_fields.proto"
//      'uses_any.proto',
  )
}

val generateSwiftTests by tasks.creating {
  group = "Generate Tests"
  description = "Generates Swift classes from the test protos"
  dependsOn(
    generateSwiftProto2Tests,
    generateSwiftProto3Tests
  )
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
  main = "com.squareup.wire.WireCompiler"
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--java_out=wire-library/wire-moshi-adapter/src/test/java",
          "--compact",
          "all_types_proto2.proto"
  )
}

val generateMoshiAdapterJavaTests by tasks.creating(JavaExec::class) {
  group = "Generate MoshiAdapter Java Tests"
  description = "Generates Java classes from the MoshiAdapter test protos"
  classpath = wire
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-library/wire-moshi-adapter/src/test/java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-library/wire-moshi-adapter/src/test/java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-library/wire-moshi-adapter/src/test/java",
          "person_kotlin.proto",
          "dinosaur_kotlin.proto",
          "period_kotlin.proto",
          "getters_kotlin.proto"
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--java_out=wire-library/wire-tests/src/jvmJsonJavaTest/proto-java",
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
  main = "com.squareup.wire.WireCompiler"
  args = listOf(
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto2",
          "--proto_path=wire-library/wire-tests/src/commonTest/shared/proto/proto3",
          "--kotlin_out=wire-library/wire-tests/src/jvmJsonKotlinTest/proto-kotlin",
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
    generateJavaTests,
    generateCompactTests,
    generateNoOptionsTests,
    generateGsonTests,
    generateIncludesExcludesTests,
    generateAndroidTests,
    generateAndroidCompactTests,
    generateKotlinTests,
    generateKotlinZipTests,
    generateKotlinServicesTests,
    generateKotlinAndroidTests,
    generateKotlinJavaInteropTests,
    generateJavaForKotlinJavaInteropTests,
    generateSwiftTests,
    generateGrpcTests,
    generateMoshiTests,
    generateProto3KotlinTests,
    generateProto3JavaTests,
    generateSharedJson
  )
}

val cleanGeneratedTests by tasks.creating(Delete::class) {
  group = "Generate Tests"
  description = "Delete all generated tests"
  delete(
    "wire-grpc-tests/src/test/proto-grpc",
    "wire-library/wire-gson-support/src/test/java/com/squareup/wire/protos",
    "wire-library/wire-gson-support/src/test/java/squareup/proto2/keywords/",
    "wire-library/wire-moshi-adapter/src/test/java/com/squareup/wire/protos",
    "wire-library/wire-moshi-adapter/src/test/java/squareup/proto2/keywords/",
    "wire-library/wire-tests/src/commonTest/proto-java",
    "wire-library/wire-tests/src/commonTest/proto-kotlin",
    "wire-library/wire-tests/src/jvmJavaAndroidCompactTest/proto-java",
    "wire-library/wire-tests/src/jvmJavaAndroidTest/proto-java",
    "wire-library/wire-tests/src/jvmJavaCompactTest/proto-java",
    "wire-library/wire-tests/src/jvmJavaNoOptionsTest/proto-java",
    "wire-library/wire-tests/src/jvmJavaPrunedTest/proto-java",
    "wire-library/wire-tests/src/jvmJavaTest/proto-java",
    "wire-library/wire-tests/src/jvmKotlinAndroidTest/proto-kotlin",
    "wire-library/wire-tests/src/jvmKotlinInteropTest/proto-kotlin",
    "wire-library/wire-tests/src/jvmJsonJavaTest/proto-java",
    "wire-library/wire-tests/src/jvmJsonKotlinTest/proto-kotlin"
  )
}
