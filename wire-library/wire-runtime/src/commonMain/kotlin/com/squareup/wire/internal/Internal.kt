/*
 * Copyright 2019 Square Inc.
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
@file:JvmName("Internal")
@file:JvmMultifileClass

package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// Methods for generated code use only. Not subject to public API rules.

fun <T> newMutableList(): MutableList<T> = MutableOnWriteList(emptyList())

fun <K, V> newMutableMap(): MutableMap<K, V> = LinkedHashMap()

@Deprecated(
    message = "Please regenerate code using wire-compiler version 3.0.0 or higher.",
    replaceWith = ReplaceWith("com.squareup.internal.Internal.copyOf(list)")
)
fun <T> copyOf(name: String, list: List<T>?): MutableList<T> = copyOf(list!!)

fun <T> copyOf(list: List<T>): MutableList<T> {
  return if (list === emptyList<T>() || list is ImmutableList<*>) {
    MutableOnWriteList(list)
  } else {
    ArrayList(list)
  }
}

@Deprecated(
    message = "Please regenerate code using wire-compiler version 3.0.0 or higher.",
    replaceWith = ReplaceWith("com.squareup.internal.Internal.copyOf(map)")
)
fun <K, V> copyOf(name: String, map: Map<K, V>?): MutableMap<K, V> = copyOf(map!!)

fun <K, V> copyOf(map: Map<K, V>): MutableMap<K, V> = LinkedHashMap(map)

fun <T> immutableCopyOf(name: String, list: List<T>): List<T> {
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
  return result as List<T>
}

fun <K, V> immutableCopyOf(name: String, map: Map<K?, V?>): Map<K, V> {
  if (map.isEmpty()) {
    return emptyMap()
  }
  val result = LinkedHashMap(map)
  // Check after the map has been copied to defend against races.
  require(null !in result.keys) { "$name.containsKey(null)" }
  require(null !in result.values) { "$name.containsValue(null)" }
  return (result as MutableMap<K, V>).toUnmodifiableMap()
}


@JvmName("-redactElements") // Hide from Java
fun <T> List<T>.redactElements(adapter: ProtoAdapter<T>): List<T> = map(adapter::redact)

@JvmName("-redactElements") // Hide from Java
fun <K, V> Map<K, V>.redactElements(adapter: ProtoAdapter<V>): Map<K, V> {
  return mapValues { (_, value) -> adapter.redact(value) }
}

fun equals(a: Any?, b: Any?): Boolean = a === b || (a != null && a == b)

/**
 * Create an exception for missing required fields.
 *
 * @param args Alternating field value and field name pairs.
 */
fun missingRequiredFields(vararg args: Any?): IllegalStateException {
  var plural = ""
  val fields = buildString {
    for (i in 0 until args.size step 2) {
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
  for (i in 0 until list.size) {
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
