/*
 * Copyright 2021 Square Inc.
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

import com.squareup.wire.kotlinxserialization.asSerializer
import com.squareup.wire.protos.kotlin.person.Person
import com.squareup.wire.protos.kotlin.person.Person.PhoneNumber
import com.squareup.wire.protos.kotlin.person.Person.PhoneType.MOBILE
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class KotlinxSerializationTest {
  @Test fun encodeAndDecodeAsJson() {
    val expectedModel = Person(
      id = 1,
      name = "Sandy Winchester",
      phone = listOf(
        PhoneNumber(
          number = "555-1234",
          type = MOBILE
        )
      )
    )
    val serializer = Person.ADAPTER.asSerializer()

    val expectedJson = "[" +
      "10,16,83,97,110,100,121,32,87,105,110,99,104,101,115," +
      "116,101,114,16,1,34,12,10,8,53,53,53,45,49,50,51,52,16,0" +
      "]"
    val actualJson = Json.encodeToString(serializer, expectedModel)
    assertEquals(expectedJson, actualJson)

    val actualModel = Json.decodeFromString(serializer, expectedJson)
    assertEquals(expectedModel, actualModel)
  }

  /**
   * When using [Protobuf] and [asSerializer] for the top-level value, the output is prefixed with
   * the varint32 length of the encoded data. This is bad! But it's symmetric for encoding and
   * decoding.
   */
  @Test fun encodeAndDecodeAsKotlinxSerializationProtobufTopLevel() {
    val expectedModel = Person(
      id = 1,
      name = "Sandy Winchester",
      phone = listOf(
        PhoneNumber(
          number = "555-1234",
          type = MOBILE
        )
      )
    )

    val wireBytes = expectedModel.encodeByteString()
    val expectedBytes = Buffer()
      .writeByte(wireBytes.size)
      .write(wireBytes)
      .readByteString()

    val serializer = Person.ADAPTER.asSerializer()

    val actualBytes = ProtoBuf.encodeToByteArray(serializer, expectedModel).toByteString()
    assertEquals(expectedBytes, actualBytes)

    val actualModel = ProtoBuf.decodeFromByteArray(serializer, expectedBytes.toByteArray())
    assertEquals(expectedModel, actualModel)
  }

  /**
   * This test demonstrates embedding a Wire message in a kotlinx.serialization message. This
   * encodes to the canonical bytes.
   */
  @Test fun encodeAndDecodeAsKotlinxSerializationProtobufMember() {
    val expectedModel = KPerson(
      id = 1,
      name = "Sandy Winchester",
      phone = PhoneNumber(
        number = "555-1234",
        type = MOBILE
      )
    )

    val expectedBytes = "0a1053616e64792057696e636865737465721001220c0a083535352d313233341000"
      .decodeHex()

    val protoBuf = ProtoBuf {
      serializersModule = SerializersModule {
        contextual(PhoneNumber::class, PhoneNumber.ADAPTER.asSerializer())
      }
    }

    val actualBytes = protoBuf.encodeToByteArray(KPerson.serializer(), expectedModel).toByteString()
    assertEquals(expectedBytes, actualBytes)

    val actualModel = protoBuf.decodeFromByteArray(KPerson.serializer(), actualBytes.toByteArray())
    assertEquals(expectedModel, actualModel)
  }

  @Serializable
  data class KPerson(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val id: Int,
    @ProtoNumber(3) val email: String? = null,
    @ProtoNumber(4) @Contextual val phone: PhoneNumber?
  )
}
