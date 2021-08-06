plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  protoLibrary = true
  includeDescriptors = true

  kotlin {
    protoPath {
      srcDir("${project(":samples:wire-descriptorgen-sample:point-protos").projectDir}/src/main/proto")
    }
  }
}

dependencies {
  api(project(":samples:wire-descriptorgen-sample:point-protos"))
  implementation(deps.wire.descriptor)
}
