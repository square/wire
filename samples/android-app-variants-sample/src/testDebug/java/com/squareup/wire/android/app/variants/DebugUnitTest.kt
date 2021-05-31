package com.squareup.wire.android.app.variants

import org.junit.Test

import org.junit.Assert.*

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
