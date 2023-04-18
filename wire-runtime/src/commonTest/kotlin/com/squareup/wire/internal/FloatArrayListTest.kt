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

    val array = arrayList.getTruncatedArray()
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
