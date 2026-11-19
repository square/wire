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

  /**
   * Returns an adapter iterates a list of the target adapter. If [skipNull] is true, null evaluated
   * value will be ignored.
   */
  abstract fun listAdapter(elementAdapter: A, skipNull: Boolean): A

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

  /**
   * Returns a list of adapters that support encoding and decoding JSON objects of each specific
   * message type.
   */
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
      field.label.isRepeated -> listAdapter(
        elementAdapter = singleAdapter,
        // We ignore unrecognized constants in repeated fields for enums.
        skipNull = jsonStringAdapter is EnumJsonFormatter<*>,
      )
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
      ProtoAdapter.FIELD_MASK -> return FieldMaskJsonFormatter
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

  private fun mapKeyJsonFormatter(protoAdapter: ProtoAdapter<*>): JsonFormatter<*> = when (protoAdapter) {
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

  /**
   * Describes the map key type that [formatter] reads, for error messages. This never includes the
   * key text, because that text comes from the JSON input.
   */
  protected fun mapKeyTypeName(formatter: JsonFormatter<*>): String = when (formatter) {
    StringJsonFormatter -> "a string"
    IntAsStringJsonFormatter -> "a signed 32-bit integer"
    UnsignedIntAsStringJsonFormatter -> "an unsigned 32-bit integer"
    LongAsStringJsonFormatter -> "a signed 64-bit integer"
    UnsignedLongAsStringJsonFormatter -> "an unsigned 64-bit integer"
    else -> "a supported scalar"
  }

  /** Encodes a unsigned value without quotes, like `123`. */
  private object UnsignedLongAsNumberJsonFormatter : JsonFormatter<Long> {
    // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
    private val power64 = BigInteger("18446744073709551616")
    private val maxUnsignedLong = power64.subtract(BigInteger.ONE)

    // Wire has always decoded a negative value that fits in a signed 64-bit integer to the same bits.
    private val minSignedLong = BigInteger.valueOf(Long.MIN_VALUE)

    override fun fromString(value: String): Long {
      checkNumberSize(value)
      val bigInteger = try {
        BigInteger(value)
      } catch (e: NumberFormatException) {
        BigDecimal(value).toBigInteger() // Handle extra trailing values like 5.0.
      }
      if (bigInteger < minSignedLong || bigInteger > maxUnsignedLong) {
        throw NumberFormatException("value is out of range for a 64-bit integer")
      }
      return bigInteger.toLong() // The low 64 bits, which is the unsigned value.
    }

    override fun toStringOrNumber(value: Long): Any = when {
      value < 0L -> power64.add(BigInteger.valueOf(value))
      else -> value
    }
  }

  /** Encodes an unsigned value with quotes, like `"123"`. */
  private object UnsignedLongAsStringJsonFormatter : JsonFormatter<Long> {
    override fun toStringOrNumber(value: Long) = UnsignedLongAsNumberJsonFormatter.toStringOrNumber(value).toString()

    override fun fromString(value: String) = UnsignedLongAsNumberJsonFormatter.fromString(value)
  }

  /** Encodes an signed value with quotes, like `"-123"`. */
  private object LongAsStringJsonFormatter : JsonFormatter<Long> {
    override fun toStringOrNumber(value: Long) = value.toString()
    override fun fromString(value: String): Long {
      checkNumberSize(value)
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
    @Suppress("ktlint:standard:property-naming")
    private const val power32 = 1L shl 32

    @Suppress("ktlint:standard:property-naming")
    private const val maxInt = Int.MAX_VALUE.toLong()

    override fun fromString(value: String): Int {
      checkNumberSize(value)
      // BigDecimal accepts forms like 5.0 and 1e2, but not NaN, Infinity, or suffixes like 5f.
      val bigInteger = BigDecimal(value).toBigInteger()
      if (bigInteger.bitLength() > 63) {
        throw NumberFormatException("value is out of range for an unsigned 32-bit integer")
      }
      val longValue = bigInteger.toLong()
      checkUnsignedIntRange(longValue)
      return when {
        longValue >= maxInt -> (longValue - power32).toInt()
        else -> longValue.toInt()
      }
    }

    override fun toStringOrNumber(value: Int): Any = when {
      value < 0 -> value + power32
      else -> value
    }
  }

  /** Encodes an unsigned value with quotes, like `"123"`. */
  private object UnsignedIntAsStringJsonFormatter : JsonFormatter<Int> {
    // 2^32, used to convert sint32 values >= 2^31 to unsigned decimal form
    @Suppress("ktlint:standard:property-naming")
    private const val power32 = 1L shl 32

    override fun fromString(value: String): Int {
      val longValue = value.toLong()
      checkUnsignedIntRange(longValue)
      return longValue.toInt()
    }

    override fun toStringOrNumber(value: Int): Any = when {
      value < 0 -> (value + power32).toString()
      else -> value.toString()
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

/**
 * Accepts the unsigned 32-bit range, and also negative values that fit in a signed 32-bit
 * integer. Wire has always decoded those to the same bits.
 */
private fun checkUnsignedIntRange(value: Long) {
  if (value < Int.MIN_VALUE.toLong() || value > UInt.MAX_VALUE.toLong()) {
    throw NumberFormatException("value is out of range for an unsigned 32-bit integer")
  }
}

// Real 64-bit values need at most 20 digits. The extra room allows leading and trailing zeros.
private const val MAX_NUMBER_LENGTH = 1000
private const val MAX_NUMBER_EXPONENT = 1000

/** Throws if [value] is long, or has an exponent, that would make [BigDecimal] or [BigInteger] slow. */
private fun checkNumberSize(value: String) {
  if (value.length > MAX_NUMBER_LENGTH) throw NumberFormatException("value is too long")
  val exponentIndex = value.indexOfFirst { it == 'e' || it == 'E' }
  if (exponentIndex == -1) return
  val exponent = value.substring(exponentIndex + 1).toInt()
  if (exponent > MAX_NUMBER_EXPONENT || exponent < -MAX_NUMBER_EXPONENT) {
    throw NumberFormatException("exponent is too large")
  }
}
