/*
 * Copyright 2018 Square Inc.
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
package com.squareup.wire.testing

import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets.UTF_8
import okio.ByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink

fun FileSystem.add(
  pathString: String,
  contents: String,
  charset: Charset = UTF_8,
  bom: ByteString = ByteString.EMPTY
) {
  val path = pathString.toPath()
  if (path.parent != null) {
    createDirectories(path.parent!!)
  }
  write(path) {
    write(bom.toByteArray())
    writeString(contents, charset)
  }
}

fun FileSystem.readUtf8(pathString: String): String {
  read(pathString.toPath()) {
    return readUtf8()
  }
}

fun FileSystem.findFiles(path: String): Set<String> {
  return listRecursively(path.toPath())
      .filter { !metadata(it).isDirectory }
      .map { it.toString() }
      .toSet()
}

fun FileSystem.addZip(pathString: String, vararg contents: Pair<String, String>) {
  val path = pathString.toPath()
  if (path.parent != null) {
    createDirectories(path.parent!!)
  }

  write(path) {
    ZipOutputStream(outputStream()).use { zipOut ->
      val zipSink = zipOut.sink().buffer()
      for ((elementPath, elementContents) in contents) {
        zipOut.putNextEntry(ZipEntry(elementPath))
        zipSink.writeUtf8(elementContents)
        zipSink.flush()
      }
    }
  }
}
