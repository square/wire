plugins {
  kotlin("jvm")
  id("com.squareup.wire")
}

wire {
  protoLibrary = true
  includeDescriptors = true

  java {}
}

dependencies {
  implementation(deps.wire.descriptor)
}
