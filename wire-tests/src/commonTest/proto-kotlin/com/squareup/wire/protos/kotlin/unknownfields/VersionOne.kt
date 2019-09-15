// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: unknown_fields.proto
package com.squareup.wire.protos.kotlin.unknownfields

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class VersionOne(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val i: Int? = null,
  @field:WireField(
    tag = 7,
    adapter = "com.squareup.wire.protos.kotlin.unknownfields.NestedVersionOne#ADAPTER"
  )
  val obj: NestedVersionOne? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<VersionOne, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is VersionOne) return false
    return unknownFields == other.unknownFields
        && i == other.i
        && obj == other.obj
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = i.hashCode()
      result = result * 37 + obj.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (i != null) result += """i=$i"""
    if (obj != null) result += """obj=$obj"""
    return result.joinToString(prefix = "VersionOne{", separator = ", ", postfix = "}")
  }

  fun copy(
    i: Int? = this.i,
    obj: NestedVersionOne? = this.obj,
    unknownFields: ByteString = this.unknownFields
  ): VersionOne = VersionOne(i, obj, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<VersionOne> = object : ProtoAdapter<VersionOne>(
      FieldEncoding.LENGTH_DELIMITED, 
      VersionOne::class
    ) {
      override fun encodedSize(value: VersionOne): Int = 
        ProtoAdapter.INT32.encodedSizeWithTag(1, value.i) +
        NestedVersionOne.ADAPTER.encodedSizeWithTag(7, value.obj) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: VersionOne) {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i)
        NestedVersionOne.ADAPTER.encodeWithTag(writer, 7, value.obj)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): VersionOne {
        var i: Int? = null
        var obj: NestedVersionOne? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> i = ProtoAdapter.INT32.decode(reader)
            7 -> obj = NestedVersionOne.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return VersionOne(
          i = i,
          obj = obj,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: VersionOne): VersionOne = value.copy(
        obj = value.obj?.let(NestedVersionOne.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
