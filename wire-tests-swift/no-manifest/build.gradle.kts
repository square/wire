plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  sourceCompatibility = SwiftVersion.SWIFT5
  dependencies {
    api(projects.wireRuntimeSwift)
  }

  module.set("WireTests")
}
