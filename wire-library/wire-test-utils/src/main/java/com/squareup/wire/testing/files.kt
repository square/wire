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

import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import org.assertj.core.api.IterableAssert
import org.assertj.core.api.ListAssert
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets.UTF_8

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
  return listRecursively(path.withPlatformSlashes().toPath())
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

private val slash = Path.DIRECTORY_SEPARATOR
private val otherSlash = if (slash == "/") "\\" else "/"

/**
 * This returns a string where all other slashes are replaced with the slash of the local platform.
 * On Windows, `/` will be replaced with `\`. On other platforms, `\` will be replaced with `/`.
 */
fun String.withPlatformSlashes(): String {
  return replace(otherSlash, slash)
}

/**
 * This asserts that [this] contains exactly in any order all [values] regardless of the slash they
 * may contain. This is useful to write one assertion which can be run on both macOS and Windows.
 */
fun IterableAssert<String>.containsRelativePaths(vararg values: String): IterableAssert<String> {
  val values = values.map { it.withPlatformSlashes() }
  return containsExactlyInAnyOrder(*values.toTypedArray())
}

/**
 * This asserts that [this] contains exactly in any order all [values] regardless of the slash they
 * may contain. This is useful to write one assertion which can be run on both macOS and Windows.
 */
fun ListAssert<String>.containsRelativePaths(vararg values: String): ListAssert<String> {
  val values = values.map { it.withPlatformSlashes() }
  return containsExactlyInAnyOrder(*values.toTypedArray())
}
