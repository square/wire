include(":samples:simple-sample")
include(":samples:android-app-java-sample")
include(":samples:android-app-kotlin-sample")
include(":samples:android-app-variants-sample")
include(":samples:android-lib-java-sample")
include(":samples:android-lib-kotlin-sample")
include(":samples:wire-codegen-sample")
include(":samples:wire-grpc-sample:client")
include(":samples:wire-grpc-sample:protos")
include(":samples:wire-grpc-sample:server")
include(":samples:wire-grpc-sample:server-plain")
include(":wire-benchmarks")
include(":wire-gradle-plugin-playground")
include(":wire-grpc-tests")
include(":wire-protoc-compatibility-tests")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("wire-library") {
  // Declaring dependency substitutions explicitly ensures that references to types declared inside
  // wire-library are resolved properly by the IDE. It's unclear to me why this is the case: it
  // might be either a known composite builds limitation (as described in
  // https://docs.gradle.org/current/userguide/composite_builds.html), or a known or unknown Kotlin
  // Gradle plugin bug.
  //
  // TODO(egor): Find out what the underlying issue is and what is the best fix/workaround.

  dependencySubstitution {
    substitute(module("com.squareup.wire:wire-compiler")).using(project(":wire-compiler"))
    substitute(module("com.squareup.wire:wire-gradle-plugin")).using(project(":wire-gradle-plugin"))
    substitute(module("com.squareup.wire:wire-grpc-client")).using(project(":wire-grpc-client"))
    substitute(module("com.squareup.wire:wire-grpc-mockwebserver")).using(project(":wire-grpc-mockwebserver"))
    substitute(module("com.squareup.wire:wire-grpc-server")).using(project(":wire-grpc-server"))
    substitute(module("com.squareup.wire:wire-gson-support")).using(project(":wire-gson-support"))
    substitute(module("com.squareup.wire:wire-java-generator")).using(project(":wire-java-generator"))
    substitute(module("com.squareup.wire:wire-kotlin-generator")).using(project(":wire-kotlin-generator"))
    substitute(module("com.squareup.wire:wire-moshi-adapter")).using(project(":wire-moshi-adapter"))
    substitute(module("com.squareup.wire:wire-runtime")).using(project(":wire-runtime"))
    substitute(module("com.squareup.wire:wire-schema")).using(project(":wire-schema"))
    substitute(module("com.squareup.wire:wire-schema-tests")).using(project(":wire-schema-tests"))
    substitute(module("com.squareup.wire:wire-test-utils")).using(project(":wire-test-utils"))
  }
}
