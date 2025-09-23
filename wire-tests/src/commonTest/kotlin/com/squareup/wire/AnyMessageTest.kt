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

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import assertk.assertions.startsWith
import com.squareup.wire.protos.kotlin.edgecases.NoFields
import com.squareup.wire.protos.kotlin.edgecases.OneField
import com.squareup.wire.protos.usesany.UsesAny
import kotlin.IllegalStateException
import kotlin.jvm.JvmField
import kotlin.test.Test
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

    assertThat(usesAny.just_one!!.unpack(OneField.ADAPTER)).isEqualTo(three)
    assertThat(usesAny.many_anys.map { it.unpack(OneField.ADAPTER) }).isEqualTo(
      listOf(three, four),
    )

    assertFailure {
      usesAny.just_one.unpack(ProtoAdapter.BOOL)
    }.isInstanceOf<IllegalStateException>()
    assertThat(usesAny.just_one.unpackOrNull(ProtoAdapter.BOOL)).isEqualTo(null)
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

    assertThat(usesAny.encodeByteString().hex()).isEqualTo(hex)
    assertThat(UsesAny.ADAPTER.decode(hex.decodeHex())).isEqualTo(usesAny)
  }

  @Test fun decodingWithMissingTypeUrl() {
    val hex = "0a0412020803120412020803120412020804"

    val usesAny = UsesAny.ADAPTER.decode(hex.decodeHex())
    assertThat(usesAny.just_one!!.typeUrl).isEqualTo("")
    assertThat(usesAny.many_anys.size).isEqualTo(2)
    for (manyAny in usesAny.many_anys) {
      assertThat(manyAny.typeUrl).isEqualTo("")
    }
  }

  @Test fun unpackWithWrongTypeUrlThrow() {
    val usesAny = UsesAny(
      just_one = AnyMessage.pack(OneField(opt_int32 = 3)),
      many_anys = listOf(),
    )

    assertFailure {
      usesAny.just_one!!.unpack(NoFields.ADAPTER)
    }.isInstanceOf<IllegalStateException>().hasMessage(
      "type mismatch: type.googleapis.com/squareup.protos.kotlin.edgecases.OneField " +
        "!= type.googleapis.com/squareup.protos.kotlin.edgecases.NoFields",
    )
  }

  @Test fun unpackOrNullWithWrongTypeUrlReturnsNull() {
    val usesAny = UsesAny(
      just_one = AnyMessage.pack(OneField(opt_int32 = 3)),
      many_anys = listOf(),
    )

    assertThat(usesAny.just_one!!.unpackOrNull(NoTypeUrlMessage.ADAPTER)).isNull()
  }

  @Test fun packWithoutATypeUrlThrows() {
    assertFailure {
      AnyMessage.pack(NoTypeUrlMessage())
    }.isInstanceOf<IllegalStateException>()
      .message().isNotNull().all {
        startsWith("recompile class ")
        endsWith("NoTypeUrlMessage to use it with AnyMessage")
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
