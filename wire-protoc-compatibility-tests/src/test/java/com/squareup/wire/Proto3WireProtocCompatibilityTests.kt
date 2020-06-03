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
@file:Suppress("UsePropertyAccessSyntax")

package com.squareup.wire

import com.google.protobuf.Any
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Duration
import com.google.protobuf.FieldOptions
import com.google.protobuf.util.JsonFormat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto3.requiredextension.RequiredExtension
import com.squareup.wire.proto3.requiredextension.RequiredExtensionMessage
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import squareup.proto3.alltypes.All64
import squareup.proto3.alltypes.All64OuterClass
import squareup.proto3.alltypes.AllTypes
import squareup.proto3.alltypes.AllTypesOuterClass
import squareup.proto3.alltypes.CamelCase
import squareup.proto3.alltypes.CamelCase.NestedCamelCase
import squareup.proto3.alltypes.CamelCaseOuterClass
import squareup.proto3.pizza.BuyOneGetOnePromotion
import squareup.proto3.pizza.FreeGarlicBreadPromotion
import squareup.proto3.pizza.Pizza
import squareup.proto3.pizza.PizzaDelivery
import squareup.proto3.pizza.PizzaOuterClass

class Proto3WireProtocCompatibilityTests {
  // Note: this test mostly make sure we compile required extension without failing.
  @Test fun protocAndRequiredExtensions() {
    val wireMessage = RequiredExtensionMessage("Yo")

    val googleMessage = RequiredExtension.RequiredExtensionMessage.newBuilder()
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
        .setDeliveredWithinOrFree(Duration.newBuilder()
            .setSeconds(1_799)
            .setNanos(500_000_000)
            .build())
        .addPizzas(PizzaOuterClass.Pizza.newBuilder()
            .addToppings("pineapple")
            .addToppings("onion")
            .build())
        .setPromotion(Any.pack(PizzaOuterClass.BuyOneGetOnePromotion.newBuilder()
            .setCoupon("MAUI")
            .build()))
        .build()

    val json = """
        |{
        |  "address": "507 Cross Street",
        |  "pizzas": [{
        |    "toppings": ["pineapple", "onion"]
        |  }],
        |  "promotion": {
        |    "@type": "type.googleapis.com/squareup.proto3.pizza.BuyOneGetOnePromotion",
        |    "coupon": "MAUI"
        |  },
        |  "deliveredWithinOrFree": "1799.500s"
        |}
        """.trimMargin()

    val typeRegistry = JsonFormat.TypeRegistry.newBuilder()
        .add(PizzaOuterClass.BuyOneGetOnePromotion.getDescriptor())
        .add(PizzaOuterClass.FreeGarlicBreadPromotion.getDescriptor())
        .build()

    val jsonPrinter = JsonFormat.printer()
        .usingTypeRegistry(typeRegistry)
    assertThat(jsonPrinter.print(pizzaDelivery)).isEqualTo(json)

    val jsonParser = JsonFormat.parser().usingTypeRegistry(typeRegistry)
    val parsed = PizzaOuterClass.PizzaDelivery.newBuilder()
        .apply { jsonParser.merge(json, this) }
        .build()
    assertThat(parsed).isEqualTo(pizzaDelivery)
  }

  @Test fun wireJson() {
    val pizzaDelivery = PizzaDelivery(
        address = "507 Cross Street",
        pizzas = listOf(Pizza(toppings = listOf("pineapple", "onion"))),
        promotion = AnyMessage.pack(BuyOneGetOnePromotion(coupon = "MAUI")),
        delivered_within_or_free = java.time.Duration.ofSeconds(1_799L, 500_000_000L)
    )
    val json = """
        |{
        |  "address": "507 Cross Street",
        |  "deliveredWithinOrFree": "1799.500s",
        |  "pizzas": [
        |    {
        |      "toppings": [
        |        "pineapple",
        |        "onion"
        |      ]
        |    }
        |  ],
        |  "promotion": {
        |    "@type": "type.googleapis.com/squareup.proto3.pizza.BuyOneGetOnePromotion",
        |    "coupon": "MAUI"
        |  }
        |}
        """.trimMargin()

    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory()
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER, FreeGarlicBreadPromotion.ADAPTER)))
        .build()

    val jsonAdapter = moshi.adapter(PizzaDelivery::class.java).indent("  ")
    assertJsonEquals(jsonAdapter.toJson(pizzaDelivery), json)
    assertThat(jsonAdapter.fromJson(json)).isEqualTo(pizzaDelivery)
  }

  @Test fun wireProtocJsonRoundTrip() {
    val protocMessage = PizzaOuterClass.PizzaDelivery.newBuilder()
        .setAddress("507 Cross Street")
        .addPizzas(PizzaOuterClass.Pizza.newBuilder()
            .addToppings("pineapple")
            .addToppings("onion")
            .build())
        .setPromotion(Any.pack(PizzaOuterClass.BuyOneGetOnePromotion.newBuilder()
            .setCoupon("MAUI")
            .build()))
        .build()

    val typeRegistry = JsonFormat.TypeRegistry.newBuilder()
        .add(PizzaOuterClass.BuyOneGetOnePromotion.getDescriptor())
        .add(PizzaOuterClass.FreeGarlicBreadPromotion.getDescriptor())
        .build()

    val jsonPrinter = JsonFormat.printer()
        .usingTypeRegistry(typeRegistry)
    val protocMessageJson = jsonPrinter.print(protocMessage)

    val wireMessage = PizzaDelivery(
        address = "507 Cross Street",
        pizzas = listOf(Pizza(toppings = listOf("pineapple", "onion"))),
        promotion = AnyMessage.pack(BuyOneGetOnePromotion(coupon = "MAUI"))
    )

    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory()
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER, FreeGarlicBreadPromotion.ADAPTER)))
        .build()
    val jsonAdapter = moshi.adapter(PizzaDelivery::class.java)
    val moshiMessageJson = jsonAdapter.toJson(wireMessage)

    // Parsing the two json because this should be order insensitive.
    assertJsonEquals(moshiMessageJson, protocMessageJson)

    // Now each parses the other's json.
    val jsonParser = JsonFormat.parser().usingTypeRegistry(typeRegistry)
    val protocMessageDecodedFromWireJson = PizzaOuterClass.PizzaDelivery.newBuilder()
        .apply { jsonParser.merge(moshiMessageJson, this) }
        .build()

    val wireMessageDecodedFromProtocJson = jsonAdapter.fromJson(protocMessageJson)

    assertThat(protocMessageDecodedFromWireJson).isEqualTo(protocMessage)
    assertThat(wireMessageDecodedFromProtocJson).isEqualTo(wireMessage)
  }

  @Test fun unregisteredTypeOnReading() {
    val jsonAdapter = moshi.adapter(PizzaDelivery::class.java)

    val json = """
        |{
        |  "address": "507 Cross Street",
        |  "pizzas": [
        |    {
        |      "toppings": [
        |        "pineapple",
        |        "onion"
        |      ]
        |    }
        |  ],
        |  "promotion": {
        |    "@type": "type.googleapis.com/squareup.proto3.pizza.BuyOneGetOnePromotion",
        |    "coupon": "MAUI"
        |  }
        |}
        """.trimMargin()

    try {
      jsonAdapter.fromJson(json)
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Cannot resolve type: " +
          "type.googleapis.com/squareup.proto3.pizza.BuyOneGetOnePromotion in \$.promotion")
    }
  }

  @Test fun unregisteredTypeOnWriting() {
    val pizzaDelivery = PizzaDelivery(
        address = "507 Cross Street",
        pizzas = listOf(Pizza(toppings = listOf("pineapple", "onion"))),
        promotion = AnyMessage.pack(BuyOneGetOnePromotion(coupon = "MAUI"))
    )

    val jsonAdapter = moshi.adapter(PizzaDelivery::class.java)
    try {
      jsonAdapter.toJson(pizzaDelivery)
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected)
          .hasMessage("Cannot find type for url: " +
              "type.googleapis.com/squareup.proto3.pizza.BuyOneGetOnePromotion " +
              "in \$.promotion.@type")
    }
  }

  @Test fun anyJsonWithoutType() {
    val jsonAdapter = moshi.adapter(PizzaDelivery::class.java)

    val json = """
        |{
        |  "address": "507 Cross Street",
        |  "pizzas": [
        |    {
        |      "toppings": [
        |        "pineapple",
        |        "onion"
        |      ]
        |    }
        |  ],
        |  "promotion": {
        |    "coupon": "MAUI"
        |  }
        |}
        """.trimMargin()

    try {
      jsonAdapter.fromJson(json)
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("expected @type in \$.promotion")
    }
  }

  @Test fun serializeDefaultAllTypesProtoc() {
    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(DEFAULT_ALL_TYPES_JSON, jsonPrinter.print(defaultAllTypesProtoc))
  }

  @Ignore("TODO")
  @Test fun serializeDefaultAllTypesMoshi() {
    assertJsonEquals(DEFAULT_ALL_TYPES_JSON,
        moshi.adapter(AllTypes::class.java).toJson(defaultAllTypesWire))
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

  @Ignore("TODO")
  @Test fun deserializeDefaultAllTypesMoshi() {
    val allTypesAdapter: JsonAdapter<AllTypes> = moshi.adapter(AllTypes::class.java)

    val allTypes = defaultAllTypesWire
    val parsed = allTypesAdapter.fromJson(DEFAULT_ALL_TYPES_JSON)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(parsed.toString()).isEqualTo(allTypes.toString())
    assertJsonEquals(allTypesAdapter.toJson(parsed), allTypesAdapter.toJson(allTypes))
  }

  @Test fun serializeIdentityAllTypesProtoc() {
    val identityAllTypes = AllTypesOuterClass.AllTypes.newBuilder().build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(IDENTITY_ALL_TYPES_JSON, jsonPrinter.print(identityAllTypes))
  }

  @Ignore("TODO")
  @Test fun serializeIdentityAllTypesMoshi() {
    val allTypes = AllTypes()
    assertJsonEquals(IDENTITY_ALL_TYPES_JSON, moshi.adapter(AllTypes::class.java).toJson(allTypes))
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
    assertJsonEquals(jsonPrinter.print(parsed), jsonPrinter.print(identityAllTypes))}

  @Test fun deserializeIdentityAllTypesMoshi() {
    val allTypesAdapter: JsonAdapter<AllTypes> = moshi.adapter(AllTypes::class.java)

    val allTypes = AllTypes()
    val parsed = allTypesAdapter.fromJson(IDENTITY_ALL_TYPES_JSON)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(parsed.toString()).isEqualTo(allTypes.toString())
    assertJsonEquals(allTypesAdapter.toJson(parsed), allTypesAdapter.toJson(allTypes))
  }

  @Ignore("TODO")
  @Test fun `int64s are encoded with quotes and decoded with either`() {
    val allTypesAdapter: JsonAdapter<AllTypes> = moshi.adapter(AllTypes::class.java)

    val model = AllTypes(squareup_proto3_alltypes_sint64 = 123)
    assertThat(allTypesAdapter.fromJson("""{"sint64":"123"}""")).isEqualTo(model)
    assertThat(allTypesAdapter.fromJson("""{"sint64":123}""")).isEqualTo(model)
    assertThat(allTypesAdapter.fromJson("""{"sint64":123.0}""")).isEqualTo(model)

    assertThat(allTypesAdapter.toJson(model)).isEqualTo("""{"sint64":"123"}""")
  }

  @Test fun `field names are encoded with camel case and decoded with either`() {
    val nestedAdapter: JsonAdapter<NestedCamelCase> = moshi.adapter(NestedCamelCase::class.java)
    val camelAdapter: JsonAdapter<CamelCase> = moshi.adapter(CamelCase::class.java)

    val nested = NestedCamelCase(1)
    assertThat(nestedAdapter.fromJson("""{"oneInt32":1}""")).isEqualTo(nested)
    assertThat(nestedAdapter.fromJson("""{"one_int32":1}""")).isEqualTo(nested)

    // Unknown fields.
    assertThat(nestedAdapter.fromJson("""{"one__int32":1}""")).isEqualTo(NestedCamelCase())
    assertThat(nestedAdapter.fromJson("""{"oneint32":1}""")).isEqualTo(NestedCamelCase())
    assertThat(nestedAdapter.fromJson("""{"one_int_32":1}""")).isEqualTo(NestedCamelCase())
    assertThat(nestedAdapter.fromJson("""{"OneInt32":1}""")).isEqualTo(NestedCamelCase())
    assertThat(nestedAdapter.fromJson("""{"One_Int32":1}""")).isEqualTo(NestedCamelCase())

    // Encoding.
    assertThat(nestedAdapter.toJson(nested)).isEqualTo("""{"oneInt32":1}""")

    // More fields
    assertThat(camelAdapter.fromJson("""{"nestedMessage":{"oneInt32":1}}""")).isEqualTo(CamelCase(nested__message = NestedCamelCase(one_int32 = 1)))
    assertThat(camelAdapter.fromJson("""{"nested__message":{"one_int32":1}}""")).isEqualTo(CamelCase(nested__message = NestedCamelCase(one_int32 = 1)))
    assertThat(camelAdapter.fromJson("""{"RepInt32":[1, 2]}""")).isEqualTo(CamelCase(_Rep_int32 = listOf(1, 2)))
    assertThat(camelAdapter.fromJson("""{"_Rep_int32":[1, 2]}""")).isEqualTo(CamelCase(_Rep_int32 = listOf(1, 2)))
    assertThat(camelAdapter.fromJson("""{"iDitItMyWAy":"frank"}""")).isEqualTo(CamelCase(IDitIt_my_wAy = "frank"))
    assertThat(camelAdapter.fromJson("""{"IDitIt_my_wAy":"frank"}""")).isEqualTo(CamelCase(IDitIt_my_wAy = "frank"))
    assertThat(camelAdapter.fromJson("""{"mapInt32Int32":{"1":2}}""")).isEqualTo(CamelCase(map_int32_Int32 = mapOf(1 to 2)))
    assertThat(camelAdapter.fromJson("""{"map_int32_Int32":{"1":2}}""")).isEqualTo(CamelCase(map_int32_Int32 = mapOf(1 to 2)))

    // Encoding.
    val camel = CamelCase(
        nested__message = NestedCamelCase(1),
        _Rep_int32 = listOf(1, 2),
        IDitIt_my_wAy = "frank",
        map_int32_Int32 = mapOf(1 to 2)
    )
    assertThat(camelAdapter.toJson(camel)).isEqualTo(
        """{"nestedMessage":{"oneInt32":1},"RepInt32":[1,2],"iDitItMyWAy":"frank","mapInt32Int32":{"1":2}}""")

    // Confirm protoc prints the same.
    val protocCamel = CamelCaseOuterClass.CamelCase.newBuilder()
        .setNestedMessage(CamelCaseOuterClass.CamelCase.NestedCamelCase.newBuilder().setOneInt32(1))
        .addAllRepInt32(listOf(1, 2))
        .setIDitItMyWAy("frank")
        .putMapInt32Int32(1, 2)
    assertJsonEquals(camelAdapter.toJson(camel), JsonFormat.printer().print(protocCamel))
  }

  @Test fun all64JsonProtoc(){
    val all64 = All64OuterClass.All64.newBuilder()
        .setMyInt64(Long.MIN_VALUE)
        .setMyUint64(Long.MIN_VALUE)
        .setMySint64(Long.MIN_VALUE)
        .setMyFixed64(Long.MIN_VALUE)
        .setMySfixed64(Long.MIN_VALUE)
        .addAllRepInt64(list(Long.MIN_VALUE))
        .addAllRepUint64(list(Long.MIN_VALUE))
        .addAllRepSint64(list(Long.MIN_VALUE))
        .addAllRepFixed64(list(Long.MIN_VALUE))
        .addAllRepSfixed64(list(Long.MIN_VALUE))
        .addAllPackInt64(list(Long.MIN_VALUE))
        .addAllPackUint64(list(Long.MIN_VALUE))
        .addAllPackSint64(list(Long.MIN_VALUE))
        .addAllPackFixed64(list(Long.MIN_VALUE))
        .addAllPackSfixed64(list(Long.MIN_VALUE))
        .setOneofInt64(Long.MIN_VALUE)
        .putMapInt64Sfixed64(Long.MIN_VALUE, Long.MIN_VALUE)
        .build()

    val jsonPrinter = JsonFormat.printer()
    assertJsonEquals(jsonPrinter.print(all64), ALL_64_JSON)

    val jsonParser = JsonFormat.parser()
    val parsed = All64OuterClass.All64.newBuilder()
        .apply { jsonParser.merge(ALL_64_JSON, this) }
        .build()
    assertThat(parsed).isEqualTo(all64)
  }

  @Ignore("TODO")
  @Test fun all64JsonMoshi() {
    val all64 = All64(
        my_int64 = Long.MIN_VALUE,
        my_uint64 = Long.MIN_VALUE,
        my_sint64 = Long.MIN_VALUE,
        my_fixed64 = Long.MIN_VALUE,
        my_sfixed64 = Long.MIN_VALUE,
        rep_int64 = list(Long.MIN_VALUE),
        rep_uint64 = list(Long.MIN_VALUE),
        rep_sint64 = list(Long.MIN_VALUE),
        rep_fixed64 = list(Long.MIN_VALUE),
        rep_sfixed64 = list(Long.MIN_VALUE),
        pack_int64 = list(Long.MIN_VALUE),
        pack_uint64 = list(Long.MIN_VALUE),
        pack_sint64 = list(Long.MIN_VALUE),
        pack_fixed64 = list(Long.MIN_VALUE),
        pack_sfixed64 = list(Long.MIN_VALUE),
        oneof_int64 = Long.MIN_VALUE,
        map_int64_sfixed64 = mapOf(Long.MIN_VALUE to Long.MIN_VALUE)
    )

    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()
    val jsonAdapter = moshi.adapter(All64::class.java).indent("  ")
    assertJsonEquals(jsonAdapter.toJson(all64), ALL_64_JSON)
    assertThat(jsonAdapter.fromJson(ALL_64_JSON)).isEqualTo(all64)
  }

  @Test fun durationProto() {
    val googleMessage = PizzaOuterClass.PizzaDelivery.newBuilder()
        .setDeliveredWithinOrFree(Duration.newBuilder()
            .setSeconds(1_799)
            .setNanos(500_000_000)
            .build())
        .build()

    val wireMessage = PizzaDelivery(
        delivered_within_or_free = java.time.Duration.ofSeconds(1_799L, 500_000_000L)
    )

    val googleMessageBytes = googleMessage.toByteArray()
    assertThat(wireMessage.encode()).isEqualTo(googleMessageBytes)
    assertThat(PizzaDelivery.ADAPTER.decode(googleMessageBytes)).isEqualTo(wireMessage)
  }

  companion object {
    private val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()

    private val defaultAllTypesProtoc = AllTypesOuterClass.AllTypes.newBuilder()
        .setInt32(111)
        .setUint32(112)
        .setSint32(113)
        .setFixed32(114)
        .setSfixed32(115)
        .setInt64(116L)
        .setUint64(117L)
        .setSint64(118L)
        .setFixed64(119L)
        .setSfixed64(120L)
        .setBool(true)
        .setFloat(122.0F)
        .setDouble(123.0)
        .setString("124")
        .setBytes(com.google.protobuf.ByteString.copyFrom(ByteString.of(123, 125).toByteArray()))
        .setNestedEnum(AllTypesOuterClass.AllTypes.NestedEnum.A)
        .setNestedMessage(AllTypesOuterClass.AllTypes.NestedMessage.newBuilder().setA(999).build())
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

    private val defaultAllTypesWire = AllTypes(
        squareup_proto3_alltypes_int32 = 111,
        squareup_proto3_alltypes_uint32 = 112,
        squareup_proto3_alltypes_sint32 = 113,
        squareup_proto3_alltypes_fixed32 = 114,
        squareup_proto3_alltypes_sfixed32 = 115,
        squareup_proto3_alltypes_int64 = 116L,
        squareup_proto3_alltypes_uint64 = 117L,
        squareup_proto3_alltypes_sint64 = 118L,
        squareup_proto3_alltypes_fixed64 = 119L,
        squareup_proto3_alltypes_sfixed64 = 120L,
        squareup_proto3_alltypes_bool = true,
        squareup_proto3_alltypes_float = 122.0F,
        squareup_proto3_alltypes_double = 123.0,
        squareup_proto3_alltypes_string = "124",
        squareup_proto3_alltypes_bytes = ByteString.of(123, 125),
        nested_enum = AllTypes.NestedEnum.A,
        nested_message = AllTypes.NestedMessage(a = 999),
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
        map_string_message = mapOf("message" to AllTypes.NestedMessage(1)),
        map_string_enum = mapOf("enum" to AllTypes.NestedEnum.A),
        oneof_int32 = 0
    )

    private val DEFAULT_ALL_TYPES_JSON = """{
        |"int32":111,
        |"uint32":112,
        |"sint32":113,
        |"fixed32":114,
        |"sfixed32":115,
        |"int64":"116",
        |"uint64":"117",
        |"sint64":"118",
        |"fixed64":"119",
        |"sfixed64":"120",
        |"bool":true,
        |"float":122.0,
        |"double":123.0,
        |"string":"124",
        |"bytes":"e30=",
        |"nestedEnum":"A",
        |"nestedMessage":{"a":999},
        |"repInt32":[111,111],
        |"repUint32":[112,112],
        |"repSint32":[113,113],
        |"repFixed32":[114,114],
        |"repSfixed32":[115,115],
        |"repInt64":["116","116"],
        |"repUint64":["117","117"],
        |"repSint64":["118","118"],
        |"repFixed64":["119","119"],
        |"repSfixed64":["120","120"],
        |"repBool":[true,true],
        |"repFloat":[122.0,122.0],
        |"repDouble":[123.0,123.0],
        |"repString":["124", "124"],
        |"repBytes":["e30=", "e30="],
        |"repNestedEnum":["A", "A"],
        |"repNestedMessage":[{"a":999},{"a":999}],
        |"packInt32":[111,111],
        |"packUint32":[112,112],
        |"packSint32":[113,113],
        |"packFixed32":[114,114],
        |"packSfixed32":[115,115],
        |"packInt64":["116","116"],
        |"packUint64":["117","117"],
        |"packSint64":["118","118"],
        |"packFixed64":["119","119"],
        |"packSfixed64":["120","120"],
        |"packBool":[true,true],
        |"packFloat":[122.0,122.0],
        |"packDouble":[123.0,123.0],
        |"packNestedEnum":["A", "A"],
        |"mapInt32Int32":{"1":2},
        |"mapStringString":{"key":"value"},
        |"mapStringMessage":{"message":{"a":1}},
        |"mapStringEnum":{"enum":"A"},
        |"oneofInt32" : 0.0
        |}""".trimMargin()

    private val ALL_64_JSON = """
        |{
        |  "myInt64": "-9223372036854775808",
        |  "myUint64": "9223372036854775808",
        |  "mySint64": "-9223372036854775808",
        |  "myFixed64": "9223372036854775808",
        |  "mySfixed64": "-9223372036854775808",
        |  "repInt64": ["-9223372036854775808", "-9223372036854775808"],
        |  "repUint64": ["9223372036854775808", "9223372036854775808"],
        |  "repSint64": ["-9223372036854775808", "-9223372036854775808"],
        |  "repFixed64": ["9223372036854775808", "9223372036854775808"],
        |  "repSfixed64": ["-9223372036854775808", "-9223372036854775808"],
        |  "packInt64": ["-9223372036854775808", "-9223372036854775808"],
        |  "packUint64": ["9223372036854775808", "9223372036854775808"],
        |  "packSint64": ["-9223372036854775808", "-9223372036854775808"],
        |  "packFixed64": ["9223372036854775808", "9223372036854775808"],
        |  "packSfixed64": ["-9223372036854775808", "-9223372036854775808"],
        |  "oneofInt64": "-9223372036854775808",
        |  "mapInt64Sfixed64": {
        |    "-9223372036854775808": "-9223372036854775808"
        |  }
        |}""".trimMargin()

    private const val IDENTITY_ALL_TYPES_JSON = "{}"

    private fun <T : kotlin.Any> list(t: T): List<T> {
      return listOf(t, t)
    }
  }
}
