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
import com.google.gson.GsonBuilder
import com.google.protobuf.FieldMask as GoogleFieldMask
import com.google.protobuf.util.JsonFormat
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import org.junit.Test

class FieldMaskRoundTripTest {
  private val moshi = Moshi.Builder()
    .add(WireJsonAdapterFactory())
    .build()

  private val gson = GsonBuilder()
    .registerTypeAdapterFactory(WireTypeAdapterFactory())
    .disableHtmlEscaping()
    .create()

  @Test fun `binary round trip`() {
    val googleMessage = GoogleFieldMask.newBuilder()
      .addPaths("user.display_name")
      .addPaths("photo")
      .build()

    val wireMessage = FieldMask(listOf("user.display_name", "photo"))

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.FIELD_MASK.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.FIELD_MASK.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.FIELD_MASK.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `empty binary round trip`() {
    val googleMessage = GoogleFieldMask.newBuilder().build()

    val wireMessage = FieldMask()

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(ProtoAdapter.FIELD_MASK.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(ProtoAdapter.FIELD_MASK.decode(googleMessageBytes)).isEqualTo(wireMessage)
    assertThat(ProtoAdapter.FIELD_MASK.encodedSize(wireMessage)).isEqualTo(googleMessageBytes.size)
  }

  @Test fun `json round trip`() {
    val googleMessage = GoogleFieldMask.newBuilder()
      .addPaths("user.display_name")
      .addPaths("photo")
      .addPaths("foo_bar.baz_qux")
      .build()

    val wireMessage = FieldMask(listOf("user.display_name", "photo", "foo_bar.baz_qux"))

    val googleJson = JsonFormat.printer().print(googleMessage)
    assertThat(googleJson).isEqualTo("\"user.displayName,photo,fooBar.bazQux\"")

    assertThat(moshi.adapter(FieldMask::class.java).toJson(wireMessage)).isEqualTo(googleJson)
    assertThat(gson.toJson(wireMessage, FieldMask::class.java)).isEqualTo(googleJson)

    val googleParsed = GoogleFieldMask.newBuilder()
      .apply { JsonFormat.parser().merge(googleJson, this) }
      .build()
    assertThat(googleParsed).isEqualTo(googleMessage)
    assertThat(moshi.adapter(FieldMask::class.java).fromJson(googleJson)).isEqualTo(wireMessage)
    assertThat(gson.fromJson(googleJson, FieldMask::class.java)).isEqualTo(wireMessage)
  }

  @Test fun `degenerate paths match protobuf-java`() {
    // These paths violate the protobuf style guide and don't survive the camelCase round trip.
    // Wire intentionally matches protobuf-java's lossy conversions instead of erroring, so both
    // the printed JSON and the re-parsed paths must be identical to protobuf-java's.
    val paths = listOf("fooBar", "_foo", "foo_", "foo__bar", "foo_1bar", "Foo.Bar")
    val googleMessage = GoogleFieldMask.newBuilder().addAllPaths(paths).build()
    val wireMessage = FieldMask(paths)

    val googleJson = JsonFormat.printer().print(googleMessage)
    assertThat(moshi.adapter(FieldMask::class.java).toJson(wireMessage)).isEqualTo(googleJson)
    assertThat(gson.toJson(wireMessage, FieldMask::class.java)).isEqualTo(googleJson)

    val googleParsed = GoogleFieldMask.newBuilder()
      .apply { JsonFormat.parser().merge(googleJson, this) }
      .build()
    val wireParsed = moshi.adapter(FieldMask::class.java).fromJson(googleJson)!!
    assertThat(wireParsed.paths).isEqualTo(googleParsed.pathsList)
    assertThat(gson.fromJson(googleJson, FieldMask::class.java)).isEqualTo(wireParsed)
  }

  @Test fun `binary round trip packed in any`() {
    val googleMessage = com.google.protobuf.Any.pack(
      GoogleFieldMask.newBuilder()
        .addPaths("user.display_name")
        .addPaths("photo")
        .build(),
    )

    val wireMessage = AnyMessage.pack(
      ProtoAdapter.FIELD_MASK,
      FieldMask(listOf("user.display_name", "photo")),
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(AnyMessage.ADAPTER.encode(wireMessage)).isEqualTo(googleMessageBytes)
    assertThat(AnyMessage.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun `json round trip packed in any`() {
    val googleMessage = com.google.protobuf.Any.pack(
      GoogleFieldMask.newBuilder()
        .addPaths("user.display_name")
        .addPaths("photo")
        .build(),
    )

    val wireMessage = AnyMessage.pack(
      ProtoAdapter.FIELD_MASK,
      FieldMask(listOf("user.display_name", "photo")),
    )

    val typeRegistry = JsonFormat.TypeRegistry.newBuilder()
      .add(GoogleFieldMask.getDescriptor())
      .build()

    val googleJson = JsonFormat.printer().usingTypeRegistry(typeRegistry).print(googleMessage)
    assertJsonEquals(googleJson, moshi.adapter(AnyMessage::class.java).toJson(wireMessage))
    assertJsonEquals(googleJson, gson.toJson(wireMessage, AnyMessage::class.java))

    val googleParsed = com.google.protobuf.Any.newBuilder()
      .apply { JsonFormat.parser().usingTypeRegistry(typeRegistry).merge(googleJson, this) }
      .build()
    assertThat(googleParsed).isEqualTo(googleMessage)
    assertThat(moshi.adapter(AnyMessage::class.java).fromJson(googleJson)).isEqualTo(wireMessage)
    assertThat(gson.fromJson(googleJson, AnyMessage::class.java)).isEqualTo(wireMessage)
  }
}
