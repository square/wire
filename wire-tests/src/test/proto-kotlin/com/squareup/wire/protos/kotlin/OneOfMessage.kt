// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: one_of.proto
package com.squareup.wire.protos.kotlin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import java.lang.UnsupportedOperationException
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * It's a one of message.
 */
data class OneOfMessage(val choice: Choice? = null, val unknownFields: ByteString =
    ByteString.EMPTY) : Message<OneOfMessage, OneOfMessage.Builder>(ADAPTER, unknownFields) {
  @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Builder = Builder(this.copy())

  class Builder(private val message: OneOfMessage) : Message.Builder<OneOfMessage, Builder>() {
    override fun build(): OneOfMessage = message
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<OneOfMessage> = object : ProtoAdapter<OneOfMessage>(
      FieldEncoding.LENGTH_DELIMITED, 
      OneOfMessage::class
    ) {
      override fun encodedSize(value: OneOfMessage): Int = 
        when (value.choice) {
          is Choice.Foo -> ProtoAdapter.INT32.encodedSizeWithTag(1, value.choice.foo)
          is Choice.Bar -> ProtoAdapter.STRING.encodedSizeWithTag(3, value.choice.bar)
          is Choice.Baz -> ProtoAdapter.STRING.encodedSizeWithTag(4, value.choice.baz)
          else -> 0
        } +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: OneOfMessage) {
        when (value.choice) {
          is Choice.Foo -> ProtoAdapter.INT32.encodeWithTag(writer, 1, value.choice.foo)
          is Choice.Bar -> ProtoAdapter.STRING.encodeWithTag(writer, 3, value.choice.bar)
          is Choice.Baz -> ProtoAdapter.STRING.encodeWithTag(writer, 4, value.choice.baz)
        }
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): OneOfMessage {
        var choice: Choice? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> choice = Choice.Foo(ProtoAdapter.INT32.decode(reader))
            3 -> choice = Choice.Bar(ProtoAdapter.STRING.decode(reader))
            4 -> choice = Choice.Baz(ProtoAdapter.STRING.decode(reader))
            else -> TagHandler.UNKNOWN_TAG
          }
        }
        return OneOfMessage(
          choice = choice,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: OneOfMessage): OneOfMessage {
        throw
            UnsupportedOperationException("Redacting messages with oneof fields is not supported yet!")
      }
    }
  }

  sealed class Choice {
    data class Foo(val foo: Int) : Choice()

    data class Bar(val bar: String) : Choice()

    data class Baz(val baz: String) : Choice()
  }
}
