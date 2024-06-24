// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.custom_options.MessageWithOptions in custom_options.proto
@file:Suppress("DEPRECATION")

package com.squareup.wire.protos.custom_options

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.`internal`.JvmField
import com.squareup.wire.`internal`.JvmSynthetic
import com.squareup.wire.protos.kotlin.foreign.ForeignEnum
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import okio.ByteString

@MyMessageOptionTwoMessageOption(91011.0f)
@MyMessageOptionFourMessageOption(FooBar.FooBarBazEnum.FOO)
@MyMessageOptionSevenMessageOption(value = [
  33
])
@MyMessageOptionEightMessageOption(value = [
  "g",
  "h"
])
@MyMessageOptionNineMessageOption(value = [
  ForeignEnum.BAV
])
public class MessageWithOptions(
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<MessageWithOptions, MessageWithOptions.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is MessageWithOptions) return false
    if (unknownFields != other.unknownFields) return false
    return true
  }

  override fun hashCode(): Int = unknownFields.hashCode()

  override fun toString(): String = "MessageWithOptions{}"

  public fun copy(unknownFields: ByteString = this.unknownFields): MessageWithOptions =
      MessageWithOptions(unknownFields)

  public class Builder : Message.Builder<MessageWithOptions, Builder>() {
    override fun build(): MessageWithOptions = MessageWithOptions(
      unknownFields = buildUnknownFields()
    )
  }

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<MessageWithOptions> = object :
        ProtoAdapter<MessageWithOptions>(
      FieldEncoding.LENGTH_DELIMITED, 
      MessageWithOptions::class, 
      "type.googleapis.com/squareup.protos.custom_options.MessageWithOptions", 
      PROTO_2, 
      null, 
      "custom_options.proto"
    ) {
      override fun encodedSize(`value`: MessageWithOptions): Int {
        var size = value.unknownFields.size
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: MessageWithOptions) {
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: MessageWithOptions) {
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): MessageWithOptions {
        val unknownFields = reader.forEachTag(reader::readUnknownField)
        return MessageWithOptions(
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: MessageWithOptions): MessageWithOptions = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L

    @JvmSynthetic
    public inline fun build(body: Builder.() -> Unit): MessageWithOptions =
        Builder().apply(body).build()
  }
}
