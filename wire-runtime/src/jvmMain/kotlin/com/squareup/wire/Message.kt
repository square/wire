/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import java.io.IOException
import java.io.ObjectStreamException
import java.io.OutputStream
import java.io.Serializable
import okio.Buffer
import okio.BufferedSink
import okio.ByteString

/** A protocol buffer message. */
actual abstract class Message<M : Message<M, B>, B : Message.Builder<M, B>>
protected actual constructor(
  /** The [ProtoAdapter] for encoding and decoding messages of this type. */
  @field:Transient
  @get:JvmName("adapter")
  actual val adapter: ProtoAdapter<M>,
  unknownFields: ByteString,
) : Serializable {
  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  @field:Transient
  @get:JvmName("unknownFields")
  @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
  actual open val unknownFields: ByteString = unknownFields
    get() {
      // Some naughty libraries construct Messages by reflection which causes this non-null field
      // to have a null value. We defend against this with an otherwise-redundant null check.
      @Suppress("SENSELESS_COMPARISON")
      if (field == null) return ByteString.EMPTY
      return field
    }

  /** If not `0` then the serialized size of this message. */
  @Transient internal var cachedSerializedSize = 0

  /** If non-zero, the hash code of this message. Accessed by generated code. */
  @Transient @JvmField
  protected actual var hashCode = 0

  /**
   * Returns a new builder initialized with the data in this message.
   */
  actual abstract fun newBuilder(): B

  /** Returns this message with any unknown fields removed. */
  fun withoutUnknownFields(): M = newBuilder().clearUnknownFields().build()

  @Suppress("UNCHECKED_CAST")
  override fun toString(): String = adapter.toString(this as M)

  @Suppress("UNCHECKED_CAST")
  @Throws(ObjectStreamException::class)
  protected fun writeReplace(): Any = MessageSerializedForm(encode(), javaClass as Class<M>)

  /** Encode this message and write it to `stream`. */
  @Throws(IOException::class)
  actual fun encode(sink: BufferedSink) {
    @Suppress("UNCHECKED_CAST")
    adapter.encode(sink, this as M)
  }

  /** Encode this message as a `byte[]`.  */
  @Suppress("UNCHECKED_CAST")
  actual fun encode(): ByteArray = adapter.encode(this as M)

  /** Encode this message as a `ByteString`. */
  actual fun encodeByteString(): ByteString {
    @Suppress("UNCHECKED_CAST")
    return adapter.encodeByteString(this as M)
  }

  /** Encode this message and write it to `stream`.  */
  @Throws(IOException::class)
  fun encode(stream: OutputStream) {
    @Suppress("UNCHECKED_CAST")
    adapter.encode(stream, this as M)
  }

  /**
   * Superclass for protocol buffer message builders.
   */
  actual abstract class Builder<M : Message<M, B>, B : Builder<M, B>> protected actual constructor() {
    @Transient internal actual var unknownFieldsByteString = ByteString.EMPTY

    @Transient internal actual var unknownFieldsBuffer: Buffer? = null

    @Transient internal actual var unknownFieldsWriter: ProtoWriter? = null

    actual fun addUnknownFields(unknownFields: ByteString): Builder<M, B> = apply {
      if (unknownFields.size > 0) {
        prepareForNewUnknownFields()
        unknownFieldsWriter!!.writeBytes(unknownFields)
      }
    }

    actual fun addUnknownField(
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any?,
    ): Builder<M, B> = apply {
      prepareForNewUnknownFields()
      @Suppress("UNCHECKED_CAST")
      val protoAdapter = fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>
      protoAdapter.encodeWithTag(unknownFieldsWriter!!, tag, value)
    }

    actual fun clearUnknownFields(): Builder<M, B> = apply {
      unknownFieldsByteString = ByteString.EMPTY
      if (unknownFieldsBuffer != null) {
        unknownFieldsBuffer!!.clear()
        unknownFieldsBuffer = null
      }
      unknownFieldsWriter = null
    }

    actual fun buildUnknownFields(): ByteString {
      if (unknownFieldsBuffer != null) {
        // Reads and caches the unknown fields from the buffer.
        unknownFieldsByteString = unknownFieldsBuffer!!.readByteString()
        unknownFieldsBuffer = null
        unknownFieldsWriter = null
      }
      return unknownFieldsByteString
    }

    actual abstract fun build(): M

    private fun prepareForNewUnknownFields() {
      if (unknownFieldsBuffer == null) {
        unknownFieldsBuffer = Buffer()
        unknownFieldsWriter = ProtoWriter(unknownFieldsBuffer!!)
        // Writes the cached unknown fields to the buffer.
        unknownFieldsWriter!!.writeBytes(unknownFieldsByteString)
        unknownFieldsByteString = ByteString.EMPTY
      }
    }
  }

  companion object {
    @Suppress("ConstPropertyName")
    private const val serialVersionUID = 0L
  }
}
