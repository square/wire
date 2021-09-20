// If false - JS targets will not be configured in multiplatform projects.
val kmpJsEnabled = System.getProperty("kjs", "true").toBoolean()

// If false - Native targets will not be configured in multiplatform projects.
val kmpNativeEnabled = System.getProperty("knative", "true").toBoolean()

object versions {
  val android = "4.1.1.4"
  val animalSniffer = "1.16"
  val animalSnifferGradle = "1.5.0"
  val assertj = "3.11.0"
  val coroutines = "1.5.2"
  val dokka = "1.5.30"
  val errorprone = "2.0.21"
  val grpc = "1.38.1"
  val gson = "2.8.6"
  val guava = "20.0"
  val javapoet = "1.13.0"
  val jimfs = "1.0"
  val jmh = "1.23"
  val jmhPlugin = "0.5.0"
  val jsr305 = "3.0.2"
  val junit = "4.12"
  val kotlin = "1.5.31"
  val kotlinpoet = "1.9.0"
  val moshi = "1.12.0"
  val okhttp = "4.9.1"
  val okio = "3.0.0-alpha.9"
  val protobuf = "3.17.3"
  val protobufGradlePlugin = "0.8.16"
}

object deps {
  object plugins {
    val android = "com.android.tools.build:gradle:4.0.1"
    val jmh = "me.champeau.gradle:jmh-gradle-plugin:${versions.jmhPlugin}"
    val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
    val kotlinSerialization = "org.jetbrains.kotlin:kotlin-serialization:${versions.kotlin}"
    val shadow = "com.github.jengelman.gradle.plugins:shadow:4.0.1"
    val japicmp = "me.champeau.gradle:japicmp-gradle-plugin:0.2.8"
  }

  val android = "com.google.android:android:${versions.android}"

  object androidx {
    val annotations = "androidx.annotation:annotation:1.1.0"
    val appcompat = "androidx.appcompat:appcompat:1.0.2"
    val constraintLayout = "androidx.constraintlayout:constraintlayout:1.1.3"
    val ktx = "androidx.core:core-ktx:1.0.2"
  }

  val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${versions.dokka}"
  val dokkaCore = "org.jetbrains.dokka:dokka-core:${versions.dokka}"
  val guava = "com.google.guava:guava:${versions.guava}"

  object okio {
    val jvm = "com.squareup.okio:okio:${versions.okio}"
    val fakefilesystem = "com.squareup.okio:okio-fakefilesystem:${versions.okio}"
    val multiplatform = "com.squareup.okio:okio-multiplatform:${versions.okio}"
  }

  val jsr305 = "com.google.code.findbugs:jsr305:${versions.jsr305}"

  object grpc {
    val genJava = "io.grpc:protoc-gen-grpc-java:${versions.grpc}"
    val netty = "io.grpc:grpc-netty:${versions.grpc}"
    val protobuf = "io.grpc:grpc-protobuf:${versions.grpc}"
    val stub = "io.grpc:grpc-stub:${versions.grpc}"
  }

  val gson = "com.google.code.gson:gson:${versions.gson}"
  val javapoet = "com.squareup:javapoet:${versions.javapoet}"
  val kotlinpoet = "com.squareup:kotlinpoet:${versions.kotlinpoet}"
  val mockwebserver = "com.squareup.okhttp3:mockwebserver:${versions.okhttp}"
  val swiftpoet = "io.outfoxx:swiftpoet:1.0.0"
  val okhttp = "com.squareup.okhttp3:okhttp:${versions.okhttp}"

  object jmh {
    val core = "org.openjdk.jmh:jmh-core:${versions.jmh}"
    val generator = "org.openjdk.jmh:jmh-generator-annprocess:${versions.jmh}"
  }

  object kotlin {
    object test {
      val common = "org.jetbrains.kotlin:kotlin-test-common"
      val annotations = "org.jetbrains.kotlin:kotlin-test-annotations-common"
      val junit = "org.jetbrains.kotlin:kotlin-test-junit"
      val js = "org.jetbrains.kotlin:kotlin-test-js"
    }

    object coroutines {
      val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}"
      val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}"
    }

    val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1"
    val reflect = "org.jetbrains.kotlin:kotlin-reflect:${versions.kotlin}"
    val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${versions.kotlin}"
  }

  val kaml = "com.charleskorn.kaml:kaml:0.20.0"
  val misk = "com.squareup.misk:misk:0.11.0"
  val moshi = "com.squareup.moshi:moshi:${versions.moshi}"
  val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${versions.moshi}"
  val junit = "junit:junit:${versions.junit}"
  val assertj = "org.assertj:assertj-core:${versions.assertj}"
  val jimfs = "com.google.jimfs:jimfs:${versions.jimfs}"

  object animalSniffer {
    val gradle = "ru.vyarus:gradle-animalsniffer-plugin:${versions.animalSnifferGradle}"
    val annotations = "org.codehaus.mojo:animal-sniffer-annotations:${versions.animalSniffer}"
  }

  object protobuf {
    val gradlePlugin = "com.google.protobuf:protobuf-gradle-plugin:${versions.protobufGradlePlugin}"
    val java = "com.google.protobuf:protobuf-java:${versions.protobuf}"
    val javaUtil = "com.google.protobuf:protobuf-java-util:${versions.protobuf}"
    val protoc = "com.google.protobuf:protoc:${versions.protobuf}"
  }

  val truth = "com.google.truth:truth:1.1.3"
  val vanniktechPublishPlugin = "com.vanniktech:gradle-maven-publish-plugin:0.18.0"

  object wire {
    val compiler = "com.squareup.wire:wire-compiler"
    val gradlePlugin = "com.squareup.wire:wire-gradle-plugin"
    val grpcClient = "com.squareup.wire:wire-grpc-client"
    val grpcServer = "com.squareup.wire:wire-grpc-server"
    val grpcMockWebServer = "com.squareup.wire:wire-grpc-mockwebserver"
    val gsonSupport = "com.squareup.wire:wire-gson-support"
    val javaGenerator = "com.squareup.wire:wire-java-generator"
    val kotlinGenerator = "com.squareup.wire:wire-kotlin-generator"
    val moshiAdapter = "com.squareup.wire:wire-moshi-adapter"
    val runtime = "com.squareup.wire:wire-runtime"
    val schema = "com.squareup.wire:wire-schema"
    val testUtils = "com.squareup.wire:wire-test-utils"
  }
}
