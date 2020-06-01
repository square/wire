// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: edge_cases.proto
package com.squareup.wire.protos.kotlin.edgecases

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

class NoFields(
  unknownFields: ByteString = ByteString.EMPTY
) : Message<NoFields, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is NoFields) return false
    var result = unknownFields == other.unknownFields
    return result
  }

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "NoFields{}"

  fun copy(unknownFields: ByteString = this.unknownFields): NoFields = NoFields(unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<NoFields> = object : ProtoAdapter<NoFields>(
      FieldEncoding.LENGTH_DELIMITED, 
      NoFields::class, 
      "type.googleapis.com/squareup.protos.kotlin.edgecases.NoFields"
    ) {
      override fun encodedSize(value: NoFields): Int {
        var size = value.unknownFields.size
        return size
      }

      override fun encode(writer: ProtoWriter, value: NoFields) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): NoFields {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return NoFields(
          unknownFields = unknownFields
        )
      }

      override fun redact(value: NoFields): NoFields = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
