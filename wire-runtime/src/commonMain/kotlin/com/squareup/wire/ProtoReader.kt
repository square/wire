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

import com.squareup.wire.internal.ProtocolException
import kotlin.Throws
import com.squareup.wire.internal.and
import com.squareup.wire.internal.shl
import kotlin.jvm.JvmName
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.EOFException
import okio.IOException

/**
 * Reads and decodes protocol message fields.
 */
open class ProtoReader(private val source: BufferedSource) {
  /*
   * Introducing new methods here?
   *
   * You'll want to mirror those changes in ProtoReader32. That class duplicates this one with a
   * different cursor type for better performance on Kotlin/JS.
   *
   * Please also track changes in [ProtoReader32AsProtoReader], which treats this class as if it is
   * an interface to be implemented.
   */

  /** The current position in the input source, starting at 0 and increasing monotonically. */
  private var pos: Long = 0

  /** The absolute position of the end of the current message. */
  private var limit = Long.MAX_VALUE

  /** The current number of levels of message nesting. */
  private var recursionDepth = 0

  /** How to interpret the next read call. */
  private var state = STATE_LENGTH_DELIMITED

  /** The most recently read tag. Used to make packed values look like regular values. */
  private var tag = -1

  /** Limit once we complete the current length-delimited value. */
  private var pushedLimit: Long = -1

  /** The encoding of the next value to be read. */
  private var nextFieldEncoding: FieldEncoding? = null

  /** Pooled buffers for unknown fields, indexed by [recursionDepth]. */
  private val bufferStack = mutableListOf<Buffer>()

  /**
   * Begin a nested message. A call to this method will restrict the reader so that [nextTag]
   * returns -1 when the message is complete. An accompanying call to [endMessage] must then occur
   * with the opaque token returned from this method.
   */
  @Throws(IOException::class)
  open fun beginMessage(): Long {
    check(state == STATE_LENGTH_DELIMITED) { "Unexpected call to beginMessage()" }
    if (++recursionDepth > RECURSION_LIMIT) {
      throw IOException("Wire recursion limit exceeded")
    }
    // Allocate a buffer to store unknown fields encountered at this recursion level.
    if (recursionDepth > bufferStack.size) bufferStack += Buffer()
    // Give the pushed limit to the caller to hold. The value is returned in endMessage() where we
    // resume using it as our limit.
    val token = pushedLimit
    pushedLimit = -1L
    state = STATE_TAG
    return token
  }

  /**
   * End a length-delimited nested message. Calls to this method must be symmetric with calls to
   * [beginMessage].
   *
   * @param token value returned from the corresponding call to [beginMessage].
   */
  @Throws(IOException::class)
  open fun endMessageAndGetUnknownFields(token: Long): ByteString {
    check(state == STATE_TAG) { "Unexpected call to endMessage()" }
    check(--recursionDepth >= 0 && pushedLimit == -1L) { "No corresponding call to beginMessage()" }
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

  /**
   * End a length-delimited nested message. Calls to this method must be symmetric with calls to
   * [beginMessage].
   *
   * @param token value returned from the corresponding call to [beginMessage].
   */
  @Throws(IOException::class)
  @Deprecated(
    level = DeprecationLevel.WARNING,
    message = "prefer endMessageAndGetUnknownFields()",
    replaceWith = ReplaceWith("endMessageAndGetUnknownFields(token)"),
  )
  fun endMessage(token: Long) {
    endMessageAndGetUnknownFields(token)
  }

  /** Reads and returns the length of the next message in a length-delimited stream. */
  @Throws(IOException::class)
  open fun nextLengthDelimited(): Int {
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
    if (pushedLimit != -1L) throw IllegalStateException()
    // Push the current limit, and set a new limit to the length of this value.
    pushedLimit = limit
    limit = pos + length
    if (limit > pushedLimit) throw EOFException()
    return length
  }

  /**
   * Reads and returns the next tag of the message, or -1 if there are no further tags. Use
   * [peekFieldEncoding] after calling this method to query its encoding. This silently skips
   * groups.
   */
  @Throws(IOException::class)
  open fun nextTag(): Int {
    if (state == STATE_PACKED_TAG) {
      state = STATE_LENGTH_DELIMITED
      return tag
    } else if (state != STATE_TAG) {
      throw IllegalStateException("Unexpected call to nextTag()")
    }

    loop@ while (pos < limit && !source.exhausted()) {
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

  /**
   * Returns the encoding of the next field value. [nextTag] must be called before this method.
   */
  open fun peekFieldEncoding(): FieldEncoding? = nextFieldEncoding

  /**
   * Skips the current field's value. This is only safe to call immediately following a call to
   * [nextTag].
   */
  @Throws(IOException::class)
  open fun skip() {
    when (state) {
      STATE_LENGTH_DELIMITED -> {
        val byteCount = beforeLengthDelimitedScalar()
        source.skip(byteCount)
      }
      STATE_VARINT -> readVarint64()
      STATE_FIXED64 -> readFixed64()
      STATE_FIXED32 -> readFixed32()
      else -> throw IllegalStateException("Unexpected call to skip()")
    }
  }

  /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
  private fun skipGroup(expectedEndTag: Int) {
    while (pos < limit && !source.exhausted()) {
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
          pos += length.toLong()
          source.skip(length.toLong())
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

  /**
   * Reads a `bytes` field value from the stream. The length is read from the stream prior to the
   * actual data.
   */
  @Throws(IOException::class)
  open fun readBytes(): ByteString {
    val byteCount = beforeLengthDelimitedScalar()
    source.require(byteCount) // Throws EOFException if insufficient bytes are available.
    return source.readByteString(byteCount)
  }

  /**
   * Prepares to read a value and returns true if the read should proceed. If there's nothing to
   * read (because a packed value has length 0), this will clear the reader state.
   */
  internal open fun beforePossiblyPackedScalar(): Boolean {
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

  /** Reads a `string` field value from the stream. */
  @Throws(IOException::class)
  open fun readString(): String {
    val byteCount = beforeLengthDelimitedScalar()
    source.require(byteCount) // Throws EOFException if insufficient bytes are available.
    return source.readUtf8(byteCount)
  }

  /**
   * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
   */
  @Throws(IOException::class)
  open fun readVarint32(): Int {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected VARINT or LENGTH_DELIMITED but was $state")
    }
    val result = internalReadVarint32()
    afterPackableScalar(STATE_VARINT)
    return result
  }

  private fun internalReadVarint32(): Int {
    source.require(1) // Throws EOFException if insufficient bytes are available.
    pos++
    var tmp = source.readByte()
    if (tmp >= 0) {
      return tmp.toInt()
    }
    var result = tmp and 0x7f
    source.require(1) // Throws EOFException if insufficient bytes are available.
    pos++
    tmp = source.readByte()
    if (tmp >= 0) {
      result = result or (tmp shl 7)
    } else {
      result = result or (tmp and 0x7f shl 7)
      source.require(1) // Throws EOFException if insufficient bytes are available.
      pos++
      tmp = source.readByte()
      if (tmp >= 0) {
        result = result or (tmp shl 14)
      } else {
        result = result or (tmp and 0x7f shl 14)
        source.require(1) // Throws EOFException if insufficient bytes are available.
        pos++
        tmp = source.readByte()
        if (tmp >= 0) {
          result = result or (tmp shl 21)
        } else {
          result = result or (tmp and 0x7f shl 21)
          source.require(1) // Throws EOFException if insufficient bytes are available.
          pos++
          tmp = source.readByte()
          result = result or (tmp shl 28)
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (i in 0..4) {
              source.require(1) // Throws EOFException if insufficient bytes are available.
              pos++
              if (source.readByte() >= 0) {
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

  /** Reads a raw varint up to 64 bits in length from the stream.  */
  @Throws(IOException::class)
  open fun readVarint64(): Long {
    if (state != STATE_VARINT && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected VARINT or LENGTH_DELIMITED but was $state")
    }
    var shift = 0
    var result: Long = 0
    while (shift < 64) {
      source.require(1) // Throws EOFException if insufficient bytes are available.
      pos++
      val b = source.readByte()
      result = result or ((b and 0x7F).toLong() shl shift)
      if (b and 0x80 == 0) {
        afterPackableScalar(STATE_VARINT)
        return result
      }
      shift += 7
    }
    throw ProtocolException("WireInput encountered a malformed varint")
  }

  /** Reads a 32-bit little-endian integer from the stream.  */
  @Throws(IOException::class)
  open fun readFixed32(): Int {
    if (state != STATE_FIXED32 && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected FIXED32 or LENGTH_DELIMITED but was $state")
    }
    source.require(4) // Throws EOFException if insufficient bytes are available.
    pos += 4
    val result = source.readIntLe()
    afterPackableScalar(STATE_FIXED32)
    return result
  }

  /** Reads a 64-bit little-endian integer from the stream.  */
  @Throws(IOException::class)
  open fun readFixed64(): Long {
    if (state != STATE_FIXED64 && state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected FIXED64 or LENGTH_DELIMITED but was $state")
    }
    source.require(8) // Throws EOFException if insufficient bytes are available.
    pos += 8
    val result = source.readLongLe()
    afterPackableScalar(STATE_FIXED64)
    return result
  }

  @Throws(IOException::class)
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

  @Throws(IOException::class)
  private fun beforeLengthDelimitedScalar(): Long {
    if (state != STATE_LENGTH_DELIMITED) {
      throw ProtocolException("Expected LENGTH_DELIMITED but was $state")
    }
    val byteCount = limit - pos
    source.require(byteCount) // Throws EOFException if insufficient bytes are available.
    state = STATE_TAG
    // We've completed a length-delimited scalar. Pop the limit.
    pos = limit
    limit = pushedLimit
    pushedLimit = -1
    return byteCount
  }

  /** Reads each tag, handles it, and returns a byte string with the unknown fields. */
  @JvmName("-forEachTag") // hide from Java
  inline fun forEachTag(tagHandler: (Int) -> Any): ByteString {
    val token = beginMessage()
    while (true) {
      val tag = nextTag()
      if (tag == -1) break
      tagHandler(tag)
    }
    return endMessageAndGetUnknownFields(token)
  }

  /**
   * Read an unknown field and store temporarily. Once the entire message is read, call
   * [endMessageAndGetUnknownFields] to retrieve unknown fields.
   */
  open fun readUnknownField(tag: Int) {
    val fieldEncoding = peekFieldEncoding()
    val protoAdapter = fieldEncoding!!.rawProtoAdapter()
    val value = protoAdapter.decode(this)
    addUnknownField(tag, fieldEncoding, value)
  }

  /**
   * Store an already read field temporarily. Once the entire message is read, call
   * [endMessageAndGetUnknownFields] to retrieve unknown fields.
   */
  open fun addUnknownField(
    tag: Int,
    fieldEncoding: FieldEncoding,
    value: Any?,
  ) {
    val unknownFieldsWriter = ProtoWriter(bufferStack[recursionDepth - 1])
    val protoAdapter = fieldEncoding.rawProtoAdapter()
    @Suppress("UNCHECKED_CAST") // We encode and decode the same types.
    (protoAdapter as ProtoAdapter<Any>).encodeWithTag(unknownFieldsWriter, tag, value)
  }

  /**
   * Returns the min length of the next field in bytes. Some encodings have a fixed length, while others
   * have a variable length. LENGTH_DELIMITED fields have a known variable length, while VARINT fields
   * could be as small as a single byte.
   */
  open fun nextFieldMinLengthInBytes(): Long {
    return when (nextFieldEncoding) {
      FieldEncoding.LENGTH_DELIMITED -> limit - pos
      FieldEncoding.FIXED32 -> 4
      FieldEncoding.FIXED64 -> 8
      FieldEncoding.VARINT -> 1
      null -> throw IllegalStateException("nextFieldEncoding is not set")
    }
  }

  companion object {
    /** The standard number of levels of message nesting to allow. */
    internal const val RECURSION_LIMIT = 100

    internal const val FIELD_ENCODING_MASK = 0x7
    internal const val TAG_FIELD_ENCODING_BITS = 3

    /** Read states. These constants correspond to field encodings where both exist. */
    internal const val STATE_VARINT = 0
    internal const val STATE_FIXED64 = 1
    internal const val STATE_LENGTH_DELIMITED = 2
    internal const val STATE_START_GROUP = 3
    internal const val STATE_END_GROUP = 4
    internal const val STATE_FIXED32 = 5
    internal const val STATE_TAG = 6 // Note: not a field encoding.
    internal const val STATE_PACKED_TAG = 7 // Note: not a field encoding.
  }
}
