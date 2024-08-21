/*
 * Copyright (C) 2019 Square, Inc.
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
@file:JvmName("Internal")
@file:JvmMultifileClass
@file:Suppress("FunctionName")

package com.squareup.wire.internal

import com.squareup.wire.Duration
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Instant
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoReader32
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// Methods for generated code use only. Not subject to public API rules.

fun <T> newMutableList(): MutableList<T> = MutableOnWriteList(emptyList())

fun <K, V> newMutableMap(): MutableMap<K, V> = LinkedHashMap()

@Deprecated(
  message = "Please regenerate code using wire-compiler version 3.0.0 or higher.",
  replaceWith = ReplaceWith("com.squareup.internal.Internal.copyOf(list)"),
)
fun <T> copyOf(@Suppress("UNUSED_PARAMETER") name: String, list: List<T>?): MutableList<T> = copyOf(list!!)

fun <T> copyOf(list: List<T>): MutableList<T> {
  return if (list === emptyList<T>() || list is ImmutableList<*>) {
    MutableOnWriteList(list)
  } else {
    ArrayList(list)
  }
}

@Deprecated(
  message = "Please regenerate code using wire-compiler version 3.0.0 or higher.",
  replaceWith = ReplaceWith("com.squareup.internal.Internal.copyOf(map)"),
)
fun <K, V> copyOf(@Suppress("UNUSED_PARAMETER") name: String, map: Map<K, V>?): MutableMap<K, V> = copyOf(map!!)

fun <K, V> copyOf(map: Map<K, V>): MutableMap<K, V> = LinkedHashMap(map)

fun <T> immutableCopyOf(name: String, list: List<T>): List<T> {
  @Suppress("NAME_SHADOWING")
  var list = list
  if (list is MutableOnWriteList<*>) {
    list = (list as MutableOnWriteList<T>).mutableList
  }
  if (list === emptyList<T>() || list is ImmutableList<*>) {
    return list
  }
  val result = ImmutableList(list)
  // Check after the list has been copied to defend against races.
  require(null !in result) { "$name.contains(null)" }
  @Suppress("UNCHECKED_CAST")
  return result as List<T>
}

fun <K, V> immutableCopyOf(name: String, map: Map<K, V>): Map<K, V> {
  if (map.isEmpty()) {
    return emptyMap()
  }
  val result = LinkedHashMap(map)
  // Check after the map has been copied to defend against races.
  require(null !in (result.keys as Collection<K?>)) { "$name.containsKey(null)" }
  require(null !in (result.values as Collection<V?>)) { "$name.containsValue(null)" }
  return result.toUnmodifiableMap()
}

/** Confirms the values of [map] are structs and returns an immutable copy. */
fun <K, V> immutableCopyOfMapWithStructValues(name: String, map: Map<K, V>): Map<K, V> {
  val copy = mutableMapOf<K, Any?>()
  for ((k, v) in map) {
    require(k != null) { "$name.containsKey(null)" }
    copy[k] = immutableCopyOfStruct(name, v)
  }
  @Suppress("UNCHECKED_CAST")
  return copy.toUnmodifiableMap() as Map<K, V>
}

/** Confirms [value] is a struct and returns an immutable copy. */
fun <T> immutableCopyOfStruct(name: String, value: T): T {
  return when (value) {
    null -> value
    is Boolean -> value
    is Double -> value
    is String -> value
    is List<*> -> {
      val copy = mutableListOf<Any?>()
      for (element in value) {
        copy += immutableCopyOfStruct(name, element)
      }
      @Suppress("UNCHECKED_CAST")
      copy.toUnmodifiableList() as T
    }
    is Map<*, *> -> {
      val copy = mutableMapOf<Any?, Any?>()
      for ((k, v) in value) {
        copy[immutableCopyOfStruct(name, k)] = immutableCopyOfStruct(name, v)
      }
      @Suppress("UNCHECKED_CAST")
      copy.toUnmodifiableMap() as T
    }
    else -> {
      throw IllegalArgumentException(
        "struct value $name must be a JSON type " +
          "(null, Boolean, Double, String, List, or Map) but was ${value.typeName}: $value",
      )
    }
  }
}

private val Any.typeName
  get() = this::class

@JvmName("-redactElements") // Hide from Java
fun <T> List<T>.redactElements(adapter: ProtoAdapter<T>): List<T> = map(adapter::redact)

@JvmName("-redactElements") // Hide from Java
fun <K, V> Map<K, V>.redactElements(adapter: ProtoAdapter<V>): Map<K, V> {
  return mapValues { (_, value) -> adapter.redact(value) }
}

@Suppress("SuspiciousEqualsCombination")
fun equals(a: Any?, b: Any?): Boolean = a === b || (a != null && a == b)

/**
 * Create an exception for missing required fields.
 *
 * @param args Alternating field value and field name pairs.
 */
fun missingRequiredFields(vararg args: Any?): IllegalStateException {
  var plural = ""
  val fields = buildString {
    for (i in args.indices step 2) {
      if (args[i] == null) {
        if (isNotEmpty()) {
          plural = "s" // Found more than one missing field
        }
        append("\n  ")
        append(args[i + 1])
      }
    }
  }
  throw IllegalStateException("Required field$plural not set:$fields")
}

/** Throw [NullPointerException] if any of `list`'s items is null. */
fun checkElementsNotNull(list: List<*>) {
  for (i in list.indices) {
    if (list[i] == null) {
      throw NullPointerException("Element at index $i is null")
    }
  }
}

/** Throw [NullPointerException] if any of`map`'s keys or values is null. */
fun checkElementsNotNull(map: Map<*, *>) {
  for ((key, value) in map) {
    if (key == null) {
      throw NullPointerException("map.containsKey(null)")
    }
    if (value == null) {
      throw NullPointerException("Value for key $key is null")
    }
  }
}

/** Returns the number of non-null values in `a, b`. */
fun countNonNull(a: Any?, b: Any?): Int = (if (a != null) 1 else 0) + (if (b != null) 1 else 0)

/** Returns the number of non-null values in `a, b, c`. */
fun countNonNull(a: Any?, b: Any?, c: Any?): Int {
  return (if (a != null) 1 else 0) + (if (b != null) 1 else 0) + (if (c != null) 1 else 0)
}

/** Returns the number of non-null values in `a, b, c, d, rest`. */
fun countNonNull(a: Any?, b: Any?, c: Any?, d: Any?, vararg rest: Any?): Int {
  var result = 0
  if (a != null) result++
  if (b != null) result++
  if (c != null) result++
  if (d != null) result++
  for (o in rest) {
    if (o != null) result++
  }
  return result
}

private const val ESCAPED_CHARS = ",[]{}\\"

/** Return a string where `,[]{}\` are escaped with a `\`. */
fun sanitize(value: String): String {
  return buildString(value.length) {
    value.forEach { char ->
      if (char in ESCAPED_CHARS) append('\\')
      append(char)
    }
  }
}

/** Return a string where `,[]{}\` are escaped with a `\`. */
fun sanitize(values: List<String>): String {
  return values.joinToString(prefix = "[", postfix = "]", transform = ::sanitize)
}

fun boxedOneOfClassName(oneOfName: String): String {
  return oneOfName.replaceFirstChar(Char::titlecase)
}

/**
 * Maps [oneOfName] and [fieldName] to the companion object key representing a boxed oneof field.
 */
fun boxedOneOfKeyFieldName(oneOfName: String, fieldName: String): String {
  return (oneOfName + "_" + fieldName).uppercase()
}

/** Maps [oneOfName] to the companion object field of type `Set` containing the eligible keys.  */
fun boxedOneOfKeysFieldName(oneOfName: String): String {
  return "${oneOfName}_keys".uppercase()
}

fun encodeArray_int32(array: IntArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeSignedVarint32(array[i])
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_uint32(array: IntArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeVarint32(array[i])
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_sint32(array: IntArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeVarint32(ProtoWriter.encodeZigZag32(array[i]))
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_fixed32(array: IntArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeFixed32(array[i])
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_sfixed32(array: IntArray, writer: ReverseProtoWriter, tag: Int) {
  return encodeArray_fixed32(array, writer, tag)
}

fun encodeArray_int64(array: LongArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeVarint64(array[i])
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_uint64(array: LongArray, writer: ReverseProtoWriter, tag: Int) =
  encodeArray_int64(array, writer, tag)

fun encodeArray_sint64(array: LongArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeVarint64(ProtoWriter.encodeZigZag64(array[i]))
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_fixed64(array: LongArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeFixed64(array[i])
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_sfixed64(array: LongArray, writer: ReverseProtoWriter, tag: Int) =
  encodeArray_fixed64(array, writer, tag)

fun encodeArray_float(array: FloatArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeFixed32(array[i].toBits())
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun encodeArray_double(array: DoubleArray, writer: ReverseProtoWriter, tag: Int) {
  if (array.isNotEmpty()) {
    val byteCountBefore = writer.byteCount
    for (i in (array.size - 1) downTo 0) {
      writer.writeFixed64(array[i].toBits())
    }
    writer.writeVarint32(writer.byteCount - byteCountBefore)
    writer.writeTag(tag, FieldEncoding.LENGTH_DELIMITED)
  }
}

fun decodePrimitive_double(reader: ProtoReader32): Double = Double.fromBits(reader.readFixed64())
fun decodePrimitive_double(reader: ProtoReader): Double = Double.fromBits(reader.readFixed64())
fun decodePrimitive_fixed32(reader: ProtoReader32): Int = reader.readFixed32()
fun decodePrimitive_fixed32(reader: ProtoReader): Int = reader.readFixed32()
fun decodePrimitive_fixed64(reader: ProtoReader32): Long = reader.readFixed64()
fun decodePrimitive_fixed64(reader: ProtoReader): Long = reader.readFixed64()
fun decodePrimitive_float(reader: ProtoReader32): Float = Float.fromBits(reader.readFixed32())
fun decodePrimitive_float(reader: ProtoReader): Float = Float.fromBits(reader.readFixed32())
fun decodePrimitive_int32(reader: ProtoReader32): Int = reader.readVarint32()
fun decodePrimitive_int32(reader: ProtoReader): Int = reader.readVarint32()
fun decodePrimitive_int64(reader: ProtoReader32): Long = reader.readVarint64()
fun decodePrimitive_int64(reader: ProtoReader): Long = reader.readVarint64()
fun decodePrimitive_sfixed32(reader: ProtoReader32): Int = reader.readFixed32()
fun decodePrimitive_sfixed32(reader: ProtoReader): Int = reader.readFixed32()
fun decodePrimitive_sfixed64(reader: ProtoReader32): Long = reader.readFixed64()
fun decodePrimitive_sfixed64(reader: ProtoReader): Long = reader.readFixed64()
fun decodePrimitive_sint32(reader: ProtoReader32): Int = ProtoWriter.decodeZigZag32(reader.readVarint32())
fun decodePrimitive_sint32(reader: ProtoReader): Int = ProtoWriter.decodeZigZag32(reader.readVarint32())
fun decodePrimitive_sint64(reader: ProtoReader32): Long = ProtoWriter.decodeZigZag64(reader.readVarint64())
fun decodePrimitive_sint64(reader: ProtoReader): Long = ProtoWriter.decodeZigZag64(reader.readVarint64())
fun decodePrimitive_uint32(reader: ProtoReader32): Int = reader.readVarint32()
fun decodePrimitive_uint32(reader: ProtoReader): Int = reader.readVarint32()
fun decodePrimitive_uint64(reader: ProtoReader32): Long = reader.readVarint64()
fun decodePrimitive_uint64(reader: ProtoReader): Long = reader.readVarint64()

internal fun Instant.commonEquals(other: Any?): Boolean {
  if (this === other) return true
  if (other == null || other !is Instant) return false

  if (getEpochSecond() != other.getEpochSecond()) return false
  if (getNano() != other.getNano()) return false

  return true
}

internal fun Instant.commonHashCode(): Int {
  var result = getEpochSecond().hashCode()
  result = 31 * result + getNano().hashCode()
  return result
}

internal fun Duration.commonEquals(other: Any?): Boolean {
  if (this === other) return true
  if (other == null || other !is Duration) return false

  if (getSeconds() != other.getSeconds()) return false
  if (getNano() != other.getNano()) return false

  return true
}

internal fun Duration.commonHashCode(): Int {
  var result = getSeconds().hashCode()
  result = 31 * result + getNano().hashCode()
  return result
}
