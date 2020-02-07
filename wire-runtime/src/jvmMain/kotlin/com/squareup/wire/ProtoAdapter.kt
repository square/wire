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

  // Obsolete; for Java classes generated before typeUrl was added.
  constructor(fieldEncoding: FieldEncoding, type: Class<*>) : this(fieldEncoding, type.kotlin)

  // Obsolete; for Kotlin classes generated before typeUrl was added.
  constructor(fieldEncoding: FieldEncoding, type: KClass<*>?) : this(fieldEncoding, type, null)

  constructor(fieldEncoding: FieldEncoding, type: Class<*>, typeUrl: String?) :
      this(fieldEncoding, type.kotlin, typeUrl)

  actual abstract fun redact(value: E): E

  actual abstract fun encodedSize(value: E): Int

  actual open fun encodedSizeWithTag(tag: Int, value: E?): Int {
    return commonEncodedSizeWithTag(tag, value)
  }

  @Throws(IOException::class)
  actual abstract fun encode(writer: ProtoWriter, value: E)

  @Throws(IOException::class)
  actual open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
    commonEncodeWithTag(writer, tag, value)
  }

  @Throws(IOException::class)
  actual fun encode(sink: BufferedSink, value: E) {
    commonEncode(sink, value)
  }

  actual fun encode(value: E): ByteArray {
    return commonEncode(value)
  }

  actual fun encodeByteString(value: E): ByteString {
    return commonEncodeByteString(value)
  }

  @Throws(IOException::class)
  fun encode(stream: OutputStream, value: E) {
    val buffer = stream.sink().buffer()
    encode(buffer, value)
    buffer.emit()
  }

  @Throws(IOException::class)
  actual abstract fun decode(reader: ProtoReader): E

  @Throws(IOException::class)
  actual fun decode(bytes: ByteArray): E {
    return commonDecode(bytes)
  }

  @Throws(IOException::class)
  actual fun decode(bytes: ByteString): E {
    return commonDecode(bytes)
  }

  @Throws(IOException::class)
  actual fun decode(source: BufferedSource): E {
    return commonDecode(source)
  }

  @Throws(IOException::class)
  fun decode(stream: InputStream): E = decode(stream.source().buffer())

  actual open fun toString(value: E): String {
    return commonToString(value)
  }

  internal actual fun withLabel(label: WireField.Label): ProtoAdapter<*> {
    return commonWithLabel(label)
  }

  actual fun asPacked(): ProtoAdapter<List<E>> {
    require(fieldEncoding != FieldEncoding.LENGTH_DELIMITED) {
      "Unable to pack a length-delimited type."
    }
    return packedAdapter ?: throw UnsupportedOperationException(
        "Can't create a packed adapter from a packed or repeated adapter.")
  }

  actual fun asRepeated(): ProtoAdapter<List<E>> {
    return repeatedAdapter ?: throw UnsupportedOperationException(
        "Can't create a repeated adapter from a repeated or packed adapter.")
  }

  actual class EnumConstantNotFoundException actual constructor(
    @JvmField actual val value: Int,
    type: KClass<*>?
  ) : IllegalArgumentException("Unknown enum tag $value for ${type?.java?.name}") {
    constructor(value: Int, type: Class<*>): this(value, type.kotlin)
  }

  actual companion object {
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
      return RuntimeMessageAdapter.create(type, null)
    }

    /** Creates a new proto adapter for `type`. */
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>,
      typeUrl: String
    ): ProtoAdapter<M> {
      return RuntimeMessageAdapter.create(type, typeUrl)
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

    @JvmField actual val BOOL: ProtoAdapter<Boolean> = commonBool()
    @JvmField actual val INT32: ProtoAdapter<Int> = commonInt32()
    @JvmField actual val UINT32: ProtoAdapter<Int> = commonUint32()
    @JvmField actual val SINT32: ProtoAdapter<Int> = commonSint32()
    @JvmField actual val FIXED32: ProtoAdapter<Int> = commonFixed32()
    @JvmField actual val SFIXED32: ProtoAdapter<Int> = commonSfixed32()
    @JvmField actual val INT64: ProtoAdapter<Long> = commonInt64()
    @JvmField actual val UINT64: ProtoAdapter<Long> = commonUint64()
    @JvmField actual val SINT64: ProtoAdapter<Long> = commonSint64()
    @JvmField actual val FIXED64: ProtoAdapter<Long> = commonFixed64()
    @JvmField actual val SFIXED64: ProtoAdapter<Long> = commonSfixed64()
    @JvmField actual val FLOAT: ProtoAdapter<Float> = commonFloat()
    @JvmField actual val DOUBLE: ProtoAdapter<Double> = commonDouble()
    @JvmField actual val BYTES: ProtoAdapter<ByteString> = commonBytes()
    @JvmField actual val STRING: ProtoAdapter<String> = commonString()
  }
}
