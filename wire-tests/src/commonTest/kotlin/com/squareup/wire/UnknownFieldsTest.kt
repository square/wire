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

import com.squareup.wire.protos.kotlin.unknownfields.EnumVersionTwo
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionOne
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionTwo
import com.squareup.wire.protos.kotlin.unknownfields.VersionOne
import com.squareup.wire.protos.kotlin.unknownfields.VersionTwo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class UnknownFieldsTest {
  private val v1Adapter = VersionOne.ADAPTER
  private val v2Adapter = VersionTwo.ADAPTER

  @Test
  fun testUnknownFields() {
    val v1_obj = NestedVersionOne(i = 111)
    val v2_obj = NestedVersionTwo(
      i = 111,
      v2_i = 12345,
      v2_s = "222",
      v2_f32 = 67890,
      v2_f64 = 98765L,
      v2_rs = listOf("1", "2"),
    )

    val v2 = VersionTwo(
      i = 111,
      v2_i = 12345,
      v2_s = "222",
      v2_f32 = 67890,
      v2_f64 = 98765L,
      v2_rs = listOf("1", "2"),
      obj = v2_obj,
    )
    assertEquals(111, v2.i)
    assertEquals(v2.obj!!.copy(), v2.obj)
    // Check v.2 fields
    assertEquals(12345, v2.v2_i)
    assertEquals("222", v2.v2_s)
    assertEquals(67890, v2.v2_f32)
    assertEquals(98765L, v2.v2_f64)
    assertEquals(listOf("1", "2"), v2.v2_rs)
    // Serialized
    val v2Bytes = v2Adapter.encode(v2)

    // Parse
    val v1 = v1Adapter.decode(v2Bytes)
    // v.1 fields are visible, v.2 fields are in unknownFieldSet
    assertEquals(111, v1.i)
    assertEquals(v1_obj, v1.obj!!.copy(unknownFields = ByteString.EMPTY))
    // Serialized output should still contain the v.2 fields
    val v1Bytes = v1Adapter.encode(v1)

    // Unknown fields participate in equals() and hashCode()
    val v1Simple = VersionOne(i = 111, obj = v1_obj)
    assertNotEquals(v1Simple, v1)
    assertNotEquals(v1Simple.hashCode(), v1.hashCode())
    assertArrayNotEquals(v1Adapter.encode(v1Simple), v1Adapter.encode(v1))

    // Unknown fields can be removed for equals() and hashCode();
    val v1Known = v1.copy(
      obj = v1.obj.copy(unknownFields = ByteString.EMPTY),
      unknownFields = ByteString.EMPTY,
    )
    assertEquals(v1Simple, v1Known)
    assertEquals(v1Simple.hashCode(), v1Known.hashCode())
    assertArrayEquals(v1Adapter.encode(v1Simple), v1Adapter.encode(v1Known))

    // Re-parse
    val v2B = v2Adapter.decode(v1Bytes)
    assertEquals(111, v2B.i)
    assertEquals(12345, v2B.v2_i)
    assertEquals("222", v2B.v2_s)
    assertEquals(67890, v2B.v2_f32)
    assertEquals(98765L, v2B.v2_f64)
    assertEquals(listOf("1", "2"), v2B.v2_rs)
    assertEquals(v2_obj, v2B.obj)

    // "Modify" v1 via a merged builder, serialize, and re-parse
    val v1Modified = v1.copy(i = 777, obj = v1_obj.copy(i = 777))
    assertEquals(777, v1Modified.i)
    assertEquals(v1_obj.copy(i = 777), v1Modified.obj)
    val v1ModifiedBytes = v1Adapter.encode(v1Modified)

    val v2C = v2Adapter.decode(v1ModifiedBytes)
    assertEquals(777, v2C.i)
    assertEquals(12345, v2C.v2_i)
    assertEquals("222", v2C.v2_s)
    assertEquals(67890, v2C.v2_f32)
    assertEquals(98765L, v2C.v2_f64)
    assertEquals(NestedVersionTwo(i = 777), v2C.obj)
    assertEquals(listOf("1", "2"), v2C.v2_rs)
  }

  @Test fun unknownEnumFields() {
    val v2 = VersionTwo(en = EnumVersionTwo.PUSS_IN_BOOTS_V2, i = 100)
    val v2Serialized = VersionTwo.ADAPTER.encode(v2)
    val v1 = VersionOne.ADAPTER.decode(v2Serialized)
    assertEquals(100, v1.i)
    assertNull(v1.en)
    // 40 = 8 << 3 | 0 (tag: 8, field encoding: VARINT(0))
    // 04 = PUSS_IN_BOOTS(4)
    assertEquals("4004".decodeHex(), v1.unknownFields)
  }
}
