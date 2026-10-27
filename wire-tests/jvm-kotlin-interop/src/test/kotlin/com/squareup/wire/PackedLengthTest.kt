/*
 * Copyright (C) 2015 Square, Inc.
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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import kotlin.test.Test
import okio.Buffer
import okio.EOFException
import okio.ForwardingSource
import okio.buffer

class PackedLengthTest {
  @Test fun generatedPackedAdaptersRejectTruncatedPayloadWithoutLargeAllocation() {
    // Cover every packed scalar list and every supported primitive array in all_types.proto.
    for (tag in (301..313) + (601..612)) {
      val source = Buffer()
      val writer = ProtoWriter(source)
      writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
      writer.writeVarint32(Int.MAX_VALUE)
      val encoded = source.readByteArray()

      // Fail safely before invoking generated code if the allocation hint regresses.
      val reader = ProtoReader(Buffer().write(encoded))
      reader.beginMessage()
      assertThat(reader.nextTag()).isEqualTo(tag)
      assertThat(reader.nextFieldMinLengthInBytes()).isEqualTo(0L)

      assertFailure {
        AllTypes.ADAPTER.decode(Buffer().write(encoded))
      }.isInstanceOf<EOFException>()
      assertFailure {
        AllTypes.ADAPTER.decode(encoded)
      }.isInstanceOf<EOFException>()
    }
  }

  @Test fun generatedPackedAdaptersDecodeOneByteAtATime() {
    val expected = AllTypes.ADAPTER.decode(TestAllTypesData.expectedOutput.toByteArray())
      .newBuilder()
      .array_int32(intArrayOf(1, 2))
      .array_int64(longArrayOf(1, 2))
      .array_float(floatArrayOf(1.0f, 2.0f))
      .array_double(doubleArrayOf(1.0, 2.0))
      .build()
    val source = object : ForwardingSource(Buffer().write(AllTypes.ADAPTER.encode(expected))) {
      override fun read(sink: Buffer, byteCount: Long): Long = super.read(sink, minOf(byteCount, 1L))
    }.buffer()

    assertThat(AllTypes.ADAPTER.decode(source)).isEqualTo(expected)
  }
}
