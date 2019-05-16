/*
 * Copyright 2013 Square Inc.
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

import com.squareup.wire.internal.ObjectStreamException
import com.squareup.wire.internal.Serializable
import com.squareup.wire.internal.Throws
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.IOException
import kotlin.jvm.JvmField
import kotlin.jvm.Transient
import kotlin.reflect.KClass

/** A protocol buffer message.  */
abstract class Message<M : Message<M, B>, B : Message.Builder<M, B>> protected constructor(
  @field:Transient internal val adapter: ProtoAdapter<M>,
  /** Unknown fields, proto-encoded. We permit null to support magic deserialization.  */
  @field:Transient private val unknownFields: ByteString?
) : Serializable {
  /** If not `0` then the serialized size of this message.  */
  @Transient internal var cachedSerializedSize = 0

  /** If non-zero, the hash code of this message. Accessed by generated code.  */
  @Transient @JvmField protected var cachedHashCode = 0

  init {
    if (unknownFields == null) throw NullPointerException("unknownFields == null")
  }

  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  fun unknownFields(): ByteString = unknownFields ?: ByteString.EMPTY

  /**
   * Returns a new builder initialized with the data in this message.
   */
  abstract fun newBuilder(): B

  /** Returns this message with any unknown fields removed.  */
  fun withoutUnknownFields(): M = newBuilder().clearUnknownFields().build()

  override fun toString(): String = adapter.toString(this as M)

  @Throws(ObjectStreamException::class)
  protected fun writeReplace(): Any = MessageSerializedForm(encode(), this::class as KClass<M>)

  /** The [ProtoAdapter] for encoding and decoding messages of this type. */
  fun adapter(): ProtoAdapter<M> = adapter

  /** Encode this message and write it to `stream`.  */
  @Throws(IOException::class)
  fun encode(sink: BufferedSink) {
    adapter.encode(sink, this as M)
  }

  /** Encode this message as a `byte[]`.  */
  fun encode(): ByteArray = adapter.encode(this as M)

  /**
   * Superclass for protocol buffer message builders.
   */
  abstract class Builder<M : Message<M, B>, B : Builder<M, B>> protected constructor() {
    /**
     * Caches unknown fields as a [ByteString] when [buildUnknownFields] is called.
     * When the caller adds an additional unknown field after that, it will be written to the new
     * [unknownFieldsBuffer] to ensure that all unknown fields are retained between calls to
     * [buildUnknownFields].
     */
    @Transient internal var unknownFieldsByteString = ByteString.EMPTY
    /**
     * [Buffer] of the message's unknown fields that is lazily instantiated between calls to
     * [buildUnknownFields]. It's automatically cleared in [buildUnknownFields], and can also be
     * manually cleared by calling [clearUnknownFields].
     */
    @Transient internal var unknownFieldsBuffer: Buffer? = null
    @Transient internal var unknownFieldsWriter: ProtoWriter? = null

    fun addUnknownFields(unknownFields: ByteString): Builder<M, B> = apply {
      if (unknownFields.size > 0) {
        prepareForNewUnknownFields()
        unknownFieldsWriter!!.writeBytes(unknownFields)
      }
    }

    fun addUnknownField(
      tag: Int,
      fieldEncoding: FieldEncoding,
      value: Any?
    ): Builder<M, B> = apply {
      prepareForNewUnknownFields()
      val protoAdapter = fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>
      protoAdapter.encodeWithTag(unknownFieldsWriter!!, tag, value)
    }

    fun clearUnknownFields(): Builder<M, B> = apply {
      unknownFieldsByteString = ByteString.EMPTY
      if (unknownFieldsBuffer != null) {
        unknownFieldsBuffer!!.clear()
        unknownFieldsBuffer = null
      }
      unknownFieldsWriter = null
    }

    /**
     * Returns a byte string with this message's unknown fields. Returns an empty byte string if
     * this message has no unknown fields.
     */
    fun buildUnknownFields(): ByteString {
      if (unknownFieldsBuffer != null) {
        // Reads and caches the unknown fields from the buffer.
        unknownFieldsByteString = unknownFieldsBuffer!!.readByteString()
        unknownFieldsBuffer = null
        unknownFieldsWriter = null
      }
      return unknownFieldsByteString
    }

    /** Returns an immutable [Message] based on the fields that set in this builder. */
    abstract fun build(): M

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
    private const val serialVersionUID = 0L
  }
}
