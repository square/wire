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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.squareup.wire.TestAllTypesData.list
import com.squareup.wire.TestAllTypesData.message
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import kotlin.test.Test
import okio.Buffer
import okio.ByteString

class TestAllTypes {
  private val adapter = AllTypes.ADAPTER

  private val allTypes = TestAllTypesData.allTypes

  @Test fun testHashCodes() {
    val message = message()
    val messageHashCode = message.hashCode()
    assertThat(messageHashCode).isEqualTo(allTypes.hashCode())
  }

  @Test fun testBuilder() {
    var builder = message()
    val nestedMessage = AllTypes.NestedMessage(a = 999)

    assertThat(builder.opt_int32).isEqualTo(111)
    assertThat(builder.opt_uint32).isEqualTo(112)
    assertThat(builder.opt_sint32).isEqualTo(113)
    assertThat(builder.opt_fixed32).isEqualTo(114)
    assertThat(builder.opt_sfixed32).isEqualTo(115)
    assertThat(builder.opt_int64).isEqualTo(116L)
    assertThat(builder.opt_uint64).isEqualTo(117L)
    assertThat(builder.opt_sint64).isEqualTo(118L)
    assertThat(builder.opt_fixed64).isEqualTo(119L)
    assertThat(builder.opt_sfixed64).isEqualTo(120L)
    assertThat(builder.opt_bool).isEqualTo(true)
    assertThat(builder.opt_float).isEqualTo(122.0f)
    assertThat(builder.opt_double).isEqualTo(123.0)
    assertThat(builder.opt_string).isEqualTo("124")
    assertThat(builder.opt_bytes).isEqualTo(ByteString.of(125.toByte(), 225.toByte()))
    assertThat(builder.opt_nested_enum).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(builder.opt_nested_message).isEqualTo(nestedMessage)

    assertThat(builder.req_int32).isEqualTo(111)
    assertThat(builder.req_uint32).isEqualTo(112)
    assertThat(builder.req_sint32).isEqualTo(113)
    assertThat(builder.req_fixed32).isEqualTo(114)
    assertThat(builder.req_sfixed32).isEqualTo(115)
    assertThat(builder.req_int64).isEqualTo(116L)
    assertThat(builder.req_uint64).isEqualTo(117L)
    assertThat(builder.req_sint64).isEqualTo(118L)
    assertThat(builder.req_fixed64).isEqualTo(119L)
    assertThat(builder.req_sfixed64).isEqualTo(120L)
    assertThat(builder.req_bool).isEqualTo(true)
    assertThat(builder.req_float).isEqualTo(122.0f)
    assertThat(builder.req_double).isEqualTo(123.0)
    assertThat(builder.req_string).isEqualTo("124")
    assertThat(builder.req_bytes).isEqualTo(ByteString.of(125.toByte(), 225.toByte()))
    assertThat(builder.req_nested_enum).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(builder.req_nested_message).isEqualTo(nestedMessage)

    assertThat(builder.rep_int32.size).isEqualTo(2)
    assertThat(builder.rep_int32[0]).isEqualTo(111)
    assertThat(builder.rep_int32[1]).isEqualTo(111)
    assertThat(builder.rep_uint32.size).isEqualTo(2)
    assertThat(builder.rep_uint32[0]).isEqualTo(112)
    assertThat(builder.rep_uint32[1]).isEqualTo(112)
    assertThat(builder.rep_sint32.size).isEqualTo(2)
    assertThat(builder.rep_sint32[0]).isEqualTo(113)
    assertThat(builder.rep_sint32[1]).isEqualTo(113)
    assertThat(builder.rep_fixed32.size).isEqualTo(2)
    assertThat(builder.rep_fixed32[0]).isEqualTo(114)
    assertThat(builder.rep_fixed32[1]).isEqualTo(114)
    assertThat(builder.rep_sfixed32.size).isEqualTo(2)
    assertThat(builder.rep_sfixed32[0]).isEqualTo(115)
    assertThat(builder.rep_sfixed32[1]).isEqualTo(115)
    assertThat(builder.rep_int64.size).isEqualTo(2)
    assertThat(builder.rep_int64[0]).isEqualTo(116L)
    assertThat(builder.rep_int64[1]).isEqualTo(116L)
    assertThat(builder.rep_uint64.size).isEqualTo(2)
    assertThat(builder.rep_uint64[0]).isEqualTo(117L)
    assertThat(builder.rep_uint64[1]).isEqualTo(117L)
    assertThat(builder.rep_sint64.size).isEqualTo(2)
    assertThat(builder.rep_sint64[0]).isEqualTo(118L)
    assertThat(builder.rep_sint64[1]).isEqualTo(118L)
    assertThat(builder.rep_fixed64.size).isEqualTo(2)
    assertThat(builder.rep_fixed64[0]).isEqualTo(119L)
    assertThat(builder.rep_fixed64[1]).isEqualTo(119L)
    assertThat(builder.rep_sfixed64.size).isEqualTo(2)
    assertThat(builder.rep_sfixed64[0]).isEqualTo(120L)
    assertThat(builder.rep_sfixed64[1]).isEqualTo(120L)
    assertThat(builder.rep_bool.size).isEqualTo(2)
    assertThat(builder.rep_bool[0]).isEqualTo(true)
    assertThat(builder.rep_bool[1]).isEqualTo(true)
    assertThat(builder.rep_float.size).isEqualTo(2)
    assertThat(builder.rep_float[0]).isEqualTo(122.0f)
    assertThat(builder.rep_float[1]).isEqualTo(122.0f)
    assertThat(builder.rep_double.size).isEqualTo(2)
    assertThat(builder.rep_double[0]).isEqualTo(123.0)
    assertThat(builder.rep_double[1]).isEqualTo(123.0)
    assertThat(builder.rep_string.size).isEqualTo(2)
    assertThat(builder.rep_string[0]).isEqualTo("124")
    assertThat(builder.rep_string[1]).isEqualTo("124")
    assertThat(builder.rep_bytes.size).isEqualTo(2)
    assertThat(builder.rep_bytes[0]).isEqualTo(ByteString.of(125.toByte(), 225.toByte()))
    assertThat(builder.rep_bytes[1]).isEqualTo(ByteString.of(125.toByte(), 225.toByte()))
    assertThat(builder.rep_nested_enum.size).isEqualTo(2)
    assertThat(builder.rep_nested_enum[0]).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(builder.rep_nested_enum[1]).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(builder.rep_nested_message.size).isEqualTo(2)
    assertThat(builder.rep_nested_message[0]).isEqualTo(nestedMessage)
    assertThat(builder.rep_nested_message[1]).isEqualTo(nestedMessage)

    assertThat(builder.pack_int32.size).isEqualTo(2)
    assertThat(builder.pack_int32[0]).isEqualTo(111)
    assertThat(builder.pack_int32[1]).isEqualTo(111)
    assertThat(builder.pack_uint32.size).isEqualTo(2)
    assertThat(builder.pack_uint32[0]).isEqualTo(112)
    assertThat(builder.pack_uint32[1]).isEqualTo(112)
    assertThat(builder.pack_sint32.size).isEqualTo(2)
    assertThat(builder.pack_sint32[0]).isEqualTo(113)
    assertThat(builder.pack_sint32[1]).isEqualTo(113)
    assertThat(builder.pack_fixed32.size).isEqualTo(2)
    assertThat(builder.pack_fixed32[0]).isEqualTo(114)
    assertThat(builder.pack_fixed32[1]).isEqualTo(114)
    assertThat(builder.pack_sfixed32.size).isEqualTo(2)
    assertThat(builder.pack_sfixed32[0]).isEqualTo(115)
    assertThat(builder.pack_sfixed32[1]).isEqualTo(115)
    assertThat(builder.pack_int64.size).isEqualTo(2)
    assertThat(builder.pack_int64[0]).isEqualTo(116L)
    assertThat(builder.pack_int64[1]).isEqualTo(116L)
    assertThat(builder.pack_uint64.size).isEqualTo(2)
    assertThat(builder.pack_uint64[0]).isEqualTo(117L)
    assertThat(builder.pack_uint64[1]).isEqualTo(117L)
    assertThat(builder.pack_sint64.size).isEqualTo(2)
    assertThat(builder.pack_sint64[0]).isEqualTo(118L)
    assertThat(builder.pack_sint64[1]).isEqualTo(118L)
    assertThat(builder.pack_fixed64.size).isEqualTo(2)
    assertThat(builder.pack_fixed64[0]).isEqualTo(119L)
    assertThat(builder.pack_fixed64[1]).isEqualTo(119L)
    assertThat(builder.pack_sfixed64.size).isEqualTo(2)
    assertThat(builder.pack_sfixed64[0]).isEqualTo(120L)
    assertThat(builder.pack_sfixed64[1]).isEqualTo(120L)
    assertThat(builder.pack_bool.size).isEqualTo(2)
    assertThat(builder.pack_bool[0]).isEqualTo(true)
    assertThat(builder.pack_bool[1]).isEqualTo(true)
    assertThat(builder.pack_float.size).isEqualTo(2)
    assertThat(builder.pack_float[0]).isEqualTo(122.0f)
    assertThat(builder.pack_float[1]).isEqualTo(122.0f)
    assertThat(builder.pack_double.size).isEqualTo(2)
    assertThat(builder.pack_double[0]).isEqualTo(123.0)
    assertThat(builder.pack_double[1]).isEqualTo(123.0)
    assertThat(builder.pack_nested_enum.size).isEqualTo(2)
    assertThat(builder.pack_nested_enum[0]).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(builder.pack_nested_enum[1]).isEqualTo(AllTypes.NestedEnum.A)

    assertThat(builder.ext_opt_bool).isEqualTo(true)
    assertThat(builder.ext_rep_bool).isEqualTo(list(true))
    assertThat(builder.ext_pack_bool).isEqualTo(list(true))

    builder = builder.copy(
      ext_opt_bool = false,
      ext_rep_bool = list(false),
      ext_pack_bool = list(false),
    )

    assertThat(builder.ext_opt_bool).isEqualTo(false)
    assertThat(builder.ext_rep_bool).isEqualTo(list(false))
    assertThat(builder.ext_pack_bool).isEqualTo(list(false))
  }

  @Test fun testWrite() {
    val output = adapter.encode(allTypes)
    assertThat(ByteString.of(*output)).isEqualTo(TestAllTypesData.expectedOutput)
    assertThat(output.size).isEqualTo(TestAllTypesData.expectedOutput.size)
  }

  @Test fun testWriteSource() {
    val sink = Buffer()
    adapter.encode(sink, allTypes)
    assertThat(sink.readByteString()).isEqualTo(TestAllTypesData.expectedOutput)
  }

  @Test fun testWriteBytes() {
    val output = adapter.encode(allTypes)
    assertThat(output.size).isEqualTo(TestAllTypesData.expectedOutput.size)
    assertThat(ByteString.of(*output)).isEqualTo(TestAllTypesData.expectedOutput)
  }

  @Test fun testReadSource() {
    val data = adapter.encode(allTypes)
    val input = Buffer().write(data)
    val parsed = adapter.decode(input)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(allTypes.ext_opt_bool).isEqualTo(true)
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true))
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true))
  }

  @Test fun testReadBytes() {
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(allTypes.ext_opt_bool).isEqualTo(true)
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true))
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true))
  }

  @Test fun testReadLongMessages() {
    val allTypes = message(50)
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(allTypes.ext_opt_bool).isEqualTo(true)
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true, 50))
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true, 50))
  }

  @Test fun testReadNoExtension() {
    val data = adapter.encode(allTypes)
    val parsed = AllTypes.ADAPTER.decode(data)
    assertThat(parsed).isEqualTo(allTypes)
  }

  @Test fun testReadNonPacked() {
    val parsed = adapter.decode(Buffer().write(TestAllTypesData.nonPacked))
    assertThat(parsed).isEqualTo(allTypes)
  }

  @IgnoreJs // https://youtrack.jetbrains.com/issue/KT-35078
  @Test
  fun testToString() {
    val data = adapter.encode(allTypes)
    val parsed = adapter.decode(data)
    assertThat(parsed.toString()).isEqualTo(TestAllTypesData.expectedToString)
  }

  @Test fun testDefaults() {
    assertThat(AllTypes.DEFAULT_DEFAULT_BOOL).isEqualTo(true)
    // original: "<c-cedilla>ok\a\b\f\n\r\t\v\1\01\001\17\017\176\x1\x01\x11\X1\X01\X11g<u umlaut>zel"
    assertThat(AllTypes.DEFAULT_DEFAULT_STRING).isEqualTo(
      "çok\u0007\b\u000C\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001\u0011" +
        "\u0001\u0001\u0011güzel",
    )
  }

  @Test fun testEnums() {
    assertThat(AllTypes.NestedEnum.fromValue(1)).isEqualTo(AllTypes.NestedEnum.A)
    assertThat(AllTypes.NestedEnum.fromValue(10)).isNull()
    assertThat(AllTypes.NestedEnum.A.value).isEqualTo(1)
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
    arraycopy(
      TestAllTypesData.expectedOutput.toByteArray(),
      17,
      data,
      index,
      TestAllTypesData.expectedOutput.size - 17,
    )
    val parsed = adapter.decode(data)
    assertThat(parsed).isEqualTo(allTypes)
  }

  private fun arraycopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, length: Int) {
    for (offset in 0 until length) {
      dest[destPos + offset] = src[srcPos + offset]
    }
  }
}
