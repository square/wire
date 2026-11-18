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

import com.squareup.wire.FieldMask
import com.squareup.wire.MergeContext
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import okio.Buffer
import okio.ByteString

/**
 * Holds the repeated occurrences of singular message fields while a message is decoded, so that
 * each field is merged once at the end instead of once per occurrence.
 *
 * Generated code decodes the first occurrence of a field as usual. It passes later occurrences to
 * [addMerge] and calls [finishMerge] for each field after the last tag. This is for generated code
 * use only.
 */
class MessageMerges internal constructor(
  private val context: MergeContext,
) {
  private val occurrences = LinkedHashMap<Int, MutableList<ByteString>>()

  internal fun add(tag: Int, bytes: ByteString) {
    occurrences.getOrPut(tag) { mutableListOf() }.add(bytes)
  }

  internal fun discard(tag: Int) {
    occurrences.remove(tag)
  }

  /** Returns the tags that have occurrences waiting to be merged. */
  fun tags(): IntArray = occurrences.keys.toIntArray()

  internal fun <E> finish(tag: Int, adapter: ProtoAdapter<E>, existing: E?): E? {
    val pieces = occurrences.remove(tag) ?: return existing
    if (existing == null) return existing
    if (adapter === ProtoAdapter.FIELD_MASK) {
      @Suppress("UNCHECKED_CAST")
      var result = existing as FieldMask
      for (piece in pieces) {
        val reader = ProtoReader(Buffer().write(piece), context)
        result = result.append(ProtoAdapter.FIELD_MASK.decode(reader).paths)
      }
      @Suppress("UNCHECKED_CAST")
      return result as E
    }
    val buffer = Buffer()
    adapter.encode(buffer, existing)
    for (piece in pieces) {
      buffer.write(piece)
    }
    return adapter.decode(ProtoReader(buffer, context))
  }
}
