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

import com.squareup.wire.protos.kotlin.unknownfields.VersionOne
import com.squareup.wire.protos.kotlin.unknownfields.VersionTwo
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnknownFieldsTest {
  private val v1Adapter = VersionOne.ADAPTER
  private val v2Adapter = VersionTwo.ADAPTER

  @Test
  fun repeatedCallsToBuildRetainUnknownFields() {
    val v2 = VersionTwo.Builder()
        .i(111)
        .v2_i(12345)
        .v2_s("222")
        .v2_f32(67890)
        .v2_f64(98765L)
        .v2_rs(listOf("1", "2"))
        .build()

    // Serializes v2 and decodes it as a VersionOne.
    val v2Bytes = v2Adapter.encode(v2)
    val v1Builder = v1Adapter.decode(v2Bytes).newBuilder()

    // Builds v1Builder. It should equal to v2.
    val v1A = v1Builder.build()
    val fromV1A = v2Adapter.decode(v1Adapter.encode(v1A))
    assertThat(fromV1A).isEqualTo(v2)

    // Build v1Builder again. It should retain unknown fields.
    val v1B = v1Builder.build()
    val fromV1B = v2Adapter.decode(v1Adapter.encode(v1B))
    assertThat(fromV1B).isEqualTo(v2)
  }

  @Test
  fun unknownFieldsCanBeAddedBetweenCallsToBuild() {
    val v2A = VersionTwo.Builder()
        .i(111)
        .v2_i(12345)
        .v2_s("222")
        .v2_f32(67890)
        .build()
    val v2B = VersionTwo.Builder()
        .v2_f64(98765L)
        .build()
    val v2C = VersionTwo.Builder()
        .v2_rs(listOf("1", "2"))
        .build()
    // A combination of v1A and v1B.
    val v2AB = v2A.newBuilder()
        .v2_f64(v2B.v2_f64)
        .build()
    // A combination of v1A, v1B and v1C.
    val v2All = v2AB.newBuilder()
        .v2_rs(v2C.v2_rs)
        .build()

    // Serializes v2A and decodes it as a VersionOne.
    val v2ABytes = v2Adapter.encode(v2A)
    val v1 = v1Adapter.decode(v2ABytes)
    val v1Builder = v1.newBuilder()

    // Serializes v2B and decodes it as a VersionOne.
    val v2BBytes = v2Adapter.encode(v2B)
    val v1B = v1Adapter.decode(v2BBytes)

    // Serializes v2C and decodes it as a VersionOne.
    val v2CBytes = v2Adapter.encode(v2C)
    val v1C = v1Adapter.decode(v2CBytes)

    // Adds the unknown fields of v1B to v1Builder. The built message should equal to v2AB.
    val v1AB = v1Builder.addUnknownFields(v1B.unknownFields).build()
    val fromV1AB = v2Adapter.decode(v1Adapter.encode(v1AB))
    assertThat(fromV1AB).isEqualTo(v2AB)
    assertThat(fromV1AB.i).isEqualTo(111)
    assertThat(fromV1AB.v2_i).isEqualTo(12345)
    assertThat(fromV1AB.v2_s).isEqualTo("222")
    assertThat(fromV1AB.v2_f32).isEqualTo(67890)
    assertThat(fromV1AB.v2_f64).isEqualTo(98765L)
    assertThat(fromV1AB.v2_rs).isEmpty()

    // Also Adds the unknown fields of v1C to v1Builder. The built message should equals to v2All.
    val v1All = v1Builder.addUnknownFields(v1C.unknownFields).build()
    val fromV1All = v2Adapter.decode(v1Adapter.encode(v1All))
    assertThat(fromV1All).isEqualTo(v2All)
    assertThat(fromV1All.i).isEqualTo(111)
    assertThat(fromV1All.v2_i).isEqualTo(12345)
    assertThat(fromV1All.v2_s).isEqualTo("222")
    assertThat(fromV1All.v2_f32).isEqualTo(67890)
    assertThat(fromV1All.v2_f64).isEqualTo(98765L)
    assertThat(fromV1All.v2_rs).containsExactly("1", "2")
  }

  @Test
  fun unknownFieldsCanBeAddedAfterClearingUnknownFields() {
    val v2 = VersionTwo.Builder()
        .i(111)
        .v2_i(12345)
        .v2_s("222")
        .v2_f32(67890)
        .v2_f64(98765L)
        .v2_rs(listOf("1", "2"))
        .build()

    // Serializes v2 and decodes it as a VersionOne.
    val v2Bytes = v2Adapter.encode(v2)
    val v1 = v1Adapter.decode(v2Bytes)
    val v1Builder = v1.newBuilder()

    // Clears the unknown fields from v1Builder.
    val v1Known = v1Builder.clearUnknownFields().build()
    assertThat(v1Known.unknownFields).isEqualTo(ByteString.EMPTY)

    // Adds unknown fields of v1 to v1Builder.
    val addedUnknown = v1Builder.addUnknownFields(v1.unknownFields).build()
    assertThat(addedUnknown.unknownFields).isEqualTo(v1.unknownFields)
  }

  @Test
  fun addedUnknownFieldsCanBeClearedFromBuilder() {
    val v2 = VersionTwo.Builder()
        .i(111)
        .v2_i(12345)
        .v2_s("222")
        .v2_f32(67890)
        .build()

    // Serializes v2 and decodes it as a VersionOne.
    val v2Bytes = v2Adapter.encode(v2)
    val fromV2 = v1Adapter.decode(v2Bytes)

    // Adds unknown fields to an empty builder and clears them again.
    val emptyV1 = VersionOne.Builder()
        .addUnknownFields(fromV2.unknownFields)
        .clearUnknownFields()
        .build()
    assertThat(emptyV1.unknownFields).isEqualTo(ByteString.EMPTY)
  }
}
