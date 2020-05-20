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

import com.squareup.wire.proto2.simple.SimpleMessage
import com.squareup.wire.proto2.simple.SimpleMessageOuterClass
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import squareup.proto2.alltypes.AllTypes
import squareup.proto2.alltypes.AllTypesOuterClass
import squareup.proto2.alltypes.AllTypesOuterClass.extOptBool
import squareup.proto2.alltypes.AllTypesOuterClass.extPackBool
import squareup.proto2.alltypes.AllTypesOuterClass.extRepBool

class Proto2WireProtocCompatibilityTests {
  @Test fun simpleMessage() {
    val wireMessage = SimpleMessage(
        optional_nested_msg = SimpleMessage.NestedMessage(806),
        no_default_nested_enum = SimpleMessage.NestedEnum.BAR,
        repeated_double = listOf(1.0, 33.0),
        required_int32 = 46,
        other = "hello"
    )

    val googleMessage = SimpleMessageOuterClass.SimpleMessage.newBuilder()
        .setOptionalNestedMsg(
            SimpleMessageOuterClass.SimpleMessage.NestedMessage.newBuilder().setBb(806).build()
        )
        .setNoDefaultNestedEnum(SimpleMessageOuterClass.SimpleMessage.NestedEnum.BAR)
        .addAllRepeatedDouble(listOf(1.0, 33.0))
        .setRequiredInt32(46)
        .setOther("hello")
        .build()

    val encodedWireMessage: ByteArray = wireMessage.encode()
    val encodedGoogleMessage: ByteArray = googleMessage.toByteArray()

    assertThat(encodedWireMessage).isEqualTo(encodedGoogleMessage)

    val wireMessageDecodedFromGoogleMessage =
        SimpleMessage.ADAPTER.decode(encodedGoogleMessage)
    val googleMessageDecodedFromWireMessage =
        SimpleMessageOuterClass.SimpleMessage.parseFrom(encodedWireMessage)

    assertThat(wireMessageDecodedFromGoogleMessage).isEqualTo(wireMessage)
    assertThat(googleMessageDecodedFromWireMessage).isEqualTo(googleMessage)
  }

  @Test fun allTypesSerialization() {
    val byteArrayWire = AllTypes.ADAPTER.encode(defaultAllTypesWire)
    val byteArrayProtoc = defaultAllTypesProtoc.toByteArray()

    assertThat(byteArrayWire).isEqualTo(byteArrayProtoc)
    assertThat(AllTypes.ADAPTER.decode(byteArrayProtoc)).isEqualTo(defaultAllTypesWire)
    assertThat(AllTypesOuterClass.AllTypes.parseFrom(byteArrayWire))
        .isEqualTo(defaultAllTypesProtoc)
  }

  @Test fun allTypesSerializationWithEmptyOrIdentityValues() {
    val byteArrayWire = AllTypes.ADAPTER.encode(identityAllTypesWire)
    val byteArrayProtoc = identityAllTypesProtoc.toByteArray()

    assertThat(byteArrayWire).isEqualTo(byteArrayProtoc)
    assertThat(AllTypes.ADAPTER.decode(byteArrayProtoc)).isEqualTo(identityAllTypesWire)
    assertThat(AllTypesOuterClass.AllTypes.parseFrom(byteArrayWire))
        .isEqualTo(identityAllTypesProtoc)
  }

  companion object {
    private val defaultAllTypesWire = AllTypes(
        opt_int32 = 111,
        opt_uint32 = 112,
        opt_sint32 = 113,
        opt_fixed32 = 114,
        opt_sfixed32 = 115,
        opt_int64 = 116L,
        opt_uint64 = 117L,
        opt_sint64 = 118L,
        opt_fixed64 = 119L,
        opt_sfixed64 = 120L,
        opt_bool = true,
        opt_float = 122.0F,
        opt_double = 123.0,
        opt_string = "124",
        opt_bytes = ByteString.of(123, 125),
        opt_nested_enum = AllTypes.NestedEnum.A,
        opt_nested_message = AllTypes.NestedMessage(a = 999),
        req_int32 = 111,
        req_uint32 = 112,
        req_sint32 = 113,
        req_fixed32 = 114,
        req_sfixed32 = 115,
        req_int64 = 116L,
        req_uint64 = 117L,
        req_sint64 = 118L,
        req_fixed64 = 119L,
        req_sfixed64 = 120L,
        req_bool = true,
        req_float = 122.0F,
        req_double = 123.0,
        req_string = "124",
        req_bytes = ByteString.of(123, 125),
        req_nested_enum = AllTypes.NestedEnum.A,
        req_nested_message = AllTypes.NestedMessage(a = 999),
        rep_int32 = list(111),
        rep_uint32 = list(112),
        rep_sint32 = list(113),
        rep_fixed32 = list(114),
        rep_sfixed32 = list(115),
        rep_int64 = list(116L),
        rep_uint64 = list(117L),
        rep_sint64 = list(118L),
        rep_fixed64 = list(119L),
        rep_sfixed64 = list(120L),
        rep_bool = list(true),
        rep_float = list(122.0F),
        rep_double = list(123.0),
        rep_string = list("124"),
        rep_bytes = list(ByteString.of(123, 125)),
        rep_nested_enum = list(AllTypes.NestedEnum.A),
        rep_nested_message = list(AllTypes.NestedMessage(a = 999)),
        pack_int32 = list(111),
        pack_uint32 = list(112),
        pack_sint32 = list(113),
        pack_fixed32 = list(114),
        pack_sfixed32 = list(115),
        pack_int64 = list(116L),
        pack_uint64 = list(117L),
        pack_sint64 = list(118L),
        pack_fixed64 = list(119L),
        pack_sfixed64 = list(120L),
        pack_bool = list(true),
        pack_float = list(122.0F),
        pack_double = list(123.0),
        pack_nested_enum = list(AllTypes.NestedEnum.A),
        map_int32_int32 = mapOf(1 to 2),
        map_string_string = mapOf("key" to "value"),
        map_string_message = mapOf("message" to AllTypes.NestedMessage(a = 1)),
        map_string_enum = mapOf("enum" to AllTypes.NestedEnum.A),
        oneof_int32 = 0
    )

    private val defaultAllTypesProtoc = AllTypesOuterClass.AllTypes.newBuilder()
        .setOptInt32(111)
        .setOptUint32(112)
        .setOptSint32(113)
        .setOptFixed32(114)
        .setOptSfixed32(115)
        .setOptInt64(116L)
        .setOptUint64(117L)
        .setOptSint64(118L)
        .setOptFixed64(119L)
        .setOptSfixed64(120L)
        .setOptBool(true)
        .setOptFloat(122.0F)
        .setOptDouble(123.0)
        .setOptString("124")
        .setOptBytes(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray()))
        .setOptNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.A)
        .setOptNestedMessage(
            AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build())
        .setReqInt32(111)
        .setReqUint32(112)
        .setReqSint32(113)
        .setReqFixed32(114)
        .setReqSfixed32(115)
        .setReqInt64(116L)
        .setReqUint64(117L)
        .setReqSint64(118L)
        .setReqFixed64(119L)
        .setReqSfixed64(120L)
        .setReqBool(true)
        .setReqFloat(122.0F)
        .setReqDouble(123.0)
        .setReqString("124")
        .setReqBytes(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray()))
        .setReqNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.A)
        .setReqNestedMessage(
            AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build())
        .addAllRepInt32(list(111))
        .addAllRepUint32(list(112))
        .addAllRepSint32(list(113))
        .addAllRepFixed32(list(114))
        .addAllRepSfixed32(list(115))
        .addAllRepInt64(list(116L))
        .addAllRepUint64(list(117L))
        .addAllRepSint64(list(118L))
        .addAllRepFixed64(list(119L))
        .addAllRepSfixed64(list(120L))
        .addAllRepBool(list(true))
        .addAllRepFloat(list(122.0F))
        .addAllRepDouble(list(123.0))
        .addAllRepString(list("124"))
        .addAllRepBytes(
            list(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray())))
        .addAllRepNestedEnum(list(AllTypesOuterClass.AllTypes.NestedEnum.A))
        .addAllRepNestedMessage(list(
            AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build()))
        .addAllPackInt32(list(111))
        .addAllPackUint32(list(112))
        .addAllPackSint32(list(113))
        .addAllPackFixed32(list(114))
        .addAllPackSfixed32(list(115))
        .addAllPackInt64(list(116L))
        .addAllPackUint64(list(117L))
        .addAllPackSint64(list(118L))
        .addAllPackFixed64(list(119L))
        .addAllPackSfixed64(list(120L))
        .addAllPackBool(list(true))
        .addAllPackFloat(list(122.0F))
        .addAllPackDouble(list(123.0))
        .addAllPackNestedEnum(list(AllTypesOuterClass.AllTypes.NestedEnum.A))
        .putMapInt32Int32(1, 2)
        .putMapStringString("key", "value")
        .putMapStringMessage("message",
            AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(1).build())
        .putMapStringEnum("enum", AllTypesOuterClass.AllTypes.NestedEnum.A)
        .setOneofInt32(0)
        .build()

    private val identityAllTypesWire = AllTypes(
        req_int32 = 0,
        req_uint32 = 0,
        req_sint32 = 0,
        req_fixed32 = 0,
        req_sfixed32 = 0,
        req_int64 = 0L,
        req_uint64 = 0L,
        req_sint64 = 0L,
        req_fixed64 = 0L,
        req_sfixed64 = 0L,
        req_bool = false,
        req_float = 0F,
        req_double = 0.0,
        req_string = "",
        req_bytes = ByteString.EMPTY,
        req_nested_enum = AllTypes.NestedEnum.UNKNOWN,
        req_nested_message = AllTypes.NestedMessage(a = 0),
        rep_int32 = emptyList(),
        rep_uint32 = emptyList(),
        rep_sint32 = emptyList(),
        rep_fixed32 = emptyList(),
        rep_sfixed32 = emptyList(),
        rep_int64 = emptyList(),
        rep_uint64 = emptyList(),
        rep_sint64 = emptyList(),
        rep_fixed64 = emptyList(),
        rep_sfixed64 = emptyList(),
        rep_bool = emptyList(),
        rep_float = emptyList(),
        rep_double = emptyList(),
        rep_string = emptyList(),
        rep_bytes = emptyList(),
        rep_nested_enum = emptyList(),
        rep_nested_message = emptyList(),
        pack_int32 = list(0),
        pack_uint32 = list(0),
        pack_sint32 = list(0),
        pack_fixed32 = list(0),
        pack_sfixed32 = list(0),
        pack_int64 = list(0L),
        pack_uint64 = list(0L),
        pack_sint64 = list(0L),
        pack_fixed64 = list(0L),
        pack_sfixed64 = list(0L),
        pack_bool = list(false),
        pack_float = list(0F),
        pack_double = list(0.0),
        pack_nested_enum = list(AllTypes.NestedEnum.UNKNOWN),
        map_int32_int32 = mapOf(0 to 0),
        map_string_message = mapOf("" to AllTypes.NestedMessage()),
        map_string_enum = mapOf("" to AllTypes.NestedEnum.UNKNOWN)
    )

    private val identityAllTypesProtoc = AllTypesOuterClass.AllTypes.newBuilder()
        .setReqInt32(0)
        .setReqUint32(0)
        .setReqSint32(0)
        .setReqFixed32(0)
        .setReqSfixed32(0)
        .setReqInt64(0L)
        .setReqUint64(0L)
        .setReqSint64(0L)
        .setReqFixed64(0L)
        .setReqSfixed64(0L)
        .setReqBool(false)
        .setReqFloat(0F)
        .setReqDouble(0.0)
        .setReqString("")
        .setReqBytes(com.google.protobuf.ByteString.copyFrom(ByteString.EMPTY.toByteArray()))
        .setReqNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN)
        .setReqNestedMessage(AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(0).build())
        .addAllRepInt32(emptyList())
        .addAllRepUint32(emptyList())
        .addAllRepSint32(emptyList())
        .addAllRepFixed32(emptyList())
        .addAllRepSfixed32(emptyList())
        .addAllRepInt64(emptyList())
        .addAllRepUint64(emptyList())
        .addAllRepSint64(emptyList())
        .addAllRepFixed64(emptyList())
        .addAllRepSfixed64(emptyList())
        .addAllRepBool(emptyList())
        .addAllRepFloat(emptyList())
        .addAllRepDouble(emptyList())
        .addAllRepString(emptyList())
        .addAllRepBytes(emptyList())
        .addAllRepNestedEnum(emptyList())
        .addAllRepNestedMessage(emptyList())
        .addAllPackInt32(list(0))
        .addAllPackUint32(list(0))
        .addAllPackSint32(list(0))
        .addAllPackFixed32(list(0))
        .addAllPackSfixed32(list(0))
        .addAllPackInt64(list(0L))
        .addAllPackUint64(list(0L))
        .addAllPackSint64(list(0L))
        .addAllPackFixed64(list(0L))
        .addAllPackSfixed64(list(0L))
        .addAllPackBool(list(false))
        .addAllPackFloat(list(0F))
        .addAllPackDouble(list(0.0))
        .addAllPackNestedEnum(list(AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN))
        .putMapInt32Int32(0, 0)
        .putMapStringMessage("", AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().build())
        .putMapStringEnum("", AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN)
        .build()

    private fun <T : kotlin.Any> list(t: T): List<T> {
      return listOf(t, t)
    }
  }
}
