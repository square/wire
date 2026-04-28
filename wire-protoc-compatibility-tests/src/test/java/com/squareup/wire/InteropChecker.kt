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
import assertk.assertions.support.expected
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString

class InteropChecker(
  private val protocMessage: Message,

  /** JSON representation of the message expected for Wire and Protoc using all integrations. */
  private val canonicalJson: String,

  /**
   * In proto2, JSON encoding was not specified by the protocol buffers spec. Wire uses
   * snake_case everywhere and protoc uses camelCase everywhere. Avoiding gross difference is one of
   * the best reasons to upgrade to proto3.
   */
  private val wireCanonicalJson: String = canonicalJson,

  /** JSON representations that should also decode to the message. */
  private val alternateJsons: List<String> = listOf(),

  /** Alternate forms of JSON we expect Wire to support but protoc doesn't. */
  private val wireAlternateJsons: List<String> = listOf(),
) {
  private var protocBytes: ByteString? = null

  private val typeRegistry = JsonFormat.TypeRegistry.newBuilder()
    .build()

  private val jsonPrinter = JsonFormat.printer()
    .omittingInsignificantWhitespace()
    .usingTypeRegistry(typeRegistry)

  private val jsonParser = JsonFormat.parser()
    .usingTypeRegistry(typeRegistry)

  private val gson = GsonBuilder()
    .registerTypeAdapterFactory(WireTypeAdapterFactory())
    .disableHtmlEscaping()
    .create()

  private val moshi = Moshi.Builder()
    .add(WireJsonAdapterFactory())
    .build()

  fun check(message: Any) {
    protocBytes = protocMessage.toByteArray().toByteString()

    roundtripProtocJson()
    roundtripWireBytes(message)
    roundtripGson(message)
    roundtripMoshi(message)
  }

  private fun roundtripProtocJson() {
    assertThat(jsonPrinter.print(protocMessage)).isEqualTo(canonicalJson)

    val parsed = jsonParser.parse(canonicalJson)
    assertThat(parsed).isEqualTo(protocMessage)
    for (json in alternateJsons) {
      assertThat(jsonParser.parse(json)).isEqualTo(protocMessage)
    }
  }

  private fun JsonFormat.Parser.parse(json: String): Message = protocMessage.newBuilderForType()
    .apply { merge(json, this) }
    .build()

  private fun roundtripWireBytes(message: Any) {
    val adapter = ProtoAdapter.get(message::class.java)

    @Suppress("UNCHECKED_CAST")
    val wireBytes = (adapter as ProtoAdapter<Any>).encodeByteString(message)
    assertProtobufByteStringEquality(expected = protocBytes!!, actual = wireBytes)
    assertProtobufByteStringEquality(expected = adapter.encodeByteString(message), actual = wireBytes)
    assertThat(adapter.decode(protocBytes!!)).isEqualTo(message)
  }

  private fun roundtripGson(message: Any) {
    @Suppress("UNCHECKED_CAST")
    val adapter = gson.getAdapter(message::class.java) as TypeAdapter<Any>

    assertJsonEquals(expected = wireCanonicalJson, value = adapter.toJson(message))
    val fromJson = adapter.fromJson(wireCanonicalJson)
    assertThat(fromJson, displayActual = {
      """
      |Expected :$message (unknownFields: `${(message as com.squareup.wire.Message<*, *>).unknownFields.hex()}`)
      |Actual   :$fromJson (unknownFields: `${(fromJson as com.squareup.wire.Message<*, *>).unknownFields.hex()}`)
      """.trimMargin()
    })
      .isEqualTo(message)
    for (json in alternateJsons + wireAlternateJsons) {
      assertThat(adapter.fromJson(json)).isEqualTo(message)
    }
  }

  private fun roundtripMoshi(message: Any) {
    @Suppress("UNCHECKED_CAST")
    val adapter = moshi.adapter(message::class.java) as JsonAdapter<Any>

    assertJsonEquals(expected = wireCanonicalJson, value = adapter.toJson(message))
    val fromJson = adapter.fromJson(wireCanonicalJson)
    assertThat(fromJson, displayActual = {
      """
      |Expected :$message (unknownFields: `${(message as com.squareup.wire.Message<*, *>).unknownFields.hex()}`)
      |Actual   :$fromJson (unknownFields: `${(fromJson as com.squareup.wire.Message<*, *>).unknownFields.hex()}`)
      """.trimMargin()
    })
      .isEqualTo(message)
    for (json in alternateJsons + wireAlternateJsons) {
      assertThat(adapter.fromJson(json)).isEqualTo(message)
    }
  }

  /**
   * Protoc and Wire might serialize their Protobuf messages in a different order. We thus try two
   * comparisons.
   */
  fun assertProtobufByteStringEquality(expected: ByteString, actual: ByteString) {
    if (actual == expected) return

    val sortedActual = actual.sortedByTagUnsafe()
    if (sortedActual == expected) return

    assertThat(actual).expected(":<$expected> but was:<$actual> or sorted by tags: <$sortedActual>")
  }
}

/**
 * Not safe for production code. Returns a re-encoded copy of this protobuf message with fields
 * ordered by tag number. Fields with the same tag (e.g. repeated fields) retain their relative
 * order. Nested message bytes are kept opaque. This is used solely for testing.
 */
private fun ByteString.sortedByTagUnsafe(): ByteString {
  data class Record(val tag: Int, val fieldEncoding: FieldEncoding, val value: Any)

  val records = mutableListOf<Record>()
  val reader = ProtoReader(Buffer().write(this))

  reader.forEachTag { tag ->
    val fieldEncoding = reader.peekFieldEncoding()!!

    @Suppress("UNCHECKED_CAST")
    val value = (fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>).decode(reader)
    records.add(Record(tag, fieldEncoding, value))
  }

  records.sortBy { it.tag }

  val buffer = Buffer()
  val writer = ProtoWriter(buffer)
  for ((tag, fieldEncoding, value) in records) {
    @Suppress("UNCHECKED_CAST")
    (fieldEncoding.rawProtoAdapter() as ProtoAdapter<Any>).encodeWithTag(writer, tag, value)
  }
  return buffer.readByteString()
}
