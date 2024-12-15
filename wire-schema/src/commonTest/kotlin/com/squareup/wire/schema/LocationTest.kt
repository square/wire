/*
 * Copyright (C) 2024 Square, Inc.
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

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class LocationTest {

  @Test fun getWithBaseAndRelativePath() {
    val location = Location.get("/base/dir", "test/file.proto")
    assertThat(location.base).isEqualTo("/base/dir")
    assertThat(location.path).isEqualTo("test/file.proto")
  }

  @Test fun getWithWindowsStylePaths() {
    val location = Location.get(
      "C:\\Users\\protoDir",
      "languageDir\\language.proto",
    )
    assertThat(location.base).isEqualTo("C:/Users/protoDir")
    assertThat(location.path).isEqualTo("languageDir/language.proto")
  }
}
