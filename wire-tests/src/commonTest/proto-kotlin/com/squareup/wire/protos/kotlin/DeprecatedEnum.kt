// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: deprecated_enum.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.EnumAdapter
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireEnum
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

enum class DeprecatedEnum(
  override val value: Int,
  val deprecated: Boolean?
) : WireEnum {
  @Deprecated(message = "DISABLED is deprecated")
  DISABLED(1, true),

  @Deprecated(message = "ENABLED is deprecated")
  ENABLED(2, true),

  ON(3, false),

  OFF(4, null);

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<DeprecatedEnum> = object : EnumAdapter<DeprecatedEnum>(
      DeprecatedEnum::class
    ) {
      override fun fromValue(value: Int): DeprecatedEnum? = DeprecatedEnum.fromValue(value)
    }

    @JvmStatic
    fun fromValue(value: Int): DeprecatedEnum? = when (value) {
      1 -> DISABLED
      2 -> ENABLED
      3 -> ON
      4 -> OFF
      else -> null
    }
  }
}
