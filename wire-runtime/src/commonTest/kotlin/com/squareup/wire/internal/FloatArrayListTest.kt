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
package com.squareup.wire.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = FloatArrayList(0)
    arrayList.add(1f)
    arrayList.add(2f)
    arrayList.add(3f)

    val array = arrayList.toArray()
    assertEquals(array.size, 3)
    for (i in 0..2) {
      assertEquals(array[i], (i + 1).toFloat())
    }
  }

  @Test
  fun getStringDelegatesToTheUnderlyingArray() {
    val arrayList = FloatArrayList(0)
    arrayList.add(1f)
    arrayList.add(2f)
    arrayList.add(3f)

    assertEquals(arrayList.toString(), floatArrayOf(1f, 2f, 3f).contentToString())
  }
}
