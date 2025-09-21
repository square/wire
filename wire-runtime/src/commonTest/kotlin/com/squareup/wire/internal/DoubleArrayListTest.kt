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

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DoubleArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = DoubleArrayList(0)
    arrayList.add(1.0)
    arrayList.add(2.0)
    arrayList.add(3.0)

    val array = arrayList.toArray()
    assertThat(array).hasSize(3)
    for (i in 0..2) {
      assertThat((i + 1).toDouble()).isEqualTo(array[i])
    }
  }

  @Test
  fun getStringDelegatesToTheUnderlyingArray() {
    val arrayList = DoubleArrayList(0)
    arrayList.add(1.0)
    arrayList.add(2.0)
    arrayList.add(3.0)

    assertThat(
      doubleArrayOf(
        1.0,
        2.0,
        3.0,
      ).contentToString(),
    ).isEqualTo(arrayList.toString())
  }
}
