plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  sourceCompatibility = SwiftVersion.SWIFT6
  dependencies {
    api(projects.wireRuntimeSwift)
  }

  module.set("WireTestsProto3")
}
