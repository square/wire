plugins {
  id("java-library")
  kotlin("jvm")
}

val main by sourceSets.getting {
  java.srcDir("$buildDir/wire")
}

dependencies {
  api(projects.wireCompiler)
  api(projects.wireGrpcClient)
  api(projects.wireRuntime)
  api(projects.wireSchema)
  implementation(libs.okio.core)
  api(libs.guava)
  implementation("io.grpc:grpc-protobuf:1.58.0")
  implementation("com.google.protobuf:protoc:3.24.3")

  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.jimfs)
}

val generateReflectionProtosClasspath by configurations.creating

dependencies {
  generateReflectionProtosClasspath(projects.wireCompiler)
}

val generateReflectionProtos by tasks.creating(JavaExec::class) {
  mainClass.set("com.squareup.wire.WireCompiler")
  classpath = generateReflectionProtosClasspath
  args(
    "--proto_path=$projectDir/src/main/resources",
    "--kotlin_out=$buildDir/wire",
    "grpc/reflection/v1alpha/reflection.proto"
  )
}

val compileKotlin by tasks.getting {
  dependsOn(generateReflectionProtos)
}
