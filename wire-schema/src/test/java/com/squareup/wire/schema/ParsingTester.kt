/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.ProtoParser
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong

/** Recursively traverse a directory and attempt to parse all of its proto files.  */
object ParsingTester {
  /** Directory under which to search for protos. Change as needed.  */
  private val ROOT = Paths.get("/path/to/protos")

  @Throws(IOException::class)
  @JvmStatic
  fun main(args: Array<String>) {
    var total = 0L
    var failed = 0L

    Files.walkFileTree(ROOT, object : SimpleFileVisitor<Path>() {
      @Throws(IOException::class)
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (file.fileName.toString().endsWith(".proto")) {
          total++

          val data = file.toFile().readText()
          val location = Location.get(ROOT.toString(), file.toString())
          try {
            ProtoParser.parse(location, data)
          } catch (e: Exception) {
            e.printStackTrace()
            failed++
          }
        }
        return FileVisitResult.CONTINUE
      }
    })

    println("\nTotal: $total  Failed: $failed")

    if (failed == 0L) {
      SchemaLoader().addSource(ROOT).load()
      println("All files linked successfully.")
    }
  }
}
