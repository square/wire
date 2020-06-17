/*
 * Copyright 2020 Square Inc.
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

import com.google.protobuf.Empty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import squareup.proto3.alltypes.AllEmpty
import squareup.proto3.alltypes.AllEmptyOuterClass
import com.squareup.wire.Empty as WireEmpty

class EmptyRoundTripTest {
  @Test fun empty() {
    val googleMessage = Empty.newBuilder().build()

    val wireMessage = WireEmpty

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.EMPTY.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.EMPTY.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.EMPTY.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun allEmpty() {
    val googleMessage = AllEmptyOuterClass.AllEmpty.newBuilder()
        .setEmpty(Empty.newBuilder().build())
        .addRepEmpty(Empty.newBuilder().build())
        .addRepEmpty(Empty.newBuilder().build())
        .putMapInt32Empty(1, Empty.newBuilder().build())
        .setOneofEmpty(Empty.newBuilder().build())
        .build()

    val wireMessage = AllEmpty(
        empty = WireEmpty,
        rep_empty = listOf(WireEmpty, WireEmpty),
        map_int32_empty = mapOf(1 to WireEmpty),
        oneof_empty = WireEmpty
    )

    val googleMessageBytes = googleMessage.toByteArray()
    val wireMessageBytes = AllEmpty.ADAPTER.encode(wireMessage)
    assertThat(AllEmptyOuterClass.AllEmpty.parseFrom(wireMessageBytes)).isEqualTo(googleMessage)
    assertThat(AllEmpty.ADAPTER.decode(wireMessageBytes)).isEqualTo(wireMessage)
    assertThat(AllEmpty.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(AllEmpty.ADAPTER.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }
}
