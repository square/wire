/*
 * Copyright 2021 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.protobuf")
}

protobuf {
  protoc {
    artifact = deps.protobuf.protoc
  }
}

dependencies {
  api(deps.moshi)
  api(project(":wire-runtime"))
  api(project(":wire-schema"))
  implementation(project(":wire-compiler"))
  implementation(project(":wire-java-generator"))
  implementation(project(":wire-kotlin-generator"))
  implementation(project(":wire-profiles"))
  implementation(deps.assertj)
  implementation(deps.guava)
  implementation(deps.jimfs)
  implementation(deps.junit)
  implementation(deps.protobuf.java)
  implementation(deps.okio.jvm)
  implementation(deps.okio.fakefilesystem)
}
