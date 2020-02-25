/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser

import com.squareup.wire.schema.Location
import okio.buffer
import okio.source
import java.io.File
import java.util.ArrayDeque
import java.util.Collections

/** Recursively traverse a directory and attempt to parse all of its proto files.  */
object ParsingTester {
  /** Directory under which to search for protos. Change as needed.  */
  private val ROOT = File("/path/to/protos")

  @JvmStatic
  fun main() {
    var total = 0
    var failed = 0

    val fileQueue = ArrayDeque<File>()
    fileQueue.add(ROOT)
    while (fileQueue.isNotEmpty()) {
      val file = fileQueue.removeFirst()
      if (file.isDirectory) {
        Collections.addAll(fileQueue, *file.listFiles()!!)
      } else if (file.name.endsWith(".proto")) {
        println("Parsing ${file.path}")
        total += 1

        try {
          file.source()
              .buffer()
              .use { `in` ->
                val data = `in`.readUtf8()
                ProtoParser.parse(Location.get(file.path), data)
              }
        } catch (e: Exception) {
          e.printStackTrace()
          failed += 1
        }
      }
    }

    println("\nTotal: $total  Failed: $failed")
  }
}
