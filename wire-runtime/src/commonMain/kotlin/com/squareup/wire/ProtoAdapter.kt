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

import com.squareup.wire.FieldEncoding.LENGTH_DELIMITED
import com.squareup.wire.FieldEncoding.VARINT
import com.squareup.wire.ProtoWriter.Companion.decodeZigZag32
import com.squareup.wire.ProtoWriter.Companion.decodeZigZag64
import com.squareup.wire.ProtoWriter.Companion.encodeZigZag32
import com.squareup.wire.ProtoWriter.Companion.encodeZigZag64
import com.squareup.wire.ProtoWriter.Companion.int32Size
import com.squareup.wire.ProtoWriter.Companion.tagSize
import com.squareup.wire.ProtoWriter.Companion.varint32Size
import com.squareup.wire.ProtoWriter.Companion.varint64Size
import com.squareup.wire.internal.JvmStatic
import kotlin.reflect.KClass
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.IOException
import okio.utf8Size

expect abstract class ProtoAdapter<E>(
  fieldEncoding: FieldEncoding,
  type: KClass<*>?,
  typeUrl: String?,
  syntax: Syntax,
  identity: E? = null,
  sourceFile: String? = null,
) {
  internal val fieldEncoding: FieldEncoding
  val type: KClass<*>?

  /**
   * Identifies this type for inclusion in a `google.protobuf.Any`. This is a string like
   * "type.googleapis.com/packagename.messagename" or null if this type is either not a message
   * (such as scalars and enums), or was code-generated before Wire 3.2 which introduced support for
   * type URLS.
   */
  val typeUrl: String?

  /**
   * Identifies the syntax in which [type] is defined in the proto schema. This string contains
   * either "proto2" or "proto3".
   */
  val syntax: Syntax

  /**
   * A special value that is used when a field is absent from an encoded proto3 message. When
   * encoding a proto3 message, fields that hold this value will be omitted.
   *
   * ```
   * | TYPE                                           | IDENTITY                      |
   * | :--------------------------------------------- | :---------------------------- |
   * | All numeric types (int32, float, double, etc.) | 0                             |
   * | Boolean                                        | false                         |
   * | String                                         | empty string: ""              |
   * | Bytes                                          | empty bytes: ByteString.EMPTY |
   * | Enums                                          | enum constant with tag 0      |
   * | Lists (repeated types)                         | empty list: listOf()          |
   * ```
   */
  val identity: E?

  /**
   * Path to the file containing the protobuf definition of this type.
   */
  val sourceFile: String?

  internal val packedAdapter: ProtoAdapter<List<E>>?
  internal val repeatedAdapter: ProtoAdapter<List<E>>?

  /** Returns the redacted form of `value`. */
  abstract fun redact(value: E): E

  /**
   * The size of the non-null data `value`. This does not include the size required for a
   * length-delimited prefix (should the type require one).
   */
  abstract fun encodedSize(value: E): Int

  /**
   * The size of `tag` and `value` in the wire format. This size includes the tag, type,
   * length-delimited prefix (should the type require one), and value. Returns 0 if `value` is
   * null.
   */
  open fun encodedSizeWithTag(tag: Int, value: E?): Int

  /** Write non-null `value` to `writer`. */
  abstract fun encode(writer: ProtoWriter, value: E)

  /** Write non-null `value` to `writer`. */
  open fun encode(writer: ReverseProtoWriter, value: E)

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?)

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  open fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: E?)

  /** Encode `value` and write it to `stream`. */
  fun encode(sink: BufferedSink, value: E)

  /** Encode `value` as a `byte[]`. */
  fun encode(value: E): ByteArray

  /** Encode `value` as a [ByteString]. */
  fun encodeByteString(value: E): ByteString

  /** Read a non-null value from `reader`. */
  abstract fun decode(reader: ProtoReader): E

  /** Read a non-null value from `reader`. */
  open fun decode(reader: ProtoReader32): E

  /** Read an encoded message from `bytes`. */
  fun decode(bytes: ByteArray): E

  /** Read an encoded message from `bytes`. */
  fun decode(bytes: ByteString): E

  /** Read an encoded message from `source`. */
  fun decode(source: BufferedSource): E

  /**
   * Reads a value and appends it to [destination] if this has data available. Otherwise, it
   * will only clear the reader state.
   */
  fun tryDecode(reader: ProtoReader, destination: MutableList<E>)

  /**
   * Reads a value and appends it to [destination] if this has data available. Otherwise, it
   * will only clear the reader state.
   */
  fun tryDecode(reader: ProtoReader32, destination: MutableList<E>)

  /** Returns a human-readable version of the given `value`. */
  open fun toString(value: E): String

  internal fun withLabel(label: WireField.Label): ProtoAdapter<*>

  /** Returns an adapter for `E` but as a packed, repeated value. */
  fun asPacked(): ProtoAdapter<List<E>>

  /**
   * Returns an adapter for `E` but as a repeated value.
   *
   * Note: Repeated items are not required to be encoded sequentially. Thus, when decoding using
   * the returned adapter, only single-element lists will be returned and it is the caller's
   * responsibility to merge them into the final list.
   */
  fun asRepeated(): ProtoAdapter<List<E>>

  class EnumConstantNotFoundException(
    value: Int,
    type: KClass<*>?,
  ) : IllegalArgumentException {
    val value: Int
  }

  companion object {
    /**
     * Creates a new proto adapter for a map using `keyAdapter` and `valueAdapter`.
     *
     * Note: Map entries are not required to be encoded sequentially. Thus, when decoding using
     * the returned adapter, only single-element maps will be returned and it is the caller's
     * responsibility to merge them into the final map.
     */
    @JvmStatic
    fun <K, V> newMapAdapter(
      keyAdapter: ProtoAdapter<K>,
      valueAdapter: ProtoAdapter<V>,
    ): ProtoAdapter<Map<K, V>>

    val BOOL: ProtoAdapter<Boolean>

    val INT32: ProtoAdapter<Int>

    val INT32_ARRAY: ProtoAdapter<IntArray>

    val UINT32: ProtoAdapter<Int>

    val UINT32_ARRAY: ProtoAdapter<IntArray>

    val SINT32: ProtoAdapter<Int>

    val SINT32_ARRAY: ProtoAdapter<IntArray>

    val FIXED32: ProtoAdapter<Int>

    val FIXED32_ARRAY: ProtoAdapter<IntArray>

    val SFIXED32: ProtoAdapter<Int>

    val SFIXED32_ARRAY: ProtoAdapter<IntArray>

    val INT64: ProtoAdapter<Long>

    val INT64_ARRAY: ProtoAdapter<LongArray>

    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    val UINT64: ProtoAdapter<Long>

    val UINT64_ARRAY: ProtoAdapter<LongArray>

    val SINT64: ProtoAdapter<Long>

    val SINT64_ARRAY: ProtoAdapter<LongArray>

    val FIXED64: ProtoAdapter<Long>

    val FIXED64_ARRAY: ProtoAdapter<LongArray>

    val SFIXED64: ProtoAdapter<Long>

    val SFIXED64_ARRAY: ProtoAdapter<LongArray>

    val FLOAT: ProtoAdapter<Float>

    val FLOAT_ARRAY: ProtoAdapter<FloatArray>

    val DOUBLE: ProtoAdapter<Double>

    val DOUBLE_ARRAY: ProtoAdapter<DoubleArray>

    val BYTES: ProtoAdapter<ByteString>

    val STRING: ProtoAdapter<String>

    val DURATION: ProtoAdapter<Duration>

    val INSTANT: ProtoAdapter<Instant>

    val EMPTY: ProtoAdapter<Unit>

    val STRUCT_MAP: ProtoAdapter<Map<String, *>?>

    val STRUCT_LIST: ProtoAdapter<List<*>?>

    val STRUCT_NULL: ProtoAdapter<Nothing?>

    val STRUCT_VALUE: ProtoAdapter<Any?>

    val DOUBLE_VALUE: ProtoAdapter<Double?>

    val FLOAT_VALUE: ProtoAdapter<Float?>

    val INT64_VALUE: ProtoAdapter<Long?>

    val UINT64_VALUE: ProtoAdapter<Long?>

    val INT32_VALUE: ProtoAdapter<Int?>

    val UINT32_VALUE: ProtoAdapter<Int?>

    val BOOL_VALUE: ProtoAdapter<Boolean?>

    val STRING_VALUE: ProtoAdapter<String?>

    val BYTES_VALUE: ProtoAdapter<ByteString?>
  }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncodedSizeWithTag(tag: Int, value: E?): Int {
  if (value == null) return 0
  var size = encodedSize(value)
  if (fieldEncoding == LENGTH_DELIMITED) {
    size += varint32Size(size)
  }
  return size + tagSize(tag)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.delegateEncode(
  writer: ReverseProtoWriter,
  value: E,
) {
  writer.writeForward { forwardWriter ->
    encode(forwardWriter, value)
  }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncodeWithTag(
  writer: ProtoWriter,
  tag: Int,
  value: E?,
) {
  if (value == null) return
  writer.writeTag(tag, fieldEncoding)
  if (fieldEncoding == LENGTH_DELIMITED) {
    writer.writeVarint32(encodedSize(value))
  }
  encode(writer, value)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncodeWithTag(
  writer: ReverseProtoWriter,
  tag: Int,
  value: E?,
) {
  if (value == null) return
  if (fieldEncoding == LENGTH_DELIMITED) {
    val byteCountBefore = writer.byteCount
    encode(writer, value)
    writer.writeVarint32(writer.byteCount - byteCountBefore)
  } else {
    encode(writer, value)
  }
  writer.writeTag(tag, fieldEncoding)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncode(sink: BufferedSink, value: E) {
  val writer = ReverseProtoWriter()
  encode(writer, value)
  writer.writeTo(sink)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncode(value: E): ByteArray {
  val buffer = Buffer()
  encode(buffer, value)
  return buffer.readByteArray()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncodeByteString(value: E): ByteString {
  val buffer = Buffer()
  encode(buffer, value)
  return buffer.readByteString()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonDecode(bytes: ByteArray): E {
  return decode(ProtoReader32(bytes))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonDecode(bytes: ByteString): E {
  return decode(ProtoReader32(bytes))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonDecode(source: BufferedSource): E {
  return decode(ProtoReader(source))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonTryDecode(
  reader: ProtoReader,
  destination: MutableList<E>,
) {
  if (reader.beforePossiblyPackedScalar()) {
    destination.add(decode(reader))
  }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonTryDecode(
  reader: ProtoReader32,
  destination: MutableList<E>,
) {
  if (reader.beforePossiblyPackedScalar()) {
    destination.add(decode(reader))
  }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> commonToString(value: E): String = value.toString()

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonWithLabel(label: WireField.Label): ProtoAdapter<*> {
  if (label.isRepeated) {
    return if (label.isPacked) asPacked() else asRepeated()
  }
  return this
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonCreatePacked(): ProtoAdapter<List<E>> {
  require(fieldEncoding != LENGTH_DELIMITED) {
    "Unable to pack a length-delimited type."
  }
  return PackedProtoAdapter(originalAdapter = this)
}

internal class PackedProtoAdapter<E>(
  private val originalAdapter: ProtoAdapter<E>,
) : ProtoAdapter<List<E>>(
  LENGTH_DELIMITED,
  List::class,
  null,
  originalAdapter.syntax,
  listOf<E>(),
) {
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: List<E>?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodedSize(value: List<E>): Int {
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSize(value[i])
    }
    return size
  }

  override fun encodedSizeWithTag(tag: Int, value: List<E>?): Int {
    return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
  }

  override fun encode(writer: ProtoWriter, value: List<E>) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: List<E>) {
    for (i in (value.size - 1) downTo 0) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun decode(reader: ProtoReader): List<E> = listOf(originalAdapter.decode(reader))

  override fun decode(reader: ProtoReader32): List<E> = listOf(originalAdapter.decode(reader))

  override fun redact(value: List<E>): List<E> = emptyList()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonCreateRepeated(): ProtoAdapter<List<E>> {
  return RepeatedProtoAdapter(originalAdapter = this)
}

internal class RepeatedProtoAdapter<E>(
  private val originalAdapter: ProtoAdapter<E>,
) : ProtoAdapter<List<E>>(
  originalAdapter.fieldEncoding,
  List::class,
  null,
  originalAdapter.syntax,
  listOf<E>(),
) {
  override fun encodedSize(value: List<E>): Int {
    throw UnsupportedOperationException("Repeated values can only be sized with a tag.")
  }

  override fun encodedSizeWithTag(tag: Int, value: List<E>?): Int {
    if (value == null) return 0
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSizeWithTag(tag, value[i])
    }
    return size
  }

  override fun encode(writer: ProtoWriter, value: List<E>) {
    throw UnsupportedOperationException("Repeated values can only be encoded with a tag.")
  }

  override fun encode(writer: ReverseProtoWriter, value: List<E>) {
    throw UnsupportedOperationException("Repeated values can only be encoded with a tag.")
  }

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
    if (value == null) return
    for (i in 0 until value.size) {
      originalAdapter.encodeWithTag(writer, tag, value[i])
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: List<E>?) {
    if (value == null) return
    for (i in (value.size - 1) downTo 0) {
      originalAdapter.encodeWithTag(writer, tag, value[i])
    }
  }

  override fun decode(reader: ProtoReader): List<E> = listOf(originalAdapter.decode(reader))

  override fun decode(reader: ProtoReader32): List<E> = listOf(originalAdapter.decode(reader))

  override fun redact(value: List<E>): List<E> = emptyList()
}

internal class DoubleArrayProtoAdapter(
  private val originalAdapter: ProtoAdapter<Double>,
) : ProtoAdapter<DoubleArray>(
  LENGTH_DELIMITED,
  DoubleArray::class,
  null,
  originalAdapter.syntax,
  DoubleArray(0),
) {
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: DoubleArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: DoubleArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodedSize(value: DoubleArray): Int {
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSize(value[i])
    }
    return size
  }

  override fun encodedSizeWithTag(tag: Int, value: DoubleArray?): Int {
    return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
  }

  override fun encode(writer: ProtoWriter, value: DoubleArray) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: DoubleArray) {
    for (i in (value.size - 1) downTo 0) {
      writer.writeFixed64(value[i].toBits())
    }
  }

  override fun decode(reader: ProtoReader): DoubleArray =
    doubleArrayOf(Double.fromBits(reader.readFixed64()))

  override fun decode(reader: ProtoReader32): DoubleArray =
    doubleArrayOf(Double.fromBits(reader.readFixed64()))

  override fun redact(value: DoubleArray): DoubleArray = doubleArrayOf()
}

internal class LongArrayProtoAdapter(
  private val originalAdapter: ProtoAdapter<Long>,
) : ProtoAdapter<LongArray>(
  LENGTH_DELIMITED,
  LongArray::class,
  null,
  originalAdapter.syntax,
  LongArray(0),
) {
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: LongArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: LongArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodedSize(value: LongArray): Int {
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSize(value[i])
    }
    return size
  }

  override fun encodedSizeWithTag(tag: Int, value: LongArray?): Int {
    return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
  }

  override fun encode(writer: ProtoWriter, value: LongArray) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: LongArray) {
    for (i in (value.size - 1) downTo 0) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun decode(reader: ProtoReader): LongArray = longArrayOf(originalAdapter.decode(reader))

  override fun decode(reader: ProtoReader32): LongArray = longArrayOf(originalAdapter.decode(reader))

  override fun redact(value: LongArray): LongArray = longArrayOf()
}

internal class FloatArrayProtoAdapter(
  private val originalAdapter: ProtoAdapter<Float>,
) : ProtoAdapter<FloatArray>(
  LENGTH_DELIMITED,
  FloatArray::class,
  null,
  originalAdapter.syntax,
  FloatArray(0),
) {
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: FloatArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: FloatArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodedSize(value: FloatArray): Int {
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSize(value[i])
    }
    return size
  }

  override fun encodedSizeWithTag(tag: Int, value: FloatArray?): Int {
    return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
  }

  override fun encode(writer: ProtoWriter, value: FloatArray) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: FloatArray) {
    for (i in (value.size - 1) downTo 0) {
      writer.writeFixed32(value[i].toBits())
    }
  }

  override fun decode(reader: ProtoReader): FloatArray =
    floatArrayOf(Float.fromBits(reader.readFixed32()))

  override fun decode(reader: ProtoReader32): FloatArray =
    floatArrayOf(Float.fromBits(reader.readFixed32()))

  override fun redact(value: FloatArray): FloatArray = floatArrayOf()
}

internal class IntArrayProtoAdapter(
  private val originalAdapter: ProtoAdapter<Int>,
) : ProtoAdapter<IntArray>(
  LENGTH_DELIMITED,
  IntArray::class,
  null,
  originalAdapter.syntax,
  IntArray(0),
) {
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: IntArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: IntArray?) {
    if (value != null && value.isNotEmpty()) {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodedSize(value: IntArray): Int {
    var size = 0
    for (i in 0 until value.size) {
      size += originalAdapter.encodedSize(value[i])
    }
    return size
  }

  override fun encodedSizeWithTag(tag: Int, value: IntArray?): Int {
    return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
  }

  override fun encode(writer: ProtoWriter, value: IntArray) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: IntArray) {
    for (i in (value.size - 1) downTo 0) {
      originalAdapter.encode(writer, value[i])
    }
  }

  override fun decode(reader: ProtoReader): IntArray = intArrayOf(originalAdapter.decode(reader))

  override fun decode(reader: ProtoReader32): IntArray = intArrayOf(originalAdapter.decode(reader))

  override fun redact(value: IntArray): IntArray = intArrayOf()
}

internal class MapProtoAdapter<K, V> internal constructor(
  keyAdapter: ProtoAdapter<K>,
  valueAdapter: ProtoAdapter<V>,
) : ProtoAdapter<Map<K, V>>(
  LENGTH_DELIMITED,
  Map::class,
  null,
  valueAdapter.syntax,
  mapOf<K, V>(),
) {
  private val entryAdapter = MapEntryProtoAdapter(keyAdapter, valueAdapter)

  override fun encodedSize(value: Map<K, V>): Int {
    throw UnsupportedOperationException("Repeated values can only be sized with a tag.")
  }

  override fun encodedSizeWithTag(tag: Int, value: Map<K, V>?): Int {
    if (value == null) return 0
    var size = 0
    for (entry in value.entries) {
      size += entryAdapter.encodedSizeWithTag(tag, entry)
    }
    return size
  }

  override fun encode(writer: ProtoWriter, value: Map<K, V>) {
    throw UnsupportedOperationException("Repeated values can only be encoded with a tag.")
  }

  override fun encode(writer: ReverseProtoWriter, value: Map<K, V>) {
    throw UnsupportedOperationException("Repeated values can only be encoded with a tag.")
  }

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Map<K, V>?) {
    if (value == null) return
    for (entry in value.entries) {
      entryAdapter.encodeWithTag(writer, tag, entry)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: Map<K, V>?) {
    if (value == null) return
    for (entry in value.entries.toTypedArray().apply { reverse() }) {
      entryAdapter.encodeWithTag(writer, tag, entry)
    }
  }

  override fun decode(reader: ProtoReader): Map<K, V> {
    var key: K? = entryAdapter.keyAdapter.identity
    var value: V? = entryAdapter.valueAdapter.identity

    val token = reader.beginMessage()
    while (true) {
      val tag = reader.nextTag()
      if (tag == -1) break
      when (tag) {
        1 -> key = entryAdapter.keyAdapter.decode(reader)
        2 -> value = entryAdapter.valueAdapter.decode(reader)
        // Ignore unknown tags in map entries.
      }
    }
    reader.endMessageAndGetUnknownFields(token)

    check(key != null) { "Map entry with null key" }
    check(value != null) { "Map entry with null value" }
    return mapOf(key to value)
  }

  override fun decode(reader: ProtoReader32): Map<K, V> {
    var key: K? = entryAdapter.keyAdapter.identity
    var value: V? = entryAdapter.valueAdapter.identity

    val token = reader.beginMessage()
    while (true) {
      val tag = reader.nextTag()
      if (tag == -1) break
      when (tag) {
        1 -> key = entryAdapter.keyAdapter.decode(reader)
        2 -> value = entryAdapter.valueAdapter.decode(reader)
        // Ignore unknown tags in map entries.
      }
    }
    reader.endMessageAndGetUnknownFields(token)

    check(key != null) { "Map entry with null key" }
    check(value != null) { "Map entry with null value" }
    return mapOf(key to value)
  }

  override fun redact(value: Map<K, V>): Map<K, V> = emptyMap()
}

private class MapEntryProtoAdapter<K, V> internal constructor(
  internal val keyAdapter: ProtoAdapter<K>,
  internal val valueAdapter: ProtoAdapter<V>,
) : ProtoAdapter<Map.Entry<K, V>>(
  LENGTH_DELIMITED,
  Map.Entry::class,
  null,
  valueAdapter.syntax,
) {

  override fun encodedSize(value: Map.Entry<K, V>): Int {
    return keyAdapter.encodedSizeWithTag(1, value.key) +
      valueAdapter.encodedSizeWithTag(2, value.value)
  }

  override fun encode(writer: ProtoWriter, value: Map.Entry<K, V>) {
    keyAdapter.encodeWithTag(writer, 1, value.key)
    valueAdapter.encodeWithTag(writer, 2, value.value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Map.Entry<K, V>) {
    valueAdapter.encodeWithTag(writer, 2, value.value)
    keyAdapter.encodeWithTag(writer, 1, value.key)
  }

  override fun decode(reader: ProtoReader): Map.Entry<K, V> {
    throw UnsupportedOperationException()
  }

  override fun redact(value: Map.Entry<K, V>): Map.Entry<K, V> {
    throw UnsupportedOperationException()
  }
}

private const val FIXED_BOOL_SIZE = 1
private const val FIXED_32_SIZE = 4
private const val FIXED_64_SIZE = 8

@Suppress("NOTHING_TO_INLINE")
internal inline fun <K, V> commonNewMapAdapter(
  keyAdapter: ProtoAdapter<K>,
  valueAdapter: ProtoAdapter<V>,
): ProtoAdapter<Map<K, V>> {
  return MapProtoAdapter(keyAdapter, valueAdapter)
}

internal fun commonBool(): ProtoAdapter<Boolean> = object : ProtoAdapter<Boolean>(
  VARINT,
  Boolean::class,
  null,
  Syntax.PROTO_2,
  false,
) {
  override fun encodedSize(value: Boolean): Int = FIXED_BOOL_SIZE

  override fun encode(writer: ProtoWriter, value: Boolean) {
    writer.writeVarint32(if (value) 1 else 0)
  }

  override fun encode(writer: ReverseProtoWriter, value: Boolean) {
    writer.writeVarint32(if (value) 1 else 0)
  }

  override fun decode(reader: ProtoReader): Boolean = when (reader.readVarint32()) {
    0 -> false
    // We are lenient to match protoc behavior.
    else -> true
  }

  override fun decode(reader: ProtoReader32): Boolean = when (reader.readVarint32()) {
    0 -> false
    // We are lenient to match protoc behavior.
    else -> true
  }

  override fun redact(value: Boolean): Boolean = throw UnsupportedOperationException()
}

internal fun commonInt32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
  VARINT,
  Int::class,
  null,
  Syntax.PROTO_2,
  0,
) {
  override fun encodedSize(value: Int): Int = int32Size(value)

  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeSignedVarint32(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Int) {
    writer.writeSignedVarint32(value)
  }

  override fun decode(reader: ProtoReader): Int = reader.readVarint32()

  override fun decode(reader: ProtoReader32): Int = reader.readVarint32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonUint32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
  VARINT,
  Int::class,
  null,
  Syntax.PROTO_2,
  0,
) {
  override fun encodedSize(value: Int): Int = varint32Size(value)

  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeVarint32(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Int) {
    writer.writeVarint32(value)
  }

  override fun decode(reader: ProtoReader): Int = reader.readVarint32()

  override fun decode(reader: ProtoReader32): Int = reader.readVarint32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonSint32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
  VARINT,
  Int::class,
  null,
  Syntax.PROTO_2,
  0,
) {
  override fun encodedSize(value: Int): Int = varint32Size(encodeZigZag32(value))

  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeVarint32(encodeZigZag32(value))
  }

  override fun encode(writer: ReverseProtoWriter, value: Int) {
    writer.writeVarint32(encodeZigZag32(value))
  }

  override fun decode(reader: ProtoReader): Int = decodeZigZag32(reader.readVarint32())

  override fun decode(reader: ProtoReader32): Int = decodeZigZag32(reader.readVarint32())

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonFixed32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
  FieldEncoding.FIXED32,
  Int::class,
  null,
  Syntax.PROTO_2,
  0,
) {
  override fun encodedSize(value: Int): Int = FIXED_32_SIZE

  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeFixed32(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Int) {
    writer.writeFixed32(value)
  }

  override fun decode(reader: ProtoReader): Int = reader.readFixed32()

  override fun decode(reader: ProtoReader32): Int = reader.readFixed32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonSfixed32() = commonFixed32()

internal fun commonInt64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
  VARINT,
  Long::class,
  null,
  Syntax.PROTO_2,
  0L,
) {
  override fun encodedSize(value: Long): Int = varint64Size(value)

  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  override fun decode(reader: ProtoReader): Long = reader.readVarint64()

  override fun decode(reader: ProtoReader32): Long = reader.readVarint64()

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

/**
 * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
 * in JSON.
 */
internal fun commonUint64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
  VARINT,
  Long::class,
  null,
  Syntax.PROTO_2,
  0L,
) {
  override fun encodedSize(value: Long): Int = varint64Size(value)

  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  override fun decode(reader: ProtoReader): Long = reader.readVarint64()

  override fun decode(reader: ProtoReader32): Long = reader.readVarint64()

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal fun commonSint64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
  VARINT,
  Long::class,
  null,
  Syntax.PROTO_2,
  0L,
) {
  override fun encodedSize(value: Long): Int = varint64Size(encodeZigZag64(value))

  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(encodeZigZag64(value))
  }

  override fun encode(writer: ReverseProtoWriter, value: Long) {
    writer.writeVarint64(encodeZigZag64(value))
  }

  override fun decode(reader: ProtoReader): Long = decodeZigZag64(reader.readVarint64())

  override fun decode(reader: ProtoReader32): Long = decodeZigZag64(reader.readVarint64())

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal fun commonFixed64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
  FieldEncoding.FIXED64,
  Long::class,
  null,
  Syntax.PROTO_2,
  0L,
) {
  override fun encodedSize(value: Long): Int = FIXED_64_SIZE

  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeFixed64(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: Long) {
    writer.writeFixed64(value)
  }

  override fun decode(reader: ProtoReader): Long = reader.readFixed64()

  override fun decode(reader: ProtoReader32): Long = reader.readFixed64()

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal class FloatProtoAdapter : ProtoAdapter<Float>(
  FieldEncoding.FIXED32,
  Float::class,
  null,
  Syntax.PROTO_2,
  0.0f,
) {
  override fun encode(writer: ProtoWriter, value: Float) {
    writer.writeFixed32(value.toBits())
  }

  override fun encode(writer: ReverseProtoWriter, value: Float) {
    writer.writeFixed32(value.toBits())
  }

  override fun decode(reader: ProtoReader): Float {
    return Float.fromBits(reader.readFixed32())
  }

  override fun decode(reader: ProtoReader32): Float {
    return Float.fromBits(reader.readFixed32())
  }

  override fun encodedSize(value: Float): Int = FIXED_32_SIZE

  override fun redact(value: Float): Float = throw UnsupportedOperationException()
}

internal fun commonSfixed64() = commonFixed64()
internal fun commonFloat(): FloatProtoAdapter = FloatProtoAdapter()

internal class DoubleProtoAdapter : ProtoAdapter<Double>(
  FieldEncoding.FIXED64,
  Double::class,
  null,
  Syntax.PROTO_2,
  0.0,
) {
  override fun encodedSize(value: Double): Int = FIXED_64_SIZE

  override fun encode(writer: ProtoWriter, value: Double) {
    writer.writeFixed64(value.toBits())
  }

  override fun encode(writer: ReverseProtoWriter, value: Double) {
    writer.writeFixed64(value.toBits())
  }

  override fun decode(reader: ProtoReader): Double = Double.fromBits(reader.readFixed64())

  override fun decode(reader: ProtoReader32): Double = Double.fromBits(reader.readFixed64())

  override fun redact(value: Double): Double = throw UnsupportedOperationException()
}

internal fun commonDouble(): DoubleProtoAdapter = DoubleProtoAdapter()

internal fun commonString(): ProtoAdapter<String> = object : ProtoAdapter<String>(
  LENGTH_DELIMITED,
  String::class,
  null,
  Syntax.PROTO_2,
  "",
) {
  override fun encodedSize(value: String): Int = value.utf8Size().toInt()

  override fun encode(writer: ProtoWriter, value: String) {
    writer.writeString(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: String) {
    writer.writeString(value)
  }

  override fun decode(reader: ProtoReader): String = reader.readString()

  override fun decode(reader: ProtoReader32): String = reader.readString()

  override fun redact(value: String): String = throw UnsupportedOperationException()
}

internal fun commonBytes(): ProtoAdapter<ByteString> = object : ProtoAdapter<ByteString>(
  LENGTH_DELIMITED,
  ByteString::class,
  null,
  Syntax.PROTO_2,
  ByteString.EMPTY,
) {
  override fun encodedSize(value: ByteString): Int = value.size

  override fun encode(writer: ProtoWriter, value: ByteString) {
    writer.writeBytes(value)
  }

  override fun encode(writer: ReverseProtoWriter, value: ByteString) {
    writer.writeBytes(value)
  }

  override fun decode(reader: ProtoReader): ByteString = reader.readBytes()

  override fun decode(reader: ProtoReader32): ByteString = reader.readBytes()

  override fun redact(value: ByteString): ByteString = throw UnsupportedOperationException()
}

internal fun commonDuration(): ProtoAdapter<Duration> = object : ProtoAdapter<Duration>(
  LENGTH_DELIMITED,
  Duration::class,
  "type.googleapis.com/google.protobuf.Duration",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Duration): Int {
    var result = 0
    val seconds = value.sameSignSeconds
    if (seconds != 0L) result += INT64.encodedSizeWithTag(1, seconds)
    val nanos = value.sameSignNanos
    if (nanos != 0) result += INT32.encodedSizeWithTag(2, nanos)
    return result
  }

  override fun encode(writer: ProtoWriter, value: Duration) {
    val seconds = value.sameSignSeconds
    if (seconds != 0L) INT64.encodeWithTag(writer, 1, seconds)
    val nanos = value.sameSignNanos
    if (nanos != 0) INT32.encodeWithTag(writer, 2, nanos)
  }

  override fun encode(writer: ReverseProtoWriter, value: Duration) {
    val nanos = value.sameSignNanos
    if (nanos != 0) INT32.encodeWithTag(writer, 2, nanos)
    val seconds = value.sameSignSeconds
    if (seconds != 0L) INT64.encodeWithTag(writer, 1, seconds)
  }

  override fun decode(reader: ProtoReader): Duration {
    var seconds = 0L
    var nanos = 0
    reader.forEachTag { tag ->
      when (tag) {
        1 -> seconds = INT64.decode(reader)
        2 -> nanos = INT32.decode(reader)
        else -> reader.readUnknownField(tag)
      }
    }
    return durationOfSeconds(seconds, nanos.toLong())
  }

  override fun decode(reader: ProtoReader32): Duration {
    var seconds = 0L
    var nanos = 0
    reader.forEachTag { tag ->
      when (tag) {
        1 -> seconds = INT64.decode(reader)
        2 -> nanos = INT32.decode(reader)
        else -> reader.readUnknownField(tag)
      }
    }
    return durationOfSeconds(seconds, nanos.toLong())
  }

  override fun redact(value: Duration): Duration = value

  /**
   * Returns a value like 1 for 1.200s and -1 for -1.200s. This is different from the Duration
   * seconds field which is always the integer floor when seconds is negative.
   */
  private val Duration.sameSignSeconds: Long
    get() {
      return when {
        getSeconds() < 0L && getNano() != 0 -> getSeconds() + 1L
        else -> getSeconds()
      }
    }

  /**
   * Returns a value like 200_000_000 for 1.200s and -200_000_000 for -1.200s. This is different
   * from the Duration nanos field which can be positive when seconds is negative.
   */
  private val Duration.sameSignNanos: Int
    get() {
      return when {
        getSeconds() < 0L && getNano() != 0 -> getNano() - 1_000_000_000
        else -> getNano()
      }
    }
}

internal fun commonInstant(): ProtoAdapter<Instant> = object : ProtoAdapter<Instant>(
  LENGTH_DELIMITED,
  Instant::class,
  "type.googleapis.com/google.protobuf.Timestamp",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Instant): Int {
    var result = 0
    val seconds = value.getEpochSecond()
    if (seconds != 0L) result += INT64.encodedSizeWithTag(1, seconds)
    val nanos = value.getNano()
    if (nanos != 0) result += INT32.encodedSizeWithTag(2, nanos)
    return result
  }

  override fun encode(writer: ProtoWriter, value: Instant) {
    val seconds = value.getEpochSecond()
    if (seconds != 0L) INT64.encodeWithTag(writer, 1, seconds)
    val nanos = value.getNano()
    if (nanos != 0) INT32.encodeWithTag(writer, 2, nanos)
  }

  override fun encode(writer: ReverseProtoWriter, value: Instant) {
    val nanos = value.getNano()
    if (nanos != 0) INT32.encodeWithTag(writer, 2, nanos)
    val seconds = value.getEpochSecond()
    if (seconds != 0L) INT64.encodeWithTag(writer, 1, seconds)
  }

  override fun decode(reader: ProtoReader): Instant {
    var seconds = 0L
    var nanos = 0
    reader.forEachTag { tag ->
      when (tag) {
        1 -> seconds = INT64.decode(reader)
        2 -> nanos = INT32.decode(reader)
        else -> reader.readUnknownField(tag)
      }
    }
    return ofEpochSecond(seconds, nanos.toLong())
  }

  override fun decode(reader: ProtoReader32): Instant {
    var seconds = 0L
    var nanos = 0
    reader.forEachTag { tag ->
      when (tag) {
        1 -> seconds = INT64.decode(reader)
        2 -> nanos = INT32.decode(reader)
        else -> reader.readUnknownField(tag)
      }
    }
    return ofEpochSecond(seconds, nanos.toLong())
  }

  override fun redact(value: Instant): Instant = value
}

internal fun commonEmpty(): ProtoAdapter<Unit> = object : ProtoAdapter<Unit>(
  LENGTH_DELIMITED,
  Unit::class,
  "type.googleapis.com/google.protobuf.Empty",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Unit): Int = 0

  override fun encode(writer: ProtoWriter, value: Unit) = Unit

  override fun encode(writer: ReverseProtoWriter, value: Unit) = Unit

  override fun decode(reader: ProtoReader) {
    reader.forEachTag { tag -> reader.readUnknownField(tag) }
  }

  override fun decode(reader: ProtoReader32) {
    reader.forEachTag { tag -> reader.readUnknownField(tag) }
  }

  override fun redact(value: Unit): Unit = value
}

internal fun commonStructMap(): ProtoAdapter<Map<String, *>?> = object : ProtoAdapter<Map<String, *>?>(
  LENGTH_DELIMITED,
  Map::class,
  "type.googleapis.com/google.protobuf.Struct",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Map<String, *>?): Int {
    if (value == null) return 0

    var size = 0
    for ((k, v) in value) {
      val entrySize = STRING.encodedSizeWithTag(1, k) + STRUCT_VALUE.encodedSizeWithTag(2, v)
      size += tagSize(1) + varint32Size(entrySize) + entrySize
    }
    return size
  }

  override fun encode(writer: ProtoWriter, value: Map<String, *>?) {
    if (value == null) return

    for ((k, v) in value) {
      val entrySize = STRING.encodedSizeWithTag(1, k) + STRUCT_VALUE.encodedSizeWithTag(2, v)
      writer.writeTag(1, LENGTH_DELIMITED)
      writer.writeVarint32(entrySize)
      STRING.encodeWithTag(writer, 1, k)
      STRUCT_VALUE.encodeWithTag(writer, 2, v)
    }
  }

  override fun encode(
    writer: ReverseProtoWriter,
    value: Map<String, *>?,
  ) {
    if (value == null) return

    for ((k, v) in value.entries.toTypedArray().apply { reverse() }) {
      val byteCountBefore = writer.byteCount
      STRUCT_VALUE.encodeWithTag(writer, 2, v)
      STRING.encodeWithTag(writer, 1, k)
      writer.writeVarint32(writer.byteCount - byteCountBefore)
      writer.writeTag(1, LENGTH_DELIMITED)
    }
  }

  override fun decode(reader: ProtoReader): Map<String, *>? {
    val result = mutableMapOf<String, Any?>()
    reader.forEachTag { entryTag ->
      if (entryTag != 1) return@forEachTag reader.skip()

      var key: String? = null
      var value: Any? = null
      reader.forEachTag { tag ->
        when (tag) {
          1 -> key = STRING.decode(reader)
          2 -> value = STRUCT_VALUE.decode(reader)
          else -> reader.readUnknownField(tag)
        }
      }
      if (key != null) result[key!!] = value
    }
    return result
  }

  override fun decode(reader: ProtoReader32): Map<String, *>? {
    val result = mutableMapOf<String, Any?>()
    reader.forEachTag { entryTag ->
      if (entryTag != 1) return@forEachTag reader.skip()

      var key: String? = null
      var value: Any? = null
      reader.forEachTag { tag ->
        when (tag) {
          1 -> key = STRING.decode(reader)
          2 -> value = STRUCT_VALUE.decode(reader)
          else -> reader.readUnknownField(tag)
        }
      }
      if (key != null) result[key!!] = value
    }
    return result
  }

  override fun redact(value: Map<String, *>?) = value?.mapValues { STRUCT_VALUE.redact(it) }
}

internal fun commonStructList(): ProtoAdapter<List<*>?> = object : ProtoAdapter<List<*>?>(
  LENGTH_DELIMITED,
  Map::class,
  "type.googleapis.com/google.protobuf.ListValue",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: List<*>?): Int {
    if (value == null) return 0

    var result = 0
    for (v in value) {
      result += STRUCT_VALUE.encodedSizeWithTag(1, v)
    }
    return result
  }

  override fun encode(writer: ProtoWriter, value: List<*>?) {
    if (value == null) return
    for (v in value) {
      STRUCT_VALUE.encodeWithTag(writer, 1, v)
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: List<*>?) {
    if (value == null) return
    for (v in (value.size - 1) downTo 0) {
      STRUCT_VALUE.encodeWithTag(writer, 1, value[v])
    }
  }

  override fun decode(reader: ProtoReader): List<*>? {
    val result = mutableListOf<Any?>()
    reader.forEachTag { entryTag ->
      if (entryTag != 1) return@forEachTag reader.skip()
      result.add(STRUCT_VALUE.decode(reader))
    }
    return result
  }

  override fun decode(reader: ProtoReader32): List<*>? {
    val result = mutableListOf<Any?>()
    reader.forEachTag { entryTag ->
      if (entryTag != 1) return@forEachTag reader.skip()
      result.add(STRUCT_VALUE.decode(reader))
    }
    return result
  }

  override fun redact(value: List<*>?) = value?.map { STRUCT_VALUE.redact(it) }
}

internal fun commonStructNull(): ProtoAdapter<Nothing?> = object : ProtoAdapter<Nothing?>(
  VARINT,
  Nothing::class,
  "type.googleapis.com/google.protobuf.NullValue",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Nothing?): Int = varint32Size(0)

  override fun encodedSizeWithTag(tag: Int, value: Nothing?): Int {
    val size = encodedSize(value)
    return tagSize(tag) + varint32Size(size)
  }

  override fun encode(writer: ProtoWriter, value: Nothing?) {
    writer.writeVarint32(0)
  }

  override fun encode(writer: ReverseProtoWriter, value: Nothing?) {
    writer.writeVarint32(0)
  }

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Nothing?) {
    writer.writeTag(tag, fieldEncoding)
    encode(writer, value)
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: Nothing?) {
    encode(writer, value)
    writer.writeTag(tag, fieldEncoding)
  }

  override fun decode(reader: ProtoReader): Nothing? {
    val value = reader.readVarint32()
    if (value != 0) throw IOException("expected 0 but was $value")
    return null
  }

  override fun decode(reader: ProtoReader32): Nothing? {
    val value = reader.readVarint32()
    if (value != 0) throw IOException("expected 0 but was $value")
    return null
  }

  override fun redact(value: Nothing?): Nothing? = null
}

internal fun commonStructValue(): ProtoAdapter<Any?> = object : ProtoAdapter<Any?>(
  LENGTH_DELIMITED,
  Any::class,
  "type.googleapis.com/google.protobuf.Value",
  Syntax.PROTO_3,
) {
  override fun encodedSize(value: Any?): Int {
    @Suppress("UNCHECKED_CAST") // Assume map keys are strings.
    return when (value) {
      null -> STRUCT_NULL.encodedSizeWithTag(1, value)
      is Number -> DOUBLE.encodedSizeWithTag(2, value.toDouble())
      is String -> STRING.encodedSizeWithTag(3, value)
      is Boolean -> BOOL.encodedSizeWithTag(4, value)
      is Map<*, *> -> STRUCT_MAP.encodedSizeWithTag(5, value as Map<String, *>)
      is List<*> -> STRUCT_LIST.encodedSizeWithTag(6, value)
      else -> throw IllegalArgumentException("unexpected struct value: $value")
    }
  }

  override fun encodedSizeWithTag(tag: Int, value: Any?): Int {
    if (value == null) {
      val size = encodedSize(value)
      return tagSize(tag) + varint32Size(size) + size
    } else {
      return super.encodedSizeWithTag(tag, value)
    }
  }

  override fun encode(writer: ProtoWriter, value: Any?) {
    @Suppress("UNCHECKED_CAST") // Assume map keys are strings.
    return when (value) {
      null -> STRUCT_NULL.encodeWithTag(writer, 1, value)
      is Number -> DOUBLE.encodeWithTag(writer, 2, value.toDouble())
      is String -> STRING.encodeWithTag(writer, 3, value)
      is Boolean -> BOOL.encodeWithTag(writer, 4, value)
      is Map<*, *> -> STRUCT_MAP.encodeWithTag(writer, 5, value as Map<String, *>)
      is List<*> -> STRUCT_LIST.encodeWithTag(writer, 6, value)
      else -> throw IllegalArgumentException("unexpected struct value: $value")
    }
  }

  override fun encode(writer: ReverseProtoWriter, value: Any?) {
    @Suppress("UNCHECKED_CAST") // Assume map keys are strings.
    return when (value) {
      null -> STRUCT_NULL.encodeWithTag(writer, 1, value)
      is Number -> DOUBLE.encodeWithTag(writer, 2, value.toDouble())
      is String -> STRING.encodeWithTag(writer, 3, value)
      is Boolean -> BOOL.encodeWithTag(writer, 4, value)
      is Map<*, *> -> STRUCT_MAP.encodeWithTag(writer, 5, value as Map<String, *>)
      is List<*> -> STRUCT_LIST.encodeWithTag(writer, 6, value)
      else -> throw IllegalArgumentException("unexpected struct value: $value")
    }
  }

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Any?) {
    if (value == null) {
      writer.writeTag(tag, fieldEncoding)
      writer.writeVarint32(encodedSize(value))
      encode(writer, value)
    } else {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: Any?) {
    if (value == null) {
      val byteCountBefore = writer.byteCount
      encode(writer, value)
      writer.writeVarint32(writer.byteCount - byteCountBefore)
      writer.writeTag(tag, fieldEncoding)
    } else {
      super.encodeWithTag(writer, tag, value)
    }
  }

  override fun decode(reader: ProtoReader): Any? {
    var result: Any? = null
    reader.forEachTag { tag ->
      when (tag) {
        1 -> result = STRUCT_NULL.decode(reader)
        2 -> result = DOUBLE.decode(reader)
        3 -> result = STRING.decode(reader)
        4 -> result = BOOL.decode(reader)
        5 -> result = STRUCT_MAP.decode(reader)
        6 -> result = STRUCT_LIST.decode(reader)
        else -> reader.skip()
      }
    }
    return result
  }

  override fun decode(reader: ProtoReader32): Any? {
    var result: Any? = null
    reader.forEachTag { tag ->
      when (tag) {
        1 -> result = STRUCT_NULL.decode(reader)
        2 -> result = DOUBLE.decode(reader)
        3 -> result = STRING.decode(reader)
        4 -> result = BOOL.decode(reader)
        5 -> result = STRUCT_MAP.decode(reader)
        6 -> result = STRUCT_LIST.decode(reader)
        else -> reader.skip()
      }
    }
    return result
  }

  override fun redact(value: Any?): Any? {
    @Suppress("UNCHECKED_CAST") // Assume map keys are strings.
    return when (value) {
      null -> STRUCT_NULL.redact(value)
      is Number -> value
      is String -> null
      is Boolean -> value
      is Map<*, *> -> STRUCT_MAP.redact(value as Map<String, *>)
      is List<*> -> STRUCT_LIST.redact(value)
      else -> throw IllegalArgumentException("unexpected struct value: $value")
    }
  }
}

/**
 * Wire implements scalar wrapper types as nullable scalar ones. Protoc omits both null wrappers
 * and wrappers whose scalar value is the identity value of the scalar type. This is why we are
 * checking for both null and identities in the wrapper adapter methods.
 */
internal fun <T : Any> commonWrapper(delegate: ProtoAdapter<T>, typeUrl: String): ProtoAdapter<T?> {
  return object : ProtoAdapter<T?>(
    LENGTH_DELIMITED,
    delegate.type,
    typeUrl,
    Syntax.PROTO_3,
    delegate.identity,
  ) {
    override fun encodedSize(value: T?): Int {
      if (value == null || value == delegate.identity) return 0
      return delegate.encodedSizeWithTag(1, value)
    }

    override fun encode(writer: ProtoWriter, value: T?) {
      if (value != null && value != delegate.identity) {
        delegate.encodeWithTag(writer, 1, value)
      }
    }

    override fun encode(writer: ReverseProtoWriter, value: T?) {
      if (value != null && value != delegate.identity) {
        delegate.encodeWithTag(writer, 1, value)
      }
    }

    override fun decode(reader: ProtoReader): T? {
      var result: T? = delegate.identity
      reader.forEachTag { tag ->
        when (tag) {
          1 -> result = delegate.decode(reader)
          else -> reader.readUnknownField(tag)
        }
      }
      return result
    }

    override fun decode(reader: ProtoReader32): T? {
      var result: T? = delegate.identity
      reader.forEachTag { tag ->
        when (tag) {
          1 -> result = delegate.decode(reader)
          else -> reader.readUnknownField(tag)
        }
      }
      return result
    }

    override fun redact(value: T?): T? {
      if (value == null) return null
      return delegate.redact(value)
    }
  }
}
