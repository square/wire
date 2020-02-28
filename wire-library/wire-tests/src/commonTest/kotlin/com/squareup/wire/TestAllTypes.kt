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

import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import okio.Buffer
import okio.ByteString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestAllTypes {

  private fun <T> list(x: T, numRepeated: Int = 2): List<T> = MutableList(numRepeated) { x }

  private fun message(numRepeated: Int = 2): AllTypes {
    val bytes = ByteString.of(125.toByte(), 225.toByte())
    val nestedMessage = AllTypes.NestedMessage(a = 999)
    return AllTypes(
        opt_int32 = 111,
        opt_uint32 = 112,
        opt_sint32 = 113,
        opt_fixed32 = 114,
        opt_sfixed32 = 115,
        opt_int64 = 116L,
        opt_uint64 = 117L,
        opt_sint64 = 118L,
        opt_fixed64 = 119L,
        opt_sfixed64 = 120L,
        opt_bool = true,
        opt_float = 122.0f,
        opt_double = 123.0,
        opt_string = "124",
        opt_bytes = bytes,
        opt_nested_enum = AllTypes.NestedEnum.A,
        opt_nested_message = nestedMessage,
        req_int32 = 111,
        req_uint32 = 112,
        req_sint32 = 113,
        req_fixed32 = 114,
        req_sfixed32 = 115,
        req_int64 = 116L,
        req_uint64 = 117L,
        req_sint64 = 118L,
        req_fixed64 = 119L,
        req_sfixed64 = 120L,
        req_bool = true,
        req_float = 122.0f,
        req_double = 123.0,
        req_string = "124",
        req_bytes = bytes,
        req_nested_enum = AllTypes.NestedEnum.A,
        req_nested_message = nestedMessage,
        rep_int32 = list(111, numRepeated),
        rep_uint32 = list(112, numRepeated),
        rep_sint32 = list(113, numRepeated),
        rep_fixed32 = list(114, numRepeated),
        rep_sfixed32 = list(115, numRepeated),
        rep_int64 = list(116L, numRepeated),
        rep_uint64 = list(117L, numRepeated),
        rep_sint64 = list(118L, numRepeated),
        rep_fixed64 = list(119L, numRepeated),
        rep_sfixed64 = list(120L, numRepeated),
        rep_bool = list(true, numRepeated),
        rep_float = list(122.0f, numRepeated),
        rep_double = list(123.0, numRepeated),
        rep_string = list("124", numRepeated),
        rep_bytes = list(bytes, numRepeated),
        rep_nested_enum = list(AllTypes.NestedEnum.A, numRepeated),
        rep_nested_message = list(nestedMessage, numRepeated),
        pack_int32 = list(111, numRepeated),
        pack_uint32 = list(112, numRepeated),
        pack_sint32 = list(113, numRepeated),
        pack_fixed32 = list(114, numRepeated),
        pack_sfixed32 = list(115, numRepeated),
        pack_int64 = list(116L, numRepeated),
        pack_uint64 = list(117L, numRepeated),
        pack_sint64 = list(118L, numRepeated),
        pack_fixed64 = list(119L, numRepeated),
        pack_sfixed64 = list(120L, numRepeated),
        pack_bool = list(true, numRepeated),
        pack_float = list(122.0f, numRepeated),
        pack_double = list(123.0, numRepeated),
        pack_nested_enum = list(AllTypes.NestedEnum.A, numRepeated),
        ext_opt_bool = true,
        ext_rep_bool = list(true, numRepeated),
        ext_pack_bool = list(true, numRepeated)
    )
  }

  private val allTypes = message()
  private val adapter = AllTypes.ADAPTER

  @Test fun testHashCodes() {
    val message = message()
    val messageHashCode = message.hashCode()
    assertEquals(allTypes.hashCode(), messageHashCode)
  }

  @Test @Ignore fun testBuilder() {
    var builder = message()
    val nestedMessage = AllTypes.NestedMessage(a = 999)

    assertEquals(111, builder.opt_int32)
    assertEquals(112, builder.opt_uint32)
    assertEquals(113, builder.opt_sint32)
    assertEquals(114, builder.opt_fixed32)
    assertEquals(115, builder.opt_sfixed32)
    assertEquals(116L, builder.opt_int64)
    assertEquals(117L, builder.opt_uint64)
    assertEquals(118L, builder.opt_sint64)
    assertEquals(119L, builder.opt_fixed64)
    assertEquals(120L, builder.opt_sfixed64)
    assertEquals(true, builder.opt_bool)
    assertEquals(122.0f, builder.opt_float)
    assertEquals(123.0, builder.opt_double)
    assertEquals("124", builder.opt_string)
    assertEquals(ByteString.of(125.toByte(), 225.toByte()), builder.opt_bytes)
    assertEquals(AllTypes.NestedEnum.A, builder.opt_nested_enum)
    assertEquals(nestedMessage, builder.opt_nested_message)

    assertEquals(111, builder.req_int32)
    assertEquals(112, builder.req_uint32)
    assertEquals(113, builder.req_sint32)
    assertEquals(114, builder.req_fixed32)
    assertEquals(115, builder.req_sfixed32)
    assertEquals(116L, builder.req_int64)
    assertEquals(117L, builder.req_uint64)
    assertEquals(118L, builder.req_sint64)
    assertEquals(119L, builder.req_fixed64)
    assertEquals(120L, builder.req_sfixed64)
    assertEquals(true, builder.req_bool)
    assertEquals(122.0f, builder.req_float)
    assertEquals(123.0, builder.req_double)
    assertEquals("124", builder.req_string)
    assertEquals(ByteString.of(125.toByte(), 225.toByte()), builder.req_bytes)
    assertEquals(AllTypes.NestedEnum.A, builder.req_nested_enum)
    assertEquals(nestedMessage, builder.req_nested_message)

    assertEquals(2, builder.rep_int32.size)
    assertEquals(111, builder.rep_int32[0])
    assertEquals(111, builder.rep_int32[1])
    assertEquals(2, builder.rep_uint32.size)
    assertEquals(112, builder.rep_uint32[0])
    assertEquals(112, builder.rep_uint32[1])
    assertEquals(2, builder.rep_sint32.size)
    assertEquals(113, builder.rep_sint32[0])
    assertEquals(113, builder.rep_sint32[1])
    assertEquals(2, builder.rep_fixed32.size)
    assertEquals(114, builder.rep_fixed32[0])
    assertEquals(114, builder.rep_fixed32[1])
    assertEquals(2, builder.rep_sfixed32.size)
    assertEquals(115, builder.rep_sfixed32[0])
    assertEquals(115, builder.rep_sfixed32[1])
    assertEquals(2, builder.rep_int64.size)
    assertEquals(116L, builder.rep_int64[0])
    assertEquals(116L, builder.rep_int64[1])
    assertEquals(2, builder.rep_uint64.size)
    assertEquals(117L, builder.rep_uint64[0])
    assertEquals(117L, builder.rep_uint64[1])
    assertEquals(2, builder.rep_sint64.size)
    assertEquals(118L, builder.rep_sint64[0])
    assertEquals(118L, builder.rep_sint64[1])
    assertEquals(2, builder.rep_fixed64.size)
    assertEquals(119L, builder.rep_fixed64[0])
    assertEquals(119L, builder.rep_fixed64[1])
    assertEquals(2, builder.rep_sfixed64.size)
    assertEquals(120L, builder.rep_sfixed64[0])
    assertEquals(120L, builder.rep_sfixed64[1])
    assertEquals(2, builder.rep_bool.size)
    assertEquals(true, builder.rep_bool[0])
    assertEquals(true, builder.rep_bool[1])
    assertEquals(2, builder.rep_float.size)
    assertEquals(122.0f, builder.rep_float[0])
    assertEquals(122.0f, builder.rep_float[1])
    assertEquals(2, builder.rep_double.size)
    assertEquals(123.0, builder.rep_double[0])
    assertEquals(123.0, builder.rep_double[1])
    assertEquals(2, builder.rep_string.size)
    assertEquals("124", builder.rep_string[0])
    assertEquals("124", builder.rep_string[1])
    assertEquals(2, builder.rep_bytes.size)
    assertEquals(ByteString.of(125.toByte(), 225.toByte()), builder.rep_bytes[0])
    assertEquals(ByteString.of(125.toByte(), 225.toByte()), builder.rep_bytes[1])
    assertEquals(2, builder.rep_nested_enum.size)
    assertEquals(AllTypes.NestedEnum.A, builder.rep_nested_enum[0])
    assertEquals(AllTypes.NestedEnum.A, builder.rep_nested_enum[1])
    assertEquals(2, builder.rep_nested_message.size)
    assertEquals(nestedMessage, builder.rep_nested_message[0])
    assertEquals(nestedMessage, builder.rep_nested_message[1])

    assertEquals(2, builder.pack_int32.size)
    assertEquals(111, builder.pack_int32[0])
    assertEquals(111, builder.pack_int32[1])
    assertEquals(2, builder.pack_uint32.size)
    assertEquals(112, builder.pack_uint32[0])
    assertEquals(112, builder.pack_uint32[1])
    assertEquals(2, builder.pack_sint32.size)
    assertEquals(113, builder.pack_sint32[0])
    assertEquals(113, builder.pack_sint32[1])
    assertEquals(2, builder.pack_fixed32.size)
    assertEquals(114, builder.pack_fixed32[0])
    assertEquals(114, builder.pack_fixed32[1])
    assertEquals(2, builder.pack_sfixed32.size)
    assertEquals(115, builder.pack_sfixed32[0])
    assertEquals(115, builder.pack_sfixed32[1])
    assertEquals(2, builder.pack_int64.size)
    assertEquals(116L, builder.pack_int64[0])
    assertEquals(116L, builder.pack_int64[1])
    assertEquals(2, builder.pack_uint64.size)
    assertEquals(117L, builder.pack_uint64[0])
    assertEquals(117L, builder.pack_uint64[1])
    assertEquals(2, builder.pack_sint64.size)
    assertEquals(118L, builder.pack_sint64[0])
    assertEquals(118L, builder.pack_sint64[1])
    assertEquals(2, builder.pack_fixed64.size)
    assertEquals(119L, builder.pack_fixed64[0])
    assertEquals(119L, builder.pack_fixed64[1])
    assertEquals(2, builder.pack_sfixed64.size)
    assertEquals(120L, builder.pack_sfixed64[0])
    assertEquals(120L, builder.pack_sfixed64[1])
    assertEquals(2, builder.pack_bool.size)
    assertEquals(true, builder.pack_bool[0])
    assertEquals(true, builder.pack_bool[1])
    assertEquals(2, builder.pack_float.size)
    assertEquals(122.0f, builder.pack_float[0])
    assertEquals(122.0f, builder.pack_float[1])
    assertEquals(2, builder.pack_double.size)
    assertEquals(123.0, builder.pack_double[0])
    assertEquals(123.0, builder.pack_double[1])
    assertEquals(2, builder.pack_nested_enum.size)
    assertEquals(AllTypes.NestedEnum.A, builder.pack_nested_enum[0])
    assertEquals(AllTypes.NestedEnum.A, builder.pack_nested_enum[1])

    assertEquals(true, builder.ext_opt_bool)
    assertEquals(list(true), builder.ext_rep_bool)
    assertEquals(list(true), builder.ext_pack_bool)

    builder = builder.copy(
        ext_opt_bool = false,
        ext_rep_bool = list(false),
        ext_pack_bool = list(false)
    )

    assertEquals(false, builder.ext_opt_bool)
    assertEquals(list(false), builder.ext_rep_bool)
    assertEquals(list(false), builder.ext_pack_bool)
  }

  @Test fun testWrite() {
    val output = adapter.encode(allTypes)
    assertEquals(TestAllTypesData.expectedOutput.size, output.size)
    assertEquals(TestAllTypesData.expectedOutput, ByteString.of(*output))
  }

  @Test fun testWriteSource() {
    val sink = Buffer()
    adapter.encode(sink, allTypes)
    assertEquals(TestAllTypesData.expectedOutput, sink.readByteString())
  }

  @Test fun testWriteBytes() {
    val output = adapter.encode(allTypes)
    assertEquals(TestAllTypesData.expectedOutput.size, output.size)
    assertEquals(TestAllTypesData.expectedOutput, ByteString.of(*output))
  }

  @Test fun testReadSource() {
    val data = adapter.encode(allTypes)
    val input = Buffer().write(data)
    val parsed = adapter.decode(input)
    assertEquals(allTypes, parsed)
    assertEquals(true, allTypes.ext_opt_bool)
    assertEquals(list(true), allTypes.ext_rep_bool)
    assertEquals(list(true), allTypes.ext_pack_bool)
  }

  @Test fun testReadBytes() {
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertEquals(allTypes, parsed)
    assertEquals(true, allTypes.ext_opt_bool)
    assertEquals(list(true), allTypes.ext_rep_bool)
    assertEquals(list(true), allTypes.ext_pack_bool)
  }

  @Test fun testReadLongMessages() {
    val allTypes = message(50)
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertEquals(allTypes, parsed)
    assertEquals(true, allTypes.ext_opt_bool)
    assertEquals(list(true, 50), allTypes.ext_rep_bool)
    assertEquals(list(true, 50), allTypes.ext_pack_bool)
  }

  @Test fun testReadNoExtension() {
    val data = adapter.encode(allTypes)
    val parsed = AllTypes.ADAPTER.decode(data)
    assertEquals(allTypes, parsed)
  }

  @Test fun testReadNonPacked() {
    val parsed = adapter.decode(Buffer().write(TestAllTypesData.nonPacked))
    assertEquals(allTypes, parsed)
  }

  @IgnoreJs // https://youtrack.jetbrains.com/issue/KT-35078
  @Test fun testToString() {
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertEquals(TestAllTypesData.expectedToString, parsed.toString())
  }

  @Test fun testDefaults() {
    assertEquals(true, AllTypes.DEFAULT_DEFAULT_BOOL)
    // original: "<c-cedilla>ok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11g<u umlaut>zel"
    assertEquals("çok\u0007\b\u000C\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011" +
        "\u0001\u0001\u0011güzel", AllTypes.DEFAULT_DEFAULT_STRING)
  }

  @Test fun testEnums() {
    assertEquals(AllTypes.NestedEnum.A, AllTypes.NestedEnum.fromValue(1))
    assertNull(AllTypes.NestedEnum.fromValue(10))
    assertEquals(1, AllTypes.NestedEnum.A.value)
  }

  @Test fun testSkipGroup() {
    val data = ByteArray(TestAllTypesData.expectedOutput.size + 27)
    arraycopy(TestAllTypesData.expectedOutput.toByteArray(), 0, data, 0, 17)
    var index = 17
    data[index++] = 0xa3.toByte() // start group, tag = 20, type = 3
    data[index++] = 0x01.toByte()
    data[index++] = 0x08.toByte() // tag = 1, type = 0 (varint)
    data[index++] = 0x81.toByte()
    data[index++] = 0x82.toByte()
    data[index++] = 0x6f.toByte()
    data[index++] = 0x21.toByte() // tag = 2, type = 1 (fixed64)
    data[index++] = 0x01.toByte()
    data[index++] = 0x02.toByte()
    data[index++] = 0x03.toByte()
    data[index++] = 0x04.toByte()
    data[index++] = 0x05.toByte()
    data[index++] = 0x06.toByte()
    data[index++] = 0x07.toByte()
    data[index++] = 0x08.toByte()
    data[index++] = 0x1a.toByte() // tag = 3, type = 2 (length-delimited)
    data[index++] = 0x03.toByte() // length = 3
    data[index++] = 0x01.toByte()
    data[index++] = 0x02.toByte()
    data[index++] = 0x03.toByte()
    data[index++] = 0x25.toByte() // tag = 4, type = 5 (fixed32)
    data[index++] = 0x01.toByte()
    data[index++] = 0x02.toByte()
    data[index++] = 0x03.toByte()
    data[index++] = 0x04.toByte()
    data[index++] = 0xa4.toByte() // end group, tag = 20, type = 4
    data[index++] = 0x01.toByte()
    arraycopy(TestAllTypesData.expectedOutput.toByteArray(), 17, data, index,
        TestAllTypesData.expectedOutput.size - 17)
    val parsed = adapter.decode(data)
    assertEquals(allTypes, parsed)
  }

  private fun arraycopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
    for (offset in 0 until length) {
      dest[destPos + offset] = src[srcPos + offset]
    }
  }
}
