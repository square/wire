
plugins {
  java
  kotlin("jvm")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(libs.protobuf.java)
//  implementation(libs.wire.schema)
  implementation("com.squareup.wire:wire-schema:4.4.1")
}
