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

class DebugUnitTest {
  @Test
  fun commonType() {
    val commonText = CommonText("name")
    val commonType = CommonType(32, "name")

    if (commonText.value_ != commonType.name) {
      throw AssertionError("BOOM!: ${commonText.value_} != ${commonType.name}")
    }
  }

  @Test
  fun releaseTypeIsAbsent() {
    java.lang.Class.forName("com.squareup.wire.android.app.variants.DebugType")
    try {
      java.lang.Class.forName("com.squareup.wire.android.app.variants.ReleaseType")
      fail()
    } catch (e: ClassNotFoundException) {
      // Expected exception.
    }
  }

  @Test
  fun debugType() {
    val commonType = CommonType(32, "name")
    if (commonType.id != 32) {
      throw AssertionError("BOOM!: ${commonType.id} != 32")
    }

    val debugType = DebugType(commonType, "debug")
    if (debugType.payload != "debug") {
      throw AssertionError("""BOOM!: ${debugType.payload} != "debug"""")
    }
  }
}
