package com.squareup.wire

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatArrayListTest {
  @Test
  fun getTruncatedArrayReturnsCorrectlySizedArray() {
    val arrayList = FloatArrayList(0)
    arrayList.add(1f)
    arrayList.add(2f)
    arrayList.add(3f)

    val array = arrayList.getTruncatedArray()
    assertEquals(array.size, 3)
    for (i in 0..2) {
      assertEquals(array[i], (i + 1).toFloat())
    }
  }

  @Test
  fun toStringIsReasonable() {
    val arrayList = FloatArrayList(0)
    arrayList.add(1f)
    arrayList.add(2f)
    arrayList.add(3f)

    assertEquals(arrayList.toString(), "[1.0, 2.0, 3.0]")
  }
}
