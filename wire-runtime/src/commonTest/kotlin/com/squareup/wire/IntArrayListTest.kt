package com.squareup.wire

import kotlin.test.Test
import kotlin.test.assertEquals

class IntArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = IntArrayList(0)
    arrayList.add(1)
    arrayList.add(2)
    arrayList.add(3)

    val array = arrayList.getTruncatedArray()
    assertEquals(array.size, 3)
    for (i in 0..2) {
      assertEquals(array[i], i + 1)
    }
  }

  @Test
  fun toStringIsReasonable() {
    val arrayList = IntArrayList(0)
    arrayList.add(1)
    arrayList.add(2)
    arrayList.add(3)

    assertEquals(arrayList.toString(), "[1, 2, 3]")
  }
}
