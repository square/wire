/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.ProtoAdapter

/**
 * Returns the adapter for one element if this is the adapter of a field that uses a primitive
 * array, like `ProtoAdapter.INT32_ARRAY`. Returns null for every other adapter.
 */
internal fun ProtoAdapter<*>.primitiveArrayElementAdapter(): ProtoAdapter<*>? = when (this) {
  ProtoAdapter.INT32_ARRAY -> ProtoAdapter.INT32
  ProtoAdapter.UINT32_ARRAY -> ProtoAdapter.UINT32
  ProtoAdapter.SINT32_ARRAY -> ProtoAdapter.SINT32
  ProtoAdapter.FIXED32_ARRAY -> ProtoAdapter.FIXED32
  ProtoAdapter.SFIXED32_ARRAY -> ProtoAdapter.SFIXED32
  ProtoAdapter.INT64_ARRAY -> ProtoAdapter.INT64
  ProtoAdapter.UINT64_ARRAY -> ProtoAdapter.UINT64
  ProtoAdapter.SINT64_ARRAY -> ProtoAdapter.SINT64
  ProtoAdapter.FIXED64_ARRAY -> ProtoAdapter.FIXED64
  ProtoAdapter.SFIXED64_ARRAY -> ProtoAdapter.SFIXED64
  ProtoAdapter.FLOAT_ARRAY -> ProtoAdapter.FLOAT
  ProtoAdapter.DOUBLE_ARRAY -> ProtoAdapter.DOUBLE
  else -> null
}

/** Returns a list view of a primitive array, and returns every other value as is. */
internal fun Any?.primitiveArrayAsList(): Any? = when (this) {
  is IntArray -> asList()
  is LongArray -> asList()
  is FloatArray -> asList()
  is DoubleArray -> asList()
  else -> this
}
