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
import com.squareup.wire.`internal`.JvmSynthetic
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import okio.ByteString

@Deprecated(message = "NoFields is deprecated")
public class NoFields(
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<NoFields, NoFields.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is NoFields) return false
    if (unknownFields != other.unknownFields) return false
    return true
  }

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "NoFields{}"

  public fun copy(unknownFields: ByteString = this.unknownFields): NoFields = NoFields(unknownFields)

  public class Builder : Message.Builder<NoFields, Builder>() {
    override fun build(): NoFields = NoFields(
      unknownFields = buildUnknownFields()
    )
  }

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

    @JvmSynthetic
    public inline fun build(body: Builder.() -> Unit): NoFields = Builder().apply(body).build()
  }
}
