plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  dependencies {
    api(projects.wireRuntimeSwift)
    implementation(projects.wireTestsSwift.manifest.moduleOne)
  }

  module.set("module_three")

  source.from(file("."))
}
