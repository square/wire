rootProject.name = "build-support"

include(":wire-compiler")
project(":wire-compiler").projectDir = File("../wire-compiler")
include(":wire-gradle-plugin")
project(":wire-gradle-plugin").projectDir = File("../wire-gradle-plugin")
include(":wire-grpc-client")
project(":wire-grpc-client").projectDir = File("../wire-grpc-client")
include(":wire-gson-support")
project(":wire-gson-support").projectDir = File("../wire-gson-support")
include(":wire-java-generator")
project(":wire-java-generator").projectDir = File("../wire-java-generator")
include(":wire-kotlin-generator")
project(":wire-kotlin-generator").projectDir = File("../wire-kotlin-generator")
include(":wire-moshi-adapter")
project(":wire-moshi-adapter").projectDir = File("../wire-moshi-adapter")
include(":wire-reflector")
project(":wire-reflector").projectDir = File("../wire-reflector")
include(":wire-runtime")
project(":wire-runtime").projectDir = File("../wire-runtime")
include(":wire-schema-tests")
project(":wire-schema-tests").projectDir = File("../wire-schema-tests")
include(":wire-schema")
project(":wire-schema").projectDir = File("../wire-schema")
include(":wire-swift-generator")
project(":wire-swift-generator").projectDir = File("../wire-swift-generator")
include(":wire-test-utils")
project(":wire-test-utils").projectDir = File("../wire-test-utils")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs").from(files("../gradle/libs.versions.toml"))
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}
