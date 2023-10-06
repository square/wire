plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  dependencies {
    api(projects.wireRuntimeSwift)
  }

  module.set("WireTests")
}
