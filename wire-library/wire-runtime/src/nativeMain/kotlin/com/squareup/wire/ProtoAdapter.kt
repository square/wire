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
package com.squareup.wire

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import kotlin.reflect.KClass

actual abstract class ProtoAdapter<E> actual constructor(
  internal actual val fieldEncoding: FieldEncoding,
  actual val type: KClass<*>?,
  actual val typeUrl: String?
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

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  actual open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
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
        "Can't create a packed adapter from a packed or repeated adapter.")
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
        "Can't create a repeated adapter from a repeated or packed adapter.")
  }

  actual class EnumConstantNotFoundException actual constructor(
    actual val value: Int,
    type: KClass<*>?
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
      valueAdapter: ProtoAdapter<V>
    ): ProtoAdapter<Map<K, V>> {
      return commonNewMapAdapter(keyAdapter, valueAdapter)
    }

    actual val BOOL: ProtoAdapter<Boolean> = commonBool()
    actual val INT32: ProtoAdapter<Int> = commonInt32()
    actual val UINT32: ProtoAdapter<Int> = commonUint32()
    actual val SINT32: ProtoAdapter<Int> = commonSint32()
    actual val FIXED32: ProtoAdapter<Int> = commonFixed32()
    actual val SFIXED32: ProtoAdapter<Int> = commonSfixed32()
    actual val INT64: ProtoAdapter<Long> = commonInt64()
    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    actual val UINT64: ProtoAdapter<Long> = commonUint64()
    actual val SINT64: ProtoAdapter<Long> = commonSint64()
    actual val FIXED64: ProtoAdapter<Long> = commonFixed64()
    actual val SFIXED64: ProtoAdapter<Long> = commonSfixed64()
    actual val FLOAT: ProtoAdapter<Float> = commonFloat()
    actual val DOUBLE: ProtoAdapter<Double> = commonDouble()
    actual val BYTES: ProtoAdapter<ByteString> = commonBytes()
    actual val STRING: ProtoAdapter<String> = commonString()
  }
}
