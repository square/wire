package com.squareup.wire.android.app.variants

import org.junit.Test

import org.junit.Assert.*

class ReleaseUnitTest {
  @Test
  fun commonType() {
    val commonText = CommonText("name")
    val commonType = CommonType(32, "name")

    if (commonText.value_ != commonType.name) {
      throw AssertionError("BOOM!: ${commonText.value_} != ${commonType.name}")
    }
  }

  @Test
  fun debugTypeIsAbsent() {
    java.lang.Class.forName("com.squareup.wire.android.app.variants.ReleaseType")
    try {
      java.lang.Class.forName("com.squareup.wire.android.app.variants.DebugType")
      fail()
    } catch (e: ClassNotFoundException) {
      // Expected exception.
    }
  }

  @Test
  fun releaseType() {
    val commonType = CommonType(32, "name")
    if (commonType.id != 32) {
      throw AssertionError("BOOM!: ${commonType.id} != 32")
    }

    val releaseType = ReleaseType(commonType, "payload", alpha = true)
    if (!releaseType.alpha) {
      throw AssertionError("BOOM!: releaseType.alpha is ${releaseType.alpha}")
    }
  }
}
