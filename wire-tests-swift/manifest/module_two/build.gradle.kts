plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  sourceCompatibility = SwiftVersion.SWIFT5
  dependencies {
    api(projects.wireRuntimeSwift)
    implementation(projects.wireTestsSwift.manifest.moduleOne)
  }

  module.set("module_two")

  source.from(file("."))
}

tasks.matching { it.name == "compileDebugSwift" }.configureEach {
  dependsOn("compileReleaseSwift", "linkRelease")
}
tasks.getByName("spotlessJava").dependsOn("compileDebugSwift")
tasks.getByName("spotlessKotlin").dependsOn("compileDebugSwift")
tasks.getByName("spotlessSwift").dependsOn("compileDebugSwift")

