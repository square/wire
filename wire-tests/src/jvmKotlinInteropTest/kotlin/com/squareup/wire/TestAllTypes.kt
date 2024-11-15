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
import assertk.assertions.isNotSameInstanceAs
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min
import okio.Buffer
import okio.ByteString
import okio.ForwardingSource
import okio.Source
import okio.buffer
import org.junit.Test

class TestAllTypes {

  private fun <T> list(x: T, numRepeated: Int = 2): List<T> = MutableList(numRepeated) { x }

  private fun getBuilder(numRepeated: Int = 2): AllTypes.Builder {
    val bytes = ByteString.of(125.toByte(), 225.toByte())
    val nestedMessage = AllTypes.NestedMessage.Builder().a(999).build()
    return AllTypes.Builder()
      .opt_int32(111)
      .opt_uint32(112)
      .opt_sint32(113)
      .opt_fixed32(114)
      .opt_sfixed32(115)
      .opt_int64(116L)
      .opt_uint64(117L)
      .opt_sint64(118L)
      .opt_fixed64(119L)
      .opt_sfixed64(120L)
      .opt_bool(true)
      .opt_float(122.0f)
      .opt_double(123.0)
      .opt_string("124")
      .opt_bytes(bytes)
      .opt_nested_enum(AllTypes.NestedEnum.A)
      .opt_nested_message(nestedMessage)
      .req_int32(111)
      .req_uint32(112)
      .req_sint32(113)
      .req_fixed32(114)
      .req_sfixed32(115)
      .req_int64(116L)
      .req_uint64(117L)
      .req_sint64(118L)
      .req_fixed64(119L)
      .req_sfixed64(120L)
      .req_bool(true)
      .req_float(122.0f)
      .req_double(123.0)
      .req_string("124")
      .req_bytes(bytes)
      .req_nested_enum(AllTypes.NestedEnum.A)
      .req_nested_message(nestedMessage)
      .rep_int32(list(111, numRepeated))
      .rep_uint32(list(112, numRepeated))
      .rep_sint32(list(113, numRepeated))
      .rep_fixed32(list(114, numRepeated))
      .rep_sfixed32(list(115, numRepeated))
      .rep_int64(list(116L, numRepeated))
      .rep_uint64(list(117L, numRepeated))
      .rep_sint64(list(118L, numRepeated))
      .rep_fixed64(list(119L, numRepeated))
      .rep_sfixed64(list(120L, numRepeated))
      .rep_bool(list(true, numRepeated))
      .rep_float(list(122.0f, numRepeated))
      .rep_double(list(123.0, numRepeated))
      .rep_string(list("124", numRepeated))
      .rep_bytes(list(bytes, numRepeated))
      .rep_nested_enum(list(AllTypes.NestedEnum.A, numRepeated))
      .rep_nested_message(list(nestedMessage, numRepeated))
      .pack_int32(list(111, numRepeated))
      .pack_uint32(list(112, numRepeated))
      .pack_sint32(list(113, numRepeated))
      .pack_fixed32(list(114, numRepeated))
      .pack_sfixed32(list(115, numRepeated))
      .pack_int64(list(116L, numRepeated))
      .pack_uint64(list(117L, numRepeated))
      .pack_sint64(list(118L, numRepeated))
      .pack_fixed64(list(119L, numRepeated))
      .pack_sfixed64(list(120L, numRepeated))
      .pack_bool(list(true, numRepeated))
      .pack_float(list(122.0f, numRepeated))
      .pack_double(list(123.0, numRepeated))
      .pack_nested_enum(list(AllTypes.NestedEnum.A, numRepeated))
      .ext_opt_bool(true)
      .ext_rep_bool(list(true, numRepeated))
      .ext_pack_bool(list(true, numRepeated))
  }

  private fun createAllTypes(numRepeated: Int = 2): AllTypes {
    return getBuilder(numRepeated).build()
  }

  private val allTypes: AllTypes = createAllTypes()
  private val adapter = AllTypes.ADAPTER

  @Test fun testInitBuilder() {
    val builder = allTypes.newBuilder()
    assertThat(builder.build()).isEqualTo(allTypes)
    builder.opt_bool = false
    assertThat(builder.build()).isNotSameInstanceAs(allTypes)
  }

  @Test fun testWriteStream() {
    val stream = ByteArrayOutputStream()
    adapter.encode(stream, allTypes)
    val output = stream.toByteArray()
    assertThat(output.size).isEqualTo(TestAllTypesData.expectedOutput.size)
    assertThat(ByteString.of(*output)).isEqualTo(TestAllTypesData.expectedOutput)
  }

  @Test fun testReadStream() {
    val data = adapter.encode(allTypes)
    val stream = ByteArrayInputStream(data)
    val parsed = adapter.decode(stream)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(allTypes.ext_opt_bool).isEqualTo(true)
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true))
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true))
  }

  /** A source that returns 1, 2, 3, or 4 bytes at a time.  */
  private class SlowSource(delegate: Source) : ForwardingSource(delegate) {
    private var pos: Long = 0
    override fun read(sink: Buffer, byteCount: Long): Long {
      val bytesToReturn = min(byteCount, pos % 4 + 1)
      pos += bytesToReturn
      return super.read(sink, byteCount)
    }
  }

  @Test fun testReadFromSlowSource() {
    val data = adapter.encode(allTypes)
    val input = SlowSource(Buffer().write(data))
    val parsed = adapter.decode(input.buffer())
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(allTypes.ext_opt_bool).isEqualTo(true)
    assertThat(allTypes.ext_rep_bool).isEqualTo(list(true))
    assertThat(allTypes.ext_pack_bool).isEqualTo(list(true))
  }

  @Test fun testDefaults() {
    assertThat(String(AllTypes.DEFAULT_DEFAULT_BYTES.toByteArray(), Charsets.ISO_8859_1))
      .isEqualTo(
        "çok\u0007\b\u000C\n\r\t\u000b\u0001\u0001\u0001\u000f\u000f~\u0001\u0001" +
          "\u0011\u0001\u0001\u0011güzel",
      )
  }

  @Test fun testUnknownFields() {
    val builder = getBuilder()
    builder.addUnknownField(10000, FieldEncoding.VARINT, 1L)
    val withUnknownField = builder.build()
    val data = adapter.encode(withUnknownField)
    val count = TestAllTypesData.expectedOutput.size
    assertThat(data.size).isEqualTo(count + 4)
    assertThat(data[count]).isEqualTo(0x80.toByte())
    assertThat(data[count + 1]).isEqualTo(0xf1.toByte())
    assertThat(data[count + 2]).isEqualTo(0x04.toByte())
    assertThat(data[count + 3]).isEqualTo(0x01.toByte())
  }
}
