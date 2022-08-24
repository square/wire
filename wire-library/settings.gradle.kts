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
include(":wire-protoc")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs").from(files("../gradle/libs.versions.toml"))
  }
}
