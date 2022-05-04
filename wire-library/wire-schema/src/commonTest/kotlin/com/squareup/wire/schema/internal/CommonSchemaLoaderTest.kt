/*
 * Copyright 2022 Block Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Path
import okio.fakefilesystem.FakeFileSystem

class CommonSchemaLoaderTest {
  private val fs = FakeFileSystem().apply {
    if (Path.DIRECTORY_SEPARATOR == "\\") emulateWindows() else emulateUnix()
  }

  @Test
  fun locationsToCheck() {
    val newSchemaLoader = CommonSchemaLoader(fs)
    val result = newSchemaLoader.locationsToCheck(
      "java",
      listOf(
        Location.get("shared-protos.jar", "squareup/cash/money/Money.proto"),
        Location.get("src/main/proto", "squareup/cash/Service.proto"),
        Location.get("src/main/proto", "squareup/cash/cashtags/Cashtag.proto"),
        Location.get("src/main/proto", "squareup/cash/payments/Payment.proto")
      )
    )
    assertEquals(
      setOf(
        Location.get("shared-protos.jar", "java.wire"),
        Location.get("shared-protos.jar", "squareup/cash/java.wire"),
        Location.get("shared-protos.jar", "squareup/cash/money/java.wire"),
        Location.get("shared-protos.jar", "squareup/java.wire"),
        Location.get("src/main/proto", "java.wire"),
        Location.get("src/main/proto", "squareup/cash/cashtags/java.wire"),
        Location.get("src/main/proto", "squareup/cash/java.wire"),
        Location.get("src/main/proto", "squareup/cash/payments/java.wire"),
        Location.get("src/main/proto", "squareup/java.wire"),
      ),
      result
    )
  }

  @Test
  fun pathsToAttempt() {
    val newSchemaLoader = CommonSchemaLoader(fs)
    val result = newSchemaLoader.locationsToCheck(
      "android",
      listOf(
        Location.get("/a/b", "c/d/e.proto")
      )
    )
    assertEquals(
      setOf(
        Location.get("/a/b", "c/d/android.wire"),
        Location.get("/a/b", "c/android.wire"),
        Location.get("/a/b", "android.wire")
      ), result
    )
  }

  @Test
  fun pathsToAttemptMultipleRoots() {
    val newSchemaLoader = CommonSchemaLoader(fs)
    val result = newSchemaLoader.locationsToCheck(
      "android",
      listOf(
        Location.get("/a/b", "c/d/e.proto"),
        Location.get("/a/b", "c/f/g/h.proto"),
        Location.get("/i/j.zip", "k/l/m.proto"),
        Location.get("/i/j.zip", "k/l/m/n.proto")
      )
    )
    assertEquals(
      setOf(
        Location.get("/a/b", "c/d/android.wire"),
        Location.get("/a/b", "c/android.wire"),
        Location.get("/a/b", "android.wire"),
        Location.get("/a/b", "c/f/g/android.wire"),
        Location.get("/a/b", "c/f/android.wire"),
        Location.get("/i/j.zip", "k/l/android.wire"),
        Location.get("/i/j.zip", "k/android.wire"),
        Location.get("/i/j.zip", "android.wire"),
        Location.get("/i/j.zip", "k/l/m/android.wire")
      ), result
    )
  }
}
