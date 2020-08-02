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

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto2.alltypes.AllTypes
import okio.ByteString
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import squareup.proto3.All64
import squareup.proto3.AllStructs
import squareup.proto3.BuyOneGetOnePromotion
import squareup.proto3.CamelCase
import squareup.proto3.CamelCase.NestedCamelCase
import squareup.proto3.FreeDrinkPromotion
import squareup.proto3.FreeGarlicBreadPromotion
import squareup.proto3.Pizza
import squareup.proto3.PizzaDelivery
import java.io.File
import java.util.Collections

/**
 * Tests meant to be executed against both Java generated and Kotlin generated code among different
 * JSON libraries.
 */
@RunWith(Parameterized::class)
class WireJsonTest {
  @Parameterized.Parameter(0)
  internal lateinit var jsonLibrary: JsonLibrary

  @Test fun allTypesSerializeTest() {
    val value = allTypesBuilder().build()
    assertJsonEquals(ALL_TYPES_JSON, jsonLibrary.toJson(value, AllTypes::class.java))
  }

  @Test fun allTypesDeserializeTest() {
    val value = allTypesBuilder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_JSON, AllTypes::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, AllTypes::class.java),
        jsonLibrary.toJson(value, AllTypes::class.java))
  }

  @Test fun allTypesIdentitySerializeTest() {
    val value = allTypesIdentityBuilder().build()
    assertJsonEquals(ALL_TYPES_IDENTITY_JSON, jsonLibrary.toJson(value, AllTypes::class.java))
  }

  @Test fun allTypesIdentityDeserializeTest() {
    val value = allTypesIdentityBuilder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_IDENTITY_JSON, AllTypes::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, AllTypes::class.java),
        jsonLibrary.toJson(value, AllTypes::class.java))
  }

  @Test fun omitsUnknownFields() {
    val builder = allTypesBuilder()
    builder.addUnknownField(9000, FieldEncoding.FIXED32, 9000)
    builder.addUnknownField(9001, FieldEncoding.FIXED64, 9001L)
    builder.addUnknownField(9002, FieldEncoding.LENGTH_DELIMITED,
        ByteString.of('9'.toByte(), '0'.toByte(), '0'.toByte(), '2'.toByte()))
    builder.addUnknownField(9003, FieldEncoding.VARINT, 9003L)

    val value = builder.build()
    assertJsonEquals(ALL_TYPES_JSON, jsonLibrary.toJson(value, AllTypes::class.java))
  }

  @Test fun fieldNamesAreEncodedWithCamelCaseAndDecodedWithEither() {
    val nested = NestedCamelCase.Builder().one_int32(1).build()
    assertThat(jsonLibrary.fromJson("""{"oneInt32":1}""", NestedCamelCase::class.java))
        .isEqualTo(nested)
    assertThat(jsonLibrary.fromJson("""{"one_int32":1}""", NestedCamelCase::class.java))
        .isEqualTo(nested)

    // Unknown fields.
    assertThat(jsonLibrary.fromJson("""{"one__int32":1}""", NestedCamelCase::class.java))
        .isEqualTo(NestedCamelCase.Builder().build())
    assertThat(jsonLibrary.fromJson("""{"oneint32":1}""", NestedCamelCase::class.java))
        .isEqualTo(NestedCamelCase.Builder().build())
    assertThat(jsonLibrary.fromJson("""{"one_int_32":1}""", NestedCamelCase::class.java))
        .isEqualTo(NestedCamelCase.Builder().build())
    assertThat(jsonLibrary.fromJson("""{"OneInt32":1}""", NestedCamelCase::class.java))
        .isEqualTo(NestedCamelCase.Builder().build())
    assertThat(jsonLibrary.fromJson("""{"One_Int32":1}""", NestedCamelCase::class.java))
        .isEqualTo(NestedCamelCase.Builder().build())

    // Encoding.
    assertThat(jsonLibrary.toJson(nested, NestedCamelCase::class.java)).isEqualTo(
        """{"oneInt32":1}""")

    // More fields
    assertThat(jsonLibrary.fromJson("""{"nestedMessage":{"oneInt32":1}}""", CamelCase::class.java))
        .isEqualTo(
            CamelCase.Builder().nested__message(NestedCamelCase.Builder().one_int32(1).build())
                .build())
    assertThat(
        jsonLibrary.fromJson("""{"nested__message":{"one_int32":1}}""", CamelCase::class.java))
        .isEqualTo(
            CamelCase.Builder().nested__message(NestedCamelCase.Builder().one_int32(1).build())
                .build())
    assertThat(jsonLibrary.fromJson("""{"RepInt32":[1, 2]}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder()._Rep_int32(listOf(1, 2)).build())
    assertThat(jsonLibrary.fromJson("""{"_Rep_int32":[1, 2]}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder()._Rep_int32(listOf(1, 2)).build())
    assertThat(jsonLibrary.fromJson("""{"iDitItMyWAy":"frank"}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder().IDitIt_my_wAy("frank").build())
    assertThat(jsonLibrary.fromJson("""{"IDitIt_my_wAy":"frank"}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder().IDitIt_my_wAy("frank").build())
    assertThat(jsonLibrary.fromJson("""{"mapInt32Int32":{"1":2}}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder().map_int32_Int32(mapOf(1 to 2)).build())
    assertThat(jsonLibrary.fromJson("""{"map_int32_Int32":{"1":2}}""", CamelCase::class.java))
        .isEqualTo(CamelCase.Builder().map_int32_Int32(mapOf(1 to 2)).build())

    // Encoding.
    val camel = CamelCase.Builder()
        .nested__message(NestedCamelCase.Builder().one_int32(1).build())
        ._Rep_int32(listOf(1, 2))
        .IDitIt_my_wAy("frank")
        .map_int32_Int32(mapOf(1 to 2))
        .build()
    assertJsonEquals(jsonLibrary.toJson(camel, CamelCase::class.java), """
        |{
        |  "nestedMessage": {
        |    "oneInt32": 1
        |  },
        |  "RepInt32": [
        |    1,
        |    2
        |  ],
        |  "iDitItMyWAy": "frank",
        |  "mapInt32Int32": {
        |    "1": 2
        |  }
        |}
        |""".trimMargin())

    // Confirm protoc prints the same.
    assertJsonEquals(CAMEL_CASE_JSON, jsonLibrary.toJson(camel, CamelCase::class.java))
  }

  @Test fun allStruct() {
    val value = AllStructs.Builder()
        .struct(mapOf("a" to 1.0))
        .list(listOf("a", 3.0))
        .value_a("a")
        .value_b(33.0)
        .value_c(true)
        .value_e(mapOf("a" to 1.0))
        .value_f(listOf("a", 3.0))
        .build()

    assertJsonEquals(ALL_STRUCT_JSON, jsonLibrary.toJson(value, AllStructs::class.java))

    val parsed = jsonLibrary.fromJson(ALL_STRUCT_JSON, AllStructs::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, AllStructs::class.java),
        jsonLibrary.toJson(value, AllStructs::class.java))
  }

  @Test fun allStructWithIdentities() {
    val value = AllStructs.Builder()
        .struct(mapOf("a" to null))
        .list(emptyList<Any>())
        .value_a(mapOf("a" to listOf("b", 2.0, mapOf("c" to false))))
        .value_b(listOf(mapOf("d" to null, "e" to "trois")))
        .value_c(emptyList<Any>())
        .value_d(emptyMap<String, Any>())
        .build()

    assertJsonEquals(ALL_STRUCT_IDENTITY_JSON, jsonLibrary.toJson(value, AllStructs::class.java))

    val parsed = jsonLibrary.fromJson(ALL_STRUCT_IDENTITY_JSON, AllStructs::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, AllStructs::class.java),
        jsonLibrary.toJson(value, AllStructs::class.java))
  }

  @Test fun pizzaDelivery() {
    val value = PizzaDelivery.Builder()
        .address("507 Cross Street")
        .pizzas(listOf(Pizza.Builder().toppings(listOf("pineapple", "onion")).build()))
        .promotion(AnyMessage.pack(BuyOneGetOnePromotion.Builder().coupon("MAUI").build()))
        .delivered_within_or_free(durationOfSeconds(1_799L, 500_000_000L))
        .loyalty(emptyMap<String, Any?>())
        .ordered_at(ofEpochSecond(-631152000L, 250_000_000L))
        .build()

    assertJsonEquals(PIZZA_DELIVERY_JSON, jsonLibrary.toJson(value, PizzaDelivery::class.java))

    val parsed = jsonLibrary.fromJson(PIZZA_DELIVERY_JSON, PizzaDelivery::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, PizzaDelivery::class.java),
        jsonLibrary.toJson(value, PizzaDelivery::class.java))
  }

  @Test fun anyMessageWithUnregisteredTypeOnReading() {
    try {
      jsonLibrary.fromJson(PIZZA_DELIVERY_UNKNOWN_TYPE_JSON, PizzaDelivery::class.java)
      fail()
    } catch (expected: JsonDataException) {
      // Moshi.
      assertThat(expected).hasMessage("Cannot resolve type: " +
          "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion in \$.promotion")
    } catch (expected: JsonSyntaxException) {
      // Gson.
      assertThat(expected)
          .hasMessageContaining("Cannot resolve type: " +
              "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion in \$.promotion")
    }
  }

  @Test fun anyMessageWithUnregisteredTypeOnWriting() {
    val value = PizzaDelivery.Builder()
        .address("507 Cross Street")
        .pizzas(listOf(Pizza.Builder().toppings(listOf("pineapple", "onion")).build()))
        .promotion(
            AnyMessage.pack(FreeGarlicBreadPromotion.Builder().is_extra_cheesey(true).build()))
        .delivered_within_or_free(durationOfSeconds(1_799L, 500_000_000L))
        .loyalty(emptyMap<String, Any?>())
        .ordered_at(ofEpochSecond(-631152000L, 250_000_000L))
        .build()

    try {
      jsonLibrary.toJson(value, PizzaDelivery::class.java)
      fail()
    } catch (expected: JsonDataException) {
      // Moshi.
      assertThat(expected)
          .hasMessage("Cannot find type for url: " +
              "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion " +
              "in \$.promotion.@type")
    } catch (expected: JsonIOException) {
      // Gson.
      assertThat(expected)
          .hasMessageContaining("Cannot find type for url: " +
              "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion")
    }
  }

  @Test fun anyMessageWithoutType() {
    try {
      jsonLibrary.fromJson(PIZZA_DELIVERY_WITHOUT_TYPE_JSON, PizzaDelivery::class.java)
      fail()
    } catch (expected: JsonDataException) {
      // Moshi.
      assertThat(expected).hasMessage("expected @type in \$.promotion")
    } catch (expected: JsonSyntaxException) {
      // Gson.
      assertThat(expected).hasMessageContaining("expected @type in \$.promotion")
    }
  }

  @Test fun literalNullsReplacedWithIdentityInProto3() {
    val expected = PizzaDelivery.Builder()
        .pizzas(listOf(Pizza.Builder().build()))
        .promotion(AnyMessage.pack(BuyOneGetOnePromotion.Builder().build()))
        .build()
    val parsed = jsonLibrary.fromJson(PIZZA_DELIVERY_LITERAL_NULLS_JSON, PizzaDelivery::class.java)
    assertThat(parsed).isEqualTo(expected)
  }

  @Test fun enumCanBeDecodedFromInt() {
    val json = """{"drink":9}"""
    val value = jsonLibrary.fromJson(json, FreeDrinkPromotion::class.java)
    assertThat(value.drink).isEqualTo(FreeDrinkPromotion.Drink.ROOT_BEER)
  }

  @Test fun enumCanBeDecodedFromString() {
    val json = """{"drink":"ROOT_BEER"}"""
    val value = jsonLibrary.fromJson(json, FreeDrinkPromotion::class.java)
    assertThat(value.drink).isEqualTo(FreeDrinkPromotion.Drink.ROOT_BEER)
  }

  @Test fun all64MaxValue() {
    val value = All64.Builder()
        .my_int64(Long.MAX_VALUE)
        .my_uint64(Long.MAX_VALUE)
        .my_sint64(Long.MAX_VALUE)
        .my_fixed64(Long.MAX_VALUE)
        .my_sfixed64(Long.MAX_VALUE)
        .rep_int64(list(Long.MAX_VALUE))
        .rep_uint64(list(Long.MAX_VALUE))
        .rep_sint64(list(Long.MAX_VALUE))
        .rep_fixed64(list(Long.MAX_VALUE))
        .rep_sfixed64(list(Long.MAX_VALUE))
        .pack_int64(list(Long.MAX_VALUE))
        .pack_uint64(list(Long.MAX_VALUE))
        .pack_sint64(list(Long.MAX_VALUE))
        .pack_fixed64(list(Long.MAX_VALUE))
        .pack_sfixed64(list(Long.MAX_VALUE))
        .oneof_int64(Long.MAX_VALUE)
        .build()

    assertJsonEquals(ALL_64_JSON_MAX_VALUE, jsonLibrary.toJson(value, All64::class.java))

    val parsed = jsonLibrary.fromJson(ALL_64_JSON_MAX_VALUE, All64::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, All64::class.java),
        jsonLibrary.toJson(value, All64::class.java))
  }

  @Test fun all64MainValue() {
    val value = All64.Builder()
        .my_int64(Long.MIN_VALUE)
        .my_uint64(Long.MIN_VALUE)
        .my_sint64(Long.MIN_VALUE)
        .my_fixed64(Long.MIN_VALUE)
        .my_sfixed64(Long.MIN_VALUE)
        .rep_int64(list(Long.MIN_VALUE))
        .rep_uint64(list(Long.MIN_VALUE))
        .rep_sint64(list(Long.MIN_VALUE))
        .rep_fixed64(list(Long.MIN_VALUE))
        .rep_sfixed64(list(Long.MIN_VALUE))
        .pack_int64(list(Long.MIN_VALUE))
        .pack_uint64(list(Long.MIN_VALUE))
        .pack_sint64(list(Long.MIN_VALUE))
        .pack_fixed64(list(Long.MIN_VALUE))
        .pack_sfixed64(list(Long.MIN_VALUE))
        .oneof_int64(Long.MIN_VALUE)
        .build()

    assertJsonEquals(ALL_64_JSON_MIN_VALUE, jsonLibrary.toJson(value, All64::class.java))

    val parsed = jsonLibrary.fromJson(ALL_64_JSON_MIN_VALUE, All64::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, All64::class.java),
        jsonLibrary.toJson(value, All64::class.java))
  }

  @Test fun crashOnBigNumbersWhenIntIsSigned() {
    val json = """{"mySint64": "9223372036854775808"}"""

    val all64 = All64.Builder().build()

    try {
      assertThat(jsonLibrary.fromJson(json, All64::class.java)).isEqualTo(all64)
      fail()
    } catch (e: JsonDataException) {
      // Moshi.
      assertThat(e).hasMessage("decode failed: 9223372036854775808 at path \$.mySint64")
    } catch (e: JsonSyntaxException) {
      // Gson.
      assertThat(e).hasMessage("decode failed: 9223372036854775808 at path \$.mySint64")
    }
  }

  @Test fun `int64s are encoded with quotes and decoded with either`() {
    val signed = All64.Builder().my_sint64(123).rep_sint64(listOf(456)).build()
    assertThat(jsonLibrary.fromJson("""{"mySint64":"123", "repSint64": ["456"]}""",
        All64::class.java)).isEqualTo(signed)
    assertThat(jsonLibrary.fromJson("""{"mySint64":123, "repSint64": [456]}""",
        All64::class.java)).isEqualTo(signed)
    assertThat(jsonLibrary.fromJson("""{"mySint64":123.0, "repSint64": [456.0]}""",
        All64::class.java)).isEqualTo(signed)

    val signedJson = jsonLibrary.toJson(signed, , All64::class.java)
    assertThat(signedJson).contains(""""mySint64":"123"""")
    assertThat(signedJson).contains(""""repSint64":["456"]""")

    val unsigned = All64.Builder().my_uint64(123).rep_uint64(listOf(456)).build()
    assertThat(jsonLibrary.fromJson("""{"myUint64":"123", "repUint64": ["456"]}""",
        All64::class.java)).isEqualTo(unsigned)
    assertThat(jsonLibrary.fromJson("""{"myUint64":123, "repUint64": [456]}""",
        All64::class.java)).isEqualTo(unsigned)
    assertThat(jsonLibrary.fromJson("""{"myUint64":123.0, "repUint64": [456.0]}""",
        All64::class.java)).isEqualTo(unsigned)

    val unsignedJson = jsonLibrary.toJson(unsigned)
    assertThat(unsignedJson).contains(""""myUint64":"123"""")
    assertThat(unsignedJson).contains(""""repUint64":["456"]""")
  }

  companion object {
    // Return a two-element list with a given repeated value
    private fun <T> list(x: T): List<T> = listOf(x, x)

    private fun allTypesIdentityBuilder(): AllTypes.Builder {
      return AllTypes.Builder()
          .opt_int32(0)
          .opt_uint32(0)
          .opt_sint32(0)
          .opt_fixed32(0)
          .opt_sfixed32(0)
          .opt_int64(0L)
          .opt_uint64(0L)
          .opt_sint64(0L)
          .opt_fixed64(0L)
          .opt_sfixed64(0L)
          .opt_bool(false)
          .opt_float(0f)
          .opt_double(0.0)
          .opt_string("")
          .opt_bytes(ByteString.EMPTY)
          .opt_nested_enum(AllTypes.NestedEnum.A)
          .opt_nested_message(null)
          .req_int32(0)
          .req_uint32(0)
          .req_sint32(0)
          .req_fixed32(0)
          .req_sfixed32(0)
          .req_int64(0L)
          .req_uint64(0L)
          .req_sint64(0L)
          .req_fixed64(0L)
          .req_sfixed64(0L)
          .req_bool(true)
          .req_float(0f)
          .req_double(0.0)
          .req_string("")
          .req_bytes(ByteString.EMPTY)
          .req_nested_enum(AllTypes.NestedEnum.A)
          .req_nested_message(AllTypes.NestedMessage.Builder().a(0).build())
    }

    private fun allTypesBuilder(): AllTypes.Builder {
      val bytes = ByteString.of(123.toByte(), 125.toByte())
      val nestedMessage = AllTypes.NestedMessage.Builder().a(999).build()
      return AllTypes.Builder()
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
          .opt_float(122.0f)
          .opt_double(123.0)
          .opt_string("124")
          .opt_bytes(bytes)
          .opt_nested_enum(AllTypes.NestedEnum.A)
          .opt_nested_message(nestedMessage)
          .req_int32(111)
          .req_uint32(112)
          .req_sint32(113)
          .req_fixed32(114)
          .req_sfixed32(115)
          .req_int64(116L)
          .req_uint64(117L)
          .req_sint64(118L)
          .req_fixed64(119L)
          .req_sfixed64(120L)
          .req_bool(true)
          .req_float(122.0f)
          .req_double(123.0)
          .req_string("124")
          .req_bytes(bytes)
          .req_nested_enum(AllTypes.NestedEnum.A)
          .req_nested_message(nestedMessage)
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
          .rep_float(list(122.0f))
          .rep_double(list(123.0))
          .rep_string(list("124"))
          .rep_bytes(list(bytes))
          .rep_nested_enum(list(AllTypes.NestedEnum.A))
          .rep_nested_message(list(nestedMessage))
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
          .pack_float(list(122.0f))
          .pack_double(list(123.0))
          .pack_nested_enum(list(AllTypes.NestedEnum.A))
          .map_int32_int32(Collections.singletonMap(1, 2))
          .map_string_string(Collections.singletonMap("key", "value"))
          .map_string_message(Collections.singletonMap("message", AllTypes.NestedMessage(1)))
          .map_string_enum(Collections.singletonMap("enum", AllTypes.NestedEnum.A))
          .oneof_int32(4444)
          .ext_opt_int32(Int.MAX_VALUE)
          .ext_opt_int64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_uint64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_sint64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_bool(true)
          .ext_opt_float(1.2345e6f)
          .ext_opt_double(1.2345e67)
          .ext_opt_nested_enum(AllTypes.NestedEnum.A)
          .ext_opt_nested_message(nestedMessage)
          .ext_rep_int32(list(Int.MAX_VALUE))
          .ext_rep_uint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_rep_sint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_rep_bool(list(true))
          .ext_rep_float(list(1.2345e6f))
          .ext_rep_double(list(1.2345e67))
          .ext_rep_nested_enum(list(AllTypes.NestedEnum.A))
          .ext_rep_nested_message(list(nestedMessage))
          .ext_pack_int32(list(Int.MAX_VALUE))
          .ext_pack_uint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_pack_sint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_pack_bool(list(true))
          .ext_pack_float(list(1.2345e6f))
          .ext_pack_double(list(1.2345e67))
          .ext_pack_nested_enum(list(AllTypes.NestedEnum.A))
    }

    private val ALL_TYPES_JSON = loadJson("all_types_proto2.json")

    private val ALL_TYPES_IDENTITY_JSON = loadJson("all_types_identity_proto2.json")

    private val CAMEL_CASE_JSON = loadJson("camel_case_proto3.json")

    private val ALL_STRUCT_JSON = loadJson("all_struct_proto3.json")

    private val ALL_STRUCT_IDENTITY_JSON = loadJson("all_struct_identity_proto3.json")

    private val PIZZA_DELIVERY_JSON = loadJson("pizza_delivery_proto3.json")

    private val PIZZA_DELIVERY_UNKNOWN_TYPE_JSON =
        loadJson("pizza_delivery_unknown_type_proto3.json")

    private val PIZZA_DELIVERY_WITHOUT_TYPE_JSON =
        loadJson("pizza_delivery_without_type_proto3.json")

    private val PIZZA_DELIVERY_LITERAL_NULLS_JSON =
        loadJson("pizza_delivery_literal_nulls_proto3.json")

    private val ALL_64_JSON_MIN_VALUE = loadJson("all_64_min_proto3.json")

    private val ALL_64_JSON_MAX_VALUE = loadJson("all_64_max_proto3.json")

    private val moshi = object : JsonLibrary {
      private val moshi = Moshi.Builder()
          .add(WireJsonAdapterFactory().plus(listOf(BuyOneGetOnePromotion.ADAPTER)))
          .build()

      override fun toString() = "Moshi"

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return moshi.adapter(type).fromJson(json)!!
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return moshi.adapter(type).toJson(value)
      }
    }

    private val gson = object : JsonLibrary {
      private val gson = GsonBuilder()
          .registerTypeAdapterFactory(
              WireTypeAdapterFactory().plus(listOf(BuyOneGetOnePromotion.ADAPTER)))
          .disableHtmlEscaping()
          .create()

      override fun toString() = "Gson"

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return gson.fromJson(json, type)
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return gson.toJson(value, type)
      }
    }

    @Parameters(name = "{0}")
    @JvmStatic
    internal fun parameters() = listOf(arrayOf(gson), arrayOf(moshi))

    private fun loadJson(fileName: String): String {
      return File("src/commonTest/shared/json", fileName).source().use { it.buffer().readUtf8() }
    }
  }
}

internal interface JsonLibrary {
  fun <T> fromJson(json: String, type: Class<T>): T
  fun <T> toJson(value: T, type: Class<T>): String
}
