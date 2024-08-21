/*
 * Copyright (C) 2015 Square, Inc.
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

import kotlin.reflect.KClass
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString

actual abstract class ProtoAdapter<E> actual constructor(
  internal actual val fieldEncoding: FieldEncoding,
  actual val type: KClass<*>?,
  actual val typeUrl: String?,
  actual val syntax: Syntax,
  actual val identity: E?,
  actual val sourceFile: String?,
) {
  internal actual val packedAdapter: ProtoAdapter<List<E>>? = when {
    this is PackedProtoAdapter<*> || this is RepeatedProtoAdapter<*> -> null
    fieldEncoding == FieldEncoding.LENGTH_DELIMITED -> null
    else -> commonCreatePacked()
  }
  internal actual val repeatedAdapter: ProtoAdapter<List<E>>? = when {
    this is RepeatedProtoAdapter<*> || this is PackedProtoAdapter<*> -> null
    else -> commonCreateRepeated()
  }

  /** Returns the redacted form of `value`. */
  actual abstract fun redact(value: E): E

  /**
   * The size of the non-null data `value`. This does not include the size required for a
   * length-delimited prefix (should the type require one).
   */
  actual abstract fun encodedSize(value: E): Int

  /**
   * The size of `tag` and `value` in the wire format. This size includes the tag, type,
   * length-delimited prefix (should the type require one), and value. Returns 0 if `value` is
   * null.
   */
  actual open fun encodedSizeWithTag(tag: Int, value: E?): Int {
    return commonEncodedSizeWithTag(tag, value)
  }

  /** Write non-null `value` to `writer`. */
  actual abstract fun encode(writer: ProtoWriter, value: E)

  /** Write non-null `value` to `writer`. */
  actual open fun encode(writer: ReverseProtoWriter, value: E) {
    delegateEncode(writer, value)
  }

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  actual open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
    commonEncodeWithTag(writer, tag, value)
  }

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  actual open fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: E?) {
    commonEncodeWithTag(writer, tag, value)
  }

  /** Encode `value` and write it to `stream`. */
  actual fun encode(sink: BufferedSink, value: E) {
    commonEncode(sink, value)
  }

  /** Encode `value` as a `byte[]`. */
  actual fun encode(value: E): ByteArray {
    return commonEncode(value)
  }

  /** Encode `value` as a [ByteString]. */
  actual fun encodeByteString(value: E): ByteString {
    return commonEncodeByteString(value)
  }

  /** Read a non-null value from `reader`. */
  actual abstract fun decode(reader: ProtoReader): E

  /** Read a non-null value from `reader`. */
  actual open fun decode(reader: ProtoReader32): E {
    return decode(reader.asProtoReader())
  }

  /** Read an encoded message from `bytes`. */
  actual fun decode(bytes: ByteArray): E {
    return commonDecode(bytes)
  }

  /** Read an encoded message from `bytes`. */
  actual fun decode(bytes: ByteString): E {
    return commonDecode(bytes)
  }

  /** Read an encoded message from `source`. */
  actual fun decode(source: BufferedSource): E {
    return commonDecode(source)
  }

  actual fun tryDecode(reader: ProtoReader, destination: MutableList<E>) {
    return commonTryDecode(reader, destination)
  }

  actual fun tryDecode(reader: ProtoReader32, destination: MutableList<E>) {
    return commonTryDecode(reader, destination)
  }

  /** Returns a human-readable version of the given `value`. */
  actual open fun toString(value: E): String {
    return commonToString(value)
  }

  internal actual fun withLabel(label: WireField.Label): ProtoAdapter<*> {
    return commonWithLabel(label)
  }

  /** Returns an adapter for `E` but as a packed, repeated value. */
  actual fun asPacked(): ProtoAdapter<List<E>> {
    require(fieldEncoding != FieldEncoding.LENGTH_DELIMITED) {
      "Unable to pack a length-delimited type."
    }
    return packedAdapter ?: throw UnsupportedOperationException(
      "Can't create a packed adapter from a packed or repeated adapter.",
    )
  }

  /**
   * Returns an adapter for `E` but as a repeated value.
   *
   * Note: Repeated items are not required to be encoded sequentially. Thus, when decoding using
   * the returned adapter, only single-element lists will be returned and it is the caller's
   * responsibility to merge them into the final list.
   */
  actual fun asRepeated(): ProtoAdapter<List<E>> {
    return repeatedAdapter ?: throw UnsupportedOperationException(
      "Can't create a repeated adapter from a repeated or packed adapter.",
    )
  }

  actual class EnumConstantNotFoundException actual constructor(
    actual val value: Int,
    type: KClass<*>?,
  ) : IllegalArgumentException("Unknown enum tag $value for ${type?.simpleName}")

  actual companion object {
    /**
     * Creates a new proto adapter for a map using `keyAdapter` and `valueAdapter`.
     *
     * Note: Map entries are not required to be encoded sequentially. Thus, when decoding using
     * the returned adapter, only single-element maps will be returned and it is the caller's
     * responsibility to merge them into the final map.
     */
    actual fun <K, V> newMapAdapter(
      keyAdapter: ProtoAdapter<K>,
      valueAdapter: ProtoAdapter<V>,
    ): ProtoAdapter<Map<K, V>> {
      return commonNewMapAdapter(keyAdapter, valueAdapter)
    }

    actual val BOOL: ProtoAdapter<Boolean> = commonBool()

    actual val INT32: ProtoAdapter<Int> = commonInt32()
    actual val INT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(INT32)
    actual val UINT32: ProtoAdapter<Int> = commonUint32()
    actual val UINT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(UINT32)
    actual val SINT32: ProtoAdapter<Int> = commonSint32()
    actual val SINT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(SINT32)
    actual val FIXED32: ProtoAdapter<Int> = commonFixed32()
    actual val FIXED32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(FIXED32)
    actual val SFIXED32: ProtoAdapter<Int> = commonSfixed32()
    actual val SFIXED32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(SFIXED32)
    actual val INT64: ProtoAdapter<Long> = commonInt64()
    actual val INT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(INT64)

    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    actual val UINT64: ProtoAdapter<Long> = commonUint64()
    actual val UINT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(UINT64)
    actual val SINT64: ProtoAdapter<Long> = commonSint64()
    actual val SINT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(SINT64)
    actual val FIXED64: ProtoAdapter<Long> = commonFixed64()
    actual val FIXED64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(FIXED64)
    actual val SFIXED64: ProtoAdapter<Long> = commonSfixed64()
    actual val SFIXED64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(SFIXED64)
    actual val FLOAT: ProtoAdapter<Float> = commonFloat()
    actual val FLOAT_ARRAY: ProtoAdapter<FloatArray> = FloatArrayProtoAdapter(FLOAT)
    actual val DOUBLE: ProtoAdapter<Double> = commonDouble()
    actual val DOUBLE_ARRAY: ProtoAdapter<DoubleArray> = DoubleArrayProtoAdapter(DOUBLE)
    actual val BYTES: ProtoAdapter<ByteString> = commonBytes()
    actual val STRING: ProtoAdapter<String> = commonString()
    actual val DURATION: ProtoAdapter<Duration> = commonDuration()
    actual val INSTANT: ProtoAdapter<Instant> = commonInstant()
    actual val EMPTY: ProtoAdapter<Unit> = commonEmpty()
    actual val STRUCT_MAP: ProtoAdapter<Map<String, *>?> = commonStructMap()
    actual val STRUCT_LIST: ProtoAdapter<List<*>?> = commonStructList()
    actual val STRUCT_NULL: ProtoAdapter<Nothing?> = commonStructNull()
    actual val STRUCT_VALUE: ProtoAdapter<Any?> = commonStructValue()
    actual val DOUBLE_VALUE: ProtoAdapter<Double?> = commonWrapper(DOUBLE, "type.googleapis.com/google.protobuf.DoubleValue")
    actual val FLOAT_VALUE: ProtoAdapter<Float?> = commonWrapper(FLOAT, "type.googleapis.com/google.protobuf.FloatValue")
    actual val INT64_VALUE: ProtoAdapter<Long?> = commonWrapper(INT64, "type.googleapis.com/google.protobuf.Int64Value")
    actual val UINT64_VALUE: ProtoAdapter<Long?> = commonWrapper(UINT64, "type.googleapis.com/google.protobuf.UInt64Value")
    actual val INT32_VALUE: ProtoAdapter<Int?> = commonWrapper(INT32, "type.googleapis.com/google.protobuf.Int32Value")
    actual val UINT32_VALUE: ProtoAdapter<Int?> = commonWrapper(UINT32, "type.googleapis.com/google.protobuf.UInt32Value")
    actual val BOOL_VALUE: ProtoAdapter<Boolean?> = commonWrapper(BOOL, "type.googleapis.com/google.protobuf.BoolValue")
    actual val STRING_VALUE: ProtoAdapter<String?> = commonWrapper(STRING, "type.googleapis.com/google.protobuf.StringValue")
    actual val BYTES_VALUE: ProtoAdapter<ByteString?> = commonWrapper(BYTES, "type.googleapis.com/google.protobuf.BytesValue")
  }
}
