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
import com.squareup.wire.internal.Throws
import com.squareup.wire.internal.missingRequiredFields
import com.squareup.wire.internal.redactElements
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.IOException
import okio.utf8Size
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

expect abstract class ProtoAdapter<E>(
  fieldEncoding: FieldEncoding,
  type: KClass<*>?,
  typeUrl: String?,
  syntax: Syntax,
  identity: E? = null
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
   * | TYPE                                           | IDENTITY                      |
   * | :--------------------------------------------- | :---------------------------- |
   * | All numeric types (int32, float, double, etc.) | 0                             |
   * | Boolean                                        | false                         |
   * | String                                         | empty string: ""              |
   * | Bytes                                          | empty bytes: ByteString.EMPTY |
   * | Enums                                          | enum constant with tag 0      |
   * | Lists (repeated types)                         | empty list: listOf()          |
   */
  val identity: E?

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
  @Throws(IOException::class)
  abstract fun encode(writer: ProtoWriter, value: E)

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  @Throws(IOException::class)
  open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?)

  /** Encode `value` and write it to `stream`. */
  @Throws(IOException::class)
  fun encode(sink: BufferedSink, value: E)

  /** Encode `value` as a `byte[]`. */
  fun encode(value: E): ByteArray

  /** Encode `value` as a [ByteString]. */
  fun encodeByteString(value: E): ByteString

  /** Read a non-null value from `reader`. */
  @Throws(IOException::class)
  abstract fun decode(reader: ProtoReader): E

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  fun decode(bytes: ByteArray): E

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  fun decode(bytes: ByteString): E

  /** Read an encoded message from `source`. */
  @Throws(IOException::class)
  fun decode(source: BufferedSource): E

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
    type: KClass<*>?
  ) : IllegalArgumentException {
    @JvmField val value: Int
  }

  companion object {
    /**
     * Creates a new proto adapter for a map using `keyAdapter` and `valueAdapter`.
     *
     * Note: Map entries are not required to be encoded sequentially. Thus, when decoding using
     * the returned adapter, only single-element maps will be returned and it is the caller's
     * responsibility to merge them into the final map.
     */
    @JvmStatic fun <K, V> newMapAdapter(
      keyAdapter: ProtoAdapter<K>,
      valueAdapter: ProtoAdapter<V>
    ): ProtoAdapter<Map<K, V>>

    @JvmField val BOOL: ProtoAdapter<Boolean>
    @JvmField val INT32: ProtoAdapter<Int>
    @JvmField val UINT32: ProtoAdapter<Int>
    @JvmField val SINT32: ProtoAdapter<Int>
    @JvmField val FIXED32: ProtoAdapter<Int>
    @JvmField val SFIXED32: ProtoAdapter<Int>
    @JvmField val INT64: ProtoAdapter<Long>
    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    @JvmField val UINT64: ProtoAdapter<Long>
    @JvmField val SINT64: ProtoAdapter<Long>
    @JvmField val FIXED64: ProtoAdapter<Long>
    @JvmField val SFIXED64: ProtoAdapter<Long>
    @JvmField val FLOAT: ProtoAdapter<Float>
    @JvmField val DOUBLE: ProtoAdapter<Double>
    @JvmField val BYTES: ProtoAdapter<ByteString>
    @JvmField val STRING: ProtoAdapter<String>
    @JvmField val DURATION: ProtoAdapter<Duration>
    @JvmField val INSTANT: ProtoAdapter<Instant>
    @JvmField val EMPTY: ProtoAdapter<Unit>
    @JvmField val STRUCT_MAP: ProtoAdapter<Map<String, *>?>
    @JvmField val STRUCT_LIST: ProtoAdapter<List<*>?>
    @JvmField val STRUCT_NULL: ProtoAdapter<Nothing?>
    @JvmField val STRUCT_VALUE: ProtoAdapter<Any?>
    @JvmField val DOUBLE_VALUE: ProtoAdapter<Double?>
    @JvmField val FLOAT_VALUE: ProtoAdapter<Float?>
    @JvmField val INT64_VALUE: ProtoAdapter<Long?>
    @JvmField val UINT64_VALUE: ProtoAdapter<Long?>
    @JvmField val INT32_VALUE: ProtoAdapter<Int?>
    @JvmField val UINT32_VALUE: ProtoAdapter<Int?>
    @JvmField val BOOL_VALUE: ProtoAdapter<Boolean?>
    @JvmField val STRING_VALUE: ProtoAdapter<String?>
    @JvmField val BYTES_VALUE: ProtoAdapter<ByteString?>
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
internal inline fun <E> ProtoAdapter<E>.commonEncodeWithTag(
  writer: ProtoWriter,
  tag: Int,
  value: E?
) {
  if (value == null) return
  writer.writeTag(tag, fieldEncoding)
  if (fieldEncoding == LENGTH_DELIMITED) {
    writer.writeVarint32(encodedSize(value))
  }
  encode(writer, value)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonEncode(sink: BufferedSink, value: E) {
  encode(ProtoWriter(sink), value)
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
  return decode(Buffer().write(bytes))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonDecode(bytes: ByteString): E {
  return decode(Buffer().write(bytes))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonDecode(source: BufferedSource): E {
  return decode(ProtoReader(source))
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
  private val originalAdapter: ProtoAdapter<E>
) : ProtoAdapter<List<E>>(
    LENGTH_DELIMITED,
    List::class,
    null,
    originalAdapter.syntax,
    listOf<E>()
) {
  @Throws(IOException::class)
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
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

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: List<E>) {
    for (i in 0 until value.size) {
      originalAdapter.encode(writer, value[i])
    }
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): List<E> = listOf(originalAdapter.decode(reader))

  override fun redact(value: List<E>): List<E> = emptyList()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> ProtoAdapter<E>.commonCreateRepeated(): ProtoAdapter<List<E>> {
  return RepeatedProtoAdapter(originalAdapter = this)
}

internal class RepeatedProtoAdapter<E>(
  private val originalAdapter: ProtoAdapter<E>
) : ProtoAdapter<List<E>>(
    originalAdapter.fieldEncoding,
    List::class,
    null,
    originalAdapter.syntax,
    listOf<E>()
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

  @Throws(IOException::class)
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
    if (value == null) return
    for (i in 0 until value.size) {
      originalAdapter.encodeWithTag(writer, tag, value[i])
    }
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): List<E> = listOf(originalAdapter.decode(reader))

  override fun redact(value: List<E>): List<E> = emptyList()
}

internal class MapProtoAdapter<K, V> internal constructor(
  keyAdapter: ProtoAdapter<K>,
  valueAdapter: ProtoAdapter<V>
) : ProtoAdapter<Map<K, V>>(
    LENGTH_DELIMITED,
    Map::class,
    null,
    valueAdapter.syntax,
    mapOf<K, V>()
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

  @Throws(IOException::class)
  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Map<K, V>?) {
    if (value == null) return
    for (entry in value.entries) {
      entryAdapter.encodeWithTag(writer, tag, entry)
    }
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Map<K, V> {
    var key: K? = null
    var value: V? = null

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
  internal val valueAdapter: ProtoAdapter<V>
) : ProtoAdapter<Map.Entry<K, V>>(
    LENGTH_DELIMITED,
    Map.Entry::class,
    null,
    valueAdapter.syntax
) {

  override fun encodedSize(value: Map.Entry<K, V>): Int {
    return keyAdapter.encodedSizeWithTag(1, value.key) +
        valueAdapter.encodedSizeWithTag(2, value.value)
  }

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Map.Entry<K, V>) {
    keyAdapter.encodeWithTag(writer, 1, value.key)
    valueAdapter.encodeWithTag(writer, 2, value.value)
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
  valueAdapter: ProtoAdapter<V>
): ProtoAdapter<Map<K, V>> {
  return MapProtoAdapter(keyAdapter, valueAdapter)
}

internal fun commonBool(): ProtoAdapter<Boolean> = object : ProtoAdapter<Boolean>(
    VARINT,
    Boolean::class,
    null,
    Syntax.PROTO_2,
    false
) {
  override fun encodedSize(value: Boolean): Int = FIXED_BOOL_SIZE

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Boolean) {
    writer.writeVarint32(if (value) 1 else 0)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Boolean = when (val value = reader.readVarint32()) {
    0 -> false
    1 -> true
    else -> throw IOException("Invalid boolean value 0x" + value.toString(16).padStart(2, '0'))
  }

  override fun redact(value: Boolean): Boolean = throw UnsupportedOperationException()
}

internal fun commonInt32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
    VARINT,
    Int::class,
    null,
    Syntax.PROTO_2,
    0
) {
  override fun encodedSize(value: Int): Int = int32Size(value)

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeSignedVarint32(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Int = reader.readVarint32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonUint32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
    VARINT,
    Int::class,
    null,
    Syntax.PROTO_2,
    0
) {
  override fun encodedSize(value: Int): Int = varint32Size(value)

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeVarint32(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Int = reader.readVarint32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonSint32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
    VARINT,
    Int::class,
    null,
    Syntax.PROTO_2,
    0
) {
  override fun encodedSize(value: Int): Int = varint32Size(encodeZigZag32(value))

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeVarint32(encodeZigZag32(value))
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Int = decodeZigZag32(reader.readVarint32())

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonFixed32(): ProtoAdapter<Int> = object : ProtoAdapter<Int>(
    FieldEncoding.FIXED32,
    Int::class,
    null,
    Syntax.PROTO_2,
    0
) {
  override fun encodedSize(value: Int): Int = FIXED_32_SIZE

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Int) {
    writer.writeFixed32(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Int = reader.readFixed32()

  override fun redact(value: Int): Int = throw UnsupportedOperationException()
}

internal fun commonSfixed32() = commonFixed32()
internal fun commonInt64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
    VARINT,
    Long::class,
    null,
    Syntax.PROTO_2,
    0L
) {
  override fun encodedSize(value: Long): Int = varint64Size(value)

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Long = reader.readVarint64()

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
    0L
) {
  override fun encodedSize(value: Long): Int = varint64Size(value)

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Long = reader.readVarint64()

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal fun commonSint64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
    VARINT,
    Long::class,
    null,
    Syntax.PROTO_2,
    0L
) {
  override fun encodedSize(value: Long): Int = varint64Size(encodeZigZag64(value))

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeVarint64(encodeZigZag64(value))
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Long = decodeZigZag64(reader.readVarint64())

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal fun commonFixed64(): ProtoAdapter<Long> = object : ProtoAdapter<Long>(
    FieldEncoding.FIXED64,
    Long::class,
    null,
    Syntax.PROTO_2,
    0L
) {
  override fun encodedSize(value: Long): Int = FIXED_64_SIZE

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Long) {
    writer.writeFixed64(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Long = reader.readFixed64()

  override fun redact(value: Long): Long = throw UnsupportedOperationException()
}

internal fun commonSfixed64() = commonFixed64()
internal fun commonFloat(): ProtoAdapter<Float> = object : ProtoAdapter<Float>(
    FieldEncoding.FIXED32,
    Float::class,
    null,
    Syntax.PROTO_2,
    0.0f
) {
  override fun encodedSize(value: Float): Int = FIXED_32_SIZE

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Float) {
    writer.writeFixed32(value.toBits())
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Float {
    return Float.fromBits(reader.readFixed32())
  }

  override fun redact(value: Float): Float = throw UnsupportedOperationException()
}

internal fun commonDouble(): ProtoAdapter<Double> = object : ProtoAdapter<Double>(
    FieldEncoding.FIXED64,
    Double::class,
    null,
    Syntax.PROTO_2,
    0.0
) {
  override fun encodedSize(value: Double): Int = FIXED_64_SIZE

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: Double) {
    writer.writeFixed64(value.toBits())
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): Double {
    return Double.fromBits(reader.readFixed64())
  }

  override fun redact(value: Double): Double = throw UnsupportedOperationException()
}

internal fun commonString(): ProtoAdapter<String> = object : ProtoAdapter<String>(
    LENGTH_DELIMITED,
    String::class,
    null,
    Syntax.PROTO_2,
    ""
) {
  override fun encodedSize(value: String): Int = value.utf8Size().toInt()

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: String) {
    writer.writeString(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): String = reader.readString()

  override fun redact(value: String): String = throw UnsupportedOperationException()
}

internal fun commonBytes(): ProtoAdapter<ByteString> = object : ProtoAdapter<ByteString>(
    LENGTH_DELIMITED,
    ByteString::class,
    null,
    Syntax.PROTO_2,
    ByteString.EMPTY
) {
  override fun encodedSize(value: ByteString): Int = value.size

  @Throws(IOException::class)
  override fun encode(writer: ProtoWriter, value: ByteString) {
    writer.writeBytes(value)
  }

  @Throws(IOException::class)
  override fun decode(reader: ProtoReader): ByteString = reader.readBytes()

  override fun redact(value: ByteString): ByteString = throw UnsupportedOperationException()
}

internal fun commonDuration(): ProtoAdapter<Duration> = object : ProtoAdapter<Duration>(
    LENGTH_DELIMITED,
    Duration::class,
    "type.googleapis.com/google.protobuf.Duration",
    Syntax.PROTO_3
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
    Syntax.PROTO_3
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

  override fun redact(value: Instant): Instant = value
}

internal fun commonEmpty(): ProtoAdapter<Unit> = object : ProtoAdapter<Unit>(
    LENGTH_DELIMITED,
    Unit::class,
    "type.googleapis.com/google.protobuf.Empty",
    Syntax.PROTO_3
) {
  override fun encodedSize(value: Unit): Int = 0

  override fun encode(writer: ProtoWriter, value: Unit) = Unit

  override fun decode(reader: ProtoReader) {
    reader.forEachTag { tag -> reader.readUnknownField(tag) }
  }

  override fun redact(value: Unit): Unit = value
}

internal fun commonStructMap(): ProtoAdapter<Map<String, *>?> = object : ProtoAdapter<Map<String, *>?>(
    LENGTH_DELIMITED,
    Map::class,
    "type.googleapis.com/google.protobuf.Struct",
    Syntax.PROTO_3
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

  override fun redact(value: Map<String, *>?) = value?.mapValues { STRUCT_VALUE.redact(it) }
}

internal fun commonStructList(): ProtoAdapter<List<*>?> = object : ProtoAdapter<List<*>?>(
    LENGTH_DELIMITED,
    Map::class,
    "type.googleapis.com/google.protobuf.ListValue",
    Syntax.PROTO_3
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

  override fun decode(reader: ProtoReader): List<*>? {
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
    Syntax.PROTO_3
) {
  override fun encodedSize(value: Nothing?): Int = varint32Size(0)

  override fun encodedSizeWithTag(tag: Int, value: Nothing?): Int {
    val size = encodedSize(value)
    return tagSize(tag) + varint32Size(size)
  }

  override fun encode(writer: ProtoWriter, value: Nothing?) {
    writer.writeVarint32(0)
  }

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Nothing?) {
    writer.writeTag(tag, fieldEncoding)
    encode(writer, value)
  }

  override fun decode(reader: ProtoReader): Nothing? {
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
    Syntax.PROTO_3
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

  override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: Any?) {
    if (value == null) {
      writer.writeTag(tag, fieldEncoding)
      writer.writeVarint32(encodedSize(value))
      encode(writer, value)
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

internal fun <T : Any> commonWrapper(delegate: ProtoAdapter<T>, typeUrl: String): ProtoAdapter<T?> {
  return object : ProtoAdapter<T?>(
      LENGTH_DELIMITED,
      delegate.type,
      typeUrl,
      Syntax.PROTO_3,
      null
  ) {
    override fun encodedSize(value: T?): Int {
      if (value == null) return 0
      return delegate.encodedSizeWithTag(1, value)
    }

    override fun encode(writer: ProtoWriter, value: T?) {
      if (value != null) {
        delegate.encodeWithTag(writer, 1, value)
      }
    }

    override fun decode(reader: ProtoReader): T? {
      var result: T? = null
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
