/*
 * Copyright (C) 2019 Square, Inc.
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

import assertk.assertions.message
import com.squareup.wire.protos.kotlin.edgecases.NoFields
import com.squareup.wire.protos.kotlin.edgecases.OneField
import com.squareup.wire.protos.usesany.UsesAny
import kotlin.jvm.JvmField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class AnyMessageTest {
  @Test fun happyPath() {
    val three = OneField(opt_int32 = 3)
    val four = OneField(opt_int32 = 4)

    val usesAny = UsesAny(
      just_one = AnyMessage.pack(three),
      many_anys = listOf(
        AnyMessage.pack(three),
        AnyMessage.pack(four),
      ),
    )

    assertEquals(three, usesAny.just_one!!.unpack(OneField.ADAPTER))
    assertEquals(listOf(three, four), usesAny.many_anys.map { it.unpack(OneField.ADAPTER) })

    assertFailsWith<IllegalStateException> {
      usesAny.just_one.unpack(ProtoAdapter.BOOL)
    }
    assertEquals(null, usesAny.just_one.unpackOrNull(ProtoAdapter.BOOL))
  }

  @Test fun encodeAndDecode() {
    val hex = "0a430a3d747970652e676f6f676c65617069732e636f6d2f73717561726575702e70726f746f732e6b" +
      "6f746c696e2e6564676563617365732e4f6e654669656c641202080312430a3d747970652e676f6f676c6561" +
      "7069732e636f6d2f73717561726575702e70726f746f732e6b6f746c696e2e6564676563617365732e4f6e65" +
      "4669656c641202080312430a3d747970652e676f6f676c65617069732e636f6d2f73717561726575702e7072" +
      "6f746f732e6b6f746c696e2e6564676563617365732e4f6e654669656c6412020804"

    val three = OneField(opt_int32 = 3)
    val four = OneField(opt_int32 = 4)

    val usesAny = UsesAny(
      just_one = AnyMessage.pack(three),
      many_anys = listOf(
        AnyMessage.pack(three),
        AnyMessage.pack(four),
      ),
    )

    assertEquals(hex, usesAny.encodeByteString().hex())
    assertEquals(usesAny, UsesAny.ADAPTER.decode(hex.decodeHex()))
  }

  @Test fun decodingWithMissingTypeUrl() {
    val hex = "0a0412020803120412020803120412020804"

    val usesAny = UsesAny.ADAPTER.decode(hex.decodeHex())
    assertEquals("", usesAny.just_one!!.typeUrl)
    assertEquals(2, usesAny.many_anys.size)
    for (manyAny in usesAny.many_anys) {
      assertEquals("", manyAny.typeUrl)
    }
  }

  @Test fun unpackWithWrongTypeUrlThrow() {
    val usesAny = UsesAny(
      just_one = AnyMessage.pack(OneField(opt_int32 = 3)),
      many_anys = listOf(),
    )

    try {
      usesAny.just_one!!.unpack(NoFields.ADAPTER)
      fail()
    } catch (expected: IllegalStateException) {
      assertEquals(
        "type mismatch: type.googleapis.com/squareup.protos.kotlin.edgecases.OneField " +
          "!= type.googleapis.com/squareup.protos.kotlin.edgecases.NoFields",
        expected.message,
      )
    }
  }

  @Test fun unpackOrNullWithWrongTypeUrlReturnsNull() {
    val usesAny = UsesAny(
      just_one = AnyMessage.pack(OneField(opt_int32 = 3)),
      many_anys = listOf(),
    )

    assertNull(usesAny.just_one!!.unpackOrNull(NoTypeUrlMessage.ADAPTER))
  }

  @Test fun packWithoutATypeUrlThrows() {
    try {
      AnyMessage.pack(NoTypeUrlMessage())
      fail()
    } catch (expected: IllegalStateException) {
      assertTrue(expected.message!!.startsWith("recompile class "), expected.message)
      assertTrue(
        expected.message!!.endsWith("NoTypeUrlMessage to use it with AnyMessage"),
        expected.message,
      )
    }
  }

  private class NoTypeUrlMessage(
    unknownFields: ByteString = ByteString.EMPTY,
  ) : Message<NoTypeUrlMessage, Nothing>(ADAPTER, unknownFields) {
    override fun newBuilder(): Nothing = throw AssertionError()

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<NoTypeUrlMessage> = object : ProtoAdapter<NoTypeUrlMessage>(
        FieldEncoding.LENGTH_DELIMITED,
        NoTypeUrlMessage::class,
        null, // TypeUrl.
        Syntax.PROTO_2,
        null, // Identity.
      ) {
        override fun encodedSize(value: NoTypeUrlMessage) = TODO()
        override fun encode(writer: ProtoWriter, value: NoTypeUrlMessage) = TODO()
        override fun encode(writer: ReverseProtoWriter, value: NoTypeUrlMessage) = TODO()
        override fun decode(reader: ProtoReader): NoTypeUrlMessage = TODO()
        override fun redact(value: NoTypeUrlMessage): NoTypeUrlMessage = TODO()
      }
    }
  }
}
