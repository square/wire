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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.EOFException
import okio.ForwardingSource
import okio.buffer

class LengthDelimitedScalarTest {
  @Test fun rootScalarsFromSourceReader() {
    assertRootScalars { ProtoReader(Buffer().write(it)) }
  }

  @Test fun rootScalarsFromArrayReader() {
    assertRootScalars { ProtoReader32(it).asProtoReader() }
  }

  private fun assertRootScalars(reader: (ByteString) -> ProtoReader) {
    for (bytes in listOf("00ff800108120200fe".decodeHex(), ByteString.EMPTY)) {
      val input = reader(bytes)
      assertThat(input.readBytes()).isEqualTo(bytes)
      assertThat(input.nextTag()).isEqualTo(-1)
    }
    for (text in listOf("Hello ☃\u0000", "")) {
      val input = reader(text.encodeUtf8())
      assertThat(input.readString()).isEqualTo(text)
      assertThat(input.nextTag()).isEqualTo(-1)
    }
  }

  @Test fun stringDecodeSource() {
    for (text in listOf("Hello ☃\u0000", "")) {
      assertThat(ProtoAdapter.STRING.decode(Buffer().writeUtf8(text))).isEqualTo(text)
    }
  }

  @Test fun stringDecodeByteString() {
    for (text in listOf("Hello ☃\u0000", "")) {
      assertThat(ProtoAdapter.STRING.decode(text.encodeUtf8())).isEqualTo(text)
    }
  }

  @Test fun stringDecodeByteArray() {
    for (text in listOf("Hello ☃\u0000", "")) {
      assertThat(ProtoAdapter.STRING.decode(text.encodeUtf8().toByteArray())).isEqualTo(text)
    }
  }

  @Test fun stringDecodeChunkedSource() {
    val text = "Hello ☃\u0000".repeat(2048)
    val source = object : ForwardingSource(Buffer().writeUtf8(text)) {
      override fun read(sink: Buffer, byteCount: Long): Long = super.read(sink, minOf(byteCount, 1L))
    }.buffer()
    assertThat(ProtoAdapter.STRING.decode(source)).isEqualTo(text)
  }

  @Test fun customBytesAdapter() {
    val adapter = delegatingAdapter(ProtoAdapter.BYTES)
    for (bytes in listOf("00ff800108120200fe".decodeHex(), ByteString.EMPTY)) {
      assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(bytes)
      assertThat(adapter.decode(bytes)).isEqualTo(bytes)
      assertThat(adapter.decode(bytes.toByteArray())).isEqualTo(bytes)
    }
  }

  @Test fun customStringAdapter() {
    val adapter = delegatingAdapter(ProtoAdapter.STRING)
    for (text in listOf("Hello ☃\u0000", "")) {
      val bytes = text.encodeUtf8()
      assertThat(adapter.decode(Buffer().write(bytes))).isEqualTo(text)
      assertThat(adapter.decode(bytes)).isEqualTo(text)
      assertThat(adapter.decode(bytes.toByteArray())).isEqualTo(text)
    }
  }

  @Test fun lengthPrefixedScalarsKeepNextFrame() {
    for (reader in readers("0300ff8000026869".decodeHex())) {
      assertThat(reader.nextLengthDelimited()).isEqualTo(3)
      assertThat(reader.readBytes()).isEqualTo("00ff80".decodeHex())
      assertThat(reader.nextLengthDelimited()).isEqualTo(0)
      assertThat(reader.readBytes()).isEqualTo(ByteString.EMPTY)
      assertThat(reader.nextLengthDelimited()).isEqualTo(2)
      assertThat(reader.readString()).isEqualTo("hi")
      assertThat(reader.nextTag()).isEqualTo(-1)
    }
  }

  @Test fun lengthPrefixedScalarsRejectTruncation() {
    for (reader in readers("0300ff".decodeHex())) {
      assertFailure {
        reader.nextLengthDelimited()
        reader.readBytes()
      }.isInstanceOf<EOFException>()
    }
  }

  @Test fun nestedScalarsKeepSiblingFields() {
    for (reader in readers("0a0300ff80120268691a002007".decodeHex())) {
      val token = reader.beginMessage()
      assertThat(reader.nextTag()).isEqualTo(1)
      assertThat(reader.readBytes()).isEqualTo("00ff80".decodeHex())
      assertThat(reader.nextTag()).isEqualTo(2)
      assertThat(reader.readString()).isEqualTo("hi")
      assertThat(reader.nextTag()).isEqualTo(3)
      assertThat(reader.readBytes()).isEqualTo(ByteString.EMPTY)
      assertThat(reader.nextTag()).isEqualTo(4)
      assertThat(reader.readVarint32()).isEqualTo(7)
      assertThat(reader.nextTag()).isEqualTo(-1)
      reader.endMessageAndGetUnknownFields(token)
    }
  }

  @Test fun nestedScalarsRejectTruncation() {
    for (hex in listOf("0a0300ff", "12036869")) {
      for (reader in readers(hex.decodeHex())) {
        reader.beginMessage()
        assertFailure {
          if (reader.nextTag() == 1) reader.readBytes() else reader.readString()
        }.isInstanceOf<EOFException>()
      }
    }
  }

  @Test fun arrayReaderRespectsExplicitWindow() {
    val bytes = ProtoReader32("ff00ff80ee".decodeHex(), pos = 1, limit = 4)
    assertThat(bytes.readBytes()).isEqualTo("00ff80".decodeHex())
    assertThat(bytes.nextTag()).isEqualTo(-1)
    val text = ProtoReader32("ff6869ee".decodeHex(), pos = 1, limit = 3)
    assertThat(text.readString()).isEqualTo("hi")
    assertThat(text.nextTag()).isEqualTo(-1)
    val empty = ProtoReader32("ffff".decodeHex(), pos = 1, limit = 1)
    assertThat(empty.readBytes()).isEqualTo(ByteString.EMPTY)
    assertThat(empty.nextTag()).isEqualTo(-1)
  }

  private fun readers(bytes: ByteString): List<ProtoReader> = listOf(
    ProtoReader(Buffer().write(bytes)),
    ProtoReader32(bytes).asProtoReader(),
  )

  private fun <E> delegatingAdapter(delegate: ProtoAdapter<E>): ProtoAdapter<E> = object : ProtoAdapter<E>(
    delegate.fieldEncoding,
    delegate.type,
    null,
    Syntax.PROTO_2,
  ) {
    override fun encodedSize(value: E): Int = delegate.encodedSize(value)
    override fun encode(writer: ProtoWriter, value: E) = delegate.encode(writer, value)
    override fun decode(reader: ProtoReader): E = delegate.decode(reader)
    override fun redact(value: E): E = delegate.redact(value)
  }
}
