/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.schema

actual interface Multimap<K, V> {
  actual fun size(): Int
  actual fun isEmpty(): Boolean
  actual fun containsKey(key: Any?): Boolean
  actual fun containsValue(value: Any?): Boolean
  actual operator fun get(key: K?): Collection<V>
  actual fun values(): Collection<V>
  actual fun asMap(): Map<K, Collection<V>>
}

private class SimpleMultimap<K, V>(private val map: Map<K, Collection<V>>) : Multimap<K, V> {
  override fun size() = map.values.sumOf { it.size }
  override fun isEmpty() = map.isEmpty()
  override fun containsKey(key: Any?) = map.containsKey(key)
  override fun containsValue(value: Any?) = map.values.any { it.contains(value) }
  override fun get(key: K?) = map[key] ?: emptyList()
  override fun values() = map.values.flatten()
  override fun asMap() = map
}

internal actual fun <K, V> Map<K, Collection<V>>.toMultimap(): Multimap<K, V> {
  return SimpleMultimap(this)
}
