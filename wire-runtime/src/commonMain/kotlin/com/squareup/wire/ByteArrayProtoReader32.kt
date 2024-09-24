// This class is derived from the CodedInputByteBuffer class in Google's "Nano" Protocol Buffer
// implementation. The original copyright notice, list of conditions, and disclaimer for those
// classes is as follows:

// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
// * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.squareup.wire

import com.squareup.wire.ProtoReader.Companion.FIELD_ENCODING_MASK
import com.squareup.wire.ProtoReader.Companion.RECURSION_LIMIT
import com.squareup.wire.ProtoReader.Companion.STATE_END_GROUP
import com.squareup.wire.ProtoReader.Companion.STATE_FIXED32
import com.squareup.wire.ProtoReader.Companion.STATE_FIXED64
import com.squareup.wire.ProtoReader.Companion.STATE_LENGTH_DELIMITED
import com.squareup.wire.ProtoReader.Companion.STATE_PACKED_TAG
import com.squareup.wire.ProtoReader.Companion.STATE_START_GROUP
import com.squareup.wire.ProtoReader.Companion.STATE_TAG
import com.squareup.wire.ProtoReader.Companion.STATE_VARINT
import com.squareup.wire.ProtoReader.Companion.TAG_FIELD_ENCODING_BITS
import com.squareup.wire.internal.ProtocolException
import com.squareup.wire.internal.and
import com.squareup.wire.internal.shl
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.EOFException
import okio.IOException

internal class ByteArrayProtoReader32(
  private val source: ByteArray,

  /** The current position in [source], starting at 0 and increasing monotonically. */
  private var pos: Int = 0,

  /** The absolute position of the end of the current message. */
  private var limit: Int = source.size,
) : ProtoReader32 {
  /** The current number of levels of message nesting. */
  private var recursionDepth = 0

  /** How to interpret the next read call. */
  private var state = STATE_LENGTH_DELIMITED

  /** The most recently read tag. Used to make packed values look like regular values. */
  private var tag = -1

  /** Limit once we complete the current length-delimited value. */
  private var pushedLimit: Int = -1

  /** The encoding of the next value to be read. */
  private var nextFieldEncoding: FieldEncoding? = null

  /** Pooled buffers for unknown fields, indexed by [recursionDepth]. */
  private val bufferStack = mutableListOf<Buffer>()

  /** Lazily-initialized. */
  private var protoReader: ProtoReader32AsProtoReader? = null

  override fun asProtoReader(): ProtoReader {
    return protoReader
      ?: ProtoReader32AsProtoReader(this).also { protoReader = it }
  }

  override fun beginMessage(): Int {
    check(state == STATE_LENGTH_DELIMITED) { "Unexpected call to beginMessage()" }
    if (++recursionDepth > RECURSION_LIMIT) {
      throw IOException("Wire recursion limit exceeded")
    }
    // Allocate a buffer to store unknown fields encountered at this recursion level.
    if (recursionDepth > bufferStack.size) bufferStack += Buffer()
    // Give the pushed limit to the caller to hold. The value is returned in endMessage() where we
    // resume using it as our limit.
    val token = pushedLimit
    pushedLimit = -1
    state = STATE_TAG
    return token
  }

  override fun endMessageAndGetUnknownFields(token: Int): ByteString {
    check(state == STATE_TAG) { "Unexpected call to endMessage()" }
    check(--recursionDepth >= 0 && pushedLimit == -1) { "No corresponding call to beginMessage()" }
    if (pos != limit && recursionDepth != 0) {
      throw IOException("Expected to end at $limit but was $pos")
    }
    limit = token
    val unknownFieldsBuffer = bufferStack[recursionDepth]
    return if (unknownFieldsBuffer.size > 0L) {
      unknownFieldsBuffer.readByteString()
    } else {
      ByteString.EMPTY
    }
  }

  override fun nextLengthDelimited(): Int {
    check(state == STATE_TAG || state == STATE_LENGTH_DELIMITED) {
      "Unexpected call to nextDelimited()"
    }
    return internalNextLengthDelimited()
  }

  private fun internalNextLengthDelimited(): Int {
    nextFieldEncoding = FieldEncoding.LENGTH_DELIMITED
    state = STATE_LENGTH_DELIMITED
    val length = internalReadVarint32()
    if (length < 0) throw ProtocolException("Negative length: $length")
    if (pushedLimit != -1) throw IllegalStateException()
    // Push the current limit, and set a new limit to the length of this value.
    pushedLimit = limit
    limit = pos + length
    if (limit > pushedLimit) throw EOFException()
    return length
  }

  override fun nextTag(): Int {
    if (state == STATE_PACKED_TAG) {
      state = STATE_LENGTH_DELIMITED
      return tag
    } else if (state != STATE_TAG) {
      throw IllegalStateException("Unexpected call to nextTag()")
    }

    loop@ while (pos < limit) {
      val tagAndFieldEncoding = internalReadVarint32()
      if (tagAndFieldEncoding == 0) throw ProtocolException("Unexpected tag 0")

      tag = tagAndFieldEncoding shr TAG_FIELD_ENCODING_BITS
      when (val groupOrFieldEncoding = tagAndFieldEncoding and FIELD_ENCODING_MASK) {
        STATE_START_GROUP -> {
          skipGroup(tag)
          continue@loop
        }

        STATE_END_GROUP -> throw ProtocolException("Unexpected end group")

        STATE_LENGTH_DELIMITED -> {
          internalNextLengthDelimited()
          return tag
        }

        STATE_VARINT -> {
          nextFieldEncoding = FieldEncoding.VARINT
          state = STATE_VARINT
          return tag
        }

        STATE_FIXED64 -> {
          nextFieldEncoding = FieldEncoding.FIXED64
          state = STATE_FIXED64
          return tag
        }

        STATE_FIXED32 -> {
          nextFieldEncoding = FieldEncoding.FIXED32
          state = STATE_FIXED32
          return tag
        }

        else -> throw ProtocolException("Unexpected field encoding: $groupOrFieldEncoding")
      }
    }
    return -1
  }

  override fun peekFieldEncoding(): FieldEncoding? = nextFieldEncoding

  override fun skip() {
    when (state) {
      STATE_LENGTH_DELIMITED -> {
        val byteCount = beforeLengthDelimitedScalar()
        skip(byteCount)
      }
      STATE_VARINT -> readVarint64()
      STATE_FIXED64 -> readFixed64()
      STATE_FIXED32 -> readFixed32()
      else -> throw IllegalStateException("Unexpected call to skip()")
    }
  }

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  private fun skipGroup(expectedEndTag: Int) {
    while (pos < limit) {
      val tagAndFieldEncoding = internalReadVarint32()
      if (tagAndFieldEncoding == 0) throw ProtocolException("Unexpected tag 0")
      val tag = tagAndFieldEncoding shr TAG_FIELD_ENCODING_BITS
      when (val groupOrFieldEncoding = tagAndFieldEncoding and FIELD_ENCODING_MASK) {
        STATE_START_GROUP -> {
          recursionDepth++
          try {
            if (recursionDepth > RECURSION_LIMIT) {
              throw IOException("Wire recursion limit exceeded")
            }
            // Nested group.
            skipGroup(tag)
          } finally {
            recursionDepth--
          }
        }
        STATE_END_GROUP -> {
          if (tag == expectedEndTag) return // Success!
          throw ProtocolException("Unexpected end group")
        }
        STATE_LENGTH_DELIMITED -> {
          val length = internalReadVarint32()
          skip(length)
        }
        STATE_VARINT -> {
          state = STATE_VARINT
          readVarint64()
        }
        STATE_FIXED64 -> {
          state = STATE_FIXED64
          readFixed64()
        }
        STATE_FIXED32 -> {
          state = STATE_FIXED32
          readFixed32()
        }
        else -> throw ProtocolException("Unexpected field encoding: $groupOrFieldEncoding")
      }
    }
    throw EOFException()
  }

  override fun readBytes(): ByteString {
    val byteCount = beforeLengthDelimitedScalar()
    return readByteString(byteCount)
  }

  override fun beforePossiblyPackedScalar(): Boolean {
    return when (state) {
      STATE_LENGTH_DELIMITED -> {
        if (pos < limit) {
          // It's packed and there's a value.
          true
        } else {
          // It's packed and there aren't any values.
          limit = pushedLimit
          pushedLimit = -1
          state = STATE_TAG
          false
        }
      }
      STATE_VARINT,
      STATE_FIXED64,
      STATE_FIXED32,
      -> true // Not packed.

      else -> throw ProtocolException("unexpected state: $state")
    }
  }

  override fun readString(): String {
    val byteCount = beforeLengthDelimitedScalar()
    return readUtf8(byteCount)
  }

  override fun readVarint32(): Int {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected VARINT or LENGTH_DELIMITED but was $state")
    }
    val result = internalReadVarint32()
    afterPackableScalar(STATE_VARINT)
    return result
  }

  private fun internalReadVarint32(): Int {
    var tmp = readByte()
    if (tmp >= 0) {
      return tmp.toInt()
    }
    var result = tmp and 0x7f
    tmp = readByte()
    if (tmp >= 0) {
      result = result or (tmp shl 7)
    } else {
      result = result or (tmp and 0x7f shl 7)
      tmp = readByte()
      if (tmp >= 0) {
        result = result or (tmp shl 14)
      } else {
        result = result or (tmp and 0x7f shl 14)
        tmp = readByte()
        if (tmp >= 0) {
          result = result or (tmp shl 21)
        } else {
          result = result or (tmp and 0x7f shl 21)
          tmp = readByte()
          result = result or (tmp shl 28)
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (i in 0..4) {
              if (readByte() >= 0) {
                return result
              }
            }
            throw ProtocolException("Malformed VARINT")
          }
        }
      }
    }
    return result
  }

  override fun readVarint64(): Long {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected VARINT or LENGTH_DELIMITED but was $state")
    }
    var shift = 0
    var result: Long = 0
    while (shift < 64) {
      val b = readByte()
      result = result or ((b and 0x7F).toLong() shl shift)
      if (b and 0x80 == 0) {
        afterPackableScalar(STATE_VARINT)
        return result
      }
      shift += 7
    }
    throw ProtocolException("WireInput encountered a malformed varint")
  }

  override fun readFixed32(): Int {
    if (state != STATE_FIXED32 && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected FIXED32 or LENGTH_DELIMITED but was $state")
    }
    val result = readIntLe()
    afterPackableScalar(STATE_FIXED32)
    return result
  }

  override fun readFixed64(): Long {
    if (state != STATE_FIXED64 && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected FIXED64 or LENGTH_DELIMITED but was $state")
    }
    val result = readLongLe()
    afterPackableScalar(STATE_FIXED64)
    return result
  }

  private fun afterPackableScalar(fieldEncoding: Int) {
    if (state == fieldEncoding) {
      state = STATE_TAG
    } else {
      when {
        pos > limit -> throw IOException("Expected to end at $limit but was $pos")
        pos == limit -> {
          // We've completed a sequence of packed values. Pop the limit.
          limit = pushedLimit
          pushedLimit = -1
          state = STATE_TAG
        }
        else -> state = STATE_PACKED_TAG
      }
    }
  }

  private fun beforeLengthDelimitedScalar(): Int {
    if (state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected LENGTH_DELIMITED but was $state")
    }
    val byteCount = limit - pos
    state = STATE_TAG
    // We've completed a length-delimited scalar. Pop the limit.
    limit = pushedLimit
    pushedLimit = -1
    return byteCount
  }

  override fun readUnknownField(tag: Int) {
    val fieldEncoding = peekFieldEncoding()
    val protoAdapter = fieldEncoding!!.rawProtoAdapter()
    val value = protoAdapter.decode(this)
    addUnknownField(tag, fieldEncoding, value)
  }

  override fun addUnknownField(
    tag: Int,
    fieldEncoding: FieldEncoding,
    value: Any?,
  ) {
    val unknownFieldsWriter = ProtoWriter(bufferStack[recursionDepth - 1])
    val protoAdapter = fieldEncoding.rawProtoAdapter()
    @Suppress("UNCHECKED_CAST") // We encode and decode the same types.
    (protoAdapter as ProtoAdapter<Any>).encodeWithTag(unknownFieldsWriter, tag, value)
  }

  override fun nextFieldMinLengthInBytes(): Int {
    return when (nextFieldEncoding) {
      FieldEncoding.LENGTH_DELIMITED -> limit - pos
      FieldEncoding.FIXED32 -> 4
      FieldEncoding.FIXED64 -> 8
      FieldEncoding.VARINT -> 1
      null -> throw IllegalStateException("nextFieldEncoding is not set")
    }
  }

  private fun skip(byteCount: Int) {
    val newPos = pos + byteCount
    if (newPos > limit) throw EOFException()
    pos = newPos
  }

  private fun readByteString(byteCount: Int): ByteString {
    val newPos = pos + byteCount
    if (newPos > limit) throw EOFException()
    val result = source.toByteString(pos, byteCount)
    pos = newPos
    return result
  }

  private fun readUtf8(byteCount: Int): String {
    val newPos = pos + byteCount
    if (newPos > limit) throw EOFException()
    val result = source.decodeToString(startIndex = pos, endIndex = newPos)
    pos = newPos
    return result
  }

  private fun readByte(): Byte {
    if (pos == limit) throw EOFException()
    return source[pos++]
  }

  private fun readIntLe(): Int {
    if (pos + 4 > limit) throw EOFException()

    val result = (
      (source[pos++] and 0xff)
        or (source[pos++] and 0xff shl 8)
        or (source[pos++] and 0xff shl 16)
        or (source[pos++] and 0xff shl 24)
      )

    return result
  }

  private fun readLongLe(): Long {
    if (pos + 8 > limit) throw EOFException()

    val result = (
      (source[pos++] and 0xffL)
        or (source[pos++] and 0xffL shl 8)
        or (source[pos++] and 0xffL shl 16)
        or (source[pos++] and 0xffL shl 24)
        or (source[pos++] and 0xffL shl 32)
        or (source[pos++] and 0xffL shl 40)
        or (source[pos++] and 0xffL shl 48)
        or (source[pos++] and 0xffL shl 56)
      )

    return result
  }
}
