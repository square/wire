rootProject.name = "build-logic"

include(":wire-gradle-plugin")
project(":wire-gradle-plugin").projectDir = File("../wire-gradle-plugin")
include(":wire-runtime")
project(":wire-runtime").projectDir = File("../wire-runtime")
include(":wire-kotlin-generator")
project(":wire-kotlin-generator").projectDir = File("../wire-kotlin-generator")
include(":wire-test-utils")
project(":wire-test-utils").projectDir = File("../wire-test-utils")
include(":wire-schema")
project(":wire-schema").projectDir = File("../wire-schema")
include(":wire-grpc-client")
project(":wire-grpc-client").projectDir = File("../wire-grpc-client")
include(":wire-grpc-server-generator")
project(":wire-grpc-server-generator").projectDir = File("../wire-grpc-server-generator")
include(":wire-grpc-server")
project(":wire-grpc-server").projectDir = File("../wire-grpc-server")
include(":wire-schema-tests")
project(":wire-schema-tests").projectDir = File("../wire-schema-tests")
include(":wire-compiler")
project(":wire-compiler").projectDir = File("../wire-compiler")
include(":wire-java-generator")
project(":wire-java-generator").projectDir = File("../wire-java-generator")
include(":wire-swift-generator")
project(":wire-swift-generator").projectDir = File("../wire-swift-generator")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs").from(files("../gradle/libs.versions.toml"))
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
