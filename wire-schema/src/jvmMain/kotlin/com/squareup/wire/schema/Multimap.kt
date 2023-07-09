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

import com.google.common.collect.LinkedHashMultimap

actual typealias Multimap<K, V> = com.google.common.collect.Multimap<K, V>

internal actual fun <K, V> Map<K, Collection<V>>.toMultimap(): Multimap<K, V> {
  val multimap = LinkedHashMultimap.create<K, V>()
  forEach { key, value ->
    multimap.putAll(key, value)
  }
  return multimap
}
