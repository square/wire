/*
 * Copyright 2013 Square Inc.
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

package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter
import java.util.Collections

// Methods for generated code use only. Not subject to public API rules.

fun <T> newMutableList(): MutableList<T> = MutableOnWriteList(Collections.emptyList())

fun <K, V> newMutableMap(): MutableMap<K, V> = LinkedHashMap()

fun <T> copyOf(name: String, list: List<T>?): MutableList<T> {
  if (list == null) throw NullPointerException("$name == null")
  return if (list === Collections.emptyList<T>() || list is ImmutableList<*>) {
    MutableOnWriteList(list)
  } else {
    ArrayList(list)
  }
}

fun <K, V> copyOf(name: String, map: Map<K, V>?): MutableMap<K, V> {
  if (map == null) throw NullPointerException("$name == null")
  return LinkedHashMap(map)
}

fun <T> immutableCopyOf(name: String, list: List<T>?): List<T> {
  var list = list ?: throw NullPointerException("$name == null")
  if (list is MutableOnWriteList<*>) {
    list = (list as MutableOnWriteList<T>).mutableList
  }
  if (list === Collections.emptyList<T>() || list is ImmutableList<*>) {
    return list
  }
  val result = ImmutableList(list)
  // Check after the list has been copied to defend against races.
  require(null !in result) { "$name.contains(null)" }
  return result
}

fun <K, V> immutableCopyOf(name: String, map: Map<K, V>?): Map<K, V> {
  if (map == null) throw NullPointerException("$name == null")
  if (map.isEmpty()) {
    return emptyMap()
  }
  val result = LinkedHashMap(map)
  // Check after the map has been copied to defend against races.
  require(null !in result.keys) { "$name.containsKey(null)" }
  require(null !in result.values) { "$name.containsValue(null)" }
  return Collections.unmodifiableMap(result)
}

// TODO(egorand)
//
// Redacting doesn't work as-is in Kotlin, because:
// - repeated fields are generated as List<T>
// - redactElements() needs a MutableList<T> in order to modify its elements
// - ProtoAdapter.redact() returns T?
// - repeated fields don't accept nullable values
//
// Fixing this will require the following changes:
// - Change ProtoAdapter.redact() to return T instead of T?
// - Default implementation of ProtoAdapter.redact() should throw UnsupportedOperationException
// instead of returning null
// - Introduce a new version of redactElements() that is an extension function on List<T> and
// creates a copy of the list instead of modifying it
// - Hide the new redactElements() from Java using JvmName

fun <T> redactElements(list: java.util.List<T>, adapter: ProtoAdapter<T>) {
  for (i in 0 until list.size) {
    list.set(i, adapter.redact(list[i]))
  }
}

@JvmName("-redactElements")
fun <T> redactElements(list: List<T>, adapter: ProtoAdapter<T>) {
  redactElements(list as java.util.List<T>, adapter)
}

fun <T> redactElements(map: java.util.Map<*, T>, adapter: ProtoAdapter<T>) {
  for (entry in map.entrySet()) {
    entry.setValue(adapter.redact(entry.value))
  }
}

@JvmName("-redactElements")
fun <T> redactElements(map: Map<*, T>, adapter: ProtoAdapter<T>) {
  redactElements(map as java.util.Map<*, T>, adapter)
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

/** Throw [NullPointerException] if `list` or one of its items are null. */
fun checkElementsNotNull(list: List<*>?) {
  if (list == null) throw NullPointerException("list == null")
  for (i in 0 until list.size) {
    if (list[i] == null) {
      throw NullPointerException("Element at index $i is null")
    }
  }
}

/** Throw [NullPointerException] if `map` or one of its keys or values are null. */
fun checkElementsNotNull(map: Map<*, *>?) {
  if (map == null) throw NullPointerException("map == null")
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
