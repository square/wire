// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.NoFields in no_fields.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
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

@Deprecated(message = "NoFields is deprecated")
public class NoFields(
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<NoFields, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw
      AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is NoFields) return false
    if (unknownFields != other.unknownFields) return false
    return true
  }

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "NoFields{}"

  public fun copy(unknownFields: ByteString = this.unknownFields): NoFields =
      NoFields(unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<NoFields> = object : ProtoAdapter<NoFields>(
      FieldEncoding.LENGTH_DELIMITED, 
      NoFields::class, 
      "type.googleapis.com/squareup.protos.kotlin.NoFields", 
      PROTO_2, 
      null, 
      "no_fields.proto"
    ) {
      override fun encodedSize(`value`: NoFields): Int {
        var size = value.unknownFields.size
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: NoFields) {
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: NoFields) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): NoFields {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return NoFields(
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: NoFields): NoFields = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
