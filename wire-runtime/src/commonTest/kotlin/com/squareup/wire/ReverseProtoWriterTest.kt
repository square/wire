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

import com.squareup.wire.FieldEncoding.LENGTH_DELIMITED
import com.squareup.wire.ProtoAdapter.Companion.newMapAdapter
import com.squareup.wire.Syntax.PROTO_2
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.utf8Size

class ReverseProtoWriterTest {
  @Test fun utf8() {
    // 0 byte strings.
    assertUtf8("", "")

    // 1 byte code points.
    assertUtf8("\u0000", "00")
    assertUtf8("A", "41")

    // 2 byte code points.
    assertUtf8("\u0080", "c280")
    assertUtf8("\u07ff", "dfbf")

    // 3 byte code points.
    assertUtf8("\u0800", "e0a080")
    assertUtf8("\ud7ff", "ed9fbf")
    assertUtf8("\ue000", "ee8080")
    assertUtf8("\uffff", "efbfbf")

    // 4 byte code points, in Java as a high surrogate followed by low surrogate.
    assertUtf8("\ud800\udc00", "f0908080")

    // Malformed UTF-16.
    assertUtf8("\ud800", "3f") // Dangling high surrogate.
    assertUtf8("\ud800A", "3f41") // High surrogate followed by a 1 byte code point.
    assertUtf8("\ud800\ue000", "3fee8080") // High surrogate followed by a 3 byte code point.
    assertUtf8("\ud800\ud800\udc00", "3ff0908080") // High surrogate followed by surrogate pair.
    assertUtf8("\udc00A", "3f41") // Unexpected low surrogate.
    assertUtf8("\udc00", "3f") // Unexpected, dangling low surrogate.
  }

  @Test fun consistentWithRegularProtoWriterStrings() {
    val forwardBuffer = Buffer()
    ProtoWriter(forwardBuffer).apply {
      writeTag(3, LENGTH_DELIMITED)
      writeVarint32(11)
      writeString("hello world")
    }

    val reverseBuffer = Buffer()
    ReverseProtoWriter().apply {
      val byteCountBefore = byteCount
      writeString("hello world")
      writeVarint32(byteCount - byteCountBefore)
      writeTag(3, LENGTH_DELIMITED)
      writeTo(reverseBuffer)
    }

    assertEquals(forwardBuffer, reverseBuffer)
  }

  @Test fun consistentWithRegularProtoWriterByteStrings() {
    val forwardBuffer = Buffer()
    ProtoWriter(forwardBuffer).apply {
      writeTag(3, LENGTH_DELIMITED)
      writeVarint32(11)
      writeBytes("hello world".encodeUtf8())
    }

    val reverseBuffer = Buffer()
    ReverseProtoWriter().apply {
      val byteCountBefore = byteCount
      writeBytes("hello world".encodeUtf8())
      writeVarint32(byteCount - byteCountBefore)
      writeTag(3, LENGTH_DELIMITED)
      writeTo(reverseBuffer)
    }

    assertEquals(forwardBuffer, reverseBuffer)
  }

  @Test fun multipleStringWritesSpanSegments() {
    val buffer = reverseWrite {
      writeString("a".repeat(SEGMENT_SIZE - 1))
      writeString("b".repeat(2))
    }
    assertEquals("b".repeat(2) + "a".repeat(SEGMENT_SIZE - 1), buffer.readUtf8())
  }

  @Test fun writeStringExactlySegmentSize() {
    val buffer = reverseWrite { writeString("a".repeat(SEGMENT_SIZE)) }
    assertEquals("a".repeat(SEGMENT_SIZE), buffer.readUtf8())
  }

  @Test fun writeStringLargerThanSegmentSize() {
    val buffer = reverseWrite { writeString("a".repeat(SEGMENT_SIZE + 1)) }
    assertEquals("a".repeat(SEGMENT_SIZE + 1), buffer.readUtf8())
  }

  @Test fun writeStringSpanningMultipleSegments() {
    val buffer = reverseWrite { writeString("a".repeat(SEGMENT_SIZE + SEGMENT_SIZE + 1)) }
    assertEquals("a".repeat(SEGMENT_SIZE + SEGMENT_SIZE + 1), buffer.readUtf8())
  }

  @Test fun multipleByteStringWritesSpanSegments() {
    val buffer = reverseWrite {
      writeBytes("a".repeat(SEGMENT_SIZE - 1).encodeUtf8())
      writeBytes("b".repeat(2).encodeUtf8())
    }
    assertEquals("b".repeat(2) + "a".repeat(SEGMENT_SIZE - 1), buffer.readUtf8())
  }

  @Test fun writeByteStringExactlySegmentSize() {
    val buffer = reverseWrite { writeBytes("a".repeat(SEGMENT_SIZE).encodeUtf8()) }
    assertEquals("a".repeat(SEGMENT_SIZE), buffer.readUtf8())
  }

  @Test fun writeByteStringLargerThanSegmentSize() {
    val buffer = reverseWrite { writeBytes("a".repeat(SEGMENT_SIZE + 1).encodeUtf8()) }
    assertEquals("a".repeat(SEGMENT_SIZE + 1), buffer.readUtf8())
  }

  @Test fun writeByteStringSpanningMultipleSegments() {
    val buffer = reverseWrite {
      writeBytes("a".repeat(SEGMENT_SIZE + SEGMENT_SIZE + 1).encodeUtf8())
    }
    assertEquals("a".repeat(SEGMENT_SIZE + SEGMENT_SIZE + 1), buffer.readUtf8())
  }

  @Test fun reverseEncodedMessageEmbedsForwardEncodedMessage() {
    val alanGrant = Person(
      name = "Alan Grant",
      birthYear = 1950,
    )
    val digUpDinosaurs = Task(
      description = "dig up dinosaurs",
      assignee = alanGrant,
    )
    val alanGrantEncoded = Person.ADAPTER.encodeByteString(alanGrant)
    assertEquals(alanGrant, Person.ADAPTER.decode(alanGrantEncoded))

    val digUpDinosaursEncoded = Task.ADAPTER.encodeByteString(digUpDinosaurs)
    assertEquals(digUpDinosaurs, Task.ADAPTER.decode(digUpDinosaursEncoded))
  }

  @Test fun mapEncodingPreservesOrder() {
    val expectedMap = mapOf("red" to 0xff0000, "green" to 0x00ff00, "blue" to 0x0000ff)
    val mapAdapter = newMapAdapter(ProtoAdapter.STRING, ProtoAdapter.INT32)
    val writer = ReverseProtoWriter()
    mapAdapter.encodeWithTag(writer, 1, expectedMap)
    val buffer = Buffer()
    writer.writeTo(buffer)
    val protoReader = ProtoReader(buffer)
    val decodedMap = mutableMapOf<String, Int>()
    protoReader.forEachTag { tag ->
      when (tag) {
        1 -> decodedMap.putAll(mapAdapter.decode(protoReader))
        else -> protoReader.readUnknownField(tag)
      }
    }
    assertEquals(expectedMap, decodedMap)
    assertEquals(expectedMap.keys.toList(), decodedMap.keys.toList())
  }

  private fun reverseWrite(block: ReverseProtoWriter.() -> Unit): Buffer {
    val writer = ReverseProtoWriter()
    block(writer)
    val result = Buffer()
    writer.writeTo(result)
    return result
  }

  private fun assertUtf8(string: String, expectedHex: String) {
    val buffer = reverseWrite {
      writeString(string)
    }
    assertEquals(expectedHex, buffer.readByteString().hex())
    assertEquals((expectedHex.length / 2).toLong(), string.utf8Size())
  }

  /**
   * This object only supports forward encoding because it only implements [ProtoAdapter.encode]
   * with a [ProtoWriter]. This is representative of code generated before we added
   * [ReverseProtoWriter].
   */
  data class Person(val name: String, val birthYear: Int) {
    companion object {
      val ADAPTER = object : ProtoAdapter<Person>(
        FieldEncoding.LENGTH_DELIMITED,
        Person::class,
        "type.googleapis.com/Person",
        PROTO_2,
        null,
      ) {
        override fun redact(value: Person) = error("unexpected call")

        public override fun encodedSize(`value`: Person): Int {
          return STRING.encodedSizeWithTag(1, value.name) +
            INT32.encodedSizeWithTag(2, value.birthYear)
        }

        public override fun encode(writer: ProtoWriter, `value`: Person) {
          STRING.encodeWithTag(writer, 1, value.name)
          INT32.encodeWithTag(writer, 2, value.birthYear)
        }

        override fun decode(reader: ProtoReader): Person {
          var name = ""
          var birthYear = 0
          reader.forEachTag { tag ->
            when (tag) {
              1 -> name = STRING.decode(reader)
              2 -> birthYear = INT32.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Person(name, birthYear)
        }
      }
    }
  }

  /**
   * This object supports reverse encoding, but it has a field that only supports forwards encoding.
   */
  data class Task(val description: String, val assignee: Person? = null) {
    companion object {
      val ADAPTER = object : ProtoAdapter<Task>(
        FieldEncoding.LENGTH_DELIMITED,
        Task::class,
        "type.googleapis.com/Task",
        PROTO_2,
        null,
      ) {
        override fun redact(value: Task) = error("unexpected call")

        public override fun encodedSize(`value`: Task): Int {
          return STRING.encodedSizeWithTag(1, value.description) +
            Person.ADAPTER.encodedSizeWithTag(2, value.assignee)
        }

        override fun encode(writer: ProtoWriter, value: Task) {
          STRING.encodeWithTag(writer, 1, value.description)
          Person.ADAPTER.encodeWithTag(writer, 2, value.assignee)
        }

        public override fun encode(writer: ReverseProtoWriter, `value`: Task) {
          Person.ADAPTER.encodeWithTag(writer, 2, value.assignee)
          STRING.encodeWithTag(writer, 1, value.description)
        }

        override fun decode(reader: ProtoReader): Task {
          var description = ""
          var assignee: Person? = null
          reader.forEachTag { tag ->
            when (tag) {
              1 -> description = STRING.decode(reader)
              2 -> assignee = Person.ADAPTER.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Task(description, assignee)
        }
      }
    }
  }

  companion object {
    const val SEGMENT_SIZE = 8192
  }
}
