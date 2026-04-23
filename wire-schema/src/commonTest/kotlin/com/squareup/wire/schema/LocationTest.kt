/*
 * Copyright (C) 2026 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import kotlin.test.Test
import kotlin.test.assertEquals

class LocationTest {
  @Test
  fun getWithForwardSlashes() {
    val location = Location.get("base/dir", "sub/file.proto")
    assertEquals("base/dir", location.base)
    assertEquals("sub/file.proto", location.path)
  }

  @Test
  fun getTrimsTrailingSlashFromBase() {
    val location = Location.get("base/dir/", "file.proto")
    assertEquals("base/dir", location.base)
  }

  @Test
  fun getNormalizesWindowsBackslashesInBase() {
    val location = Location.get("C:\\Users\\username\\protos", "language\\language.proto")
    assertEquals("C:/Users/username/protos", location.base)
    assertEquals("language/language.proto", location.path)
  }

  @Test
  fun getNormalizesWindowsBackslashesInPath() {
    val location = Location.get("", "language\\language.proto")
    assertEquals("language/language.proto", location.path)
  }

  @Test
  fun getWithMixedSeparatorsInBase() {
    val location = Location.get("C:\\Users/username\\protos/", "file.proto")
    assertEquals("C:/Users/username/protos", location.base)
  }

  @Test
  fun getPreservesWindowsDriveRootBackslashInPath() {
    // okio requires 'C:\' (backslash) to recognize Windows absolute paths; 'C:/' is not supported
    // in older okio versions. Preserve the root backslash while converting internal separators.
    val location = Location.get("", "C:\\Users\\protos")
    assertEquals("C:\\Users/protos", location.path)
  }
}
