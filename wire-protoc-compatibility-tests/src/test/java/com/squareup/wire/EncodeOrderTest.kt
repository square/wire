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
import okio.Buffer
import okio.ByteString.Companion.toByteString
import org.junit.Test
import squareup.encodeorder.EncodeOrder as P
import squareup.encodeorder.Extendable
import squareup.encodeorder.JavaKeyLike
import squareup.encodeorder.JavaOneofFirst
import squareup.encodeorder.KeyLike
import squareup.encodeorder.OneofFirst
import squareup.encodeorder.OutOfOrder
import squareup.encodeorder.SealedSplitOneof
import squareup.encodeorder.SealedTwoOneofs
import squareup.encodeorder.SplitOneof
import squareup.encodeorder.TwoOneofs

/** Confirms that `encodeInTagOrder` gives the same bytes as protoc. */
class EncodeOrderTest {
  @Test fun plainFields() {
    assertSameBytes(
      P.OutOfOrder.newBuilder().setA("A").setB("B").build(),
      OutOfOrder(a = "A", b = "B"),
    )
    assertSameBytes(
      P.KeyLike.newBuilder().setId(7).setName("x").build(),
      KeyLike(id = 7, name = "x"),
    )
  }

  @Test fun flatOneof() {
    assertSameBytes(
      P.OneofFirst.newBuilder().setA("A").setC("C").build(),
      OneofFirst(a = "A", c = "C"),
    )
    assertSameBytes(
      P.OneofFirst.newBuilder().setB("B").setC("C").build(),
      OneofFirst(b = "B", c = "C"),
    )
  }

  @Test fun boxedOneof() {
    assertSameBytes(
      P.SplitOneof.newBuilder().setA("A").setC("C").build(),
      SplitOneof(o = OneOf(SplitOneof.O_A, "A"), c = "C"),
    )
    assertSameBytes(
      P.SplitOneof.newBuilder().setB("B").setC("C").build(),
      SplitOneof(o = OneOf(SplitOneof.O_B, "B"), c = "C"),
    )
    assertSameBytes(
      P.SplitOneof.newBuilder().setD("D").setC("C").build(),
      SplitOneof(o = OneOf(SplitOneof.O_D, "D"), c = "C"),
    )
  }

  @Test fun interleavedBoxedOneofs() {
    assertSameBytes(
      P.TwoOneofs.newBuilder().setC("C").setB("B").build(),
      TwoOneofs(x = OneOf(TwoOneofs.X_C, "C"), y = OneOf(TwoOneofs.Y_B, "B")),
    )
    assertSameBytes(
      P.TwoOneofs.newBuilder().setE("E").setD("D").build(),
      TwoOneofs(x = OneOf(TwoOneofs.X_E, "E"), y = OneOf(TwoOneofs.Y_D, "D")),
    )
  }

  @Test fun sealedOneofs() {
    assertSameBytes(
      P.SealedSplitOneof.newBuilder().setA("A").setC("C").build(),
      SealedSplitOneof(o = SealedSplitOneof.O.A("A"), c = "C"),
    )
    assertSameBytes(
      P.SealedSplitOneof.newBuilder().setB("B").setC("C").build(),
      SealedSplitOneof(o = SealedSplitOneof.O.B("B"), c = "C"),
    )
    assertSameBytes(
      P.SealedTwoOneofs.newBuilder().setC("C").setB("B").build(),
      SealedTwoOneofs(x = SealedTwoOneofs.X.C("C"), y = SealedTwoOneofs.Y.B("B")),
    )
  }

  @Test fun extensions() {
    assertSameBytes(
      P.Extendable.newBuilder()
        .setA("A")
        .setC("C")
        .setExtension(P.extB, "B")
        .setExtension(P.extD, "D")
        .build(),
      Extendable(a = "A", c = "C", ext_b = "B", ext_d = "D"),
    )
  }

  @Test fun java() {
    assertSameBytes(
      P.JavaKeyLike.newBuilder().setId(7).setName("x").build(),
      JavaKeyLike.Builder().id(7).name("x").build(),
    )
    assertSameBytes(
      P.JavaOneofFirst.newBuilder().setA("A").setC("C").build(),
      JavaOneofFirst.Builder().a("A").c("C").build(),
    )
    assertSameBytes(
      P.JavaOneofFirst.newBuilder().setB("B").setC("C").build(),
      JavaOneofFirst.Builder().b("B").c("C").build(),
    )
  }

  /** Checks the reverse writer and the forward writer. */
  private fun assertSameBytes(protoc: com.google.protobuf.Message, wire: Message<*, *>) {
    val expected = protoc.toByteArray().toByteString()
    assertThat(wire.encodeByteString()).isEqualTo(expected)

    @Suppress("UNCHECKED_CAST")
    val adapter = wire.adapter as ProtoAdapter<Message<*, *>>
    val buffer = Buffer()
    adapter.encode(ProtoWriter(buffer), wire)
    assertThat(buffer.readByteString()).isEqualTo(expected)
  }
}
