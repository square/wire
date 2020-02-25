/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import okio.ByteString
import kotlin.jvm.JvmField

/**
 * TODO(oldergod): documentation
 * TODO(oldergod): JSON
 */
class AnyMessage(
  val typeUrl: String,
  val value: ByteString = ByteString.EMPTY
) : Message<AnyMessage, Nothing>(ADAPTER, ByteString.EMPTY) {

  fun <T> unpack(adapter: ProtoAdapter<T>): T {
    check(typeUrl == adapter.typeUrl) {
      "type mismatch: $typeUrl != ${adapter.typeUrl}"
    }
    return adapter.decode(value)
  }

  fun <T> unpackOrNull(adapter: ProtoAdapter<T>): T? {
    return if (typeUrl == adapter.typeUrl) adapter.decode(value) else null
  }

  @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is AnyMessage) return false
    return typeUrl == other.typeUrl && value == other.value
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = result * 37 + typeUrl.hashCode()
      result = result * 37 + value.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString() = "Any{type_url=$typeUrl, value=$value}"

  fun copy(
    typeUrl: String = this.typeUrl,
    value: ByteString = this.value
  ) = AnyMessage(typeUrl, value)

  companion object {
    fun pack(message: Message<*, *>): AnyMessage {
      val typeUrl = message.adapter.typeUrl
          ?: error("recompile ${message::class} to use it with AnyMessage")
      return AnyMessage(typeUrl, message.encodeByteString())
    }

    @JvmField
    val ADAPTER: ProtoAdapter<AnyMessage> = object : ProtoAdapter<AnyMessage>(
        FieldEncoding.LENGTH_DELIMITED,
        AnyMessage::class,
        "type.googleapis.com/google.protobuf.Any"
    ) {
      override fun encodedSize(value: AnyMessage): Int =
          STRING.encodedSizeWithTag(1, value.typeUrl) +
              BYTES.encodedSizeWithTag(2, value.value)

      override fun encode(writer: ProtoWriter, value: AnyMessage) {
        STRING.encodeWithTag(writer, 1, value.typeUrl)
        BYTES.encodeWithTag(writer, 2, value.value)
      }

      override fun decode(reader: ProtoReader): AnyMessage {
        var typeUrl = "square.github.io/wire/unknown" // TODO: what does protoc do?
        var value = ByteString.EMPTY
        reader.forEachTag { tag ->
          when (tag) {
            1 -> typeUrl = STRING.decode(reader)
            2 -> value = BYTES.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return AnyMessage(typeUrl = typeUrl, value = value)
      }

      // TODO: this is a hazard.
      override fun redact(value: AnyMessage) =
          AnyMessage("square.github.io/wire/redacted", ByteString.EMPTY)
    }
  }
}
