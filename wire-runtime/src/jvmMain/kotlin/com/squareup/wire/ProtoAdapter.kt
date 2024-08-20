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
@file:Suppress("LeakingThis")

package com.squareup.wire

import com.squareup.wire.internal.createRuntimeMessageAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.buffer
import okio.sink
import okio.source

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

  // Obsolete; for Java classes generated before typeUrl was added.
  constructor(fieldEncoding: FieldEncoding, type: Class<*>) : this(fieldEncoding, type.kotlin)

  // Obsolete; for Java classes generated before syntax was added.
  constructor(fieldEncoding: FieldEncoding, type: Class<*>, typeUrl: String?) :
    this(fieldEncoding, type.kotlin, typeUrl, Syntax.PROTO_2)

  // Obsolete; for Java classes generated before identity was added.
  constructor(fieldEncoding: FieldEncoding, type: Class<*>, typeUrl: String?, syntax: Syntax) :
    this(fieldEncoding, type.kotlin, typeUrl, syntax)

  // Obsolete; for Java classes generated before sourceFile was added.
  constructor(fieldEncoding: FieldEncoding, type: Class<*>, typeUrl: String?, syntax: Syntax, identity: E?) :
    this(fieldEncoding, type.kotlin, typeUrl, syntax, identity, null)

  // Obsolete; for Kotlin classes generated before typeUrl was added.
  constructor(fieldEncoding: FieldEncoding, type: KClass<*>?) :
    this(fieldEncoding, type, null, Syntax.PROTO_2)

  // Obsolete; for Kotlin classes generated before syntax was added.
  constructor(fieldEncoding: FieldEncoding, type: KClass<*>?, typeUrl: String?) :
    this(fieldEncoding, type, typeUrl, Syntax.PROTO_2)

  // Obsolete; for Kotlin classes generated before identity was added.
  constructor(fieldEncoding: FieldEncoding, type: KClass<*>?, typeUrl: String?, syntax: Syntax) :
    this(fieldEncoding, type, typeUrl, syntax, null)

  // Obsolete; for Kotlin classes generated before sourceFile was added.
  constructor(
    fieldEncoding: FieldEncoding,
    type: KClass<*>?,
    typeUrl: String?,
    syntax: Syntax,
    identity: E?,
  ) :
    this(fieldEncoding, type, typeUrl, syntax, identity, null)

  constructor(
    fieldEncoding: FieldEncoding,
    type: Class<*>,
    typeUrl: String?,
    syntax: Syntax,
    identity: E?,
    sourceFile: String?,
  ) : this(fieldEncoding, type.kotlin, typeUrl, syntax, identity, sourceFile)

  actual abstract fun redact(value: E): E

  actual abstract fun encodedSize(value: E): Int

  actual open fun encodedSizeWithTag(tag: Int, value: E?): Int {
    return commonEncodedSizeWithTag(tag, value)
  }

  @Throws(IOException::class)
  actual abstract fun encode(writer: ProtoWriter, value: E)

  @Throws(IOException::class)
  actual open fun encode(writer: ReverseProtoWriter, value: E) {
    delegateEncode(writer, value)
  }

  @Throws(IOException::class)
  actual open fun encodeWithTag(writer: ProtoWriter, tag: Int, value: E?) {
    commonEncodeWithTag(writer, tag, value)
  }

  @Throws(IOException::class)
  actual open fun encodeWithTag(writer: ReverseProtoWriter, tag: Int, value: E?) {
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

  /** Read a non-null value from `reader`. */
  @Throws(IOException::class)
  actual open fun decode(reader: ProtoReader32): E {
    return decode(reader.asProtoReader())
  }

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
  actual fun tryDecode(reader: ProtoReader, destination: MutableList<E>) {
    return commonTryDecode(reader, destination)
  }

  @Throws(IOException::class)
  actual fun tryDecode(reader: ProtoReader32, destination: MutableList<E>) {
    return commonTryDecode(reader, destination)
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
      "Can't create a packed adapter from a packed or repeated adapter.",
    )
  }

  actual fun asRepeated(): ProtoAdapter<List<E>> {
    return repeatedAdapter ?: throw UnsupportedOperationException(
      "Can't create a repeated adapter from a repeated or packed adapter.",
    )
  }

  internal val isStruct: Boolean
    get() = this == STRUCT_MAP || this == STRUCT_LIST || this == STRUCT_VALUE || this == STRUCT_NULL

  actual class EnumConstantNotFoundException actual constructor(
    @JvmField actual val value: Int,
    type: KClass<*>?,
  ) : IllegalArgumentException("Unknown enum tag $value for ${type?.java?.name}") {
    constructor(value: Int, type: Class<*>) : this(value, type.kotlin)
  }

  actual companion object {
    @JvmStatic actual fun <K, V> newMapAdapter(
      keyAdapter: ProtoAdapter<K>,
      valueAdapter: ProtoAdapter<V>,
    ): ProtoAdapter<Map<K, V>> {
      return commonNewMapAdapter(keyAdapter, valueAdapter)
    }

    // Obsolete; for Java classes generated before typeUrl and syntax were added.
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>,
    ): ProtoAdapter<M> {
      return createRuntimeMessageAdapter(type, null, Syntax.PROTO_2)
    }

    // Obsolete; for Java classes generated before typeUrl and syntax were added.
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>,
      typeUrl: String,
    ): ProtoAdapter<M> {
      return createRuntimeMessageAdapter(type, typeUrl, Syntax.PROTO_2)
    }

    // Obsolete; for Java classes generated before `classLoader` was added.
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>,
      typeUrl: String,
      syntax: Syntax,
    ): ProtoAdapter<M> {
      return createRuntimeMessageAdapter(type, typeUrl, syntax)
    }

    /** Creates a new proto adapter for `type`. */
    @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
      type: Class<M>,
      typeUrl: String,
      syntax: Syntax,
      classLoader: ClassLoader?,
    ): ProtoAdapter<M> {
      return createRuntimeMessageAdapter(type, typeUrl, syntax, classLoader)
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
        @Suppress("UNCHECKED_CAST")
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
      return get(adapterString, ProtoAdapter::class.java.classLoader)
    }

    /**
     * Returns the adapter for a given `adapterString`, using [classLoader]. `adapterString` is specified on a
     * proto message field's [WireField] annotation in the form `com.squareup.wire.protos.person.Person#ADAPTER`.
     */
    @JvmStatic fun get(adapterString: String, classLoader: ClassLoader?): ProtoAdapter<*> {
      try {
        val hash = adapterString.indexOf('#')
        val className = adapterString.substring(0, hash)
        val fieldName = adapterString.substring(hash + 1)
        @Suppress("UNCHECKED_CAST")
        return Class.forName(className, true, classLoader).getField(fieldName).get(null) as ProtoAdapter<Any>
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

    @JvmField actual val INT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(INT32)

    @JvmField actual val UINT32: ProtoAdapter<Int> = commonUint32()

    @JvmField actual val UINT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(UINT32)

    @JvmField actual val SINT32: ProtoAdapter<Int> = commonSint32()

    @JvmField actual val SINT32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(SINT32)

    @JvmField actual val FIXED32: ProtoAdapter<Int> = commonFixed32()

    @JvmField actual val FIXED32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(FIXED32)

    @JvmField actual val SFIXED32: ProtoAdapter<Int> = commonSfixed32()

    @JvmField actual val SFIXED32_ARRAY: ProtoAdapter<IntArray> = IntArrayProtoAdapter(SFIXED32)

    @JvmField actual val INT64: ProtoAdapter<Long> = commonInt64()

    @JvmField actual val INT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(INT64)

    @JvmField actual val UINT64: ProtoAdapter<Long> = commonUint64()

    @JvmField actual val UINT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(UINT64)

    @JvmField actual val SINT64: ProtoAdapter<Long> = commonSint64()

    @JvmField actual val SINT64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(SINT64)

    @JvmField actual val FIXED64: ProtoAdapter<Long> = commonFixed64()

    @JvmField actual val FIXED64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(FIXED64)

    @JvmField actual val SFIXED64: ProtoAdapter<Long> = commonSfixed64()

    @JvmField actual val SFIXED64_ARRAY: ProtoAdapter<LongArray> = LongArrayProtoAdapter(SFIXED64)

    @JvmField actual val FLOAT: ProtoAdapter<Float> = commonFloat()

    @JvmField actual val FLOAT_ARRAY: ProtoAdapter<FloatArray> = FloatArrayProtoAdapter(FLOAT)

    @JvmField actual val DOUBLE: ProtoAdapter<Double> = commonDouble()

    @JvmField actual val DOUBLE_ARRAY: ProtoAdapter<DoubleArray> = DoubleArrayProtoAdapter(DOUBLE)

    @JvmField actual val BYTES: ProtoAdapter<ByteString> = commonBytes()

    @JvmField actual val STRING: ProtoAdapter<String> = commonString()

    @JvmField actual val EMPTY: ProtoAdapter<Unit> = commonEmpty()

    @JvmField actual val STRUCT_MAP: ProtoAdapter<Map<String, *>?> = commonStructMap()

    @JvmField actual val STRUCT_LIST: ProtoAdapter<List<*>?> = commonStructList()

    @JvmField actual val STRUCT_NULL: ProtoAdapter<Nothing?> = commonStructNull()

    @JvmField actual val STRUCT_VALUE: ProtoAdapter<Any?> = commonStructValue()

    @JvmField actual val DOUBLE_VALUE: ProtoAdapter<Double?> = commonWrapper(DOUBLE, "type.googleapis.com/google.protobuf.DoubleValue")

    @JvmField actual val FLOAT_VALUE: ProtoAdapter<Float?> = commonWrapper(FLOAT, "type.googleapis.com/google.protobuf.FloatValue")

    @JvmField actual val INT64_VALUE: ProtoAdapter<Long?> = commonWrapper(INT64, "type.googleapis.com/google.protobuf.Int64Value")

    @JvmField actual val UINT64_VALUE: ProtoAdapter<Long?> = commonWrapper(UINT64, "type.googleapis.com/google.protobuf.UInt64Value")

    @JvmField actual val INT32_VALUE: ProtoAdapter<Int?> = commonWrapper(INT32, "type.googleapis.com/google.protobuf.Int32Value")

    @JvmField actual val UINT32_VALUE: ProtoAdapter<Int?> = commonWrapper(UINT32, "type.googleapis.com/google.protobuf.UInt32Value")

    @JvmField actual val BOOL_VALUE: ProtoAdapter<Boolean?> = commonWrapper(BOOL, "type.googleapis.com/google.protobuf.BoolValue")

    @JvmField actual val STRING_VALUE: ProtoAdapter<String?> = commonWrapper(STRING, "type.googleapis.com/google.protobuf.StringValue")

    @JvmField actual val BYTES_VALUE: ProtoAdapter<ByteString?> = commonWrapper(BYTES, "type.googleapis.com/google.protobuf.BytesValue")

    @JvmField actual val DURATION: ProtoAdapter<Duration> = try {
      commonDuration()
    } catch (_: NoClassDefFoundError) {
      @Suppress("UNCHECKED_CAST")
      UnsupportedTypeProtoAdapter() as ProtoAdapter<Duration>
    }

    @JvmField actual val INSTANT: ProtoAdapter<Instant> = try {
      commonInstant()
    } catch (_: NoClassDefFoundError) {
      @Suppress("UNCHECKED_CAST")
      UnsupportedTypeProtoAdapter() as ProtoAdapter<Instant>
    }

    /**
     * Stub [ProtoAdapter] for Wire types which are typeliased to `java.time` types on the JVM
     * such as [Duration] and [Instant]. This proto adapter is used when the corresponding
     * `java.time` type is missing from the JVM classpath.
     */
    class UnsupportedTypeProtoAdapter : ProtoAdapter<Nothing>(
      FieldEncoding.LENGTH_DELIMITED,
      Nothing::class,
    ) {
      override fun redact(value: Nothing) =
        throw IllegalStateException("Operation not supported.")
      override fun encodedSize(value: Nothing) =
        throw IllegalStateException("Operation not supported.")
      override fun encode(writer: ProtoWriter, value: Nothing) =
        throw IllegalStateException("Operation not supported.")
      override fun encode(writer: ReverseProtoWriter, value: Nothing) =
        throw IllegalStateException("Operation not supported.")
      override fun decode(reader: ProtoReader): Nothing =
        throw IllegalStateException("Operation not supported.")
      override fun decode(reader: ProtoReader32): Nothing =
        throw IllegalStateException("Operation not supported.")
    }
  }
}
