/*
 * Copyright (C) 2024 Square, Inc.
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
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import com.squareup.wire.protos.kotlin.alltypes.AllTypes.NestedMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer
import okio.ByteString

class ProtoReader32AdapterTest {
  private val bytes = ByteString.of(125.toByte(), 225.toByte())
  private val nestedMessage = AllTypes.NestedMessage(a = 999)
  private val allTypes = AllTypes(
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
    rep_int32 = List(2) { 111 },
    rep_uint32 = List(2) { 112 },
    rep_sint32 = List(2) { 113 },
    rep_fixed32 = List(2) { 114 },
    rep_sfixed32 = List(2) { 115 },
    rep_int64 = List(2) { 116L },
    rep_uint64 = List(2) { 117L },
    rep_sint64 = List(2) { 118L },
    rep_fixed64 = List(2) { 119L },
    rep_sfixed64 = List(2) { 120L },
    rep_bool = List(2) { true },
    rep_float = List(2) { 122.0f },
    rep_double = List(2) { 123.0 },
    rep_string = List(2) { "124" },
    rep_bytes = List(2) { bytes },
    rep_nested_enum = List(2) { AllTypes.NestedEnum.A },
    rep_nested_message = List(2) { nestedMessage },
    pack_int32 = List(2) { 111 },
    pack_uint32 = List(2) { 112 },
    pack_sint32 = List(2) { 113 },
    pack_fixed32 = List(2) { 114 },
    pack_sfixed32 = List(2) { 115 },
    pack_int64 = List(2) { 116L },
    pack_uint64 = List(2) { 117L },
    pack_sint64 = List(2) { 118L },
    pack_fixed64 = List(2) { 119L },
    pack_sfixed64 = List(2) { 120L },
    pack_bool = List(2) { true },
    pack_float = List(2) { 122.0f },
    pack_double = List(2) { 123.0 },
    pack_nested_enum = List(2) { AllTypes.NestedEnum.A },
    ext_opt_bool = true,
    ext_rep_bool = List(2) { true },
    ext_pack_bool = List(2) { true },
    array_int32 = List(2) { 111 }.toIntArray(),
    array_uint32 = List(2) { 112 }.toIntArray(),
    array_sint32 = List(2) { 113 }.toIntArray(),
    array_fixed32 = List(2) { 114 }.toIntArray(),
    array_sfixed32 = List(2) { 115 }.toIntArray(),
    array_int64 = List(2) { 116L }.toLongArray(),
    array_uint64 = List(2) { 117L }.toLongArray(),
    array_sint64 = List(2) { 118L }.toLongArray(),
    array_fixed64 = List(2) { 119L }.toLongArray(),
    array_sfixed64 = List(2) { 120L }.toLongArray(),
    array_float = List(2) { 122.0f }.toFloatArray(),
    array_double = List(2) { 123.0 }.toDoubleArray(),
  )

  @Test
  fun decodeProtoReader32ByteString() {
    val protoReader32 = ProtoReader32(allTypes.encodeByteString())
    assertThat(AllTypes.ADAPTER.decode(protoReader32)).isEqualTo(allTypes)
  }

  @Test
  fun decodeProtoReader32ByteArray() {
    val protoReader32 = ProtoReader32(allTypes.encode())
    assertThat(AllTypes.ADAPTER.decode(protoReader32)).isEqualTo(allTypes)
  }

  @Test
  fun decodeProtoReader32AsProtoReader() {
    val protoReader = ProtoReader32(allTypes.encode()).asProtoReader()
    assertThat(AllTypes.ADAPTER.decode(protoReader)).isEqualTo(allTypes)
  }

  /**
   * Exercise the mix-and-match cases where a type that doesn't implement the [ProtoReader32] decode
   * function has a field that does. (The regular [ProtoReader] function is used everywhere.)
   */
  @Test
  fun decodeMixAndMatchProtoReaderEnclosed() {
    val abc = Abc("one", allTypes, "three")
    val abcBytes = Abc.ADAPTER.encodeByteString(abc)
    val protoReader = ProtoReader(Buffer().write(abcBytes))
    assertEquals(abc, Abc.ADAPTER.decode(protoReader))
  }

  data class Abc(
    val a: String?,
    val b: AllTypes?,
    val c: String?,
  ) {
    companion object {
      /** Note that this implements the decode overload that accepts a [ProtoReader32]. */
      val ADAPTER: ProtoAdapter<Abc> = object : ProtoAdapter<Abc>(
        FieldEncoding.LENGTH_DELIMITED,
        NestedMessage::class,
        "type.googleapis.com/Abc",
        PROTO_2,
        null,
        "abc.proto",
      ) {
        override fun encodedSize(value: Abc) = error("unexpected call")

        override fun encode(writer: ProtoWriter, value: Abc) = error("unexpected call")

        override fun encode(writer: ReverseProtoWriter, value: Abc) {
          STRING.encodeWithTag(writer, 1, value.a)
          AllTypes.ADAPTER.encodeWithTag(writer, 2, value.b)
          STRING.encodeWithTag(writer, 3, value.c)
        }

        override fun decode(reader: ProtoReader): Abc {
          var a: String? = null
          var b: AllTypes? = null
          var c: String? = null
          reader.forEachTag { tag ->
            when (tag) {
              1 -> a = STRING.decode(reader)
              2 -> b = AllTypes.ADAPTER.decode(reader)
              3 -> c = STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Abc(
            a = a,
            b = b,
            c = c,
          )
        }

        override fun redact(value: Abc) = error("unexpected call")
      }
    }
  }
}
