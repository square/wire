package com.squareup.wire

import kotlin.test.Test
import kotlin.test.assertEquals

class LongArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = LongArrayList(0)
    arrayList.add(1L)
    arrayList.add(2L)
    arrayList.add(3L)

    val array = arrayList.getTruncatedArray()
    assertEquals(array.size, 3)
    for (i in 0..2) {
      assertEquals(array[i], (i + 1).toLong())
    }
  }
}
