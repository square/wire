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

import com.squareup.wire.internal.RuntimeMessageAdapter
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.buffer
import okio.sink
import okio.source
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

actual abstract class ProtoAdapter<E> actual constructor(
  internal actual val fieldEncoding: FieldEncoding,
  actual val type: KClass<*>?
) {
  internal actual var packedAdapter: ProtoAdapter<List<E>>? = null
  internal actual var repeatedAdapter: ProtoAdapter<List<E>>? = null

  constructor(fieldEncoding: FieldEncoding, type: Class<*>): this(fieldEncoding, type.kotlin)

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
  @Throws(IOException::class)
  actual abstract fun encode(writer: ProtoWriter, value: E)

  /** Write `tag` and `value` to `writer`. If value is null this does nothing. */
  @Throws(IOException::class)
  actual open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
    commonEncodeWithTag(writer, tag, value)
  }

  /** Encode `value` and write it to `stream`. */
  @Throws(IOException::class)
  actual fun encode(sink: BufferedSink, value: E) {
    commonEncode(sink, value)
  }

  /** Encode `value` as a `byte[]`. */
  actual fun encode(value: E): ByteArray {
    return commonEncode(value)
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
  actual abstract fun decode(reader: ProtoReader): E

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  actual fun decode(bytes: ByteArray): E {
    return commonDecode(bytes)
  }

  /** Read an encoded message from `bytes`. */
  @Throws(IOException::class)
  actual fun decode(bytes: ByteString): E {
    return commonDecode(bytes)
  }

  /** Read an encoded message from `source`. */
  @Throws(IOException::class)
  actual fun decode(source: BufferedSource): E {
    return commonDecode(source)
  }

  /** Read an encoded message from `stream`. */
  @Throws(IOException::class)
  fun decode(stream: InputStream): E = decode(stream.source().buffer())

  /** Returns a human-readable version of the given `value`. */
  actual open fun toString(value: E): String {
    return commonToString(value)
  }

  internal actual fun withLabel(label: WireField.Label): ProtoAdapter<*> {
    return commonWithLabel(label)
  }

  /** Returns an adapter for `E` but as a packed, repeated value. */
  actual fun asPacked(): ProtoAdapter<List<E>> {
    return commonAsPacked()
  }

  /**
   * Returns an adapter for `E` but as a repeated value.
   *
   * Note: Repeated items are not required to be encoded sequentially. Thus, when decoding using
   * the returned adapter, only single-element lists will be returned and it is the caller's
   * responsibility to merge them into the final list.
   */
  actual fun asRepeated(): ProtoAdapter<List<E>> {
    return commonAsRepeated()
  }

  actual class EnumConstantNotFoundException actual constructor(
    @JvmField actual val value: Int,
    type: KClass<*>?
  ) : IllegalArgumentException("Unknown enum tag $value for ${type?.simpleName}") {
    constructor(value: Int, type: Class<*>): this(value, type.kotlin)
  }

  actual companion object {
    /**
     * Creates a new proto adapter for a map using `keyAdapter` and `valueAdapter`.
     *
     * Note: Map entries are not required to be encoded sequentially. Thus, when decoding using
     * the returned adapter, only single-element maps will be returned and it is the caller's
     * responsibility to merge them into the final map.
     */
    @JvmStatic actual fun <K, V> newMapAdapter(
      keyAdapter: ProtoAdapter<K>,
      valueAdapter: ProtoAdapter<V>
    ): ProtoAdapter<Map<K, V>> {
      return commonNewMapAdapter(keyAdapter, valueAdapter)
    }

    /** Creates a new proto adapter for `type`. */
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>
    ): ProtoAdapter<M> {
      return RuntimeMessageAdapter.create(type)
    }

    /** Creates a new proto adapter for `type`. */
    @JvmStatic fun <E : WireEnum> newEnumAdapter(type: Class<E>): EnumAdapter<E> {
      return RuntimeEnumAdapter(type)
    }

    /** Returns the adapter for the type of `Message`. */
    @JvmStatic fun <M : Message<*, *>> get(message: M): ProtoAdapter<M> {
      return get(message.javaClass)
    }

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

    @JvmField actual val BOOL: ProtoAdapter<Boolean> = COMMON_BOOL
    @JvmField actual val INT32: ProtoAdapter<Int> = COMMON_INT32
    @JvmField actual val UINT32: ProtoAdapter<Int> = COMMON_UINT32
    @JvmField actual val SINT32: ProtoAdapter<Int> = COMMON_SINT32
    @JvmField actual val FIXED32: ProtoAdapter<Int> = COMMON_FIXED32
    @JvmField actual val SFIXED32: ProtoAdapter<Int> = COMMON_SFIXED32
    @JvmField actual val INT64: ProtoAdapter<Long> = COMMON_INT64
    /**
     * Like INT64, but negative longs are interpreted as large positive values, and encoded that way
     * in JSON.
     */
    @JvmField actual val UINT64: ProtoAdapter<Long> = COMMON_UINT64
    @JvmField actual val SINT64: ProtoAdapter<Long> = COMMON_SINT64
    @JvmField actual val FIXED64: ProtoAdapter<Long> = COMMON_FIXED64
    @JvmField actual val SFIXED64: ProtoAdapter<Long> = COMMON_SFIXED64
    @JvmField actual val FLOAT: ProtoAdapter<Float> = COMMON_FLOAT
    @JvmField actual val DOUBLE: ProtoAdapter<Double> = COMMON_DOUBLE
    @JvmField actual val BYTES: ProtoAdapter<ByteString> = COMMON_BYTES
    @JvmField actual val STRING: ProtoAdapter<String> = COMMON_STRING
  }
}
