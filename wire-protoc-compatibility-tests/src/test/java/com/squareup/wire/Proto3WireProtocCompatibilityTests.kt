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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.google.protobuf.Any
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Duration
import com.google.protobuf.FieldOptions
import com.google.protobuf.ListValue
import com.google.protobuf.Struct
import com.google.protobuf.Timestamp
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto3.kotlin.requiredextension.RequiredExtension as RequiredExtensionK
import com.squareup.wire.proto3.kotlin.requiredextension.RequiredExtensionMessage as RequiredExtensionMessageK
import java.io.File
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.source
import org.junit.Assert.fail
import org.junit.Test
import squareup.proto2.java.interop.type.MessageProto2 as MessageProto2J
import squareup.proto2.kotlin.interop.type.InteropTypes.MessageProto2
import squareup.proto2.kotlin.interop.type.MessageProto2 as MessageProto2K
import squareup.proto3.java.alltypes.AllTypes as AllTypesJ
import squareup.proto3.java.alltypes.AllWrappers as AllWrappersJ
import squareup.proto3.java.interop.InteropMessage as InteropMessageJ
import squareup.proto3.java.interop.type.EnumProto3 as EnumProto3J
import squareup.proto3.java.interop.type.MessageProto3 as MessageProto3J
import squareup.proto3.kotlin.MapTypes as MapTypesK
import squareup.proto3.kotlin.MapTypesOuterClass
import squareup.proto3.kotlin.alltypes.All32OuterClass
import squareup.proto3.kotlin.alltypes.All64OuterClass
import squareup.proto3.kotlin.alltypes.AllTypes as AllTypesK
import squareup.proto3.kotlin.alltypes.AllTypesOuterClass
import squareup.proto3.kotlin.alltypes.AllWrappers as AllWrappersK
import squareup.proto3.kotlin.alltypes.AllWrappersOuterClass
import squareup.proto3.kotlin.alltypes.CamelCaseOuterClass
import squareup.proto3.kotlin.extensions.WireMessageOuterClass
import squareup.proto3.kotlin.interop.InteropMessage as InteropMessageK
import squareup.proto3.kotlin.interop.InteropMessageOuterClass
import squareup.proto3.kotlin.interop.type.EnumProto3 as EnumProto3K
import squareup.proto3.kotlin.interop.type.InteropTypes.EnumProto3
import squareup.proto3.kotlin.interop.type.InteropTypes.MessageProto3
import squareup.proto3.kotlin.interop.type.MessageProto3 as MessageProto3K
import squareup.proto3.kotlin.pizza.PizzaDelivery as PizzaDeliveryK
import squareup.proto3.kotlin.pizza.PizzaOuterClass
import squareup.proto3.kotlin.unrecognized_constant.Easter as EasterK3
import squareup.proto3.kotlin.unrecognized_constant.EasterAnimal as EasterAnimalK3
import squareup.proto3.kotlin.unrecognized_constant.EasterOuterClass.Easter as EasterP3
import squareup.proto3.kotlin.unrecognized_constant.EasterOuterClass.EasterAnimal as EasterAnimalP3
import squareup.proto3.wire.extensions.WireMessage

class Proto3WireProtocCompatibilityTests {

  // Note: this test mostly make sure we compile required extension without failing.
  @Test fun protocAndRequiredExtensions() {
    val wireMessage = RequiredExtensionMessageK("Yo")

    val googleMessage = RequiredExtensionK.RequiredExtensionMessage.newBuilder()
      .setStringField("Yo")
      .build()

    assertThat(wireMessage.encode()).isEqualTo(googleMessage.toByteArray())

    // Although the custom options has no label, it shouldn't be "required" to instantiate
    // `FieldOptions`. We should not fail.
    assertThat(DescriptorProtos.FieldOptions.newBuilder().build()).isNotNull()
    assertThat(FieldOptions()).isNotNull()
  }

  @Test fun protocJson() {
    val pizzaDelivery = PizzaOuterClass.PizzaDelivery.newBuilder()
      .setAddress("507 Cross Street")
      .setDeliveredWithinOrFree(
        Duration.newBuilder()
          .setSeconds(1_799)
          .setNanos(500_000_000)
          .build(),
      )
      .addPizzas(
        PizzaOuterClass.Pizza.newBuilder()
          .addToppings("pineapple")
          .addToppings("onion")
          .build(),
      )
      .setPromotion(
        Any.pack(
          PizzaOuterClass.BuyOneGetOnePromotion.newBuilder()
            .setCoupon("MAUI")
            .build(),
        ),
      )
      .setOrderedAt(
        Timestamp.newBuilder()
          .setSeconds(-631152000L) // 1950-01-01T00:00:00.250Z.
          .setNanos(250_000_000)
          .build(),
      )
      .setLoyalty(emptyMap<String, Any>().toStruct())
      .build()

    val typeRegistry = JsonFormat.TypeRegistry.newBuilder()
      .add(PizzaOuterClass.BuyOneGetOnePromotion.getDescriptor())
      .add(PizzaOuterClass.FreeGarlicBreadPromotion.getDescriptor())
      .build()

    // The shared proto schema don't have the same package.
    val json = PIZZA_DELIVERY_JSON.replace(
      "type.googleapis.com/squareup.proto3.BuyOneGetOnePromotion",
      "type.googleapis.com/squareup.proto3.kotlin.pizza.BuyOneGetOnePromotion",
    )

    val jsonPrinter = JsonFormat.printer()
      .usingTypeRegistry(typeRegistry)
    assertJsonEquals(jsonPrinter.print(pizzaDelivery), json)

    val jsonParser = JsonFormat.parser().usingTypeRegistry(typeRegistry)
    val parsed = PizzaOuterClass.PizzaDelivery.newBuilder()
      .apply { jsonParser.merge(json, this) }
      .build()
    assertThat(parsed).isEqualTo(pizzaDelivery)
  }

  @Test fun serializeDefaultAllTypesProtoc() {
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(DEFAULT_ALL_TYPES_JSON, jsonPrinter.print(defaultAllTypesProtoc))
  }

  @Test fun defaultAllTypes() {
    val protocBytes = defaultAllTypesProtoc.toByteArray()
    assertThat(AllTypesJ.ADAPTER.encode(defaultAllTypesWireJava)).isEqualTo(protocBytes)
    assertThat(AllTypesJ.ADAPTER.decode(protocBytes)).isEqualTo(defaultAllTypesWireJava)
    assertThat(AllTypesK.ADAPTER.encode(defaultAllTypesWireKotlin)).isEqualTo(protocBytes)
    assertThat(AllTypesK.ADAPTER.decode(protocBytes)).isEqualTo(defaultAllTypesWireKotlin)
  }

  @Test fun explicitIdentityAllTypes() {
    val protocBytes = explicitIdentityAllTypesProtoc.toByteArray()
    assertThat(AllTypesJ.ADAPTER.encode(explicitIdentityAllTypesWireJava)).isEqualTo(protocBytes)
    assertThat(AllTypesJ.ADAPTER.decode(protocBytes)).isEqualTo(explicitIdentityAllTypesWireJava)
    assertThat(AllTypesK.ADAPTER.encode(explicitIdentityAllTypesWireKotlin)).isEqualTo(protocBytes)
    assertThat(AllTypesK.ADAPTER.decode(protocBytes)).isEqualTo(explicitIdentityAllTypesWireKotlin)
  }

  @Test fun implicitIdentityAllTypes() {
    val protocMessage = AllTypesOuterClass.AllTypes.newBuilder().build()
    val wireMessageJava = AllTypesJ.Builder().build()
    val wireMessageKotlin = AllTypesK()

    val protocBytes = protocMessage.toByteArray()
    assertThat(AllTypesJ.ADAPTER.encode(wireMessageJava)).isEqualTo(protocBytes)
    assertThat(AllTypesJ.ADAPTER.decode(protocBytes)).isEqualTo(wireMessageJava)
    assertThat(AllTypesK.ADAPTER.encode(wireMessageKotlin)).isEqualTo(protocBytes)
    assertThat(AllTypesK.ADAPTER.decode(protocBytes)).isEqualTo(wireMessageKotlin)
  }

  @Test fun deserializeDefaultAllTypesProtoc() {
    val allTypes = defaultAllTypesProtoc
    val jsonParser = JsonFormat.parser()
    val parsed = AllTypesOuterClass.AllTypes.newBuilder()
      .apply { jsonParser.merge(DEFAULT_ALL_TYPES_JSON, this) }
      .build()

    assertThat(parsed).isEqualTo(allTypes)
    assertThat(parsed.toString()).isEqualTo(allTypes.toString())
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(parsed), jsonPrinter.print(allTypes))
  }

  @Test fun serializeIdentityAllTypesProtoc() {
    val identityAllTypes = AllTypesOuterClass.AllTypes.newBuilder().build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(IDENTITY_ALL_TYPES_JSON, jsonPrinter.print(identityAllTypes))
  }

  @Test fun serializeExplicitIdentityAllTypesProtoc() {
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(
      EXPLICIT_IDENTITY_ALL_TYPES_JSON,
      jsonPrinter.print(explicitIdentityAllTypesProtoc),
    )
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_negativeValue_proto3Message() {
    // ┌─ 2: -1
    // ├─ 3: -1
    // ├─ 4: -1
    // ├─ 4: -1
    // ├─ 5: -1
    // ╰- 5: -1
    val bytes = "10ffffffffffffffffff0118ffffffffffffffffff0120ffffffffffffffffff0120ffffffffffffffffff012a14ffffffffffffffffff01ffffffffffffffffff01"
    val wireMessage: EasterK3 = EasterK3.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP3 = EasterP3.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(wireMessage.identity_easter_animal.value).isEqualTo(protocMessage.identityEasterAnimalValue)
    assertThat(wireMessage.optional_easter_animal!!.value).isEqualTo(protocMessage.optionalEasterAnimalValue)

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.UNRECOGNIZED)
    assertThat(protocMessage.optionalEasterAnimalValue).isEqualTo(-1)
    assertThat(protocMessage.identityEasterAnimal).isEqualTo(EasterAnimalP3.UNRECOGNIZED)
    assertThat(protocMessage.identityEasterAnimalValue).isEqualTo(-1)
    assertThat(protocMessage.easterAnimalsRepeatedList).isEqualTo(listOf(EasterAnimalP3.UNRECOGNIZED, EasterAnimalP3.UNRECOGNIZED))
    assertThat(protocMessage.easterAnimalsRepeatedValueList).isEqualTo(listOf(-1, -1))
    assertThat(protocMessage.easterAnimalsPackedList).isEqualTo(listOf(EasterAnimalP3.UNRECOGNIZED, EasterAnimalP3.UNRECOGNIZED))
    assertThat(protocMessage.easterAnimalsPackedValueList).isEqualTo(listOf(-1, -1))

    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(-1))
    assertThat(wireMessage.identity_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(-1))
    assertThat(wireMessage.easter_animals_repeated).isEqualTo(listOf(EasterAnimalK3.Unrecognized(-1), EasterAnimalK3.Unrecognized(-1)))
    assertThat(wireMessage.easter_animals_packed).isEqualTo(listOf(EasterAnimalK3.Unrecognized(-1), EasterAnimalK3.Unrecognized(-1)))
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_knownValue_proto3Message() {
    // ┌─ 2: 1
    // ├─ 3: 1
    // ├─ 4: 1
    // ├─ 4: 1
    // ├─ 4: 1
    // ├─ 5: 1
    // ├─ 5: 1
    // ├─ 5: 1
    // ╰- 5: 1
    val bytes = "100118012001200120012a0401010101"
    val wireMessage: EasterK3 = EasterK3.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP3 = EasterP3.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(wireMessage.identity_easter_animal.value).isEqualTo(protocMessage.identityEasterAnimalValue)
    assertThat(wireMessage.optional_easter_animal!!.value).isEqualTo(protocMessage.optionalEasterAnimalValue)

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.BUNNY)
    assertThat(protocMessage.optionalEasterAnimalValue).isEqualTo(EasterAnimalP3.BUNNY_VALUE)
    assertThat(protocMessage.identityEasterAnimal).isEqualTo(EasterAnimalP3.BUNNY)
    assertThat(protocMessage.identityEasterAnimalValue).isEqualTo(EasterAnimalP3.BUNNY_VALUE)
    assertThat(protocMessage.easterAnimalsRepeatedList).isEqualTo(listOf(EasterAnimalP3.BUNNY, EasterAnimalP3.BUNNY, EasterAnimalP3.BUNNY))
    assertThat(protocMessage.easterAnimalsRepeatedValueList).isEqualTo(listOf(EasterAnimalP3.BUNNY_VALUE, EasterAnimalP3.BUNNY_VALUE, EasterAnimalP3.BUNNY_VALUE))
    assertThat(protocMessage.easterAnimalsPackedList).isEqualTo(listOf(EasterAnimalP3.BUNNY, EasterAnimalP3.BUNNY, EasterAnimalP3.BUNNY, EasterAnimalP3.BUNNY))
    assertThat(protocMessage.easterAnimalsPackedValueList).isEqualTo(listOf(EasterAnimalP3.BUNNY_VALUE, EasterAnimalP3.BUNNY_VALUE, EasterAnimalP3.BUNNY_VALUE, EasterAnimalP3.BUNNY_VALUE))

    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.BUNNY)
    assertThat(wireMessage.identity_easter_animal).isEqualTo(EasterAnimalK3.BUNNY)
    assertThat(wireMessage.easter_animals_repeated).isEqualTo(listOf(EasterAnimalK3.BUNNY, EasterAnimalK3.BUNNY, EasterAnimalK3.BUNNY))
    assertThat(wireMessage.easter_animals_packed).isEqualTo(listOf(EasterAnimalK3.BUNNY, EasterAnimalK3.BUNNY, EasterAnimalK3.BUNNY, EasterAnimalK3.BUNNY))
  }

  @Test fun encodingAndDecodingOfUnrecognizedEnumConstants_unknownValue_proto3Message() {
    // ┌─ 2: 5
    // ├─ 3: 6
    // ├─ 4: 7
    // ├─ 4: 2
    // ├─ 4: 6
    // ├─ 5: 8
    // ├─ 5: 2
    // ├─ 5: 9
    // ╰- 5: 1
    val bytes = "100518062007200220062a0408020901"
    val wireMessage: EasterK3 = EasterK3.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP3 = EasterP3.parseFrom(bytes.decodeHex().toByteArray())

    assertThat(protocMessage.optionalEasterAnimal).isEqualTo(EasterAnimalP3.UNRECOGNIZED)
    assertThat(protocMessage.optionalEasterAnimalValue).isEqualTo(5)
    assertThat(protocMessage.identityEasterAnimal).isEqualTo(EasterAnimalP3.UNRECOGNIZED)
    assertThat(protocMessage.identityEasterAnimalValue).isEqualTo(6)
    assertThat(protocMessage.easterAnimalsRepeatedList).isEqualTo(listOf(EasterAnimalP3.UNRECOGNIZED, EasterAnimalP3.HEN, EasterAnimalP3.UNRECOGNIZED))
    assertThat(protocMessage.easterAnimalsRepeatedValueList).isEqualTo(listOf(7, 2, 6))
    assertThat(protocMessage.easterAnimalsPackedList).isEqualTo(listOf(EasterAnimalP3.UNRECOGNIZED, EasterAnimalP3.HEN, EasterAnimalP3.UNRECOGNIZED, EasterAnimalP3.BUNNY))
    assertThat(protocMessage.easterAnimalsPackedValueList).isEqualTo(listOf(8, 2, 9, 1))

    assertThat(wireMessage.optional_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(5))
    assertThat(wireMessage.identity_easter_animal).isEqualTo(EasterAnimalK3.Unrecognized(6))
    assertThat(wireMessage.easter_animals_repeated).isEqualTo(listOf(EasterAnimalK3.Unrecognized(7), EasterAnimalK3.HEN, EasterAnimalK3.Unrecognized(6)))
    assertThat(wireMessage.easter_animals_packed).isEqualTo(listOf(EasterAnimalK3.Unrecognized(8), EasterAnimalK3.HEN, EasterAnimalK3.Unrecognized(9), EasterAnimalK3.BUNNY))
  }

  @Test fun deserializeIdentityAllTypesProtoc() {
    val identityAllTypes = AllTypesOuterClass.AllTypes.newBuilder().build()
    val jsonParser = JsonFormat.parser()
    val parsed = AllTypesOuterClass.AllTypes.newBuilder()
      .apply { jsonParser.merge(IDENTITY_ALL_TYPES_JSON, this) }
      .build()

    assertThat(parsed).isEqualTo(identityAllTypes)
    assertThat(parsed.toString()).isEqualTo(identityAllTypes.toString())
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(parsed), jsonPrinter.print(identityAllTypes))
  }

  @Test fun deserializeExplicitIdentityAllTypesProtoc() {
    val jsonParser = JsonFormat.parser()
    val parsed = AllTypesOuterClass.AllTypes.newBuilder()
      .apply { jsonParser.merge(EXPLICIT_IDENTITY_ALL_TYPES_JSON, this) }
      .build()

    assertThat(parsed).isEqualTo(explicitIdentityAllTypesProtoc)
    assertThat(parsed.toString()).isEqualTo(explicitIdentityAllTypesProtoc.toString())
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(parsed), jsonPrinter.print(explicitIdentityAllTypesProtoc))
  }

  @Test fun `protoc validation camel case json`() {
    val protocCamel = CamelCaseOuterClass.CamelCase.newBuilder()
      .setNestedMessage(CamelCaseOuterClass.CamelCase.NestedCamelCase.newBuilder().setOneInt32(1))
      .addAllRepInt32(listOf(1, 2))
      .setIDitItMyWAy("frank")
      .putMapInt32Int32(1, 2)
    assertJsonEquals(CAMEL_CASE_JSON, JsonFormat.printer().print(protocCamel))
  }

  @Test fun all64JsonProtocMaxValue() {
    val all64 = All64OuterClass.All64.newBuilder()
      .setMyInt64(Long.MAX_VALUE)
      .setMyUint64(Long.MAX_VALUE)
      .setMySint64(Long.MAX_VALUE)
      .setMyFixed64(Long.MAX_VALUE)
      .setMySfixed64(Long.MAX_VALUE)
      .addAllRepInt64(list(-1L))
      .addAllRepUint64(list(-1L))
      .addAllRepSint64(list(-1L))
      .addAllRepFixed64(list(-1L))
      .addAllRepSfixed64(list(-1L))
      .addAllPackInt64(list(Long.MAX_VALUE))
      .addAllPackUint64(list(Long.MAX_VALUE))
      .addAllPackSint64(list(Long.MAX_VALUE))
      .addAllPackFixed64(list(Long.MAX_VALUE))
      .addAllPackSfixed64(list(Long.MAX_VALUE))
      .setOneofInt64(Long.MAX_VALUE)
      .putMapInt64Int64(Long.MAX_VALUE, Long.MAX_VALUE)
      .putMapInt64Uint64(Long.MAX_VALUE, -1L)
      .putMapInt64Sint64(Long.MAX_VALUE, Long.MAX_VALUE)
      .putMapInt64Fixed64(Long.MAX_VALUE, -1L)
      .putMapInt64Sfixed64(Long.MAX_VALUE, Long.MAX_VALUE)
      .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(all64), ALL_64_JSON_MAX_VALUE)

    val jsonParser = JsonFormat.parser()
    val parsed = All64OuterClass.All64.newBuilder()
      .apply { jsonParser.merge(ALL_64_JSON_MAX_VALUE, this) }
      .build()
    assertThat(parsed).isEqualTo(all64)
  }

  @Test fun all64JsonProtocMinValue() {
    val all64 = All64OuterClass.All64.newBuilder()
      .setMyInt64(Long.MIN_VALUE)
      .setMyUint64(Long.MIN_VALUE)
      .setMySint64(Long.MIN_VALUE)
      .setMyFixed64(Long.MIN_VALUE)
      .setMySfixed64(Long.MIN_VALUE)
      .addAllRepInt64(list(0L))
      .addAllRepUint64(list(0L))
      .addAllRepSint64(list(0L))
      .addAllRepFixed64(list(0L))
      .addAllRepSfixed64(list(0L))
      .addAllPackInt64(list(Long.MIN_VALUE))
      .addAllPackUint64(list(Long.MIN_VALUE))
      .addAllPackSint64(list(Long.MIN_VALUE))
      .addAllPackFixed64(list(Long.MIN_VALUE))
      .addAllPackSfixed64(list(Long.MIN_VALUE))
      .setOneofInt64(Long.MIN_VALUE)
      .putMapInt64Int64(Long.MIN_VALUE, Long.MIN_VALUE)
      .putMapInt64Uint64(Long.MIN_VALUE, 0L)
      .putMapInt64Sint64(Long.MIN_VALUE, Long.MIN_VALUE)
      .putMapInt64Fixed64(Long.MIN_VALUE, 0L)
      .putMapInt64Sfixed64(Long.MIN_VALUE, Long.MIN_VALUE)
      .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(all64), ALL_64_JSON_MIN_VALUE)

    val jsonParser = JsonFormat.parser()
    val parsed = All64OuterClass.All64.newBuilder()
      .apply { jsonParser.merge(ALL_64_JSON_MIN_VALUE, this) }
      .build()
    assertThat(parsed).isEqualTo(all64)
  }

  @Test fun durationProto() {
    val googleMessage = PizzaOuterClass.PizzaDelivery.newBuilder()
      .setDeliveredWithinOrFree(
        Duration.newBuilder()
          .setSeconds(1_799)
          .setNanos(500_000_000)
          .build(),
      )
      .build()

    val wireMessage = PizzaDeliveryK(
      delivered_within_or_free = durationOfSeconds(1_799L, 500_000_000L),
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(wireMessage.encode()).isEqualTo(googleMessageBytes)
    assertThat(PizzaDeliveryK.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun instantProto() {
    val googleMessage = PizzaOuterClass.PizzaDelivery.newBuilder()
      .setOrderedAt(
        Timestamp.newBuilder()
          .setSeconds(-631152000000L) // 1950-01-01T00:00:00.250Z.
          .setNanos(250_000_000)
          .build(),
      )
      .build()

    val wireMessage = PizzaDeliveryK(
      ordered_at = ofEpochSecond(-631152000000L, 250_000_000L),
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(wireMessage.encode()).isEqualTo(googleMessageBytes)
    assertThat(PizzaDeliveryK.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun structProto() {
    val googleMessage = PizzaOuterClass.PizzaDelivery.newBuilder()
      .setLoyalty(
        Struct.newBuilder()
          .putFields("stamps", Value.newBuilder().setNumberValue(5.0).build())
          .putFields(
            "members",
            Value.newBuilder().setListValue(
              ListValue.newBuilder()
                .addValues(Value.newBuilder().setStringValue("Benoît").build())
                .addValues(Value.newBuilder().setStringValue("Jesse").build())
                .build(),
            )
              .build(),
          )
          .build(),
      )
      .build()

    val wireMessage = PizzaDeliveryK(
      loyalty = mapOf("stamps" to 5.0, "members" to listOf("Benoît", "Jesse")),
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(wireMessage.encode()).isEqualTo(googleMessageBytes)
    assertThat(PizzaDeliveryK.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  @Test fun wrappersProtoc() {
    val protocBytes = defaultAllWrappersProtoc.toByteArray()
    assertThat(AllWrappersJ.ADAPTER.encode(defaultAllWrappersWireJava)).isEqualTo(protocBytes)
    assertThat(AllWrappersJ.ADAPTER.decode(protocBytes)).isEqualTo(defaultAllWrappersWireJava)
    assertThat(AllWrappersK.ADAPTER.encode(defaultAllWrappersWireKotlin)).isEqualTo(protocBytes)
    assertThat(AllWrappersK.ADAPTER.decode(protocBytes)).isEqualTo(defaultAllWrappersWireKotlin)
  }

  @Test fun interopTests() {
    val byteArrayWireJ = InteropMessageJ.ADAPTER.encode(interopWireJ)
    val byteArrayWireK = InteropMessageK.ADAPTER.encode(interopWireK)
    val byteArrayProtoc = interopProtoc.toByteArray()

    assertThat(byteArrayWireK).isEqualTo(byteArrayWireJ)
    assertThat(InteropMessageJ.ADAPTER.encode(interopWireJ)).isEqualTo(byteArrayProtoc)
    assertThat(InteropMessageJ.ADAPTER.decode(byteArrayProtoc)).isEqualTo(interopWireJ)
    assertThat(InteropMessageK.ADAPTER.encode(interopWireK)).isEqualTo(byteArrayProtoc)
    assertThat(InteropMessageK.ADAPTER.decode(byteArrayProtoc)).isEqualTo(interopWireK)
    assertThat(InteropMessageOuterClass.InteropMessage.parseFrom(byteArrayWireK))
      .isEqualTo(interopProtoc)
  }

  @Test fun wrappersProtocJson() {
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(defaultAllWrappersProtoc), ALL_WRAPPERS_JSON)

    val jsonParser = JsonFormat.parser()
    val parsed = AllWrappersOuterClass.AllWrappers.newBuilder()
      .apply { jsonParser.merge(ALL_WRAPPERS_JSON, this) }
      .build()
    assertThat(parsed).isEqualTo(defaultAllWrappersProtoc)
  }

  @Test fun minusDoubleZero() {
    val protoc = AllTypesOuterClass.AllTypes.newBuilder()
      .setMyDouble(-0.0)
      .build()
    val wireKotlin = AllTypesK(my_double = -0.0)
    val wireJava = AllTypesJ.Builder().double_(-0.0).build()

    val protocByteArray = protoc.toByteArray()
    assertThat(AllTypesK.ADAPTER.encode(wireKotlin)).isEqualTo(protocByteArray)
    assertThat(AllTypesK.ADAPTER.decode(protocByteArray)).isEqualTo(wireKotlin)
    assertThat(AllTypesJ.ADAPTER.encode(wireJava)).isEqualTo(protocByteArray)
    assertThat(AllTypesJ.ADAPTER.decode(protocByteArray)).isEqualTo(wireJava)

    val protocJson = JsonFormat.printer().print(protoc)
    assertJsonEquals("{\"myDouble\": -0.0}", protocJson)
  }

  @Test fun minusFloatZero() {
    val protoc = AllTypesOuterClass.AllTypes.newBuilder()
      .setMyFloat(-0f)
      .build()
    val wireKotlin = AllTypesK(my_float = -0f)
    val wireJava = AllTypesJ.Builder().float_(-0f).build()

    val protocByteArray = protoc.toByteArray()
    assertThat(AllTypesK.ADAPTER.encode(wireKotlin)).isEqualTo(protocByteArray)
    assertThat(AllTypesK.ADAPTER.decode(protocByteArray)).isEqualTo(wireKotlin)
    assertThat(AllTypesJ.ADAPTER.encode(wireJava)).isEqualTo(protocByteArray)
    assertThat(AllTypesJ.ADAPTER.decode(protocByteArray)).isEqualTo(wireJava)

    val protocJson = JsonFormat.printer().print(protoc)
    assertJsonEquals("{\"myFloat\": -0.0}", protocJson)
  }

  @Test fun cannotPassNullToIdentityString() {
    try {
      AllTypesJ.Builder().string(null).build()
      fail()
    } catch (exception: IllegalArgumentException) {
      assertThat(exception).hasMessage("builder.string == null")
    }
  }

  @Test fun cannotPassNullToIdentityBytes() {
    try {
      AllTypesJ.Builder().bytes(null).build()
      fail()
    } catch (exception: IllegalArgumentException) {
      assertThat(exception).hasMessage("builder.bytes == null")
    }
  }

  @Test fun cannotPassNullToIdentityEnum() {
    try {
      AllTypesJ.Builder().nested_enum(null).build()
      fail()
    } catch (exception: IllegalArgumentException) {
      assertThat(exception).hasMessage("builder.nested_enum == null")
    }
  }

  @Test fun protocDontThrowUpOnWireExtensions() {
    assertThat(WireMessageOuterClass.WireMessage.newBuilder().build()).isNotNull()
    assertThat(WireMessage()).isNotNull()
  }

  @Test fun validateMapTypesJson() {
    val value = MapTypesOuterClass.MapTypes.newBuilder()
      .putAllMapStringString(mapOf("a" to "A", "b" to "B"))
      .putAllMapInt32Int32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .putAllMapSint32Sint32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .putAllMapSfixed32Sfixed32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .putAllMapFixed32Fixed32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .putAllMapUint32Uint32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .putAllMapInt64Int64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .putAllMapSfixed64Sfixed64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .putAllMapSint64Sint64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .putAllMapFixed64Fixed64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .putAllMapUint64Uint64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(value), MAP_TYPES_JSON)

    val jsonParser = JsonFormat.parser()
    val parsed = MapTypesOuterClass.MapTypes.newBuilder()
      .apply { jsonParser.merge(MAP_TYPES_JSON, this) }
      .build()
    assertThat(parsed).isEqualTo(value)
  }

  @Test fun validateAll32MinJson() {
    val value = All32OuterClass.All32.newBuilder()
      .setMyInt32(Int.MIN_VALUE)
      .setMyUint32(Int.MIN_VALUE)
      .setMySint32(Int.MIN_VALUE)
      .setMyFixed32(Int.MIN_VALUE)
      .setMySfixed32(Int.MIN_VALUE)
      .addAllRepInt32(list(0))
      .addAllRepUint32(list(0))
      .addAllRepSint32(list(0))
      .addAllRepFixed32(list(0))
      .addAllRepSfixed32(list(0))
      .addAllPackInt32(list(Int.MIN_VALUE))
      .addAllPackUint32(list(Int.MIN_VALUE))
      .addAllPackSint32(list(Int.MIN_VALUE))
      .addAllPackFixed32(list(Int.MIN_VALUE))
      .addAllPackSfixed32(list(Int.MIN_VALUE))
      .setOneofInt32(Int.MIN_VALUE)
      .putMapInt32Int32(Int.MIN_VALUE, Int.MIN_VALUE + 1)
      .putMapInt32Uint32(Int.MIN_VALUE, 0)
      .putMapInt32Sint32(Int.MIN_VALUE, Int.MIN_VALUE + 1)
      .putMapInt32Fixed32(Int.MIN_VALUE, 0)
      .putMapInt32Sfixed32(Int.MIN_VALUE, Int.MIN_VALUE + 1)
      .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(value), ALL_32_JSON_MIN_VALUE)

    val jsonParser = JsonFormat.parser()
    val parsed = All32OuterClass.All32.newBuilder()
      .apply { jsonParser.merge(ALL_32_JSON_MIN_VALUE, this) }
      .build()
    assertThat(parsed).isEqualTo(value)
  }

  @Test fun mapKeysAndValuesDefaultsToTheirRespectiveIdentityValue() {
    // Bytes for the message `MapType` message with 2 entries on the field `map_string_string`. The
    // first one has a key but not value, the second one has a value without key. Those are manually
    // generated because Protoc and Wire don't write maps this way but can decode them though.
    val bytes = listOf(
      0x0a, // MapType.map_string_string tag -> 1|010 -> 10 -> 0x0a
      0x04, // length
      0x0a, // map key tag 1 -> 1|010 -> 10 -> 0x0a
      0x02, // length
      0x64, 0x65, // de
      0x0a, // MapType.map_string_string tag -> 1|010 -> 10 -> 0x0a
      0x04, // length
      0x12, // map value tag 2 -> 10|010 -> 18 -> 0x12
      0x02, // length
      0x65, 0x64, // ed
    ).map { it.toByte() }.toByteArray()

    val mapTypeProtoc = MapTypesOuterClass.MapTypes.parseFrom(bytes)

    assertThat(mapTypeProtoc.mapStringStringCount).isEqualTo(2)
    assertThat(mapTypeProtoc.mapStringStringMap["de"]).isEqualTo("")
    assertThat(mapTypeProtoc.mapStringStringMap[""]).isEqualTo("ed")

    val mapTypeWire = MapTypesK.ADAPTER.decode(bytes)

    assertThat(mapTypeWire.map_string_string.size).isEqualTo(2)
    assertThat(mapTypeWire.map_string_string["de"]).isEqualTo("")
    assertThat(mapTypeWire.map_string_string[""]).isEqualTo("ed")
  }

  @Test fun validateAll32MaxJson() {
    val value = All32OuterClass.All32.newBuilder()
      .setMyInt32(Int.MAX_VALUE)
      .setMyUint32(Int.MAX_VALUE)
      .setMySint32(Int.MAX_VALUE)
      .setMyFixed32(Int.MAX_VALUE)
      .setMySfixed32(Int.MAX_VALUE)
      .addAllRepInt32(list(-1))
      .addAllRepUint32(list(-1))
      .addAllRepSint32(list(-1))
      .addAllRepFixed32(list(-1))
      .addAllRepSfixed32(list(-1))
      .addAllPackInt32(list(Int.MAX_VALUE))
      .addAllPackUint32(list(Int.MAX_VALUE))
      .addAllPackSint32(list(Int.MAX_VALUE))
      .addAllPackFixed32(list(Int.MAX_VALUE))
      .addAllPackSfixed32(list(Int.MAX_VALUE))
      .setOneofInt32(Int.MAX_VALUE)
      .putMapInt32Int32(Int.MAX_VALUE, Int.MAX_VALUE - 1)
      .putMapInt32Uint32(Int.MAX_VALUE, -1)
      .putMapInt32Sint32(Int.MAX_VALUE, Int.MAX_VALUE - 1)
      .putMapInt32Fixed32(Int.MAX_VALUE, -1)
      .putMapInt32Sfixed32(Int.MAX_VALUE, Int.MAX_VALUE - 1)
      .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(value), ALL_32_JSON_MAX_VALUE)

    val jsonParser = JsonFormat.parser()
    val parsed = All32OuterClass.All32.newBuilder()
      .apply { jsonParser.merge(ALL_32_JSON_MAX_VALUE, this) }
      .build()
    assertThat(parsed).isEqualTo(value)
  }

  companion object {
    private val defaultAllTypesProtoc = AllTypesOuterClass.AllTypes.newBuilder()
      .setMyInt32(111)
      .setMyUint32(112)
      .setMySint32(113)
      .setMyFixed32(114)
      .setMySfixed32(115)
      .setMyInt64(116L)
      .setMyUint64(117L)
      .setMySint64(118L)
      .setMyFixed64(119L)
      .setMySfixed64(120L)
      .setMyBool(true)
      .setMyFloat(122.0F)
      .setMyDouble(123.0)
      .setMyString("124")
      .setMyBytes(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray()))
      .setNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.A)
      .setNestedMessage(AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build())
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
      .build()

    private val defaultAllTypesWireKotlin = AllTypesK(
      my_int32 = 111,
      my_uint32 = 112,
      my_sint32 = 113,
      my_fixed32 = 114,
      my_sfixed32 = 115,
      my_int64 = 116L,
      my_uint64 = 117L,
      my_sint64 = 118L,
      my_fixed64 = 119L,
      my_sfixed64 = 120L,
      my_bool = true,
      my_float = 122.0F,
      my_double = 123.0,
      my_string = "124",
      my_bytes = ByteString.of(123, 125),
      nested_enum = AllTypesK.NestedEnum.A,
      nested_message = AllTypesK.NestedMessage(a = 999),
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
      map_string_message = mapOf("message" to AllTypesK.NestedMessage(1)),
      map_string_enum = mapOf("enum" to AllTypesK.NestedEnum.A),
      oneof_int32 = 0,
    )

    private val defaultAllTypesWireJava = AllTypesJ.Builder()
      .int32(111)
      .uint32(112)
      .sint32(113)
      .fixed32(114)
      .sfixed32(115)
      .int64(116L)
      .uint64(117L)
      .sint64(118L)
      .fixed64(119L)
      .sfixed64(120L)
      .bool(true)
      .float_(122.0F)
      .double_(123.0)
      .string("124")
      .bytes(ByteString.of(123, 125))
      .nested_enum(AllTypesJ.NestedEnum.A)
      .nested_message(AllTypesJ.NestedMessage(999))
      .opt_int32(111)
      .opt_uint32(112)
      .opt_sint32(113)
      .opt_fixed32(114)
      .opt_sfixed32(115)
      .opt_int64(116L)
      .opt_uint64(117L)
      .opt_sint64(118L)
      .opt_fixed64(119L)
      .opt_sfixed64(120L)
      .opt_bool(true)
      .opt_float(122.0F)
      .opt_double(123.0)
      .opt_string("124")
      .opt_bytes(ByteString.of(123, 125))
      .rep_int32(list(111))
      .rep_uint32(list(112))
      .rep_sint32(list(113))
      .rep_fixed32(list(114))
      .rep_sfixed32(list(115))
      .rep_int64(list(116L))
      .rep_uint64(list(117L))
      .rep_sint64(list(118L))
      .rep_fixed64(list(119L))
      .rep_sfixed64(list(120L))
      .rep_bool(list(true))
      .rep_float(list(122.0F))
      .rep_double(list(123.0))
      .rep_string(list("124"))
      .rep_bytes(list(ByteString.of(123, 125)))
      .rep_nested_enum(list(AllTypesJ.NestedEnum.A))
      .rep_nested_message(list(AllTypesJ.NestedMessage(999)))
      .pack_int32(list(111))
      .pack_uint32(list(112))
      .pack_sint32(list(113))
      .pack_fixed32(list(114))
      .pack_sfixed32(list(115))
      .pack_int64(list(116L))
      .pack_uint64(list(117L))
      .pack_sint64(list(118L))
      .pack_fixed64(list(119L))
      .pack_sfixed64(list(120L))
      .pack_bool(list(true))
      .pack_float(list(122.0F))
      .pack_double(list(123.0))
      .pack_nested_enum(list(AllTypesJ.NestedEnum.A))
      .map_int32_int32(mapOf(1 to 2))
      .map_string_string(mapOf("key" to "value"))
      .map_string_message(mapOf("message" to AllTypesJ.NestedMessage(1)))
      .map_string_enum(mapOf("enum" to AllTypesJ.NestedEnum.A))
      .oneof_int32(0)
      .build()

    private val defaultAllWrappersProtoc = AllWrappersOuterClass.AllWrappers.newBuilder()
      .setDoubleValue(33.0.toDoubleValue())
      .setFloatValue(806f.toFloatValue())
      .setInt64Value(Long.MIN_VALUE.toInt64Value())
      .setUint64Value(Long.MIN_VALUE.toUInt64Value())
      .setInt32Value(Int.MIN_VALUE.toInt32Value())
      .setUint32Value(Int.MIN_VALUE.toUInt32Value())
      .setBoolValue(true.toBoolValue())
      .setStringValue("Bo knows wrappers".toStringValue())
      .setBytesValue(ByteString.of(123, 125).toBytesValue())
      .addAllRepDoubleValue(list((-33.0).toDoubleValue()))
      .addAllRepFloatValue(list((-806f).toFloatValue()))
      .addAllRepInt64Value(list(Long.MAX_VALUE.toInt64Value()))
      .addAllRepUint64Value(list((-1L).toUInt64Value()))
      .addAllRepInt32Value(list(Int.MAX_VALUE.toInt32Value()))
      .addAllRepUint32Value(list((-1).toUInt32Value()))
      .addAllRepBoolValue(list(true.toBoolValue()))
      .addAllRepStringValue(list("Bo knows wrappers".toStringValue()))
      .addAllRepBytesValue(list(ByteString.of(123, 125).toBytesValue()))
      .putAllMapInt32DoubleValue(mapOf(23 to 33.0.toDoubleValue()))
      .putAllMapInt32FloatValue(mapOf(23 to 806f.toFloatValue()))
      .putAllMapInt32Int64Value(mapOf(23 to Long.MIN_VALUE.toInt64Value()))
      .putAllMapInt32Uint64Value(mapOf(23 to (-1L).toUInt64Value()))
      .putAllMapInt32Int32Value(mapOf(23 to Int.MIN_VALUE.toInt32Value()))
      .putAllMapInt32Uint32Value(mapOf(23 to (-1).toUInt32Value()))
      .putAllMapInt32BoolValue(mapOf(23 to true.toBoolValue()))
      .putAllMapInt32StringValue(mapOf(23 to "Bo knows wrappers".toStringValue()))
      .putAllMapInt32BytesValue(mapOf(23 to ByteString.of(123, 125).toBytesValue()))
      .build()

    private val defaultAllWrappersWireKotlin = AllWrappersK(
      double_value = 33.0,
      float_value = 806f,
      int64_value = Long.MIN_VALUE,
      uint64_value = Long.MIN_VALUE,
      int32_value = Int.MIN_VALUE,
      uint32_value = Int.MIN_VALUE,
      bool_value = true,
      string_value = "Bo knows wrappers",
      bytes_value = ByteString.of(123, 125),
      rep_double_value = list((-33.0)),
      rep_float_value = list((-806f)),
      rep_int64_value = list(Long.MAX_VALUE),
      rep_uint64_value = list(-1L),
      rep_int32_value = list(Int.MAX_VALUE),
      rep_uint32_value = list(-1),
      rep_bool_value = list(true),
      rep_string_value = list("Bo knows wrappers"),
      rep_bytes_value = list(ByteString.of(123, 125)),
      map_int32_double_value = mapOf(23 to 33.0),
      map_int32_float_value = mapOf(23 to 806f),
      map_int32_int64_value = mapOf(23 to Long.MIN_VALUE),
      map_int32_uint64_value = mapOf(23 to -1L),
      map_int32_int32_value = mapOf(23 to Int.MIN_VALUE),
      map_int32_uint32_value = mapOf(23 to -1),
      map_int32_bool_value = mapOf(23 to true),
      map_int32_string_value = mapOf(23 to "Bo knows wrappers"),
      map_int32_bytes_value = mapOf(23 to ByteString.of(123, 125)),
    )

    private val defaultAllWrappersWireJava = AllWrappersJ.Builder()
      .double_value(33.0)
      .float_value(806f)
      .int64_value(Long.MIN_VALUE)
      .uint64_value(Long.MIN_VALUE)
      .int32_value(Int.MIN_VALUE)
      .uint32_value(Int.MIN_VALUE)
      .bool_value(true)
      .string_value("Bo knows wrappers")
      .bytes_value(ByteString.of(123, 125))
      .rep_double_value(list((-33.0)))
      .rep_float_value(list((-806f)))
      .rep_int64_value(list(Long.MAX_VALUE))
      .rep_uint64_value(list(-1L))
      .rep_int32_value(list(Int.MAX_VALUE))
      .rep_uint32_value(list(-1))
      .rep_bool_value(list(true))
      .rep_string_value(list("Bo knows wrappers"))
      .rep_bytes_value(list(ByteString.of(123, 125)))
      .map_int32_double_value(mapOf(23 to 33.0))
      .map_int32_float_value(mapOf(23 to 806f))
      .map_int32_int64_value(mapOf(23 to Long.MIN_VALUE))
      .map_int32_uint64_value(mapOf(23 to -1L))
      .map_int32_int32_value(mapOf(23 to Int.MIN_VALUE))
      .map_int32_uint32_value(mapOf(23 to -1))
      .map_int32_bool_value(mapOf(23 to true))
      .map_int32_string_value(mapOf(23 to "Bo knows wrappers"))
      .map_int32_bytes_value(mapOf(23 to ByteString.of(123, 125)))
      .build()

    private val CAMEL_CASE_JSON = loadJson("camel_case_proto3.json")

    private val DEFAULT_ALL_TYPES_JSON = loadJson("all_types_proto3.json")

    private val ALL_64_JSON_MIN_VALUE = loadJson("all_64_min_proto3.json")

    private val ALL_64_JSON_MAX_VALUE = loadJson("all_64_max_proto3.json")

    private val IDENTITY_ALL_TYPES_JSON = loadJson("all_types_identity_proto3.json")

    private val PIZZA_DELIVERY_JSON = loadJson("pizza_delivery_proto3.json")

    /** This is used to confirmed identity values are emitted in lists and maps. */
    private val EXPLICIT_IDENTITY_ALL_TYPES_JSON =
      loadJson("all_types_explicit_identity_proto3.json")

    private val ALL_WRAPPERS_JSON = loadJson("all_wrappers_proto3.json")

    private val MAP_TYPES_JSON = loadJson("map_types_proto3.json")

    private val ALL_32_JSON_MIN_VALUE = loadJson("all_32_min_proto3.json")

    private val ALL_32_JSON_MAX_VALUE = loadJson("all_32_max_proto3.json")

    private val explicitIdentityAllTypesWireKotlin = AllTypesK(
      my_int32 = 0,
      my_uint32 = 0,
      my_sint32 = 0,
      my_fixed32 = 0,
      my_sfixed32 = 0,
      my_int64 = 0L,
      my_uint64 = 0L,
      my_sint64 = 0L,
      my_fixed64 = 0L,
      my_sfixed64 = 0L,
      my_bool = false,
      my_float = 0F,
      my_double = 0.0,
      my_string = "",
      my_bytes = ByteString.EMPTY,
      nested_enum = AllTypesK.NestedEnum.UNKNOWN,
      nested_message = AllTypesK.NestedMessage(a = 0),
      rep_int32 = list(0),
      rep_uint32 = list(0),
      rep_sint32 = list(0),
      rep_fixed32 = list(0),
      rep_sfixed32 = emptyList(),
      rep_int64 = emptyList(),
      rep_uint64 = emptyList(),
      rep_sint64 = emptyList(),
      rep_fixed64 = emptyList(),
      rep_sfixed64 = emptyList(),
      rep_bool = emptyList(),
      rep_float = emptyList(),
      rep_double = emptyList(),
      rep_string = list(""),
      rep_bytes = list(ByteString.EMPTY),
      rep_nested_enum = emptyList(),
      rep_nested_message = emptyList(),
      pack_int32 = emptyList(),
      pack_uint32 = emptyList(),
      pack_sint32 = emptyList(),
      pack_fixed32 = emptyList(),
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
      oneof_int32 = 0,
    )

    private val explicitIdentityAllTypesWireJava = AllTypesJ.Builder()
      .int32(0)
      .uint32(0)
      .sint32(0)
      .fixed32(0)
      .sfixed32(0)
      .int64(0L)
      .uint64(0L)
      .sint64(0L)
      .fixed64(0L)
      .sfixed64(0L)
      .bool(false)
      .float_(0F)
      .double_(0.0)
      .string("")
      .bytes(ByteString.EMPTY)
      .nested_enum(AllTypesJ.NestedEnum.UNKNOWN)
      .nested_message(AllTypesJ.NestedMessage(0))
      .rep_int32(list(0))
      .rep_uint32(list(0))
      .rep_sint32(list(0))
      .rep_fixed32(list(0))
      .rep_sfixed32(emptyList())
      .rep_int64(emptyList())
      .rep_uint64(emptyList())
      .rep_sint64(emptyList())
      .rep_fixed64(emptyList())
      .rep_sfixed64(emptyList())
      .rep_bool(emptyList())
      .rep_float(emptyList())
      .rep_double(emptyList())
      .rep_string(list(""))
      .rep_bytes(list(ByteString.EMPTY))
      .rep_nested_enum(emptyList())
      .rep_nested_message(emptyList())
      .pack_int32(emptyList())
      .pack_uint32(emptyList())
      .pack_sint32(emptyList())
      .pack_fixed32(emptyList())
      .pack_sfixed32(list(0))
      .pack_int64(list(0L))
      .pack_uint64(list(0L))
      .pack_sint64(list(0L))
      .pack_fixed64(list(0L))
      .pack_sfixed64(list(0L))
      .pack_bool(list(false))
      .pack_float(list(0F))
      .pack_double(list(0.0))
      .pack_nested_enum(list(AllTypesJ.NestedEnum.UNKNOWN))
      .map_int32_int32(mapOf(0 to 0))
      .map_string_message(mapOf("" to AllTypesJ.NestedMessage.Builder().build()))
      .map_string_enum(mapOf("" to AllTypesJ.NestedEnum.UNKNOWN))
      .oneof_int32(0)
      .build()

    private val explicitIdentityAllTypesProtoc = AllTypesOuterClass.AllTypes.newBuilder()
      .setMyInt32(0)
      .setMyUint32(0)
      .setMySint32(0)
      .setMyFixed32(0)
      .setMySfixed32(0)
      .setMyInt64(0L)
      .setMyUint64(0L)
      .setMySint64(0L)
      .setMyFixed64(0L)
      .setMySfixed64(0L)
      .setMyBool(false)
      .setMyFloat(0F)
      .setMyDouble(0.0)
      .setMyString("")
      .setMyBytes(com.google.protobuf.ByteString.copyFrom(ByteString.EMPTY.toByteArray()))
      .setNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN)
      .setNestedMessage(AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(0).build())
      .addAllRepInt32(list(0))
      .addAllRepUint32(list(0))
      .addAllRepSint32(list(0))
      .addAllRepFixed32(list(0))
      .addAllRepSfixed32(emptyList())
      .addAllRepInt64(emptyList())
      .addAllRepUint64(emptyList())
      .addAllRepSint64(emptyList())
      .addAllRepFixed64(emptyList())
      .addAllRepSfixed64(emptyList())
      .addAllRepBool(emptyList())
      .addAllRepFloat(emptyList())
      .addAllRepDouble(emptyList())
      .addAllRepString(list(""))
      .addAllRepBytes(
        list(com.google.protobuf.ByteString.copyFrom(ByteString.EMPTY.toByteArray())),
      )
      .addAllRepNestedEnum(emptyList())
      .addAllRepNestedMessage(emptyList())
      .addAllPackInt32(emptyList())
      .addAllPackUint32(emptyList())
      .addAllPackSint32(emptyList())
      .addAllPackFixed32(emptyList())
      .addAllPackSfixed32(list(0))
      .addAllPackInt64(list(0L))
      .addAllPackUint64(list(0L))
      .addAllPackSint64(list(0L))
      .addAllPackFixed64(list(0L))
      .addAllPackSfixed64(list(0L))
      .addAllPackBool(list(false))
      .addAllPackFloat(list(0F))
      .addAllPackDouble(list(0.0))
      .addAllPackNestedEnum(
        list(
          AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN,
        ),
      )
      .putMapInt32Int32(0, 0)
      .putMapStringMessage("", AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().build())
      .putMapStringEnum("", AllTypesOuterClass.AllTypes.NestedEnum.UNKNOWN)
      .setOneofInt32(0)
      .build()

    private val interopProtoc = InteropMessageOuterClass.InteropMessage.newBuilder()
      .setProto2Message(MessageProto2.newBuilder().setA(23).setB("MJ").build())
      .setProto3Enum(EnumProto3.A)
      .setProto3Message(MessageProto3.newBuilder().setA(45).setB("MJ").build())
      .addAllRepProto2Message(list(MessageProto2.newBuilder().setA(1).setB("1").build()))
      .addAllRepProto3Enum(list(EnumProto3.UNKNOWN))
      .addAllRepProto3Message(list(MessageProto3.newBuilder().setA(2).build()))
      .addAllPackProto3Enum(list(EnumProto3.A))
      .putMapStringProto2Message("one", MessageProto2.newBuilder().setA(23).setB("MJ").build())
      .putMapStringProto3Enum("two", EnumProto3.A)
      .putMapStringProto3Message("three", MessageProto3.newBuilder().setB("three").build())
      .setOneofProto3Message(MessageProto3.newBuilder().build())
      .build()

    private val interopWireK = InteropMessageK(
      proto2_message = MessageProto2K(23, "MJ"),
      proto3_enum = EnumProto3K.A,
      proto3_message = MessageProto3K(45, "MJ"),
      rep_proto2_message = list(MessageProto2K(1, "1")),
      rep_proto3_enum = list(EnumProto3K.UNKNOWN),
      rep_proto3_message = list(MessageProto3K(2)),
      pack_proto3_enum = list(EnumProto3K.A),
      map_string_proto2_message = mapOf("one" to MessageProto2K(23, "MJ")),
      map_string_proto3_enum = mapOf("two" to EnumProto3K.A),
      map_string_proto3_message = mapOf("three" to MessageProto3K(b = "three")),
      oneof_proto3_message = MessageProto3K(),
    )

    private val interopWireJ = InteropMessageJ.Builder()
      .proto2_message(MessageProto2J(23, "MJ"))
      .proto3_enum(EnumProto3J.A)
      .proto3_message(MessageProto3J(45, "MJ"))
      .rep_proto2_message(list(MessageProto2J(1, "1")))
      .rep_proto3_enum(list(EnumProto3J.UNKNOWN))
      .rep_proto3_message(list(MessageProto3J.Builder().a(2).build()))
      .pack_proto3_enum(list(EnumProto3J.A))
      .map_string_proto2_message(mapOf("one" to MessageProto2J(23, "MJ")))
      .map_string_proto3_enum(mapOf("two" to EnumProto3J.A))
      .map_string_proto3_message(mapOf("three" to MessageProto3J.Builder().b("three").build()))
      .oneof_proto3_message(MessageProto3J.Builder().build())
      .build()

    private fun <T : kotlin.Any> list(t: T): List<T> {
      return listOf(t, t)
    }

    private fun loadJson(fileName: String): String {
      return File("../wire-tests/src/commonTest/shared/json", fileName)
        .source().use { it.buffer().readUtf8() }
    }
  }
}
