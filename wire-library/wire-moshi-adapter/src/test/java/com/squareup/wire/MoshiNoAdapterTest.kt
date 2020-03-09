package com.squareup.wire

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.wire.json.assertJsonEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import com.squareup.wire.protos.dinosaurs.java.Dinosaur as DinosaurJava
import com.squareup.wire.protos.dinosaurs.javainteropkotlin.Dinosaur as DinosaurInterop
import com.squareup.wire.protos.dinosaurs.kotlin.Dinosaur as DinosaurKotlin
import com.squareup.wire.protos.geology.java.Period as PeriodJava
import com.squareup.wire.protos.geology.javainteropkotlin.Period as PeriodInterop
import com.squareup.wire.protos.geology.kotlin.Period as PeriodKotlin

class MoshiNoAdapterTest {
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  /** This test passes with Wire 2.2. */
  @Test fun kotlinFullObject() {
    val value = DinosaurKotlin(
        name = "Stegosaurus",
        period = PeriodKotlin.JURASSIC,
        length_meters = 9.0,
        mass_kilograms = 5_000.0,
        picture_urls = listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67")
    )
    val json = """{
      |"length_meters":9.0,
      |"mass_kilograms":5000.0,
      |"name":"Stegosaurus",
      |"period":"JURASSIC",
      |"picture_urls":["http://goo.gl/LD5KY5","http://goo.gl/VYRM67"]
      |}""".trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurKotlin::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
  @Test fun kotlinListsOnly() {
    val value = DinosaurKotlin()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurKotlin::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
  @Test fun kotlinEmptyObject() {
    val value = DinosaurKotlin()
    val json = "{}"
    assertJsonEquals(moshi.adapter(DinosaurKotlin::class.java).toJson(value),
        "{\"picture_urls\":[]}")
    val decoded = moshi.adapter(DinosaurKotlin::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
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
      |}""".trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurJava::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
  @Test fun javaListsOnly() {
    val value = DinosaurJava.Builder().build()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurJava::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test fails with Wire 2.2 because absent lists are initialized to null. */
  @Ignore
  @Test fun javaEmptyObject() {
    val value = DinosaurJava.Builder().build()
    val json = "{}"
    assertJsonEquals(moshi.adapter(DinosaurJava::class.java).toJson(value),
        "{\"picture_urls\":[]}")
    val decoded = moshi.adapter(DinosaurJava::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
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
      |}""".trimMargin().replace("\n", "")
    assertJsonEquals(moshi.adapter(DinosaurInterop::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
  @Test fun interopListsOnly() {
    val value = DinosaurInterop.Builder().build()
    val json = """{"picture_urls":[]}"""
    assertJsonEquals(moshi.adapter(DinosaurInterop::class.java).toJson(value), json)
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }

  /** This test passes with Wire 2.2. */
  @Test fun interopEmptyObject() {
    val value = DinosaurInterop.Builder().build()
    val json = "{}"
    assertJsonEquals(moshi.adapter(DinosaurInterop::class.java).toJson(value),
        "{\"picture_urls\":[]}")
    val decoded = moshi.adapter(DinosaurInterop::class.java).fromJson(json)
    assertThat(decoded).isEqualTo(value)
    assertThat(decoded.hashCode()).isEqualTo(value.hashCode())
    assertThat(decoded.toString()).isEqualTo(value.toString())
  }
}
