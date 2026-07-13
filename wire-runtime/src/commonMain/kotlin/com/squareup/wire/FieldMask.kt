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
package com.squareup.wire

/**
 * A set of symbolic field paths.
 *
 * Field masks are used to specify a subset of fields on a target message. Each path uses proto
 * field names, separated by dots for nested fields.
 */
class FieldMask private constructor(
  private val pathChunks: PathChunks?,
) {
  constructor() : this(emptyList())

  constructor(paths: List<String> = emptyList()) : this(
    paths.takeIf { it.isNotEmpty() }?.let { PathChunks(previous = null, paths = it.toList()) },
  )

  /**
   * The paths in this mask. This is flattened lazily so merging encoded occurrences can append
   * chunks in constant time without repeatedly copying all paths accumulated so far.
   */
  val paths: List<String> by lazy {
    if (pathChunks == null) {
      emptyList()
    } else {
      val chunks = mutableListOf<List<String>>()
      var chunk: PathChunks? = pathChunks
      while (chunk != null) {
        chunks += chunk.paths
        chunk = chunk.previous
      }
      buildList(pathChunks.size) {
        for (i in chunks.size - 1 downTo 0) {
          addAll(chunks[i])
        }
      }
    }
  }

  internal fun append(paths: List<String>): FieldMask {
    if (paths.isEmpty()) return this
    return FieldMask(PathChunks(pathChunks, paths, (pathChunks?.size ?: 0) + paths.size))
  }

  fun copy(paths: List<String> = this.paths): FieldMask = FieldMask(paths)

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    return other is FieldMask && paths == other.paths
  }

  override fun hashCode(): Int = paths.hashCode()

  override fun toString(): String = "FieldMask{paths=$paths}"
}

private class PathChunks(
  val previous: PathChunks?,
  val paths: List<String>,
  val size: Int = paths.size,
)
