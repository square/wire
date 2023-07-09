/*
 * Copyright (C) 2020 Square, Inc.
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

expect interface Multimap<K, V> {
  fun size(): Int
  fun isEmpty(): Boolean
  fun containsKey(key: Any?): Boolean
  fun containsValue(value: Any?): Boolean

  operator fun get(key: K?): Collection<V>
  fun values(): Collection<V>
  fun asMap(): Map<K, Collection<V>>
}

internal expect fun <K, V> Map<K, Collection<V>>.toMultimap(): Multimap<K, V>
