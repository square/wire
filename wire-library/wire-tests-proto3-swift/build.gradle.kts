plugins {
  id("swift-library")
  id("xcode")
  id("xctest")
}

library {
  dependencies {
    api(project(":wire-runtime-swift"))
  }

  module.set("WireTestsProto3")
}