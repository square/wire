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
import com.google.protobuf.FieldOptions
import com.google.protobuf.util.JsonFormat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto3.requiredextension.RequiredExtension
import com.squareup.wire.proto3.requiredextension.RequiredExtensionMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
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
        |  }
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
        promotion = AnyMessage.pack(BuyOneGetOnePromotion(coupon = "MAUI"))
    )
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
    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()

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

    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()

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
    val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()

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
}
