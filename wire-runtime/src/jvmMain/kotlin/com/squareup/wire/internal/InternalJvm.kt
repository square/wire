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
@file:JvmMultifileClass

package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter

// Methods for generated code use only. Not subject to public API rules.

fun <T> redactElements(list: java.util.List<T>, adapter: ProtoAdapter<T>) {
  for (i in 0 until list.size) {
    list[i] = adapter.redact(list[i])
  }
}

fun <T> redactElements(map: java.util.Map<*, T>, adapter: ProtoAdapter<T>) {
  for (entry in map.entrySet()) {
    entry.setValue(adapter.redact(entry.value))
  }
}
