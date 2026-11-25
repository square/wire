/*
 * Copyright (C) 2026 Square, Inc.
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
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.google.gson.GsonBuilder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Reads JSON that sets two members of one oneof with Moshi and Gson, for each style of generated
 * code that Wire's reflective adapters support.
 *
 * Wire 7 rejected two members for generated Java and for generated Kotlin with flat oneofs, like
 * protobuf-java does. Wire 8 rejects them for boxed and sealed oneofs too. Binary decoding keeps
 * the last member for every style.
 */
@RunWith(Parameterized::class)
class OneOfJsonTest(
  private val style: Style,
  private val library: Library,
) {
  enum class Library {
    MOSHI,
    GSON,
  }

  /** How a style of generated code reacts to JSON that sets two members of one oneof. */
  enum class Rejection {
    /** Generated Java throws an [IllegalArgumentException] from `build()`. */
    JAVA,

    /** Generated Kotlin throws an [IllegalArgumentException] from a constructor or `build()`. */
    KOTLIN,

    /** A reflective constructor call wraps the [IllegalArgumentException] of generated Kotlin. */
    KOTLIN_REFLECTIVE,

    /** The binding throws an [IllegalArgumentException] with a message that names the two members. */
    BINDING,
  }

  enum class Style(val packageName: String, val rejection: Rejection) {
    JAVA("java", Rejection.JAVA),
    KOTLIN("kotlin", Rejection.KOTLIN_REFLECTIVE),
    KOTLIN_INTEROP("kotlin_interop", Rejection.KOTLIN),
    KOTLIN_BUILDERS("kotlin_builders", Rejection.KOTLIN),
    KOTLIN_INTEROP_BUILDERS("kotlin_interop_builders", Rejection.KOTLIN),
    BOXED("boxed", Rejection.BINDING),
    BOXED_PLAIN("boxed_plain", Rejection.BINDING),
    BOXED_MIN_SIZE("boxed_min_size", Rejection.BINDING),
    BOXED_BUILDERS("boxed_builders", Rejection.BINDING),
    SEALED_PLAIN("sealed_plain", Rejection.BINDING),
    SEALED("sealed", Rejection.BINDING),
    SEALED_BUILDERS("sealed_builders", Rejection.BINDING),
  }

  private val type: Class<*> = Class.forName("squareup.wire.json.oneof.${style.packageName}.Choice")

  @Test fun twoMembersInEitherOrder() {
    for ((json, first, second) in listOf(
      Triple("""{"text":"a","number":1}""", "text", "number"),
      Triple("""{"number":1,"text":"a"}""", "number", "text"),
      Triple("""{"text":"a","nested":{"name":"n"}}""", "text", "nested"),
      Triple("""{"nested":{"name":"n"},"number":2}""", "nested", "number"),
      Triple("""{"text":"a","other":"o","number":1}""", "text", "number"),
      Triple("""{"text":"a","number":null,"nested":{"name":"n"}}""", "text", "nested"),
    )) {
      assertRejected(first, second) { fromJson(json) }
    }
  }

  @Test fun nullNextToValueKeepsTheValue() {
    for ((json, expected) in listOf(
      """{"text":"a","number":null}""" to """{"text":"a"}""",
      """{"number":null,"text":"a"}""" to """{"text":"a"}""",
      """{"number":1,"text":null}""" to """{"number":1}""",
      """{"text":null,"number":1}""" to """{"number":1}""",
      """{"text":"a","number":null,"nested":null}""" to """{"text":"a"}""",
      """{"text":null,"number":null}""" to """{}""",
    )) {
      assertThat(roundTrip(json)).isEqualTo(expected)
    }
  }

  @Test fun sameMemberTwiceKeepsTheLastValue() {
    assertThat(roundTrip("""{"text":"a","text":"b"}""")).isEqualTo("""{"text":"b"}""")
  }

  @Test fun everyMemberRoundTripsThroughJson() {
    for (json in listOf(
      """{"text":"a"}""",
      """{"number":1}""",
      """{"nested":{"name":"n"}}""",
      """{"other":"o"}""",
      """{}""",
    )) {
      assertThat(roundTrip(json)).isEqualTo(json)
    }
  }

  @Test fun reflectiveAdapterEncodesDecodesAndRedactsEveryMember() {
    val reflective = reflectiveAdapter()
    val generated = adapterOf(type)
    for (json in listOf(
      """{"text":"a"}""",
      """{"number":1}""",
      """{"nested":{"name":"n"}}""",
      """{}""",
    )) {
      val message = fromJson(json)
      val bytes = generated.encodeByteString(message)
      assertThat(reflective.encodeByteString(message)).isEqualTo(bytes)
      assertThat(reflective.decode(bytes)).isEqualTo(message)
      assertThat(reflective.redact(message)).isEqualTo(message)
    }
  }

  @Test fun lastMemberWinsOnBinaryDecoding() {
    val reflective = reflectiveAdapter()
    val generated = adapterOf(type)
    // Tag 1 is `text` with "a", then tag 2 is `number` with 1. Then in the other order.
    val textThenNumber = Buffer().writeByte(0x0a).writeByte(1).writeUtf8("a").writeByte(0x10).writeByte(1).readByteString()
    val numberThenText = Buffer().writeByte(0x10).writeByte(1).writeByte(0x0a).writeByte(1).writeUtf8("a").readByteString()

    // The reflective builder of Kotlin without `javaInterop` or `buildersOnly` does not clear the
    // other members of a flat oneof. It has not done so in Wire 7 either, so skip it here.
    val decoders = if (style == Style.KOTLIN) listOf(generated) else listOf(reflective, generated)
    for (decoder in decoders) {
      assertThat(roundTripJson(decoder.decode(textThenNumber))).isEqualTo("""{"number":1}""")
      assertThat(roundTripJson(decoder.decode(numberThenText))).isEqualTo("""{"text":"a"}""")
    }
  }

  private fun assertRejected(first: String, second: String, block: () -> Unit) {
    when (style.rejection) {
      Rejection.JAVA -> {
        val e = assertFailsWith<IllegalArgumentException> { block() }
        assertThat(e.message).isEqualTo("at most one of text, number, nested may be non-null")
      }
      Rejection.KOTLIN -> {
        val e = assertFailsWith<IllegalArgumentException> { block() }
        assertThat(e.message).isEqualTo("At most one of text, number, nested may be non-null")
      }
      Rejection.KOTLIN_REFLECTIVE -> {
        val e = assertFailsWith<InvocationTargetException> { block() }
        assertThat(e.cause).isNotNull().isInstanceOf<IllegalArgumentException>()
        assertThat(e.cause!!.message).isEqualTo("At most one of text, number, nested may be non-null")
      }
      Rejection.BINDING -> {
        val e = assertFailsWith<IllegalArgumentException> { block() }
        assertThat(e.message).isEqualTo("At most one of $first, $second may be non-null in oneof choice")
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun reflectiveAdapter(): ProtoAdapter<Any> = ProtoAdapter::class.java.getMethod("newMessageAdapter", Class::class.java)
    .invoke(null, type) as ProtoAdapter<Any>

  @Suppress("UNCHECKED_CAST")
  private fun adapterOf(type: Class<*>): ProtoAdapter<Any> {
    val companion = type.getField("ADAPTER").get(null)
    return companion as ProtoAdapter<Any>
  }

  @Suppress("UNCHECKED_CAST")
  private fun fromJson(json: String): Any = when (library) {
    Library.MOSHI -> (moshi.adapter(type) as JsonAdapter<Any>).fromJson(json)!!
    Library.GSON -> gson.fromJson(json, type)
  }

  @Suppress("UNCHECKED_CAST")
  private fun toJson(value: Any): String = when (library) {
    Library.MOSHI -> (moshi.adapter(type) as JsonAdapter<Any>).toJson(value)
    Library.GSON -> gson.toJson(value, type)
  }

  private fun roundTrip(json: String): String = toJson(fromJson(json))

  private fun roundTripJson(value: Any): String = toJson(value)

  companion object {
    private val moshi = Moshi.Builder().add(WireJsonAdapterFactory()).build()
    private val gson = GsonBuilder().registerTypeAdapterFactory(WireTypeAdapterFactory()).create()

    @Parameters(name = "{0} {1}")
    @JvmStatic
    fun parameters(): List<Array<Any>> = Style.entries.flatMap { style ->
      Library.entries.map { library -> arrayOf<Any>(style, library) }
    }
  }
}
