rootProject.name = "wire"

pluginManagement {
  includeBuild("build-support/settings")

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

plugins {
  // id("com.gradle.enterprise").version("3.15")
  id("com.squareup.wire.settings")
}

include(":wire-bom")
include(":wire-compiler")
include(":wire-gradle-plugin")
include(":wire-grpc-client")
include(":wire-grpc-server")
include(":wire-grpc-server-generator")
include(":wire-grpc-mockwebserver")
include(":wire-gson-support")
include(":wire-java-generator")
include(":wire-kotlin-generator")
include(":wire-moshi-adapter")
include(":wire-reflector")
include(":wire-runtime")
include(":wire-schema")
include(":wire-schema-tests")
include(":wire-swift-generator")
include(":wire-test-utils")
include(":wire-tests")

if (startParameter.projectProperties.get("swift") != "false") {
  include(":wire-runtime-swift")
  include(":wire-tests-swift")
  include(":wire-tests-proto3-swift")
}

includeBuild("build-support") {
  dependencySubstitution {
    substitute(module("com.squareup.wire.build:gradle-plugin")).using(project(":"))
    substitute(module("com.squareup.wire:wire-gradle-plugin")).using(project(":wire-gradle-plugin"))
    substitute(module("com.squareup.wire:wire-runtime")).using(project(":wire-runtime"))
    substitute(module("com.squareup.wire:wire-kotlin-generator")).using(project(":wire-kotlin-generator"))
    substitute(module("com.squareup.wire:wire-test-utils")).using(project(":wire-test-utils"))
    substitute(module("com.squareup.wire:wire-schema")).using(project(":wire-schema"))
    substitute(module("com.squareup.wire:wire-grpc-client")).using(project(":wire-grpc-client"))
    substitute(module("com.squareup.wire:wire-grpc-server-generator")).using(project(":wire-grpc-server-generator"))
    substitute(module("com.squareup.wire:wire-grpc-server")).using(project(":wire-grpc-server"))
    substitute(module("com.squareup.wire:wire-schema-tests")).using(project(":wire-schema-tests"))
    substitute(module("com.squareup.wire:wire-compiler")).using(project(":wire-compiler"))
    substitute(module("com.squareup.wire:wire-java-generator")).using(project(":wire-java-generator"))
    substitute(module("com.squareup.wire:wire-swift-generator")).using(project(":wire-swift-generator"))
  }
}

include(":samples:simple-sample")
include(":samples:android-app-java-sample")
include(":samples:android-app-kotlin-sample")
include(":samples:android-app-variants-sample")
include(":samples:android-lib-java-sample")
include(":samples:android-lib-kotlin-sample")
include(":samples:js")
include(":samples:native")
include(":samples:wire-codegen-sample")
include(":samples:wire-grpc-sample:client")
include(":samples:wire-grpc-sample:protos")
include(":samples:wire-grpc-sample:server")
include(":samples:wire-grpc-sample:server-plain")
include(":wire-benchmarks")
include(":wire-golden-files")
include(":wire-gradle-plugin-playground")
include(":wire-grpc-tests")
include(":wire-protoc-compatibility-tests")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
