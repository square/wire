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

import com.squareup.wire.Message.Builder
import com.squareup.wire.ProtoWriter.Companion.decodeZigZag32
import com.squareup.wire.ProtoWriter.Companion.decodeZigZag64
import com.squareup.wire.ProtoWriter.Companion.encodeZigZag32
import com.squareup.wire.ProtoWriter.Companion.encodeZigZag64
import com.squareup.wire.ProtoWriter.Companion.int32Size
import com.squareup.wire.ProtoWriter.Companion.varint32Size
import com.squareup.wire.ProtoWriter.Companion.varint64Size
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.buffer
import okio.sink
import okio.source
import okio.utf8Size
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits

abstract class ProtoAdapter<E>(
  private val fieldEncoding: FieldEncoding,
  // TODO(egorand): Remove JvmField once RuntimeMessageAdapter is in Kotlin
  @JvmField val javaType: Class<*>?
) {
  internal var packedAdapter: ProtoAdapter<List<E>>? = null
  internal var repeatedAdapter: ProtoAdapter<List<E>>? = null

  /** Returns the redacted form of `value`. */
  open fun redact(value: E): E? = null

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
  open fun encodedSizeWithTag(tag: Int, value: E?): Int {
    if (value == null) return 0
    var size = encodedSize(value)
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      size += varint32Size(size)
    }
    return size + ProtoWriter.tagSize(tag)
  }

  /** Write non-null `value` to `writer`. */
  @Throws(IOException::class)
  abstract fun encode(writer: ProtoWriter, value: E)

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  @Throws(IOException::class)
  open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
    if (value == null) return
    writer.writeTag(tag, fieldEncoding)
    if (fieldEncoding == FieldEncoding.LENGTH_DELIMITED) {
      writer.writeVarint32(encodedSize(value))
    }
    encode(writer, value)
  }

  /** Encode `value` and write it to `stream`. */
  @Throws(IOException::class)
  fun encode(sink: BufferedSink, value: E) {
    encode(ProtoWriter(sink), value)
  }

  /** Encode `value` as a `byte[]`. */
  fun encode(value: E): ByteArray {
    val buffer = Buffer()
    encode(buffer, value)
    return buffer.readByteArray()
  }

  /** Encode `value` and write it to `stream`. */
  @Throws(IOException::class)
  fun encode(stream: OutputStream, value: E) {
    val buffer = stream.sink().buffer()
    encode(buffer, value)
    buffer.emit()
  }

  /** Read a non-null value from `reader`. */
  @Throws(IOException::class)
  abstract fun decode(reader: ProtoReader): E

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  fun decode(bytes: ByteArray): E = decode(Buffer().write(bytes))

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  fun decode(bytes: ByteString): E = decode(Buffer().write(bytes))

  /** Read an encoded message from `stream`. */
  @Throws(IOException::class)
  fun decode(stream: InputStream): E = decode(stream.source().buffer())

  /** Read an encoded message from `source`. */
  @Throws(IOException::class)
  fun decode(source: BufferedSource): E = decode(ProtoReader(source))

  /** Returns a human-readable version of the given `value`. */
  open fun toString(value: E): String = value.toString()

  // TODO(egorand): Make internal once FieldBinding is in Kotlin
  fun withLabel(label: WireField.Label): ProtoAdapter<*> {
    if (label.isRepeated) {
      return if (label.isPacked) asPacked() else asRepeated()
    }
    return this
  }

  /** Returns an adapter for `E` but as a packed, repeated value. */
  fun asPacked(): ProtoAdapter<List<E>> = packedAdapter ?: createPacked().also {
    packedAdapter = it
  }

  /**
   * Returns an adapter for `E` but as a repeated value.
   *
   * Note: Repeated items are not required to be encoded sequentially. Thus, when decoding using
   * the returned adapter, only single-element lists will be returned and it is the caller's
   * responsibility to merge them into the final list.
   */
  fun asRepeated(): ProtoAdapter<List<E>> = repeatedAdapter ?: createRepeated().also {
    repeatedAdapter = it
  }

  private fun createPacked(): ProtoAdapter<List<E>> {
    require(fieldEncoding != FieldEncoding.LENGTH_DELIMITED) {
      "Unable to pack a length-delimited type."
    }
    return object : ProtoAdapter<List<E>>(FieldEncoding.LENGTH_DELIMITED, List::class.java) {
      @Throws(IOException::class)
      override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
        if (value != null && value.isNotEmpty()) {
          super.encodeWithTag(writer, tag, value)
        }
      }

      override fun encodedSize(value: List<E>): Int {
        var size = 0
        var i = 0
        val count = value.size
        while (i < count) {
          size += this@ProtoAdapter.encodedSize(value[i])
          i++
        }
        return size
      }

      override fun encodedSizeWithTag(tag: Int, value: List<E>?): Int {
        return if (value == null || value.isEmpty()) 0 else super.encodedSizeWithTag(tag, value)
      }

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: List<E>) {
        var i = 0
        val count = value.size
        while (i < count) {
          this@ProtoAdapter.encode(writer, value[i])
          i++
        }
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): List<E> = listOf(this@ProtoAdapter.decode(reader))

      override fun redact(value: List<E>): List<E>? = emptyList()
    }
  }

  private fun createRepeated(): ProtoAdapter<List<E>> {
    return object : ProtoAdapter<List<E>>(fieldEncoding, List::class.java) {
      override fun encodedSize(value: List<E>): Int {
        throw UnsupportedOperationException("Repeated values can only be sized with a tag.")
      }

      override fun encodedSizeWithTag(tag: Int, value: List<E>?): Int {
        if (value == null) return 0
        var size = 0
        var i = 0
        val count = value.size
        while (i < count) {
          size += this@ProtoAdapter.encodedSizeWithTag(tag, value[i])
          i++
        }
        return size
      }

      override fun encode(writer: ProtoWriter, value: List<E>) {
        throw UnsupportedOperationException("Repeated values can only be encoded with a tag.")
      }

      @Throws(IOException::class)
      override fun encodeWithTag(writer: ProtoWriter, tag: Int, value: List<E>?) {
        if (value == null) return
        var i = 0
        val count = value.size
        while (i < count) {
          this@ProtoAdapter.encodeWithTag(writer, tag, value[i])
          i++
        }
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): List<E> = listOf(this@ProtoAdapter.decode(reader))

      override fun redact(value: List<E>): List<E>? = emptyList()
    }
  }

  class EnumConstantNotFoundException(
    @JvmField val value: Int,
    type: Class<*>?
  ) : IllegalArgumentException("Unknown enum tag $value for ${type?.canonicalName}")

  private class MapProtoAdapter<K, V> internal constructor(
    keyAdapter: ProtoAdapter<K>,
    valueAdapter: ProtoAdapter<V>
  ) : ProtoAdapter<Map<K, V>>(FieldEncoding.LENGTH_DELIMITED, Map::class.java) {
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
      var tag: Int
      while (true) {
        tag = reader.nextTag()
        if (tag == -1) break
        when (tag) {
          1 -> key = entryAdapter.keyAdapter.decode(reader)
          2 -> value = entryAdapter.valueAdapter.decode(reader)
          // Ignore unknown tags in map entries.
        }
      }
      reader.endMessage(token)

      check(key != null) { "Map entry with null key" }
      check(value != null) { "Map entry with null value" }
      return mapOf(key to value)
    }

    override fun redact(value: Map<K, V>): Map<K, V> = emptyMap()
  }

  private class MapEntryProtoAdapter<K, V> internal constructor(
    internal val keyAdapter: ProtoAdapter<K>,
    internal val valueAdapter: ProtoAdapter<V>
  ) : ProtoAdapter<Map.Entry<K, V>>(FieldEncoding.LENGTH_DELIMITED, Map.Entry::class.java) {

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
  }

  companion object {
    private const val FIXED_BOOL_SIZE = 1
    private const val FIXED_32_SIZE = 4
    private const val FIXED_64_SIZE = 8

    /** Creates a new proto adapter for `type`. */
    @JvmStatic
    fun <M : Message<M, B>, B : Builder<M, B>> newMessageAdapter(type: Class<M>): ProtoAdapter<M> {
      return RuntimeMessageAdapter.create(type)
    }

    /** Creates a new proto adapter for `type`.  */
    @JvmStatic fun <E : WireEnum> newEnumAdapter(type: Class<E>): EnumAdapter<E> {
      return RuntimeEnumAdapter(type)
    }

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
    ): ProtoAdapter<Map<K, V>> {
      return MapProtoAdapter(keyAdapter, valueAdapter)
    }

    /** Returns the adapter for the type of `Message`. */
    @JvmStatic fun <M : Message<*, *>> get(message: M): ProtoAdapter<M> = get(message.javaClass)

    /** Returns the adapter for `type`. */
    @JvmStatic fun <M> get(type: Class<M>): ProtoAdapter<M> {
      try {
        return type.getField("ADAPTER").get(null) as ProtoAdapter<M>
      } catch (e: IllegalAccessException) {
        throw IllegalArgumentException("failed to access ${type.name}#ADAPTER", e)
      } catch (e: NoSuchFieldException) {
        throw IllegalArgumentException("failed to access ${type.name}#ADAPTER", e)
      }
    }

    /**
     * Returns the adapter for a given `adapterString`. `adapterString` is specified on a proto
     * message field's [WireField] annotation in the form
     * `com.squareup.wire.protos.person.Person#ADAPTER`.
     */
    @JvmStatic fun get(adapterString: String): ProtoAdapter<*> {
      try {
        val hash = adapterString.indexOf('#')
        val className = adapterString.substring(0, hash)
        val fieldName = adapterString.substring(hash + 1)
        return Class.forName(className).getField(fieldName).get(null) as ProtoAdapter<Any>
      } catch (e: IllegalAccessException) {
        throw IllegalArgumentException("failed to access $adapterString", e)
      } catch (e: NoSuchFieldException) {
        throw IllegalArgumentException("failed to access $adapterString", e)
      } catch (e: ClassNotFoundException) {
        throw IllegalArgumentException("failed to access $adapterString", e)
      }
    }

    @JvmField val BOOL: ProtoAdapter<Boolean> = object : ProtoAdapter<Boolean>(
        FieldEncoding.VARINT,
        Boolean::class.javaObjectType
    ) {
      override fun encodedSize(value: Boolean): Int = FIXED_BOOL_SIZE

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Boolean) {
        writer.writeVarint32(if (value) 1 else 0)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Boolean {
        val value = reader.readVarint32()
        if (value == 0) return false
        if (value == 1) return true
        throw IOException(String.format("Invalid boolean value 0x%02x", value))
      }
    }
    @JvmField val INT32: ProtoAdapter<Int> = object : ProtoAdapter<Int>(
        FieldEncoding.VARINT,
        Int::class.javaObjectType
    ) {
      override fun encodedSize(value: Int): Int = int32Size(value)

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Int) {
        writer.writeSignedVarint32(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Int = reader.readVarint32()
    }
    @JvmField val UINT32: ProtoAdapter<Int> = object : ProtoAdapter<Int>(
        FieldEncoding.VARINT,
        Int::class.javaObjectType
    ) {
      override fun encodedSize(value: Int): Int = varint32Size(value)

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Int) {
        writer.writeVarint32(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Int = reader.readVarint32()
    }
    @JvmField val SINT32: ProtoAdapter<Int> = object : ProtoAdapter<Int>(
        FieldEncoding.VARINT,
        Int::class.javaObjectType
    ) {
      override fun encodedSize(value: Int): Int = varint32Size(encodeZigZag32(value))

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Int) {
        writer.writeVarint32(encodeZigZag32(value))
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Int = decodeZigZag32(reader.readVarint32())
    }
    @JvmField val FIXED32: ProtoAdapter<Int> = object : ProtoAdapter<Int>(
        FieldEncoding.FIXED32,
        Int::class.javaObjectType
    ) {
      override fun encodedSize(value: Int): Int = FIXED_32_SIZE

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Int) {
        writer.writeFixed32(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Int = reader.readFixed32()
    }
    @JvmField val SFIXED32 = FIXED32
    @JvmField val INT64: ProtoAdapter<Long> = object : ProtoAdapter<Long>(
        FieldEncoding.VARINT,
        Long::class.javaObjectType
    ) {
      override fun encodedSize(value: Long): Int = varint64Size(value)

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Long) {
        writer.writeVarint64(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Long = reader.readVarint64()
    }
    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    @JvmField val UINT64: ProtoAdapter<Long> = object : ProtoAdapter<Long>(
        FieldEncoding.VARINT,
        Long::class.javaObjectType
    ) {
      override fun encodedSize(value: Long): Int = varint64Size(value)

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Long) {
        writer.writeVarint64(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Long = reader.readVarint64()
    }
    @JvmField val SINT64: ProtoAdapter<Long> = object : ProtoAdapter<Long>(
        FieldEncoding.VARINT,
        Long::class.javaObjectType
    ) {
      override fun encodedSize(value: Long): Int = varint64Size(encodeZigZag64(value))

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Long) {
        writer.writeVarint64(encodeZigZag64(value))
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Long = decodeZigZag64(reader.readVarint64())
    }
    @JvmField val FIXED64: ProtoAdapter<Long> = object : ProtoAdapter<Long>(
        FieldEncoding.FIXED64,
        Long::class.javaObjectType
    ) {
      override fun encodedSize(value: Long): Int = FIXED_64_SIZE

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Long) {
        writer.writeFixed64(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Long = reader.readFixed64()
    }
    @JvmField val SFIXED64 = FIXED64
    @JvmField val FLOAT: ProtoAdapter<Float> = object : ProtoAdapter<Float>(
        FieldEncoding.FIXED32,
        Float::class.javaObjectType
    ) {
      override fun encodedSize(value: Float): Int = FIXED_32_SIZE

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Float) {
        writer.writeFixed32(floatToIntBits(value))
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Float {
        return java.lang.Float.intBitsToFloat(reader.readFixed32())
      }
    }
    @JvmField val DOUBLE: ProtoAdapter<Double> = object : ProtoAdapter<Double>(
        FieldEncoding.FIXED64,
        Double::class.javaObjectType
    ) {
      override fun encodedSize(value: Double): Int = FIXED_64_SIZE

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: Double) {
        writer.writeFixed64(doubleToLongBits(value))
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): Double {
        return java.lang.Double.longBitsToDouble(reader.readFixed64())
      }
    }
    @JvmField val STRING: ProtoAdapter<String> = object : ProtoAdapter<String>(
        FieldEncoding.LENGTH_DELIMITED,
        String::class.java
    ) {
      override fun encodedSize(value: String): Int = value.utf8Size().toInt()

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: String) {
        writer.writeString(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): String = reader.readString()
    }
    @JvmField val BYTES: ProtoAdapter<ByteString> = object : ProtoAdapter<ByteString>(
        FieldEncoding.LENGTH_DELIMITED,
        ByteString::class.java
    ) {
      override fun encodedSize(value: ByteString): Int = value.size

      @Throws(IOException::class)
      override fun encode(writer: ProtoWriter, value: ByteString) {
        writer.writeBytes(value)
      }

      @Throws(IOException::class)
      override fun decode(reader: ProtoReader): ByteString = reader.readBytes()
    }
  }
}
