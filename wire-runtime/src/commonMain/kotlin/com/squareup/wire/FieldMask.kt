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
class FieldMask(paths: List<String> = emptyList()) {
  val paths: List<String> = paths.toList()

  fun copy(paths: List<String> = this.paths): FieldMask = FieldMask(paths)

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    return other is FieldMask && paths == other.paths
  }

  override fun hashCode(): Int = paths.hashCode()

  override fun toString(): String = "FieldMask{paths=$paths}"
}
