/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.android.app.variants

import org.junit.Assert.fail
import org.junit.Test

class ReleaseUnitTest {
  @Test
  fun commonType() {
    val commonText = CommonText("name")
    val commonType = CommonType(32, "name")

    if (commonText.value_ != commonType.name) {
      throw AssertionError("BOOM!: ${commonText.value_} != ${commonType.name}")
    }
  }

  @Test
  fun debugTypeIsAbsent() {
    java.lang.Class.forName("com.squareup.wire.android.app.variants.ReleaseType")
    try {
      java.lang.Class.forName("com.squareup.wire.android.app.variants.DebugType")
      fail()
    } catch (e: ClassNotFoundException) {
      // Expected exception.
    }
  }

  @Test
  fun releaseType() {
    val commonType = CommonType(32, "name")
    if (commonType.id != 32) {
      throw AssertionError("BOOM!: ${commonType.id} != 32")
    }

    val releaseType = ReleaseType(commonType, "payload", alpha = true)
    if (!releaseType.alpha) {
      throw AssertionError("BOOM!: releaseType.alpha is ${releaseType.alpha}")
    }
  }
}
