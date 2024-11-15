/*
 * Copyright (C) 2020 Square, Inc.
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
import com.google.protobuf.Empty
import org.junit.Test
import squareup.proto3.java.alltypes.AllEmpty as AllEmptyJ
import squareup.proto3.kotlin.alltypes.AllEmpty as AllEmptyK
import squareup.proto3.kotlin.alltypes.AllEmptyOuterClass

class EmptyRoundTripTest {
  @Test fun empty() {
    val googleMessage = Empty.newBuilder().build()

    val wireMessage = Unit

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

    val wireMessageJava = AllEmptyJ.Builder()
      .empty(Unit)
      .rep_empty(listOf(Unit, Unit))
      .map_int32_empty(mapOf(1 to Unit))
      .oneof_empty(Unit)
      .build()

    val wireMessageKotlin = AllEmptyK(
      empty = Unit,
      rep_empty = listOf(Unit, Unit),
      map_int32_empty = mapOf(1 to Unit),
      oneof_empty = Unit,
    )

    val googleMessageBytes = googleMessage.toByteArray()
    val wireMessageJavaBytes = AllEmptyJ.ADAPTER.encode(wireMessageJava)
    assertThat(AllEmptyOuterClass.AllEmpty.parseFrom(wireMessageJavaBytes)).isEqualTo(googleMessage)
    assertThat(AllEmptyJ.ADAPTER.decode(wireMessageJavaBytes)).isEqualTo(wireMessageJava)
    assertThat(AllEmptyJ.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessageJava)
    assertThat(AllEmptyJ.ADAPTER.encodedSize(wireMessageJava)).isEqualTo(googleMessageBytes.size)
    val wireMessageKotlinBytes = AllEmptyK.ADAPTER.encode(wireMessageKotlin)
    assertThat(AllEmptyOuterClass.AllEmpty.parseFrom(wireMessageKotlinBytes)).isEqualTo(googleMessage)
    assertThat(AllEmptyK.ADAPTER.decode(wireMessageKotlinBytes)).isEqualTo(wireMessageKotlin)
    assertThat(AllEmptyK.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessageKotlin)
    assertThat(AllEmptyK.ADAPTER.encodedSize(wireMessageKotlin)).isEqualTo(googleMessageBytes.size)
  }
}
