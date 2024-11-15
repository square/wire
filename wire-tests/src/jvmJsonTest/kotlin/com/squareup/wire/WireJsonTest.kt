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
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto2.alltypes.AllTypes as AllTypesProto2
import com.squareup.wire.proto3.alltypes.AllTypes as AllTypesProto3
import java.io.File
import java.util.Collections
import okio.ByteString
import okio.buffer
import okio.source
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import squareup.proto3.All32
import squareup.proto3.All64
import squareup.proto3.AllStructs
import squareup.proto3.AllWrappers
import squareup.proto3.BuyOneGetOnePromotion
import squareup.proto3.CamelCase
import squareup.proto3.CamelCase.NestedCamelCase
import squareup.proto3.FreeDrinkPromotion
import squareup.proto3.FreeGarlicBreadPromotion
import squareup.proto3.MapTypes
import squareup.proto3.Pizza
import squareup.proto3.PizzaDelivery

/**
 * Tests meant to be executed against both Java generated and Kotlin generated code among different
 * JSON libraries.
 */
@RunWith(Parameterized::class)
class WireJsonTest {
  @Parameterized.Parameter(0)
  internal lateinit var jsonLibrary: JsonLibrary

  @Test fun allTypesSerializeTest() {
    val value = allTypesProto2Builder().build()
    assertJsonEquals(ALL_TYPES_PROTO2_JSON, jsonLibrary.toJson(value, AllTypesProto2::class.java))
  }

  @Test fun allTypesDeserializeTest() {
    val value = allTypesProto2Builder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_PROTO2_JSON, AllTypesProto2::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllTypesProto2::class.java),
      jsonLibrary.toJson(value, AllTypesProto2::class.java),
    )
  }

  @Test fun allTypesIdentitySerializeTest() {
    val value = allTypesProto2IdentityBuilder().build()
    assertJsonEquals(
      ALL_TYPES_IDENTITY_PROTO2_JSON,
      jsonLibrary.toJson(value, AllTypesProto2::class.java),
    )
  }

  @Test fun allTypesIdentityDeserializeTest() {
    val value = allTypesProto2IdentityBuilder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_IDENTITY_PROTO2_JSON, AllTypesProto2::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllTypesProto2::class.java),
      jsonLibrary.toJson(value, AllTypesProto2::class.java),
    )
  }

  @Test fun omitsUnknownFields() {
    val builder = allTypesProto2Builder()
    builder.addUnknownField(9000, FieldEncoding.FIXED32, 9000)
    builder.addUnknownField(9001, FieldEncoding.FIXED64, 9001L)
    builder.addUnknownField(
      9002,
      FieldEncoding.LENGTH_DELIMITED,
      ByteString.of('9'.toByte(), '0'.toByte(), '0'.toByte(), '2'.toByte()),
    )
    builder.addUnknownField(9003, FieldEncoding.VARINT, 9003L)

    val value = builder.build()
    assertJsonEquals(ALL_TYPES_PROTO2_JSON, jsonLibrary.toJson(value, AllTypesProto2::class.java))
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
      """{"oneInt32":1}""",
    )

    // More fields
    assertThat(jsonLibrary.fromJson("""{"nestedMessage":{"oneInt32":1}}""", CamelCase::class.java))
      .isEqualTo(
        CamelCase.Builder().nested__message(NestedCamelCase.Builder().one_int32(1).build())
          .build(),
      )
    assertThat(
      jsonLibrary.fromJson("""{"nested__message":{"one_int32":1}}""", CamelCase::class.java),
    )
      .isEqualTo(
        CamelCase.Builder().nested__message(NestedCamelCase.Builder().one_int32(1).build())
          .build(),
      )
    assertThat(jsonLibrary.fromJson("""{"RepInt32":[1, 2]}""", CamelCase::class.java))
      .isEqualTo(CamelCase.Builder()._Rep_int32(listOf(1, 2)).build())
    assertThat(jsonLibrary.fromJson("""{"_Rep_int32":[1, 2]}""", CamelCase::class.java))
      .isEqualTo(CamelCase.Builder()._Rep_int32(listOf(1, 2)).build())
    assertThat(jsonLibrary.fromJson("""{"IDitItMyWAy":"frank"}""", CamelCase::class.java))
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
    assertJsonEquals(
      jsonLibrary.toJson(camel, CamelCase::class.java),
      """
        |{
        |  "nestedMessage": {
        |    "oneInt32": 1
        |  },
        |  "RepInt32": [
        |    1,
        |    2
        |  ],
        |  "IDitItMyWAy": "frank",
        |  "mapInt32Int32": {
        |    "1": 2
        |  }
        |}
        |
      """.trimMargin(),
    )

    // Confirm protoc prints the same.
    assertJsonEquals(CAMEL_CASE_JSON, jsonLibrary.toJson(camel, CamelCase::class.java))
  }

  @Test fun allStruct() {
    if (jsonLibrary.writeIdentityValues) return

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
      jsonLibrary.toJson(value, AllStructs::class.java),
    )
  }

  @Test fun allStructWithIdentities() {
    val json = if (jsonLibrary.writeIdentityValues) {
      ALL_STRUCT_IDENTITY_WRITTEN_JSON
    } else {
      ALL_STRUCT_IDENTITY_JSON
    }

    val value = AllStructs.Builder()
      .struct(mapOf("a" to null))
      .list(emptyList<Any>())
      .value_a(mapOf("a" to listOf("b", 2.0, mapOf("c" to false))))
      .value_b(listOf(mapOf("d" to null, "e" to "trois")))
      .value_c(emptyList<Any>())
      .value_d(emptyMap<String, Any>())
      .build()

    assertJsonEquals(json, jsonLibrary.toJson(value, AllStructs::class.java))

    val parsed = jsonLibrary.fromJson(json, AllStructs::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllStructs::class.java),
      jsonLibrary.toJson(value, AllStructs::class.java),
    )
  }

  @Test fun pizzaDelivery() {
    if (jsonLibrary.writeIdentityValues) return

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
      jsonLibrary.toJson(value, PizzaDelivery::class.java),
    )
  }

  @Test fun anyMessageWithUnregisteredTypeOnReading() {
    try {
      jsonLibrary.fromJson(PIZZA_DELIVERY_UNKNOWN_TYPE_JSON, PizzaDelivery::class.java)
      fail()
    } catch (expected: JsonDataException) {
      // Moshi.
      assertThat(expected).hasMessage(
        "Cannot resolve type: " +
          "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion in \$.promotion",
      )
    } catch (expected: JsonSyntaxException) {
      // Gson.
      assertThat(expected)
        .messageContains(
          "Cannot resolve type: " +
            "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion in \$.promotion",
        )
    }
  }

  @Test fun anyMessageWithUnregisteredTypeOnWriting() {
    val value = PizzaDelivery.Builder()
      .address("507 Cross Street")
      .pizzas(listOf(Pizza.Builder().toppings(listOf("pineapple", "onion")).build()))
      .promotion(
        AnyMessage.pack(FreeGarlicBreadPromotion.Builder().is_extra_cheesey(true).build()),
      )
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
        .hasMessage(
          "Cannot find type for url: " +
            "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion " +
            "in \$.promotion.@type",
        )
    } catch (expected: JsonIOException) {
      // Gson.
      assertThat(expected)
        .messageContains(
          "Cannot find type for url: " +
            "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion",
        )
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
      assertThat(expected).messageContains("expected @type in \$.promotion")
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
      .rep_int64(list(-1L))
      .rep_uint64(list(-1L))
      .rep_sint64(list(-1L))
      .rep_fixed64(list(-1L))
      .rep_sfixed64(list(-1L))
      .pack_int64(list(Long.MAX_VALUE))
      .pack_uint64(list(Long.MAX_VALUE))
      .pack_sint64(list(Long.MAX_VALUE))
      .pack_fixed64(list(Long.MAX_VALUE))
      .pack_sfixed64(list(Long.MAX_VALUE))
      .oneof_int64(Long.MAX_VALUE)
      .map_int64_int64(mapOf(Long.MAX_VALUE to Long.MAX_VALUE))
      .map_int64_uint64(mapOf(Long.MAX_VALUE to -1L))
      .map_int64_sint64(mapOf(Long.MAX_VALUE to Long.MAX_VALUE))
      .map_int64_fixed64(mapOf(Long.MAX_VALUE to -1L))
      .map_int64_sfixed64(mapOf(Long.MAX_VALUE to Long.MAX_VALUE))
      .build()

    assertJsonEquals(ALL_64_JSON_MAX_VALUE, jsonLibrary.toJson(value, All64::class.java))

    val parsed = jsonLibrary.fromJson(ALL_64_JSON_MAX_VALUE, All64::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, All64::class.java),
      jsonLibrary.toJson(value, All64::class.java),
    )
  }

  @Test fun all64MinValue() {
    val value = All64.Builder()
      .my_int64(Long.MIN_VALUE)
      .my_uint64(Long.MIN_VALUE)
      .my_sint64(Long.MIN_VALUE)
      .my_fixed64(Long.MIN_VALUE)
      .my_sfixed64(Long.MIN_VALUE)
      .rep_int64(list(0L))
      .rep_uint64(list(0L))
      .rep_sint64(list(0L))
      .rep_fixed64(list(0L))
      .rep_sfixed64(list(0L))
      .pack_int64(list(Long.MIN_VALUE))
      .pack_uint64(list(Long.MIN_VALUE))
      .pack_sint64(list(Long.MIN_VALUE))
      .pack_fixed64(list(Long.MIN_VALUE))
      .pack_sfixed64(list(Long.MIN_VALUE))
      .oneof_int64(Long.MIN_VALUE)
      .map_int64_int64(mapOf(Long.MIN_VALUE to Long.MIN_VALUE))
      .map_int64_uint64(mapOf(Long.MIN_VALUE to 0L))
      .map_int64_sint64(mapOf(Long.MIN_VALUE to Long.MIN_VALUE))
      .map_int64_fixed64(mapOf(Long.MIN_VALUE to 0L))
      .map_int64_sfixed64(mapOf(Long.MIN_VALUE to Long.MIN_VALUE))
      .build()

    assertJsonEquals(ALL_64_JSON_MIN_VALUE, jsonLibrary.toJson(value, All64::class.java))

    val parsed = jsonLibrary.fromJson(ALL_64_JSON_MIN_VALUE, All64::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, All64::class.java),
      jsonLibrary.toJson(value, All64::class.java),
    )
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
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint64":"123", "repSint64": ["456"]}""",
        All64::class.java,
      ),
    ).isEqualTo(signed)
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint64":123, "repSint64": [456]}""",
        All64::class.java,
      ),
    ).isEqualTo(signed)
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint64":123.0, "repSint64": [456.0]}""",
        All64::class.java,
      ),
    ).isEqualTo(signed)

    val signedJson = jsonLibrary.toJson(signed, All64::class.java)
    assertThat(signedJson).contains(""""mySint64":"123"""")
    assertThat(signedJson).contains(""""repSint64":["456"]""")

    val unsigned = All64.Builder().my_uint64(123).rep_uint64(listOf(456)).build()
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint64":"123", "repUint64": ["456"]}""",
        All64::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint64":123, "repUint64": [456]}""",
        All64::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint64":123.0, "repUint64": [456.0]}""",
        All64::class.java,
      ),
    ).isEqualTo(unsigned)

    val unsignedJson = jsonLibrary.toJson(unsigned, All64::class.java)
    assertThat(unsignedJson).contains(""""myUint64":"123"""")
    assertThat(unsignedJson).contains(""""repUint64":["456"]""")
  }

  @Test fun `int32s are encoded as unsigned decoded with either`() {
    val signed = All32.Builder().my_sint32(Int.MIN_VALUE).rep_sint32(listOf(Int.MIN_VALUE)).build()
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint32":"-2147483648", "repSint32": ["-2147483648"]}""",
        All32::class.java,
      ),
    ).isEqualTo(signed)
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint32":-2147483648, "repSint32": [-2147483648]}""",
        All32::class.java,
      ),
    ).isEqualTo(signed)
    assertThat(
      jsonLibrary.fromJson(
        """{"mySint32":-2147483648.0, "repSint32": [-2147483648.0]}""",
        All32::class.java,
      ),
    ).isEqualTo(signed)

    val signedJson = jsonLibrary.toJson(signed, All32::class.java)
    assertThat(signedJson).contains(""""mySint32":-2147483648""")
    assertThat(signedJson).contains(""""repSint32":[-2147483648]""")

    val unsigned =
      All32.Builder().my_uint32(Int.MIN_VALUE).rep_uint32(listOf(Int.MIN_VALUE)).build()
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint32":-2147483648, "repUint32": [-2147483648]}""",
        All32::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint32":-2147483648.0, "repUint32": [-2147483648.0]}""",
        All32::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint32":2147483648.0, "repUint32": [-2147483648.0]}""",
        All32::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint32":2.147483648E9, "repUint32": [2.147483648E9]}""",
        All32::class.java,
      ),
    ).isEqualTo(unsigned)
    assertThat(
      jsonLibrary.fromJson(
        """{"myUint32":-2.147483648E9, "repUint32": [-2.147483648E9]}""",
        All32::class.java,
      ),
    ).isEqualTo(unsigned)

    val unsignedJson = jsonLibrary.toJson(unsigned, All32::class.java)
    assertThat(unsignedJson).contains(""""myUint32":2147483648""")
    assertThat(unsignedJson).contains(""""repUint32":[2147483648]""")
  }

  @Test fun serializeAllTypesProto3() {
    val json = jsonLibrary.toJson(allTypesProto3Builder().build(), AllTypesProto3::class.java)
    assertJsonEquals(ALL_TYPES_PROTO3_JSON, json)
  }

  @Test fun deserializeAllTypesProto3() {
    val allTypes = allTypesProto3Builder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_PROTO3_JSON, AllTypesProto3::class.java)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(parsed.toString()).isEqualTo(allTypes.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllTypesProto3::class.java),
      jsonLibrary.toJson(allTypes, AllTypesProto3::class.java),
    )
  }

  @Test fun serializeIdentityAllTypes() {
    val json = if (jsonLibrary.writeIdentityValues) {
      ALL_TYPES_IDENTITY_WRITTEN_PROTO3_JSON
    } else {
      ALL_TYPES_IDENTITY_PROTO3_JSON
    }
    val allTypes = AllTypesProto3.Builder().build()

    assertJsonEquals(json, jsonLibrary.toJson(allTypes, AllTypesProto3::class.java))
  }

  @Test fun deserializeIdentityAllTypes() {
    val allTypes = AllTypesProto3.Builder().build()
    val parsed = jsonLibrary.fromJson(ALL_TYPES_IDENTITY_PROTO3_JSON, AllTypesProto3::class.java)
    assertThat(parsed).isEqualTo(allTypes)
    assertThat(parsed.toString()).isEqualTo(allTypes.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllTypesProto3::class.java),
      jsonLibrary.toJson(allTypes, AllTypesProto3::class.java),
    )
  }

  @Test fun serializeExplicitIdentityAllTypes() {
    if (jsonLibrary.writeIdentityValues) return

    val value = allTypesExplicitIdentityProto3Builder().build()
    assertJsonEquals(
      ALL_TYPES_EXPLICIT_IDENTITY_PROTO3_JSON,
      jsonLibrary.toJson(value, AllTypesProto3::class.java),
    )
  }

  @Test fun deserializeExplicitIdentityAllTypes() {
    val value = allTypesExplicitIdentityProto3Builder().build()

    val parsed = jsonLibrary.fromJson(
      ALL_TYPES_EXPLICIT_IDENTITY_PROTO3_JSON,
      AllTypesProto3::class.java,
    )
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllTypesProto3::class.java),
      jsonLibrary.toJson(value, AllTypesProto3::class.java),
    )
  }

  @Test fun minusDoubleZero() {
    val value = AllTypesProto3.Builder().my_double(-0.0).build()
    val json = "\"myDouble\":-0.0"

    // -0.0 isn't the identity value for doubles so we print it.
    assertThat(jsonLibrary.toJson(value, AllTypesProto3::class.java)).contains(json)

    val parsed = jsonLibrary.fromJson("{$json}", AllTypesProto3::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
  }

  @Test fun minusFloatZero() {
    val value = AllTypesProto3.Builder().my_float(-0f).build()
    val json = "\"myFloat\":-0.0"

    // -0f isn't the identity value for floats so we print it.
    assertThat(jsonLibrary.toJson(value, AllTypesProto3::class.java)).contains(json)

    val parsed = jsonLibrary.fromJson("{$json}", AllTypesProto3::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
  }

  @Test fun wrappers() {
    val value = allWrappersBuilder().build()

    assertJsonEquals(ALL_WRAPPERS_JSON, jsonLibrary.toJson(value, AllWrappers::class.java))

    val parsed = jsonLibrary.fromJson(ALL_WRAPPERS_JSON, AllWrappers::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, AllWrappers::class.java),
      jsonLibrary.toJson(value, AllWrappers::class.java),
    )
  }

  @Test fun mapTypes() {
    val value = MapTypes.Builder()
      .map_string_string(mapOf("a" to "A", "b" to "B"))
      .map_int32_int32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .map_sint32_sint32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .map_sfixed32_sfixed32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .map_fixed32_fixed32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .map_uint32_uint32(
        mapOf(
          Int.MIN_VALUE to Int.MIN_VALUE + 1,
          Int.MAX_VALUE to Int.MAX_VALUE - 1,
        ),
      )
      .map_int64_int64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .map_sfixed64_sfixed64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .map_sint64_sint64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .map_fixed64_fixed64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .map_uint64_uint64(
        mapOf(
          Long.MIN_VALUE to Long.MIN_VALUE + 1L,
          Long.MAX_VALUE to Long.MAX_VALUE - 1L,
        ),
      )
      .build()

    assertJsonEquals(MAP_TYPES_JSON, jsonLibrary.toJson(value, MapTypes::class.java))

    val parsed = jsonLibrary.fromJson(MAP_TYPES_JSON, MapTypes::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, MapTypes::class.java),
      jsonLibrary.toJson(value, MapTypes::class.java),
    )
  }

  @Test fun all32MaxValue() {
    val value = All32.Builder()
      .my_int32(Int.MAX_VALUE)
      .my_uint32(Int.MAX_VALUE)
      .my_sint32(Int.MAX_VALUE)
      .my_fixed32(Int.MAX_VALUE)
      .my_sfixed32(Int.MAX_VALUE)
      .rep_int32(list(-1))
      .rep_uint32(list(-1))
      .rep_sint32(list(-1))
      .rep_fixed32(list(-1))
      .rep_sfixed32(list(-1))
      .pack_int32(list(Int.MAX_VALUE))
      .pack_uint32(list(Int.MAX_VALUE))
      .pack_sint32(list(Int.MAX_VALUE))
      .pack_fixed32(list(Int.MAX_VALUE))
      .pack_sfixed32(list(Int.MAX_VALUE))
      .oneof_int32(Int.MAX_VALUE)
      .map_int32_int32(mapOf(Int.MAX_VALUE to Int.MAX_VALUE - 1))
      .map_int32_uint32(mapOf(Int.MAX_VALUE to -1))
      .map_int32_sint32(mapOf(Int.MAX_VALUE to Int.MAX_VALUE - 1))
      .map_int32_fixed32(mapOf(Int.MAX_VALUE to -1))
      .map_int32_sfixed32(mapOf(Int.MAX_VALUE to Int.MAX_VALUE - 1))
      .build()

    assertJsonEquals(ALL_32_JSON_MAX_VALUE, jsonLibrary.toJson(value, All32::class.java))

    val parsed = jsonLibrary.fromJson(ALL_32_JSON_MAX_VALUE, All32::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, All32::class.java),
      jsonLibrary.toJson(value, All32::class.java),
    )
  }

  @Test fun all32MinValue() {
    val value = All32.Builder()
      .my_int32(Int.MIN_VALUE)
      .my_uint32(Int.MIN_VALUE)
      .my_sint32(Int.MIN_VALUE)
      .my_fixed32(Int.MIN_VALUE)
      .my_sfixed32(Int.MIN_VALUE)
      .rep_int32(list(0))
      .rep_uint32(list(0))
      .rep_sint32(list(0))
      .rep_fixed32(list(0))
      .rep_sfixed32(list(0))
      .pack_int32(list(Int.MIN_VALUE))
      .pack_uint32(list(Int.MIN_VALUE))
      .pack_sint32(list(Int.MIN_VALUE))
      .pack_fixed32(list(Int.MIN_VALUE))
      .pack_sfixed32(list(Int.MIN_VALUE))
      .oneof_int32(Int.MIN_VALUE)
      .map_int32_int32(mapOf(Int.MIN_VALUE to Int.MIN_VALUE + 1))
      .map_int32_uint32(mapOf(Int.MIN_VALUE to 0))
      .map_int32_sint32(mapOf(Int.MIN_VALUE to Int.MIN_VALUE + 1))
      .map_int32_fixed32(mapOf(Int.MIN_VALUE to 0))
      .map_int32_sfixed32(mapOf(Int.MIN_VALUE to Int.MIN_VALUE + 1))
      .build()

    assertJsonEquals(ALL_32_JSON_MIN_VALUE, jsonLibrary.toJson(value, All32::class.java))

    val parsed = jsonLibrary.fromJson(ALL_32_JSON_MIN_VALUE, All32::class.java)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
      jsonLibrary.toJson(parsed, All32::class.java),
      jsonLibrary.toJson(value, All32::class.java),
    )
  }

  companion object {
    // Return a two-element list with a given repeated value.
    private fun <T> list(x: T): List<T> = listOf(x, x)

    private fun allTypesProto2IdentityBuilder(): AllTypesProto2.Builder {
      return AllTypesProto2.Builder()
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
        .opt_nested_enum(AllTypesProto2.NestedEnum.A)
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
        .req_nested_enum(AllTypesProto2.NestedEnum.A)
        .req_nested_message(AllTypesProto2.NestedMessage.Builder().a(0).build())
    }

    private fun allTypesProto2Builder(): AllTypesProto2.Builder {
      val bytes = ByteString.of(123.toByte(), 125.toByte())
      val nestedMessage = AllTypesProto2.NestedMessage.Builder().a(999).build()
      return AllTypesProto2.Builder()
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
        .opt_nested_enum(AllTypesProto2.NestedEnum.A)
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
        .req_nested_enum(AllTypesProto2.NestedEnum.A)
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
        .rep_nested_enum(list(AllTypesProto2.NestedEnum.A))
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
        .pack_nested_enum(list(AllTypesProto2.NestedEnum.A))
        .map_int32_int32(Collections.singletonMap(1, 2))
        .map_string_string(Collections.singletonMap("key", "value"))
        .map_string_message(Collections.singletonMap("message", AllTypesProto2.NestedMessage(1)))
        .map_string_enum(Collections.singletonMap("enum", AllTypesProto2.NestedEnum.A))
        .oneof_int32(4444)
        .ext_opt_int32(Int.MAX_VALUE)
        .ext_opt_int64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_uint64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_sint64(Long.MIN_VALUE / 2 + 178)
        .ext_opt_bool(true)
        .ext_opt_float(1.2345e6f)
        .ext_opt_double(1.2345e67)
        .ext_opt_nested_enum(AllTypesProto2.NestedEnum.A)
        .ext_opt_nested_message(nestedMessage)
        .ext_rep_int32(list(Int.MAX_VALUE))
        .ext_rep_uint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_rep_sint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_rep_bool(list(true))
        .ext_rep_float(list(1.2345e6f))
        .ext_rep_double(list(1.2345e67))
        .ext_rep_nested_enum(list(AllTypesProto2.NestedEnum.A))
        .ext_rep_nested_message(list(nestedMessage))
        .ext_pack_int32(list(Int.MAX_VALUE))
        .ext_pack_uint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_pack_sint64(list(Long.MIN_VALUE / 2 + 178))
        .ext_pack_bool(list(true))
        .ext_pack_float(list(1.2345e6f))
        .ext_pack_double(list(1.2345e67))
        .ext_pack_nested_enum(list(AllTypesProto2.NestedEnum.A))
    }

    private fun allTypesProto3Builder(): AllTypesProto3.Builder {
      return AllTypesProto3.Builder()
        .my_int32(111)
        .my_uint32(112)
        .my_sint32(113)
        .my_fixed32(114)
        .my_sfixed32(115)
        .my_int64(116L)
        .my_uint64(117L)
        .my_sint64(118L)
        .my_fixed64(119L)
        .my_sfixed64(120L)
        .my_bool(true)
        .my_float(122.0F)
        .my_double(123.0)
        .my_string("124")
        .my_bytes(ByteString.of(123, 125))
        .nested_enum(AllTypesProto3.NestedEnum.A)
        .nested_message(AllTypesProto3.NestedMessage(999))
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
        .rep_nested_enum(list(AllTypesProto3.NestedEnum.A))
        .rep_nested_message(list(AllTypesProto3.NestedMessage(999)))
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
        .pack_nested_enum(list(AllTypesProto3.NestedEnum.A))
        .map_int32_int32(mapOf(1 to 2))
        .map_string_string(mapOf("key" to "value"))
        .map_string_message(mapOf("message" to AllTypesProto3.NestedMessage(1)))
        .map_string_enum(mapOf("enum" to AllTypesProto3.NestedEnum.A))
        .oneof_int32(0)
    }

    private fun allTypesExplicitIdentityProto3Builder(): AllTypesProto3.Builder {
      return AllTypesProto3.Builder()
        .my_int32(0)
        .my_uint32(0)
        .my_sint32(0)
        .my_fixed32(0)
        .my_sfixed32(0)
        .my_int64(0L)
        .my_uint64(0L)
        .my_sint64(0L)
        .my_fixed64(0L)
        .my_sfixed64(0L)
        .my_bool(false)
        .my_float(0F)
        .my_double(0.0)
        .my_string("")
        .my_bytes(ByteString.EMPTY)
        .nested_enum(AllTypesProto3.NestedEnum.UNKNOWN)
        .nested_message(AllTypesProto3.NestedMessage(0))
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
        .pack_nested_enum(list(AllTypesProto3.NestedEnum.UNKNOWN))
        .map_int32_int32(mapOf(0 to 0))
        .map_string_message(mapOf("" to AllTypesProto3.NestedMessage.Builder().build()))
        .map_string_enum(mapOf("" to AllTypesProto3.NestedEnum.UNKNOWN))
        .oneof_int32(0)
    }

    private fun allWrappersBuilder(): AllWrappers.Builder {
      return AllWrappers.Builder()
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
    }

    private val ALL_TYPES_PROTO2_JSON = loadJson("all_types_proto2.json")

    private val ALL_TYPES_IDENTITY_PROTO2_JSON = loadJson("all_types_identity_proto2.json")

    private val ALL_TYPES_PROTO3_JSON = loadJson("all_types_proto3.json")

    private val ALL_TYPES_IDENTITY_PROTO3_JSON = loadJson("all_types_identity_proto3.json")

    private val ALL_TYPES_EXPLICIT_IDENTITY_PROTO3_JSON =
      loadJson("all_types_explicit_identity_proto3.json")

    private val ALL_TYPES_IDENTITY_WRITTEN_PROTO3_JSON =
      loadJson("all_types_identity_written_proto3.json")

    private val CAMEL_CASE_JSON = loadJson("camel_case_proto3.json")

    private val ALL_STRUCT_JSON = loadJson("all_struct_proto3.json")

    private val ALL_STRUCT_IDENTITY_JSON = loadJson("all_struct_identity_proto3.json")

    private val ALL_STRUCT_IDENTITY_WRITTEN_JSON = loadJson("all_struct_identity_written_proto3.json")

    private val PIZZA_DELIVERY_JSON = loadJson("pizza_delivery_proto3.json")

    private val PIZZA_DELIVERY_UNKNOWN_TYPE_JSON =
      loadJson("pizza_delivery_unknown_type_proto3.json")

    private val PIZZA_DELIVERY_WITHOUT_TYPE_JSON =
      loadJson("pizza_delivery_without_type_proto3.json")

    private val PIZZA_DELIVERY_LITERAL_NULLS_JSON =
      loadJson("pizza_delivery_literal_nulls_proto3.json")

    private val ALL_64_JSON_MIN_VALUE = loadJson("all_64_min_proto3.json")

    private val ALL_64_JSON_MAX_VALUE = loadJson("all_64_max_proto3.json")

    private val ALL_32_JSON_MIN_VALUE = loadJson("all_32_min_proto3.json")

    private val ALL_32_JSON_MAX_VALUE = loadJson("all_32_max_proto3.json")

    private val ALL_WRAPPERS_JSON = loadJson("all_wrappers_proto3.json")

    private val MAP_TYPES_JSON = loadJson("map_types_proto3.json")

    private val moshi = object : JsonLibrary {
      override fun toString() = "Moshi"

      override val writeIdentityValues = false

      private val moshi = Moshi.Builder()
        .add(
          WireJsonAdapterFactory(writeIdentityValues = writeIdentityValues)
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER)),
        )
        .build()

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return moshi.adapter(type).fromJson(json)!!
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return moshi.adapter(type).toJson(value)
      }
    }

    private val gson = object : JsonLibrary {
      override fun toString() = "Gson"

      override val writeIdentityValues = false

      private val gson = GsonBuilder()
        .registerTypeAdapterFactory(
          WireTypeAdapterFactory(writeIdentityValues = writeIdentityValues)
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER)),
        )
        .disableHtmlEscaping()
        .create()

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return gson.fromJson(json, type)
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return gson.toJson(value, type)
      }
    }

    private val writeIdentitiesMoshi = object : JsonLibrary {
      override fun toString() = "WriteIdentitiesMoshi"

      override val writeIdentityValues = true

      private val moshi = Moshi.Builder()
        .add(
          WireJsonAdapterFactory(writeIdentityValues = writeIdentityValues)
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER)),
        )
        .build()

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return moshi.adapter(type).fromJson(json)!!
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return moshi.adapter(type).toJson(value)
      }
    }

    private val writeIdentitiesGson = object : JsonLibrary {
      override fun toString() = "writeIdentitiesGson"

      override val writeIdentityValues = true

      private val gson = GsonBuilder()
        .registerTypeAdapterFactory(
          WireTypeAdapterFactory(writeIdentityValues = writeIdentityValues)
            .plus(listOf(BuyOneGetOnePromotion.ADAPTER)),
        )
        .disableHtmlEscaping()
        .create()

      override fun <T> fromJson(json: String, type: Class<T>): T {
        return gson.fromJson(json, type)
      }

      override fun <T> toJson(value: T, type: Class<T>): String {
        return gson.toJson(value, type)
      }
    }

    @Parameters(name = "{0}")
    @JvmStatic
    internal fun parameters() = listOf(
      arrayOf(gson),
      arrayOf(moshi),
      arrayOf(writeIdentitiesMoshi),
      arrayOf(writeIdentitiesGson),
    )

    private fun loadJson(fileName: String): String {
      return File("src/commonTest/shared/json", fileName).source().use { it.buffer().readUtf8() }
    }
  }
}

internal interface JsonLibrary {
  val writeIdentityValues: Boolean
  fun <T> fromJson(json: String, type: Class<T>): T
  fun <T> toJson(value: T, type: Class<T>): String
}
