/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.EnumAdapter
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Read, write, and describe a tag within a message. This class knows how to assign fields to a
 * builder object, and how to extract values from a message object.
 */
class FieldBinding<M : Message<M, B>, B : Message.Builder<M, B>> internal constructor(
  wireField: WireField,
  private val messageField: Field,
  builderType: Class<B>
) {
  val label: WireField.Label = wireField.label
  val name: String = messageField.name
  val declaredName: String =
      if (wireField.declaredName.isEmpty()) messageField.name else wireField.declaredName
  val jsonName: String = if (wireField.jsonName.isEmpty()) declaredName else wireField.jsonName
  val tag: Int = wireField.tag
  private val keyAdapterString = wireField.keyAdapter
  private val adapterString = wireField.adapter
  val redacted: Boolean = wireField.redacted
  private val builderField = getBuilderField(builderType, name)
  private val builderMethod = getBuilderMethod(builderType, name, messageField.type)

  // Delegate adapters are created lazily; otherwise we could stack overflow!
  private var singleAdapter: ProtoAdapter<*>? = null
  private var keyAdapter: ProtoAdapter<*>? = null
  private var adapter: ProtoAdapter<Any>? = null

  val isMap: Boolean
    get() = keyAdapterString.isNotEmpty()

  private fun getBuilderField(builderType: Class<*>, name: String): Field {
    try {
      return builderType.getField(name)
    } catch (_: NoSuchFieldException) {
      throw AssertionError("No builder field ${builderType.name}.$name")
    }
  }

  private fun getBuilderMethod(builderType: Class<*>, name: String, type: Class<*>): Method {
    try {
      return builderType.getMethod(name, type)
    } catch (_: NoSuchMethodException) {
      throw AssertionError("No builder method ${builderType.name}.$name(${type.name})")
    }
  }

  fun singleAdapter(): ProtoAdapter<*> {
    return singleAdapter ?: ProtoAdapter.get(adapterString).also { singleAdapter = it }
  }

  fun keyAdapter(): ProtoAdapter<*> {
    return keyAdapter ?: ProtoAdapter.get(keyAdapterString).also { keyAdapter = it }
  }

  internal fun adapter(): ProtoAdapter<Any> {
    val result = adapter
    if (result != null) return result
    if (isMap) {
      val keyAdapter = keyAdapter() as ProtoAdapter<Any>
      val valueAdapter = singleAdapter() as ProtoAdapter<Any>
      return (ProtoAdapter.newMapAdapter(keyAdapter, valueAdapter) as ProtoAdapter<Any>).also {
        adapter = it
      }
    }
    return (singleAdapter().withLabel(label) as ProtoAdapter<Any>).also { adapter = it }
  }

  /** Accept a single value, independent of whether this value is single or repeated. */
  internal fun value(builder: B, value: Any) {
    when {
      label.isRepeated -> {
        when (val list = getFromBuilder(builder)) {
          is MutableList<*> -> (list as MutableList<Any>).add(value)
          is List<*> -> {
            val mutableList = list.toMutableList()
            mutableList.add(value)
            set(builder, mutableList)
          }
          else -> {
            val type = list?.let { it::class.java }
            throw ClassCastException("Expected a list type, got $type.")
          }
        }
      }
      keyAdapterString.isNotEmpty() -> {
        when (val map = getFromBuilder(builder)) {
          is MutableMap<*, *> -> map.putAll(value as Map<Nothing, Nothing>)
          is Map<*, *> -> {
            val mutableMap = map.toMutableMap()
            mutableMap.putAll(value as Map<out Any?, Any?>)
            set(builder, mutableMap)
          }
          else -> {
            val type = map?.let { it::class.java }
            throw ClassCastException("Expected a map type, got $type.")
          }
        }
      }
      else -> set(builder, value)
    }
  }

  /** Assign a single value for required/optional fields, or a list for repeated/packed fields. */
  fun set(builder: B, value: Any?) {
    if (label.isOneOf) {
      // In order to maintain the 'oneof' invariant, call the builder setter method rather
      // than setting the builder field directly.
      builderMethod.invoke(builder, value)
    } else {
      builderField.set(builder, value)
    }
  }

  operator fun get(message: M): Any? = messageField.get(message)

  internal fun getFromBuilder(builder: B): Any? = builderField.get(builder)

  fun jsonStringAdapter(syntax: Syntax): JsonFormatter<*>? {
    val adapter = singleAdapter()

    when (adapter) {
      ProtoAdapter.BYTES,
      ProtoAdapter.BYTES_VALUE -> return ByteStringJsonFormatter
      ProtoAdapter.DURATION -> return DurationJsonFormatter
      ProtoAdapter.INSTANT -> return InstantJsonFormatter
      is EnumAdapter<*> -> return EnumJsonFormatter(adapter)
    }

    if (syntax === PROTO_2) {
      return when (adapter) {
        ProtoAdapter.UINT64, ProtoAdapter.UINT64_VALUE -> UnsignedLongAsNumberJsonFormatter
        else -> null
      }
    } else {
      return when (adapter) {
        ProtoAdapter.INT64,
        ProtoAdapter.SFIXED64,
        ProtoAdapter.SINT64,
        ProtoAdapter.INT64_VALUE -> LongAsStringJsonFormatter
        ProtoAdapter.FIXED64,
        ProtoAdapter.UINT64,
        ProtoAdapter.UINT64_VALUE -> UnsignedLongAsStringJsonFormatter
        else -> null
      }
    }
  }

  /** Transforms a scalar value to and from JSON. */
  interface JsonFormatter<W : Any> {
    /** The source of [value] may have been a string or numeric literal. */
    fun fromString(value: String): W?

    /** Returns either a String or a Number. */
    fun toStringOrNumber(value: W): Any
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

  /** Encodes a byte string as base64. */
  private object ByteStringJsonFormatter : JsonFormatter<ByteString> {
    override fun toStringOrNumber(value: ByteString) = value.base64()
    override fun fromString(value: String) = value.decodeBase64()
  }
}
