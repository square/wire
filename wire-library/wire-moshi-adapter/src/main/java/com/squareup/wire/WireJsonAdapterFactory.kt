/*
 * Copyright 2018 Square Inc.
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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.math.BigInteger
import java.util.ArrayList

/**
 * A [JsonAdapter.Factory] that allows Wire messages to be serialized and deserialized using the
 * Moshi Json library.
 *
 * ```
 * Moshi moshi = new Moshi.Builder()
 *     .add(new WireJsonAdapterFactory())
 *     .build();
 * ```
 *
 * The resulting [Moshi] instance will be able to serialize and deserialize Wire [Message] types,
 * including extensions. It ignores unknown field values. The JSON encoding is intended to be
 * compatible with the [protobuf-java-format](https://code.google.com/p/protobuf-java-format/)
 * library.
 */
class WireJsonAdapterFactory private constructor(
  private val typeUrlToAdapter: Map<String, ProtoAdapter<*>>
) : JsonAdapter.Factory {
  constructor() : this(mapOf())

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapters] if they're
   * used with [AnyMessage].
   */
  fun plus(adapters: List<ProtoAdapter<*>>): WireJsonAdapterFactory {
    val newMap = typeUrlToAdapter.toMutableMap()
    for (adapter in adapters) {
      val key = adapter.typeUrl ?: throw IllegalArgumentException(
          "recompile ${adapter.type} to use it with WireJsonAdapterFactory")
      newMap[key] = adapter
    }
    return WireJsonAdapterFactory(newMap)
  }

  /**
   * Returns a new WireJsonAdapterFactory that can encode the messages for [adapter] if they're
   * used with [AnyMessage].
   */
  fun plus(adapter: ProtoAdapter<*>): WireJsonAdapterFactory {
    return plus(listOf(adapter))
  }

  override fun create(
    type: Type,
    annotations: Set<Annotation>,
    moshi: Moshi
  ): JsonAdapter<*>? {
    val rawType = Types.getRawType(type)

    val nextAnnotations = Types.nextAnnotations(annotations, OmitIdentity::class.java)
    if (nextAnnotations != null) {
      return when (type) {
        Boolean::class.javaObjectType,
        Boolean::class.javaPrimitiveType -> moshi.adapter<Boolean>(type).omitValue(false)
        ByteString::class.javaObjectType -> BYTE_STRING_JSON_ADAPTER.omitValue(ByteString.EMPTY)
        Double::class.javaObjectType,
        Double::class.javaPrimitiveType -> moshi.adapter<Double>(type).omitValue(0.0)
        Float::class.javaObjectType,
        Float::class.javaPrimitiveType -> moshi.adapter<Float>(type).omitValue(0f)
        Int::class.javaObjectType,
        Int::class.javaPrimitiveType -> moshi.adapter<Int>(type).omitValue(0)
        Long::class.javaObjectType,
        Long::class.javaPrimitiveType -> moshi.adapter<Long>(type).omitValue(0L)
        String::class.java -> moshi.adapter<String>(type).omitValue("")
        else -> moshi.adapter<Any>(type, nextAnnotations)
      }.nullSafe()
    }

    if (Types.nextAnnotations(annotations, Uint64::class.java) != null) {
      when (rawType) {
        Long::class.javaObjectType -> return UINT64_JSON_ADAPTER
        Long::class.javaPrimitiveType -> return UINT64_JSON_ADAPTER
        List::class.java -> {
          if ((type as ParameterizedType).actualTypeArguments[0] == Long::class.javaObjectType) {
            return LIST_OF_UINT64_JSON_ADAPTER
          }
        }
      }
    }

    if (annotations.isNotEmpty()) {
      return null
    }
    if (rawType == ByteString::class.java) {
      return BYTE_STRING_JSON_ADAPTER
    }
    if (rawType == AnyMessage::class.java) {
      return AnyMessageJsonAdapter(moshi, typeUrlToAdapter)
    }
    if (rawType == Duration::class.java) {
      return DurationJsonAdapter.nullSafe()
    }
    if (rawType == Any::class.java ||
        rawType == Unit::class.java ||
        type.isMapStringStar() ||
        type.isListStar()) {
      return StructJsonAdapter.serializeNulls()
    }
    return if (Message::class.java.isAssignableFrom(rawType)) {
      MessageJsonAdapter<Nothing, Nothing>(moshi, type)
    } else {
      null
    }
  }

  companion object {
    internal val BYTE_STRING_JSON_ADAPTER = object : JsonAdapter<ByteString>() {
      @Throws(IOException::class)
      override fun toJson(out: JsonWriter, byteString: ByteString?) {
        out.value(byteString?.base64())
      }

      @Throws(IOException::class)
      override fun fromJson(input: JsonReader): ByteString? {
        return input.nextString().decodeBase64()
      }
    }.nullSafe()

    /**
     * Wire uses the signed long type to store unsigned longs. Sigh. But when we encode as JSON we
     * need to emit an unsigned value.
     */
    internal val UINT64_JSON_ADAPTER = object : JsonAdapter<Long>() {
      // 2^64, used to convert sint64 values >= 2^63 to unsigned decimal form
      private val power64 = BigInteger("18446744073709551616")
      private val maxLong = BigInteger.valueOf(java.lang.Long.MAX_VALUE)

      @Throws(IOException::class)
      override fun fromJson(reader: JsonReader): Long? {
        val bigInteger = BigInteger(reader.nextString())
        return if (bigInteger.compareTo(maxLong) > 0)
          bigInteger.subtract(power64).toLong()
        else
          bigInteger.toLong()
      }

      @Throws(IOException::class)
      override fun toJson(writer: JsonWriter, value: Long?) {
        if (value!! < 0) {
          val unsigned = power64.add(BigInteger.valueOf(value))
          writer.value(unsigned)
        } else {
          writer.value(value)
        }
      }
    }.nullSafe()

    /**
     * Tragically Moshi doesn't know enough to follow a `@Uint64 List<Long>` really wants to be
     * treated as a `List<@Uint64 Long>` and so we have to do it manually.
     *
     * TODO delete when Moshi can handle that; see
     * [moshi/issues/666](https://github.com/square/moshi/issues/666)
     */
    internal val LIST_OF_UINT64_JSON_ADAPTER = object : JsonAdapter<List<Long>>() {
      @Throws(IOException::class)
      override fun fromJson(reader: JsonReader): List<Long>? {
        val result = ArrayList<Long>()
        reader.beginArray()
        while (reader.hasNext()) {
          result.add(UINT64_JSON_ADAPTER.fromJson(reader)!!)
        }
        reader.endArray()
        return result
      }

      @Throws(IOException::class)
      override fun toJson(writer: JsonWriter, value: List<Long>?) {
        writer.beginArray()
        for (v in value!!) {
          UINT64_JSON_ADAPTER.toJson(writer, v)
        }
        writer.endArray()
      }
    }.nullSafe()

    /** Returns true if [this] is a `Map<String, *>`. */
    private fun Type.isMapStringStar(): Boolean {
      if (this !is ParameterizedType) return false
      if (rawType != Map::class.java) return false

      val keyType = actualTypeArguments[0]
      val valueType = actualTypeArguments[1]
      if (keyType != String::class.java) return false

      if (valueType !is WildcardType) return false
      if (valueType.lowerBounds.isNotEmpty()) return false
      if (valueType.upperBounds != Object::class.java) return false

      return true
    }

    /** Returns true if [this] is a `List<*>`. */
    private fun Type.isListStar(): Boolean {
      if (this !is ParameterizedType) return false
      if (rawType != List::class.java) return false

      val valueType = actualTypeArguments[0]
      if (valueType !is WildcardType) return false
      if (valueType.lowerBounds.isNotEmpty()) return false
      if (valueType.upperBounds != Object::class.java) return false

      return true
    }
  }
}
