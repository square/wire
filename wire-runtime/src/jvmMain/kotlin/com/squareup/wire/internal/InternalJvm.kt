/*
 * Copyright (C) 2013 Square, Inc.
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

package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireEnum

// Methods for generated code use only. Not subject to public API rules.

fun <T> redactElements(list: MutableList<T>, adapter: ProtoAdapter<T>) {
  for (i in 0 until list.size) {
    list[i] = adapter.redact(list[i])
  }
}

fun <T> redactElements(map: MutableMap<*, T>, adapter: ProtoAdapter<T>) {
  for (entry in map.entries) {
    entry.setValue(adapter.redact(entry.value))
  }
}

/** The enum with constant 0. This is non-null for proto3 enum types. */
val <E : WireEnum> Class<E>.identityOrNull: E?
  get() = enumConstants.firstOrNull { (it as WireEnum).value == 0 }
