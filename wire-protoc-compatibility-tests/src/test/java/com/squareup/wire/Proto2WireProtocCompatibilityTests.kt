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
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.google.protobuf.ExtensionRegistry
import com.squareup.wire.proto2.kotlin.simple.SimpleMessage as SimpleMessageK
import com.squareup.wire.proto2.kotlin.simple.SimpleMessageOuterClass
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import squareup.proto2.java.interop.InteropMessage as InteropMessageJ
import squareup.proto2.java.interop.type.EnumProto2 as EnumProto2J
import squareup.proto2.java.interop.type.MessageProto2 as MessageProto2J
import squareup.proto2.kotlin.MapTypes
import squareup.proto2.kotlin.MapTypesOuterClass
import squareup.proto2.kotlin.alltypes.AllTypes as AllTypesK
import squareup.proto2.kotlin.alltypes.AllTypesOuterClass
import squareup.proto2.kotlin.alltypes.AllTypesOuterClass.extOptBool
import squareup.proto2.kotlin.alltypes.AllTypesOuterClass.extPackBool
import squareup.proto2.kotlin.alltypes.AllTypesOuterClass.extRepBool
import squareup.proto2.kotlin.extensions.WireMessageOuterClass
import squareup.proto2.kotlin.interop.InteropMessage as InteropMessageK
import squareup.proto2.kotlin.interop.InteropMessageOuterClass
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extOptProto2Enum
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extOptProto2Message
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extOptProto3Enum
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extOptProto3Message
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extRepProto2Enum
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extRepProto2Message
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extRepProto3Enum
import squareup.proto2.kotlin.interop.InteropMessageOuterClass.extRepProto3Message
import squareup.proto2.kotlin.interop.Quilt
import squareup.proto2.kotlin.interop.QuiltColor
import squareup.proto2.kotlin.interop.QuiltContainer
import squareup.proto2.kotlin.interop.RepeatedEnum
import squareup.proto2.kotlin.interop.type.EnumProto2 as EnumProto2K
import squareup.proto2.kotlin.interop.type.InteropTypes.EnumProto2
import squareup.proto2.kotlin.interop.type.InteropTypes.MessageProto2
import squareup.proto2.kotlin.interop.type.MessageProto2 as MessageProto2K
import squareup.proto2.kotlin.unrecognized_constant.Easter as EasterK2
import squareup.proto2.kotlin.unrecognized_constant.EasterOuterClass.Easter as EasterP2
import squareup.proto2.wire.extensions.WireMessage
import squareup.proto3.java.interop.type.EnumProto3 as EnumProto3J
import squareup.proto3.java.interop.type.MessageProto3 as MessageProto3J
import squareup.proto3.kotlin.interop.type.EnumProto3 as EnumProto3K
import squareup.proto3.kotlin.interop.type.InteropTypes.EnumProto3
import squareup.proto3.kotlin.interop.type.InteropTypes.MessageProto3
import squareup.proto3.kotlin.interop.type.MessageProto3 as MessageProto3K
import squareup.proto3.kotlin.unrecognized_constant.EasterAnimal as EasterAnimalK3
import squareup.proto3.kotlin.unrecognized_constant.EasterOuterClass.EasterAnimal as EasterAnimalP3

class Proto2WireProtocCompatibilityTests {
  @Test fun simpleMessage() {
    val wireMessage = SimpleMessageK(
      optional_nested_msg = SimpleMessageK.NestedMessage(806),
      no_default_nested_enum = SimpleMessageK.NestedEnum.BAR,
      repeated_double = listOf(1.0, 33.0),
      required_int32 = 46,
      other = "hello",
    )

    val googleMessage = SimpleMessageOuterClass.SimpleMessage.newBuilder()
      .setOptionalNestedMsg(
        SimpleMessageOuterClass.SimpleMessage.NestedMessage.newBuilder().setBb(806).build(),
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
      SimpleMessageK.ADAPTER.decode(encodedGoogleMessage)
    val googleMessageDecodedFromWireMessage =
      SimpleMessageOuterClass.SimpleMessage.parseFrom(encodedWireMessage)

    assertThat(wireMessageDecodedFromGoogleMessage).isEqualTo(wireMessage)
    assertThat(googleMessageDecodedFromWireMessage).isEqualTo(googleMessage)
  }

  @Test fun allTypesSerialization() {
    val byteArrayWire = AllTypesK.ADAPTER.encode(defaultAllTypesWire)
    val byteArrayProtoc = defaultAllTypesProtoc.toByteArray()

    assertThat(AllTypesK.ADAPTER.decode(byteArrayProtoc)).isEqualTo(defaultAllTypesWire)
    assertThat(AllTypesOuterClass.AllTypes.parseFrom(byteArrayWire, allTypesRegistry))
      .isEqualTo(defaultAllTypesProtoc)
  }

  @Test fun allTypesSerializationWithEmptyOrIdentityValues() {
    val byteArrayWire = AllTypesK.ADAPTER.encode(identityAllTypesWire)
    val byteArrayProtoc = identityAllTypesProtoc.toByteArray()

    assertThat(AllTypesK.ADAPTER.decode(byteArrayProtoc)).isEqualTo(identityAllTypesWire)

    assertThat(AllTypesOuterClass.AllTypes.parseFrom(byteArrayWire, allTypesRegistry))
      .isEqualTo(identityAllTypesProtoc)
  }

  @Test fun protocDontThrowUpOnWireExtensions() {
    assertThat(WireMessageOuterClass.WireMessage.newBuilder().build()).isNotNull()
    assertThat(WireMessage()).isNotNull()
  }

  @Test fun serializeProto2Proto3Interop() {
    val byteArrayWireJ = InteropMessageJ.ADAPTER.encode(interopWireJ)
    val byteArrayWireK = InteropMessageK.ADAPTER.encode(interopWireK)
    val byteArrayProtoc = interopProtoc.toByteArray()

    // Note: we don't test equality between protoc byte arrays and ours because extensions are not
    // at the same location in the array.
    assertThat(byteArrayWireK).isEqualTo(byteArrayWireJ)
    assertThat(InteropMessageJ.ADAPTER.decode(byteArrayProtoc)).isEqualTo(interopWireJ)
    assertThat(InteropMessageK.ADAPTER.decode(byteArrayProtoc)).isEqualTo(interopWireK)
    assertThat(InteropMessageOuterClass.InteropMessage.parseFrom(byteArrayWireK, interopRegistry))
      .isEqualTo(interopProtoc)
  }

  @Test fun mapKeysAndValuesDefaultsToTheirRespectiveIdentityValue() {
    // Bytes for the message `MapType` message with 2 entries on the field `map_string_string`. The
    // first one has a key but not value, the second one has a value without key. Those are manually
    // generated because Protoc and Wire don't write maps this way but can decode them though.
    val bytes = listOf(
      0x0a, // MapType.map_string_string tag -> 1|010 -> 10 -> x0a
      0x04, // length
      0x0a, // map key tag 1 -> 1|010 -> 10 -> x0a
      0x02, // length
      0x64, 0x65, // de
      0x0a, // MapType.map_string_string tag -> 1|010 -> 10 -> x0a
      0x04, // length
      0x12, // map value tag 2 -> 10|010 -> 18 -> x12
      0x02, // length
      0x65, 0x64, // ed
    ).map { it.toByte() }.toByteArray()

    val mapTypeProtoc = MapTypesOuterClass.MapTypes.parseFrom(bytes)

    assertThat(mapTypeProtoc.mapStringStringCount).isEqualTo(2)
    assertThat(mapTypeProtoc.mapStringStringMap["de"]).isEqualTo("")
    assertThat(mapTypeProtoc.mapStringStringMap[""]).isEqualTo("ed")

    val mapTypeWire = MapTypes.ADAPTER.decode(bytes)

    assertThat(mapTypeWire.map_string_string.size).isEqualTo(2)
    assertThat(mapTypeWire.map_string_string["de"]).isEqualTo("")
    assertThat(mapTypeWire.map_string_string[""]).isEqualTo("ed")
  }

  /**
   * The TS-proto library encodes enums differently from Wire or Protoc. Protoc is fine with this,
   * but it causes Wire to crash. The fix is to make Wire accept this format also.
   *
   * https://github.com/stephenh/ts-proto
   */
  @Test fun repeatedEnumIsPackedAndLengthIsZero() {
    val wireMessage = QuiltContainer(
      quilt = Quilt(
        fringe = listOf(QuiltColor.GREEN),
        cozy = true,
      ),
    )

    // A sample value encoded by ts-proto.
    val encodedByTsProto = "12091201041a0022003801".decodeHex()

    val googleMessage = RepeatedEnum.QuiltContainer.newBuilder()
      .setQuilt(
        RepeatedEnum.Quilt.newBuilder()
          .addAllFringe(listOf(RepeatedEnum.QuiltColor.GREEN))
          .setCozy(true)
          .build(),
      ).build()

    val wireMessageDecodedFromGoogleMessage =
      QuiltContainer.ADAPTER.decode(encodedByTsProto)
    val googleMessageDecodedFromWireMessage =
      RepeatedEnum.QuiltContainer.parseFrom(encodedByTsProto.toByteArray())

    assertThat(wireMessageDecodedFromGoogleMessage).isEqualTo(wireMessage)
    assertThat(googleMessageDecodedFromWireMessage).isEqualTo(googleMessage)
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_negativeValue_proto2Message() {
    // ┌─ 2: -1
    // ╰- 3: 2
    val bytes = "10ffffffffffffffffff011802"
    val wireMessage: EasterK2 = EasterK2.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP2 = EasterP2.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.EASTER_ANIMAL_DEFAULT)
    assertThat(protocMessage.optionalEasterAnimal.number).isEqualTo(0)

    // Keeping that around to clearly show that Wire has a different behavior that protoc. Not sure
    // that we want to fix this. Protoc assigns it to 0 even for proto2 messages using the proto3
    // enum.
    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(-1))
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_knownValue_proto2Message() {
    // ┌─ 2: 1
    // ╰- 3: 1
    val bytes = "10011801"
    val wireMessage: EasterK2 = EasterK2.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP2 = EasterP2.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(wireMessage.required_easter_animal.value).isEqualTo(protocMessage.requiredEasterAnimal.number)
    assertThat(wireMessage.optional_easter_animal!!.value).isEqualTo(protocMessage.optionalEasterAnimal.number)

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.BUNNY)
    assertThat(protocMessage.optionalEasterAnimal.number).isEqualTo(EasterAnimalP3.BUNNY_VALUE)
    assertThat(protocMessage.requiredEasterAnimal).isEqualTo(EasterAnimalP3.BUNNY)
    assertThat(protocMessage.requiredEasterAnimal.number).isEqualTo(EasterAnimalP3.BUNNY_VALUE)

    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.BUNNY)
    assertThat(wireMessage.required_easter_animal).isEqualTo(EasterAnimalK3.BUNNY)
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_unknownValue_proto2Message() {
    // Both Wire and Protoc throw if the required field isn't known.
    // ┌─ 2: 5
    // ├─ 3: 2
    // ├─ 4: 5
    // ╰- 4: 5
    val bytes = "1005180220052005"
    val wireMessage: EasterK2 = EasterK2.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP2 = EasterP2.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.EASTER_ANIMAL_DEFAULT)
    assertThat(protocMessage.optionalEasterAnimal.number).isEqualTo(0)
    assertThat(protocMessage.easterAnimalsList).isEmpty()

    // Keeping that around to clearly show that Wire has a different behavior that protoc. Not sure
    // that we want to fix this. Protoc assigns it to 0 even for proto2 messages using the proto3
    // enum.
    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(5))
    assertThat(wireMessage.easter_animals).isEqualTo(listOf(EasterAnimalK3.Unrecognized(5), EasterAnimalK3.Unrecognized(5)))
  }

  companion object {
    private val allTypesRegistry = ExtensionRegistry.newInstance().apply {
      add(extOptBool)
      add(extRepBool)
      add(extPackBool)
    }

    private val interopRegistry = ExtensionRegistry.newInstance().apply {
      add(extOptProto2Enum)
      add(extOptProto2Message)
      add(extOptProto3Enum)
      add(extOptProto3Message)
      add(extRepProto2Enum)
      add(extRepProto2Message)
      add(extRepProto3Enum)
      add(extRepProto3Message)
    }

    private val defaultAllTypesWire = AllTypesK(
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
      opt_nested_enum = AllTypesK.NestedEnum.A,
      opt_nested_message = AllTypesK.NestedMessage(a = 999),
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
      req_nested_enum = AllTypesK.NestedEnum.A,
      req_nested_message = AllTypesK.NestedMessage(a = 999),
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
      rep_nested_enum = list(AllTypesK.NestedEnum.A),
      rep_nested_message = list(AllTypesK.NestedMessage(a = 999)),
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
      pack_nested_enum = list(AllTypesK.NestedEnum.A),
      map_int32_int32 = mapOf(1 to 2),
      map_string_string = mapOf("key" to "value"),
      map_string_message = mapOf("message" to AllTypesK.NestedMessage(a = 1)),
      map_string_enum = mapOf("enum" to AllTypesK.NestedEnum.A),
      oneof_int32 = 0,
      ext_opt_bool = true,
      ext_rep_bool = list(true),
      ext_pack_bool = list(true),
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
        AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build(),
      )
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
        AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build(),
      )
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
        list(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray())),
      )
      .addAllRepNestedEnum(list(AllTypesOuterClass.AllTypes.NestedEnum.A))
      .addAllRepNestedMessage(
        list(
          AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build(),
        ),
      )
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
      .putMapStringMessage(
        "message",
        AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(1).build(),
      )
      .putMapStringEnum("enum", AllTypesOuterClass.AllTypes.NestedEnum.A)
      .setOneofInt32(0)
      .setExtension(extOptBool, true)
      .setExtension(extRepBool, list(true))
      .setExtension(extPackBool, list(true))
      .build()

    private val identityAllTypesWire = AllTypesK(
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
      req_nested_enum = AllTypesK.NestedEnum.UNKNOWN,
      req_nested_message = AllTypesK.NestedMessage(a = 0),
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
      pack_nested_enum = list(AllTypesK.NestedEnum.UNKNOWN),
      map_int32_int32 = mapOf(0 to 0),
      map_string_message = mapOf("" to AllTypesK.NestedMessage()),
      map_string_enum = mapOf("" to AllTypesK.NestedEnum.UNKNOWN),
      ext_opt_bool = false,
      ext_rep_bool = list(false),
      ext_pack_bool = list(false),
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
      .setExtension(extOptBool, false)
      .setExtension(extRepBool, list(false))
      .setExtension(extPackBool, list(false))
      .build()

    private val interopProtoc = InteropMessageOuterClass.InteropMessage.newBuilder()
      .setOptProto2Enum(EnumProto2.UNKNOWN)
      .setOptProto2Message(MessageProto2.newBuilder().setA(33).setB("Grant").build())
      .setOptProto3Enum(EnumProto3.A)
      .setOptProto3Message(MessageProto3.newBuilder().setA(806).build())
      .setReqProto2Enum(EnumProto2.A)
      .setReqProto2Message(MessageProto2.newBuilder().setA(1).setB("Penny").build())
      .setReqProto3Enum(EnumProto3.UNKNOWN)
      .setReqProto3Message(MessageProto3.newBuilder().setA(-1).build())
      .addAllRepProto2Enum(list(EnumProto2.A))
      .addAllRepProto2Message(list(MessageProto2.newBuilder().setA(7).setB("Pete").build()))
      .addAllRepProto3Enum(list(EnumProto3.UNKNOWN))
      .addAllRepProto3Message(list(MessageProto3.newBuilder().setA(0).build()))
      .addAllPackProto2Enum(list(EnumProto2.A))
      .addAllPackProto3Enum(list(EnumProto3.A))
      .putMapStringProto2Enum("a", EnumProto2.UNKNOWN)
      .putMapStringProto2Message("b", MessageProto2.newBuilder().setA(21).setB("Jimmy").build())
      .putMapStringProto3Enum("c", EnumProto3.A)
      .putMapStringProto3Message("d", MessageProto3.newBuilder().setA(33333).build())
      .setOneofProto3Enum(EnumProto3.A)
      .setExtension(extOptProto2Enum, EnumProto2.A)
      .setExtension(extOptProto2Message, MessageProto2.newBuilder().setA(8).setB("Kobe").build())
      .setExtension(extOptProto3Enum, EnumProto3.A)
      .setExtension(extOptProto3Message, MessageProto3.newBuilder().setA(24).build())
      .setExtension(extRepProto2Enum, list(EnumProto2.A))
      .setExtension(
        extRepProto2Message,
        list(MessageProto2.newBuilder().setA(3).setB("Dwyane").build()),
      )
      .setExtension(extRepProto3Enum, list(EnumProto3.A))
      .setExtension(extRepProto3Message, list(MessageProto3.newBuilder().setA(1).build()))
      .build()

    private val interopWireK = InteropMessageK(
      opt_proto2_enum = EnumProto2K.UNKNOWN,
      opt_proto2_message = MessageProto2K(33, "Grant"),
      opt_proto3_enum = EnumProto3K.A,
      opt_proto3_message = MessageProto3K(806),
      req_proto2_enum = EnumProto2K.A,
      req_proto2_message = MessageProto2K(1, "Penny"),
      req_proto3_enum = EnumProto3K.UNKNOWN,
      req_proto3_message = MessageProto3K(-1),
      rep_proto2_enum = list(EnumProto2K.A),
      rep_proto2_message = list(MessageProto2K(7, "Pete")),
      rep_proto3_enum = list(EnumProto3K.UNKNOWN),
      rep_proto3_message = list(MessageProto3K(0)),
      pack_proto2_enum = list(EnumProto2K.A),
      pack_proto3_enum = list(EnumProto3K.A),
      map_string_proto2_enum = mapOf("a" to EnumProto2K.UNKNOWN),
      map_string_proto2_message = mapOf("b" to MessageProto2K(21, "Jimmy")),
      map_string_proto3_enum = mapOf("c" to EnumProto3K.A),
      map_string_proto3_message = mapOf("d" to MessageProto3K(33333)),
      ext_opt_proto2_enum = EnumProto2K.A,
      ext_opt_proto2_message = MessageProto2K(8, "Kobe"),
      ext_opt_proto3_enum = EnumProto3K.A,
      ext_opt_proto3_message = MessageProto3K(24),
      ext_rep_proto2_enum = list(EnumProto2K.A),
      ext_rep_proto2_message = list(MessageProto2K(3, "Dwyane")),
      ext_rep_proto3_enum = list(EnumProto3K.A),
      ext_rep_proto3_message = list(MessageProto3K(1)),
      oneof_proto3_enum = EnumProto3K.A,
    )

    private val interopWireJ = InteropMessageJ.Builder()
      .opt_proto2_enum(EnumProto2J.UNKNOWN)
      .opt_proto2_message(MessageProto2J(33, "Grant"))
      .opt_proto3_enum(EnumProto3J.A)
      .opt_proto3_message(MessageProto3J.Builder().a(806).build())
      .req_proto2_enum(EnumProto2J.A)
      .req_proto2_message(MessageProto2J(1, "Penny"))
      .req_proto3_enum(EnumProto3J.UNKNOWN)
      .req_proto3_message(MessageProto3J.Builder().a(-1).build())
      .rep_proto2_enum(list(EnumProto2J.A))
      .rep_proto2_message(list(MessageProto2J(7, "Pete")))
      .rep_proto3_enum(list(EnumProto3J.UNKNOWN))
      .rep_proto3_message(list(MessageProto3J.Builder().a(0).build()))
      .pack_proto2_enum(list(EnumProto2J.A))
      .pack_proto3_enum(list(EnumProto3J.A))
      .map_string_proto2_enum(mapOf("a" to EnumProto2J.UNKNOWN))
      .map_string_proto2_message(mapOf("b" to MessageProto2J(21, "Jimmy")))
      .map_string_proto3_enum(mapOf("c" to EnumProto3J.A))
      .map_string_proto3_message(mapOf("d" to MessageProto3J.Builder().a(33333).build()))
      .ext_opt_proto2_enum(EnumProto2J.A)
      .ext_opt_proto2_message(MessageProto2J(8, "Kobe"))
      .ext_opt_proto3_enum(EnumProto3J.A)
      .ext_opt_proto3_message(MessageProto3J.Builder().a(24).build())
      .ext_rep_proto2_enum(list(EnumProto2J.A))
      .ext_rep_proto2_message(list(MessageProto2J(3, "Dwyane")))
      .ext_rep_proto3_enum(list(EnumProto3J.A))
      .ext_rep_proto3_message(list(MessageProto3J.Builder().a(1).build()))
      .oneof_proto3_enum(EnumProto3J.A)
      .build()

    private fun <T : Any> list(t: T): List<T> {
      return listOf(t, t)
    }
  }
}
