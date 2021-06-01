package com.squareup.wire.android.app.variants

import org.junit.Test

import org.junit.Assert.*

class CommonUnitTest {
  @Test
  fun commonType() {
    val commonText = CommonText("name")
    val commonType = CommonType(32, "name")

    if (commonText.value_ != commonType.name) {
      throw AssertionError("BOOM!: ${commonText.value_} != ${commonType.name}")
    }
  }
}
