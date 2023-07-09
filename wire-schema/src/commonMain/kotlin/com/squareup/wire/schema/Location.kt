/*
 * Copyright (C) 2015 Square, Inc.
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

import kotlin.jvm.JvmStatic

/**
 * Locates a .proto file, or a position within a .proto file, on the file system. This includes a
 * base directory or a .jar file, and a path relative to that base.
 */
data class Location(
  /** The base of this location; typically a directory or .jar file.  */
  val base: String,

  /** The path to this location relative to [base].  */
  val path: String,

  /** The line number of this location, or -1 for no specific line number.  */
  val line: Int = -1,

  /** The column on the line of this location, or -1 for no specific column.  */
  val column: Int = -1,
) {

  fun at(line: Int, column: Int) = Location(base, path, line, column)

  /** Returns a copy of this location with an empty base.  */
  fun withoutBase() = Location("", path, line, column)

  /** Returns a copy of this location including only its path.  */
  fun withPathOnly() = Location("", path, -1, -1)

  override fun toString() = buildString {
    if (base.isNotEmpty()) {
      append(base)
      append('/')
    }
    append(path)
    if (line != -1) {
      append(':')
      append(line)
      if (column != -1) {
        append(':')
        append(column)
      }
    }
  }

  companion object {
    @JvmStatic
    fun get(path: String): Location {
      return get("", path)
    }

    @JvmStatic
    fun get(
      base: String,
      path: String,
    ): Location {
      return Location(base.trimEnd('/'), path, -1, -1)
    }
  }
}
