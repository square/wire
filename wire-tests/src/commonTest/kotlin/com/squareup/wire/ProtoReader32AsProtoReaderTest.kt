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

import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.TestAllTypesData.allTypes
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import com.squareup.wire.protos.kotlin.alltypes.AllTypes.NestedMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * This test is similar to `ProtoReader32AdapterTest.kt` except it targets code generated without
 * the `emitProtoReader32` option.
 */
class ProtoReader32AsProtoReaderTest {

  @Test
  fun decodeProtoReader32ByteString() {
    val protoReader32 = ProtoReader32(allTypes.encodeByteString())
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader32))
  }

  @Test
  fun decodeProtoReader32ByteArray() {
    val protoReader32 = ProtoReader32(allTypes.encode())
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader32))
  }

  @Test
  fun decodeProtoReader32AsProtoReader() {
    val protoReader = ProtoReader32(allTypes.encode()).asProtoReader()
    assertEquals(allTypes, AllTypes.ADAPTER.decode(protoReader))
  }

  /**
   * Exercise the mix-and-match cases where a type that implements the [ProtoReader32] decode
   * function has a field that doesn't.
   */
  @Test
  fun decodeMixAndMatchProtoReader32Enclosed() {
    val abc = Abc("one", allTypes, "three")
    val abcBytes = Abc.ADAPTER.encodeByteString(abc)
    val protoReader32 = ProtoReader32(abcBytes)
    assertEquals(abc, Abc.ADAPTER.decode(protoReader32))
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

        override fun decode(reader: ProtoReader) = error("unexpected call")

        override fun decode(reader: ProtoReader32): Abc {
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
