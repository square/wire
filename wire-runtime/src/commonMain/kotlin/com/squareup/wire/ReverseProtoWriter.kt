/*
 * Copyright (C) 2021 Square, Inc.
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

import com.squareup.wire.ProtoWriter.Companion.varint32Size
import com.squareup.wire.ProtoWriter.Companion.varint64Size
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.Throws
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.IOException

/**
 * Encodes protocol buffer message fields from back-to-front for efficiency. Callers should write
 * data in the opposite order that the data will be read.
 *
 * One significant benefit of writing messages in reverse order is that length prefixes can be
 * computed in constant time. Get the length of a message by subtracting the [byteCount] before
 * writing it from [byteCount] after writing it.
 *
 * Utilities for encoding and writing protocol message fields.
 */
class ReverseProtoWriter {
  /*
   * Okio's Buffer doesn't want to receive reverse-order writes, so we need some hacks!
   *
   * We keep two buffers:
   *  * head: a single-segment buffer where we're currently writing new data to
   *  * tail: N-segments of already written data
   *
   * To write 'head' from back to front we use Okio's UnsafeCursor, which offers raw access to the
   * byte array inside the buffer. We can write wherever we want to in this array, so we write it
   * from back to front.
   *
   * When we fill up the head we move all of its data to the front of the tail.
   */

  private var tail = Buffer()
  private var head = Buffer()

  // Directly access the only segment inside the head buffer.
  private val cursor = Buffer.UnsafeCursor()
  private var array: ByteArray = EMPTY_ARRAY
  private var arrayLimit: Int = 0

  // These are cached and reused for all forward-encoded messages inside a reverse-encoded message.
  private val forwardBuffer: Buffer by lazy(mode = NONE) { Buffer() }
  private val forwardWriter: ProtoWriter by lazy(mode = NONE) { ProtoWriter(forwardBuffer) }

  /** The total number of bytes emitted thus far. */
  val byteCount: Int
    get() = tail.size.toInt() + (array.size - arrayLimit)

  @Throws(IOException::class)
  fun writeTo(sink: BufferedSink) {
    emitCurrentSegment()
    sink.writeAll(tail)
  }

  private fun require(minByteCount: Int) {
    if (arrayLimit >= minByteCount) return
    emitCurrentSegment()
    head.readAndWriteUnsafe(cursor)
    cursor.expandBuffer(minByteCount)
    check(cursor.offset == 0L && cursor.end == cursor.data!!.size)
    array = cursor.data!!
    arrayLimit = cursor.end
  }

  /** Make the current segment a prefix of [tail]. */
  private fun emitCurrentSegment() {
    if (array === EMPTY_ARRAY) return // No current segment.
    cursor.close()

    // Advance the cursor to the first byte of data.
    head.skip(arrayLimit.toLong())

    // Move 'head' data to the front of 'tail'. We first move 'tail' to the end of head, then swap.
    head.writeAll(tail)
    val swap = tail
    tail = head
    head = swap

    // Use EMPTY_ARRAY as a sentinel until we start a new segment.
    array = EMPTY_ARRAY
    arrayLimit = 0
  }

  /**
   * When a forward-writable message needs to be written while we're writing in reverse, write that
   * message forwards then copy its bytes into this.
   */
  @Throws(IOException::class)
  internal fun writeForward(block: (forwardWriter: ProtoWriter) -> Unit) {
    block(forwardWriter)
    writeBytes(forwardBuffer.readByteString())
  }

  fun writeBytes(value: ByteString) {
    var valueLimit = value.size
    while (valueLimit != 0) {
      require(1)
      val copyByteCount = minOf(arrayLimit, valueLimit)
      arrayLimit -= copyByteCount
      val valuePos = valueLimit - copyByteCount
      value.copyInto(valuePos, array, arrayLimit, copyByteCount)
      valueLimit = valuePos
    }
  }

  fun writeString(value: String) {
    // This is derived from Okio's Buffer.writeUtf8(), modified to write back-to-front. Like that
    // function, malformed UTF-16 surrogates are encoded as '?' in UTF-8.
    var i = value.length - 1
    while (i >= 0) {
      val c = value[i--].code

      when {
        c < 0x80 -> {
          require(1)
          var localArrayLimit = arrayLimit
          val localArray = array

          // Emit a 7-bit character with 1 byte.
          localArray[--localArrayLimit] = c.toByte() // 0xxxxxxx

          // Fast-path contiguous runs of ASCII characters. This is ugly, but yields a ~4x
          // performance improvement over independent calls to writeByte().
          val runLimit = maxOf(-1, i - localArrayLimit)
          while (i > runLimit) {
            val d = value[i].code
            if (d >= 0x80) break
            i--
            localArray[--localArrayLimit] = d.toByte() // 0xxxxxxx
          }

          arrayLimit = localArrayLimit
        }

        c < 0x800 -> {
          // Emit a 11-bit character with 2 bytes.
          require(2)
          /* ktlint-disable no-multi-spaces */
          array[--arrayLimit] = (c        and 0x3f or 0x80).toByte() // 10xxxxxx
          array[--arrayLimit] = (c shr  6          or 0xc0).toByte() // 110xxxxx
          /* ktlint-enable no-multi-spaces */
        }

        c < 0xd800 || c > 0xdfff -> {
          // Emit a 16-bit character with 3 bytes.
          require(3)
          /* ktlint-disable no-multi-spaces */
          array[--arrayLimit] = (c        and 0x3f or 0x80).toByte() // 10xxxxxx
          array[--arrayLimit] = (c shr  6 and 0x3f or 0x80).toByte() // 10xxxxxx
          array[--arrayLimit] = (c shr 12          or 0xe0).toByte() // 1110xxxx
          /* ktlint-enable no-multi-spaces */
        }

        else -> {
          // c is a surrogate. Make sure it is a low surrogate & that its predecessor is a high
          // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
          // character.
          val high = (if (i >= 0) value[i].code else Int.MAX_VALUE)
          if (high > 0xdbff || c !in 0xdc00..0xdfff) {
            require(1)
            array[--arrayLimit] = '?'.code.toByte()
          } else {
            i--
            // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
            // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
            // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
            val codePoint = 0x010000 + (high and 0x03ff shl 10 or (c and 0x03ff))

            // Emit a 21-bit character with 4 bytes.
            require(4)
            /* ktlint-disable no-multi-spaces */
            array[--arrayLimit] = (codePoint        and 0x3f or 0x80).toByte() // 10yyyyyy
            array[--arrayLimit] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxyyyy
            array[--arrayLimit] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
            array[--arrayLimit] = (codePoint shr 18          or 0xf0).toByte() // 11110xxx
            /* ktlint-enable no-multi-spaces */
          }
        }
      }
    }
  }

  /** Encode and write a tag. */
  fun writeTag(fieldNumber: Int, fieldEncoding: FieldEncoding) {
    writeVarint32(ProtoWriter.makeTag(fieldNumber, fieldEncoding))
  }

  /** Write an `int32` field to the stream. */
  internal fun writeSignedVarint32(value: Int) {
    if (value >= 0) {
      writeVarint32(value)
    } else {
      // Must sign-extend.
      writeVarint64(value.toLong())
    }
  }

  /**
   * Encode and write a varint. `value` is treated as unsigned, so it won't be sign-extended
   * if negative.
   */
  fun writeVarint32(value: Int) {
    val varint32Size = varint32Size(value)
    require(varint32Size)
    arrayLimit -= varint32Size
    var offset = arrayLimit

    @Suppress("NAME_SHADOWING")
    var value = value
    while (value and 0x7f.inv() != 0) {
      array[offset++] = ((value and 0x7f) or 0x80).toByte()
      value = value ushr 7
    }
    array[offset] = value.toByte()
  }

  /** Encode and write a varint. */
  fun writeVarint64(value: Long) {
    val varint64Size = varint64Size(value)
    require(varint64Size)
    arrayLimit -= varint64Size
    var offset = arrayLimit

    @Suppress("NAME_SHADOWING")
    var value = value
    while (value and 0x7fL.inv() != 0L) {
      array[offset++] = ((value and 0x7f) or 0x80).toByte()
      value = value ushr 7
    }
    array[offset] = value.toByte()
  }

  /** Write a little-endian 32-bit integer. */
  fun writeFixed32(value: Int) {
    require(4)
    arrayLimit -= 4
    var offset = arrayLimit
    array[offset++] = (value         and 0xff).toByte() // ktlint-disable no-multi-spaces
    array[offset++] = (value ushr  8 and 0xff).toByte() // ktlint-disable no-multi-spaces
    array[offset++] = (value ushr 16 and 0xff).toByte()
    array[offset  ] = (value ushr 24 and 0xff).toByte() // ktlint-disable no-multi-spaces
  }

  /** Write a little-endian 64-bit integer. */
  fun writeFixed64(value: Long) {
    require(8)
    arrayLimit -= 8
    var offset = arrayLimit
    array[offset++] = (value         and 0xffL).toByte() // ktlint-disable no-multi-spaces
    array[offset++] = (value ushr  8 and 0xffL).toByte() // ktlint-disable no-multi-spaces
    array[offset++] = (value ushr 16 and 0xffL).toByte()
    array[offset++] = (value ushr 24 and 0xffL).toByte()
    array[offset++] = (value ushr 32 and 0xffL).toByte()
    array[offset++] = (value ushr 40 and 0xffL).toByte()
    array[offset++] = (value ushr 48 and 0xffL).toByte()
    array[offset  ] = (value ushr 56 and 0xffL).toByte() // ktlint-disable no-multi-spaces
  }

  private companion object {
    private val EMPTY_ARRAY = ByteArray(0)
  }
}
