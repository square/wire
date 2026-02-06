plugins {
  id("java-library")
  kotlin("jvm")
}

val main by sourceSets.getting {
  java.srcDir(layout.buildDirectory.dir("wire"))
}

dependencies {
  api(projects.wireCompiler)
  api(projects.wireGrpcClient)
  api(projects.wireRuntime)
  api(projects.wireSchema)
  implementation(libs.okio.core)
  api(libs.guava)
  implementation("io.grpc:grpc-protobuf:1.78.0")
  implementation("com.google.protobuf:protoc:4.33.4")

  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.assertk)
  testImplementation(libs.jimfs)
}

val generateReflectionProtosClasspath by configurations.creating

dependencies {
  generateReflectionProtosClasspath(projects.wireCompiler)
}

val generateReflectionProtos by tasks.registering(JavaExec::class) {
  mainClass.set("com.squareup.wire.WireCompiler")
  classpath = generateReflectionProtosClasspath
  args(
    "--proto_path=$projectDir/src/main/resources",
    "--kotlin_out=${layout.buildDirectory.get()}/wire",
    "grpc/reflection/v1alpha/reflection.proto"
  )
}

val compileKotlin by tasks.getting {
  dependsOn(generateReflectionProtos)
}
