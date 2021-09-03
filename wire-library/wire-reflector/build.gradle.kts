import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("java-library")
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-reflector")
  }
}

val main by sourceSets.getting {
  java.srcDir("$buildDir/wire")
}

dependencies {
  api(project(":wire-compiler"))
  api(project(":wire-grpc-client"))
  api(project(":wire-runtime"))
  api(project(":wire-schema"))
  implementation(deps.okio.jvm)
  api(deps.guava)
  implementation("io.grpc:grpc-protobuf:1.21.0")
  implementation("com.google.protobuf:protoc:3.6.1")

  testImplementation(project(":wire-test-utils"))
  testImplementation(deps.junit)
  testImplementation(deps.kotlin.test.junit)
  testImplementation(deps.assertj)
  testImplementation(deps.jimfs)
}

val generateReflectionProtosClasspath by configurations.creating

dependencies {
  generateReflectionProtosClasspath(project(":wire-compiler"))
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
