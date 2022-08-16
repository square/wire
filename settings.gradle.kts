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
include(":wire-protoc-integration")
include(":wire-reflector")
include(":wire-runtime:japicmp")
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

includeBuild("build-logic") {
  dependencySubstitution {
    substitute(module("com.squareup.wire:wire-gradle-plugin")).using(project(":wire-gradle-plugin"))
  }
}

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
include(":wire-golden-files")
include(":wire-gradle-plugin-playground")
include(":wire-grpc-tests")
include(":wire-protoc-compatibility-tests")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
