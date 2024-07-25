// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.VersionOne in unknown_fields.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin.unknownfields

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Suppress
import okio.ByteString

public class VersionOne(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    schemaIndex = 0,
  )
  public val i: Int? = null,
  @field:WireField(
    tag = 7,
    adapter = "com.squareup.wire.protos.kotlin.unknownfields.NestedVersionOne#ADAPTER",
    schemaIndex = 1,
  )
  public val obj: NestedVersionOne? = null,
  @field:WireField(
    tag = 8,
    adapter = "com.squareup.wire.protos.kotlin.unknownfields.EnumVersionOne#ADAPTER",
    schemaIndex = 2,
  )
  public val en: EnumVersionOne? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<VersionOne, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw
      AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is VersionOne) return false
    if (unknownFields != other.unknownFields) return false
    if (i != other.i) return false
    if (obj != other.obj) return false
    if (en != other.en) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (i?.hashCode() ?: 0)
      result = result * 37 + (obj?.hashCode() ?: 0)
      result = result * 37 + (en?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (i != null) result += """i=$i"""
    if (obj != null) result += """obj=$obj"""
    if (en != null) result += """en=$en"""
    return result.joinToString(prefix = "VersionOne{", separator = ", ", postfix = "}")
  }

  public fun copy(
    i: Int? = this.i,
    obj: NestedVersionOne? = this.obj,
    en: EnumVersionOne? = this.en,
    unknownFields: ByteString = this.unknownFields,
  ): VersionOne = VersionOne(i, obj, en, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<VersionOne> = object : ProtoAdapter<VersionOne>(
      FieldEncoding.LENGTH_DELIMITED, 
      VersionOne::class, 
      "type.googleapis.com/squareup.protos.kotlin.unknownfields.VersionOne", 
      PROTO_2, 
      null, 
      "unknown_fields.proto"
    ) {
      override fun encodedSize(`value`: VersionOne): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.INT32.encodedSizeWithTag(1, value.i)
        size += NestedVersionOne.ADAPTER.encodedSizeWithTag(7, value.obj)
        size += EnumVersionOne.ADAPTER.encodedSizeWithTag(8, value.en)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: VersionOne) {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i)
        NestedVersionOne.ADAPTER.encodeWithTag(writer, 7, value.obj)
        EnumVersionOne.ADAPTER.encodeWithTag(writer, 8, value.en)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: VersionOne) {
        writer.writeBytes(value.unknownFields)
        EnumVersionOne.ADAPTER.encodeWithTag(writer, 8, value.en)
        NestedVersionOne.ADAPTER.encodeWithTag(writer, 7, value.obj)
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i)
      }

      override fun decode(reader: ProtoReader): VersionOne {
        var i: Int? = null
        var obj: NestedVersionOne? = null
        var en: EnumVersionOne? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> i = ProtoAdapter.INT32.decode(reader)
            7 -> obj = NestedVersionOne.ADAPTER.decode(reader)
            8 -> try {
              en = EnumVersionOne.ADAPTER.decode(reader)
            } catch (e: ProtoAdapter.EnumConstantNotFoundException) {
              reader.addUnknownField(tag, FieldEncoding.VARINT, e.value.toLong())
            }
            else -> reader.readUnknownField(tag)
          }
        }
        return VersionOne(
          i = i,
          obj = obj,
          en = en,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: VersionOne): VersionOne = value.copy(
        obj = value.obj?.let(NestedVersionOne.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
