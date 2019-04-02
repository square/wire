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
import java.math.BigInteger
import java.util.ArrayList

/**
 * A [JsonAdapter.Factory] that allows Wire messages to be serialized and deserialized using
 * the Moshi Json library.
 *
 * <pre>
 * Moshi moshi = new Moshi.Builder()
 * .add(new WireJsonAdapterFactory())
 * .build();
</pre> *
 *
 * The resulting [Moshi] instance will be able to serialize and deserialize Wire [ ] types, including extensions. It ignores unknown field values.
 * The JSON encoding is intended to be compatible with the
 * [protobuf-java-format](https://code.google.com/p/protobuf-java-format/)
 * library.
 */
class WireJsonAdapterFactory : JsonAdapter.Factory {
  override fun create(
    type: Type,
    annotations: Set<Annotation>,
    moshi: Moshi
  ): JsonAdapter<*>? {
    if ((type === Long::class.java || type === Long::class.javaPrimitiveType) &&
        Types.nextAnnotations(annotations, Uint64::class.java) != null) {
      return UINT64_JSON_ADAPTER
    }
    val rawType = Types.getRawType(type)
    if (rawType == List::class.java &&
        (type as ParameterizedType).actualTypeArguments[0] === Long::class.java &&
        Types.nextAnnotations(annotations, Uint64::class.java) != null) {
      return LIST_OF_UINT64_JSON_ADAPTER
    }
    if (!annotations.isEmpty()) {
      return null
    }
    if (rawType == ByteString::class.java) {
      return BYTE_STRING_JSON_ADAPTER
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
  }
}
