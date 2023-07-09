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
package com.squareup.wire.internal

import com.squareup.wire.EnumAdapter
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import okio.ByteString
import okio.ByteString.Companion.decodeBase64

/**
 * Integrates a JSON library like Moshi or Gson into proto. This rigid interface attempts to make it
 * easy all JSON libraries to encode and decode JSON in the exact same way.
 */
abstract class JsonIntegration<F, A> {
  /** Returns [framework]'s built-in adapter for [type]. */
  abstract fun frameworkAdapter(framework: F, type: Type): A

  /** Returns an adapter iterates a list of the target adapter. */
  abstract fun listAdapter(elementAdapter: A): A

  /** Returns an adapter iterates keys and values of the target adapter. */
  abstract fun mapAdapter(
    framework: F,
    keyFormatter: JsonFormatter<*>,
    valueAdapter: A,
  ): A

  /**
   * Returns an adapter that handles trees of Maps, Lists, and other JSON types. Should always
   * serialize nulls, including when they are values in maps.
   */
  abstract fun structAdapter(framework: F): A

  /** Returns an adapter that applies [jsonStringAdapter] to each value. */
  abstract fun formatterAdapter(jsonStringAdapter: JsonFormatter<*>): A

  /** Returns a message type that supports encoding and decoding JSON objects of type [type]. */
  fun <M : Any, B : Any> jsonAdapters(
    adapter: RuntimeMessageAdapter<M, B>,
    framework: F,
  ): List<A> {
    val fieldBindings = adapter.fields.values.toTypedArray()
    return fieldBindings.map { jsonAdapter(framework, adapter.syntax, it) }
  }

  /** Returns a JSON adapter for [field]. */
  private fun <M : Any, B : Any> jsonAdapter(
    framework: F,
    syntax: Syntax,
    field: FieldOrOneOfBinding<M, B>,
  ): A {
    if (field.singleAdapter.isStruct) {
      return structAdapter(framework)
    }

    val jsonStringAdapter = jsonFormatter(syntax, field.singleAdapter)
    val singleAdapter = when {
      jsonStringAdapter != null -> formatterAdapter(jsonStringAdapter)
      else -> frameworkAdapter(framework, field.singleAdapter.type?.javaObjectType as Type)
    }

    return when {
      field.label.isRepeated -> listAdapter(singleAdapter)
      field.isMap -> mapAdapter(
        framework = framework,
        keyFormatter = mapKeyJsonFormatter(field.keyAdapter),
        valueAdapter = singleAdapter,
      )
      else -> singleAdapter
    }
  }

  private fun jsonFormatter(syntax: Syntax, protoAdapter: ProtoAdapter<*>): JsonFormatter<*>? {
    when (protoAdapter) {
      ProtoAdapter.BYTES,
      ProtoAdapter.BYTES_VALUE,
      -> return ByteStringJsonFormatter
      ProtoAdapter.DURATION -> return DurationJsonFormatter
      ProtoAdapter.INSTANT -> return InstantJsonFormatter
      is EnumAdapter<*> -> return EnumJsonFormatter(protoAdapter)
    }

    if (syntax === Syntax.PROTO_2) {
      return when (protoAdapter) {
        ProtoAdapter.UINT64, ProtoAdapter.UINT64_VALUE -> UnsignedLongAsNumberJsonFormatter
        else -> null
      }
    } else {
      return when (protoAdapter) {
        ProtoAdapter.UINT32,
        ProtoAdapter.FIXED32,
        ProtoAdapter.UINT32_VALUE,
        -> UnsignedIntAsNumberJsonFormatter
        ProtoAdapter.INT64,
        ProtoAdapter.SFIXED64,
        ProtoAdapter.SINT64,
        ProtoAdapter.INT64_VALUE,
        -> LongAsStringJsonFormatter
        ProtoAdapter.FIXED64,
        ProtoAdapter.UINT64,
        ProtoAdapter.UINT64_VALUE,
        -> UnsignedLongAsStringJsonFormatter
        else -> null
      }
    }
  }

  private fun mapKeyJsonFormatter(protoAdapter: ProtoAdapter<*>): JsonFormatter<*> {
    return when (protoAdapter) {
      ProtoAdapter.STRING -> StringJsonFormatter
      ProtoAdapter.INT32,
      ProtoAdapter.SINT32,
      ProtoAdapter.SFIXED32,
      -> IntAsStringJsonFormatter
      ProtoAdapter.FIXED32,
      ProtoAdapter.UINT32,
      -> UnsignedIntAsStringJsonFormatter
      ProtoAdapter.INT64,
      ProtoAdapter.SFIXED64,
      ProtoAdapter.SINT64,
      -> LongAsStringJsonFormatter
      ProtoAdapter.FIXED64,
      ProtoAdapter.UINT64,
      -> UnsignedLongAsStringJsonFormatter
      else -> error("Unexpected map key type: ${protoAdapter.type}")
    }
  }

  /** Encodes a unsigned value without quotes, like `123`. */
  private object UnsignedLongAsNumberJsonFormatter : JsonFormatter<Long> {
    // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
    private val power64 = BigInteger("18446744073709551616")
    private val maxLong = BigInteger.valueOf(Long.MAX_VALUE)

    override fun fromString(value: String): Long {
      val bigInteger = try {
        BigInteger(value)
      } catch (e: Exception) {
        BigDecimal(value).toBigInteger() // Handle extra trailing values like 5.0.
      }
      return when {
        bigInteger > maxLong -> bigInteger.subtract(power64).toLong()
        else -> bigInteger.toLong()
      }
    }

    override fun toStringOrNumber(value: Long): Any {
      return when {
        value < 0L -> power64.add(BigInteger.valueOf(value))
        else -> value
      }
    }
  }

  /** Encodes an unsigned value with quotes, like `"123"`. */
  private object UnsignedLongAsStringJsonFormatter : JsonFormatter<Long> {
    override fun toStringOrNumber(value: Long) =
      UnsignedLongAsNumberJsonFormatter.toStringOrNumber(value).toString()

    override fun fromString(value: String) =
      UnsignedLongAsNumberJsonFormatter.fromString(value)
  }

  /** Encodes an signed value with quotes, like `"-123"`. */
  private object LongAsStringJsonFormatter : JsonFormatter<Long> {
    override fun toStringOrNumber(value: Long) = value.toString()
    override fun fromString(value: String): Long {
      return try {
        value.toLong()
      } catch (e: Exception) {
        BigDecimal(value).longValueExact() // Handle extra trailing values like 5.0.
      }
    }
  }

  /** Encodes a unsigned value without quotes, like `123`. */
  private object UnsignedIntAsNumberJsonFormatter : JsonFormatter<Int> {
    // 2^32, used to convert sint32 values >= 2^31 to unsigned decimal form
    private const val power32 = 1L shl 32
    private const val maxInt = Int.MAX_VALUE.toLong()

    override fun fromString(value: String): Int {
      val longValue = value.toDouble().toLong() // Handle extra trailing values like 5.0.
      return when {
        longValue >= maxInt -> (longValue - power32).toInt()
        else -> longValue.toInt()
      }
    }

    override fun toStringOrNumber(value: Int): Any {
      return when {
        value < 0 -> value + power32
        else -> value
      }
    }
  }

  /** Encodes an unsigned value with quotes, like `"123"`. */
  private object UnsignedIntAsStringJsonFormatter : JsonFormatter<Int> {
    // 2^32, used to convert sint32 values >= 2^31 to unsigned decimal form
    private const val power32 = 1L shl 32

    override fun fromString(value: String) = value.toLong().toInt()

    override fun toStringOrNumber(value: Int): Any {
      return when {
        value < 0 -> (value + power32).toString()
        else -> value.toString()
      }
    }
  }

  /** Encodes a signed value with quotes, like `"123"`. */
  private object IntAsStringJsonFormatter : JsonFormatter<Int> {
    override fun fromString(value: String) = value.toInt()
    override fun toStringOrNumber(value: Int) = value.toString()
  }

  /** Encodes a byte string as base64. */
  private object ByteStringJsonFormatter : JsonFormatter<ByteString> {
    override fun toStringOrNumber(value: ByteString) = value.base64()
    override fun fromString(value: String) = value.decodeBase64()
  }

  /** Identity encoder for strings. */
  private object StringJsonFormatter : JsonFormatter<String> {
    override fun toStringOrNumber(value: String) = value
    override fun fromString(value: String) = value
  }
}
