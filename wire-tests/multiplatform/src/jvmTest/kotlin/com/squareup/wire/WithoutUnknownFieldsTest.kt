/*
 * Copyright (C) 2026 Square, Inc.
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
import com.squareup.wire.protos.kotlin.edgecases.OneField
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionOne
import com.squareup.wire.protos.kotlin.unknownfields.NestedVersionTwo
import com.squareup.wire.protos.kotlin.unknownfields.VersionOne
import com.squareup.wire.protos.kotlin.unknownfields.VersionTwo
import kotlin.test.Test
import okio.ByteString
import okio.ByteString.Companion.decodeHex

/**
 * JVM-only: [Message.withoutUnknownFields] is a JVM runtime API. These messages are generated with
 * the default Kotlin target (`javaInterop = false`), so [Message.newBuilder] throws.
 */
class WithoutUnknownFieldsTest {
  private val v1Adapter = VersionOne.ADAPTER
  private val v2Adapter = VersionTwo.ADAPTER

  @Test
  @Suppress("ktlint:standard:property-naming")
  fun withoutUnknownFieldsStripsUnknownFieldsWhenBuildersAreDeprecated() {
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
    val v1 = v1Adapter.decode(v2Adapter.encode(v2))

    assertThat(v1.obj!!.withoutUnknownFields()).isEqualTo(v1_obj)

    val v1Simple = VersionOne(i = 111, obj = v1_obj)
    assertThat(v1).isNotEqualTo(v1Simple)
    assertThat(v1.hashCode()).isNotEqualTo(v1Simple.hashCode())

    val v1Known = v1.withoutUnknownFields().copy(obj = v1.obj.withoutUnknownFields())
    assertThat(v1Known.unknownFields).isEqualTo(ByteString.EMPTY)
    assertThat(v1Known).isEqualTo(v1Simple)
    assertThat(v1Known.hashCode()).isEqualTo(v1Simple.hashCode())
    assertArrayEquals(v1Adapter.encode(v1Simple), v1Adapter.encode(v1Known))
  }

  @Test
  fun withoutUnknownFieldsDoesNotRecurseIntoNestedMessages() {
    val v2 = VersionTwo(
      i = 111,
      obj = NestedVersionTwo(i = 111, v2_i = 12345),
    )
    val v1 = v1Adapter.decode(v2Adapter.encode(v2))

    val stripped = v1.withoutUnknownFields()
    assertThat(stripped.unknownFields).isEqualTo(ByteString.EMPTY)
    assertThat(stripped.obj!!.unknownFields).isNotEqualTo(ByteString.EMPTY)
  }

  @Test
  fun unknownTagIgnored() {
    // tag 1 / type 0: 456
    // tag 2 / type 0: 789
    val data = "08c803109506".decodeHex()
    val oneField = OneField.ADAPTER.decode(data)
    val expected = OneField(opt_int32 = 456)
    assertThat(oneField).isNotEqualTo(expected)
    assertThat(oneField.withoutUnknownFields()).isEqualTo(expected)
    assertThat(oneField.withoutUnknownFields().unknownFields).isEqualTo(ByteString.EMPTY)
  }

  @Test
  fun withoutUnknownFieldsOnMessageWithNoneEqualsOriginal() {
    val known = VersionOne(i = 111, obj = NestedVersionOne(i = 111))
    assertThat(known.unknownFields).isEqualTo(ByteString.EMPTY)
    assertThat(known.withoutUnknownFields()).isEqualTo(known)
    assertThat(known.withoutUnknownFields().unknownFields).isEqualTo(ByteString.EMPTY)
  }
}
