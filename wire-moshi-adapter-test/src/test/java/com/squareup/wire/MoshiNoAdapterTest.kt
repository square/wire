/*
 * Copyright (C) 2023 Square, Inc.
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto2.dinosaurs.java.Dinosaur as DinosaurJava
import com.squareup.wire.proto2.dinosaurs.javainteropkotlin.Dinosaur as DinosaurInterop
import com.squareup.wire.proto2.dinosaurs.kotlin.Dinosaur as DinosaurKotlin
import com.squareup.wire.proto2.geology.java.Period as PeriodJava
import com.squareup.wire.proto2.geology.javainteropkotlin.Period as PeriodInterop
import com.squareup.wire.proto2.geology.kotlin.Period as PeriodKotlin
import org.junit.Ignore
import org.junit.Test

class MoshiNoAdapterTest {
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  @Test fun kotlinFullObject() {
    val value = DinosaurKotlin(
      name = "Stegosaurus",
      period = PeriodKotlin.JURASSIC,
      length_meters = 9.0,
      mass_kilograms = 5_000.0,
      picture_urls = listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"),
    )
    val json = """{
      |"length_meters":9.0,
      |"mass_kilograms":5000.0,
      |"name":"Stegosaurus",
      |"period":"JURASSIC",
      |"picture_urls":["http://goo.gl/LD5KY5","http://goo.gl/VYRM67"]
      |}
    """.trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurKotlin::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun kotlinListsOnly() {
    val value = DinosaurKotlin()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurKotlin::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun kotlinEmptyObject() {
    val value = DinosaurKotlin()
    val json = "{}"
    assertJsonEquals(
      moshi.adapter(DinosaurKotlin::class.java).toJson(value),
      "{\"picture_urls\":[]}",
    )
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun javaFullObject() {
    val value = DinosaurJava.Builder()
      .name("Stegosaurus")
      .period(PeriodJava.JURASSIC)
      .length_meters(9.0)
      .mass_kilograms(5_000.0)
      .picture_urls(listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
      .build()
    val json = """{
      |"length_meters":9.0,
      |"mass_kilograms":5000.0,
      |"name":"Stegosaurus",
      |"period":"JURASSIC",
      |"picture_urls":["http://goo.gl/LD5KY5","http://goo.gl/VYRM67"]
      |}
    """.trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurJava::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun javaListsOnly() {
    val value = DinosaurJava.Builder().build()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurJava::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Ignore("Absent lists are initialized to null in Java.")
  @Test
  fun javaEmptyObject() {
    val value = DinosaurJava.Builder().build()
    val json = "{}"
    assertJsonEquals(
      moshi.adapter(DinosaurJava::class.java).toJson(value),
      "{\"picture_urls\":[]}",
    )
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun interopFullObject() {
    val value = DinosaurInterop.Builder()
      .name("Stegosaurus")
      .period(PeriodInterop.JURASSIC)
      .length_meters(9.0)
      .mass_kilograms(5_000.0)
      .picture_urls(listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
      .build()
    val json = """{
      |"length_meters":9.0,
      |"mass_kilograms":5000.0,
      |"name":"Stegosaurus",
      |"period":"JURASSIC",
      |"picture_urls":["http://goo.gl/LD5KY5","http://goo.gl/VYRM67"]
      |}
    """.trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurInterop::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun interopListsOnly() {
    val value = DinosaurInterop.Builder().build()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurInterop::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  @Test fun interopEmptyObject() {
    val value = DinosaurInterop.Builder().build()
    val json = "{}"
    assertJsonEquals(
      moshi.adapter(DinosaurInterop::class.java).toJson(value),
      "{\"picture_urls\":[]}",
    )
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }
}
