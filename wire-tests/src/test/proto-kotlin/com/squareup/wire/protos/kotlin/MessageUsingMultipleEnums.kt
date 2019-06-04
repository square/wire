// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: same_name_enum.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import com.squareup.wire.WireField
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * Enum names must be fully qualified in generated Kotlin
 */
data class MessageUsingMultipleEnums(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.protos.kotlin.MessageWithStatus.Status#ADAPTER"
  )
  val a: MessageWithStatus.Status? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.protos.kotlin.OtherMessageWithStatus.Status#ADAPTER"
  )
  val b: OtherMessageWithStatus.Status? = null,
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<MessageUsingMultipleEnums, MessageUsingMultipleEnums.Builder>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Builder = Builder(this.copy())

  class Builder(
    private val message: MessageUsingMultipleEnums
  ) : Message.Builder<MessageUsingMultipleEnums, Builder>() {
    override fun build(): MessageUsingMultipleEnums = message
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<MessageUsingMultipleEnums> = object :
        ProtoAdapter<MessageUsingMultipleEnums>(
      FieldEncoding.LENGTH_DELIMITED, 
      MessageUsingMultipleEnums::class
    ) {
      override fun encodedSize(value: MessageUsingMultipleEnums): Int = 
        MessageWithStatus.Status.ADAPTER.encodedSizeWithTag(1, value.a) +
        OtherMessageWithStatus.Status.ADAPTER.encodedSizeWithTag(2, value.b) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: MessageUsingMultipleEnums) {
        MessageWithStatus.Status.ADAPTER.encodeWithTag(writer, 1, value.a)
        OtherMessageWithStatus.Status.ADAPTER.encodeWithTag(writer, 2, value.b)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): MessageUsingMultipleEnums {
        var a: MessageWithStatus.Status? = null
        var b: OtherMessageWithStatus.Status? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> a = MessageWithStatus.Status.ADAPTER.decode(reader)
            2 -> b = OtherMessageWithStatus.Status.ADAPTER.decode(reader)
            else -> TagHandler.UNKNOWN_TAG
          }
        }
        return MessageUsingMultipleEnums(
          a = a,
          b = b,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: MessageUsingMultipleEnums): MessageUsingMultipleEnums = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
