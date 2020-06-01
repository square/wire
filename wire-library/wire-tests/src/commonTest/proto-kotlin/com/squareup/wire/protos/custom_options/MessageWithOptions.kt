// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: custom_options.proto
package com.squareup.wire.protos.custom_options

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

class MessageWithOptions(
  unknownFields: ByteString = ByteString.EMPTY
) : Message<MessageWithOptions, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is MessageWithOptions) return false
    var result = unknownFields == other.unknownFields
    return result
  }

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "MessageWithOptions{}"

  fun copy(unknownFields: ByteString = this.unknownFields): MessageWithOptions =
      MessageWithOptions(unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<MessageWithOptions> = object : ProtoAdapter<MessageWithOptions>(
      FieldEncoding.LENGTH_DELIMITED, 
      MessageWithOptions::class, 
      "type.googleapis.com/squareup.protos.custom_options.MessageWithOptions"
    ) {
      override fun encodedSize(value: MessageWithOptions): Int {
        var size = value.unknownFields.size
        return size
      }

      override fun encode(writer: ProtoWriter, value: MessageWithOptions) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): MessageWithOptions {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return MessageWithOptions(
          unknownFields = unknownFields
        )
      }

      override fun redact(value: MessageWithOptions): MessageWithOptions = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
