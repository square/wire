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
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import com.squareup.wire.protos.kotlin.unknownfields.EnumVersionTwo
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionOne
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionTwo
import com.squareup.wire.protos.kotlin.unknownfields.VersionOne
import com.squareup.wire.protos.kotlin.unknownfields.VersionTwo
import kotlin.test.Test
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
    assertThat(v2.i).isEqualTo(111)
    assertThat(v2.obj).isEqualTo(v2.obj!!.copy())
    // Check v.2 fields
    assertThat(v2.v2_i).isEqualTo(12345)
    assertThat(v2.v2_s).isEqualTo("222")
    assertThat(v2.v2_f32).isEqualTo(67890)
    assertThat(v2.v2_f64).isEqualTo(98765L)
    assertThat(v2.v2_rs).isEqualTo(listOf("1", "2"))
    // Serialized
    val v2Bytes = v2Adapter.encode(v2)

    // Parse
    val v1 = v1Adapter.decode(v2Bytes)
    // v.1 fields are visible, v.2 fields are in unknownFieldSet
    assertThat(v1.i).isEqualTo(111)
    assertThat(v1.obj!!.copy(unknownFields = ByteString.EMPTY)).isEqualTo(v1_obj)
    // Serialized output should still contain the v.2 fields
    val v1Bytes = v1Adapter.encode(v1)

    // Unknown fields participate in equals() and hashCode()
    val v1Simple = VersionOne(i = 111, obj = v1_obj)
    assertThat(v1).isNotEqualTo(v1Simple)
    assertThat(v1.hashCode()).isNotEqualTo(v1Simple.hashCode())
    assertArrayNotEquals(v1Adapter.encode(v1Simple), v1Adapter.encode(v1))

    // Unknown fields can be removed for equals() and hashCode();
    val v1Known = v1.copy(
      obj = v1.obj.copy(unknownFields = ByteString.EMPTY),
      unknownFields = ByteString.EMPTY,
    )
    assertThat(v1Known).isEqualTo(v1Simple)
    assertThat(v1Known.hashCode()).isEqualTo(v1Simple.hashCode())
    assertArrayEquals(v1Adapter.encode(v1Simple), v1Adapter.encode(v1Known))

    // Re-parse
    val v2B = v2Adapter.decode(v1Bytes)
    assertThat(v2B.i).isEqualTo(111)
    assertThat(v2B.v2_i).isEqualTo(12345)
    assertThat(v2B.v2_s).isEqualTo("222")
    assertThat(v2B.v2_f32).isEqualTo(67890)
    assertThat(v2B.v2_f64).isEqualTo(98765L)
    assertThat(v2B.v2_rs).isEqualTo(listOf("1", "2"))
    assertThat(v2B.obj).isEqualTo(v2_obj)

    // "Modify" v1 via a merged builder, serialize, and re-parse
    val v1Modified = v1.copy(i = 777, obj = v1_obj.copy(i = 777))
    assertThat(v1Modified.i).isEqualTo(777)
    assertThat(v1Modified.obj).isEqualTo(v1_obj.copy(i = 777))
    val v1ModifiedBytes = v1Adapter.encode(v1Modified)

    val v2C = v2Adapter.decode(v1ModifiedBytes)
    assertThat(v2C.i).isEqualTo(777)
    assertThat(v2C.v2_i).isEqualTo(12345)
    assertThat(v2C.v2_s).isEqualTo("222")
    assertThat(v2C.v2_f32).isEqualTo(67890)
    assertThat(v2C.v2_f64).isEqualTo(98765L)
    assertThat(v2C.obj).isEqualTo(NestedVersionTwo(i = 777))
    assertThat(v2C.v2_rs).isEqualTo(listOf("1", "2"))
  }

  @Test fun unknownEnumFields() {
    val v2 = VersionTwo(en = EnumVersionTwo.PUSS_IN_BOOTS_V2, i = 100)
    val v2Serialized = VersionTwo.ADAPTER.encode(v2)
    val v1 = VersionOne.ADAPTER.decode(v2Serialized)
    assertThat(v1.i).isEqualTo(100)
    assertThat(v1.en).isNull()
    // 40 = 8 << 3 | 0 (tag: 8, field encoding: VARINT(0))
    // 04 = PUSS_IN_BOOTS(4)
    assertThat(v1.unknownFields).isEqualTo("4004".decodeHex())
  }
}
