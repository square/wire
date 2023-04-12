package com.squareup.wire

import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = DoubleArrayList(0)
    arrayList.add(1.0)
    arrayList.add(2.0)
    arrayList.add(3.0)

    val array = arrayList.getTruncatedArray()
    assertEquals(array.size, 3)
    for (i in 0..2) {
      assertEquals(array[i], (i + 1).toDouble())
    }
  }

  @Test
  fun toStringIsReasonable() {
    val arrayList = DoubleArrayList(0)
    arrayList.add(1.0)
    arrayList.add(2.0)
    arrayList.add(3.0)

    assertEquals(arrayList.toString(), "[1.0, 2.0, 3.0]")
  }
}
