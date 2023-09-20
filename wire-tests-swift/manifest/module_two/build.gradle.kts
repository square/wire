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

  module.set("module_two")

  source.from(file("."))
}

tasks.matching { it.name == "compileReleaseSwift" }.configureEach {
  dependsOn("compileDebugSwift")
}
tasks.getByName("spotlessJava").dependsOn("compileDebugSwift")
tasks.getByName("spotlessKotlin").dependsOn("compileDebugSwift")
tasks.getByName("spotlessSwift").dependsOn("compileDebugSwift")

