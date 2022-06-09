import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
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
  implementation("io.grpc:grpc-protobuf:1.21.0")
  implementation("com.google.protobuf:protoc:3.6.1")

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
  main = "com.squareup.wire.WireCompiler"
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

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(javadocJar = Dokka("dokkaGfm"), sourcesJar = true)
  )
}
