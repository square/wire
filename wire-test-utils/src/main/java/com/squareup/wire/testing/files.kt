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

import okio.buffer
import okio.sink
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun FileSystem.add(pathString: String, contents: String) {
  val path = getPath(pathString)
  if (path.parent != null) {
    Files.createDirectories(path.parent)
  }
  Files.write(path, contents.toByteArray(Charsets.UTF_8))
}

fun FileSystem.symlink(linkPathString: String, targetPathString: String) {
  val linkPath = getPath(linkPathString)
  if (linkPath.parent != null) {
    Files.createDirectories(linkPath.parent)
  }
  val targetPath = getPath(targetPathString)
  Files.createSymbolicLink(linkPath, targetPath)
}

fun FileSystem.get(pathString: String): String {
  val path = getPath(pathString)
  return String(Files.readAllBytes(path), Charsets.UTF_8)
}

fun FileSystem.exists(pathString: String): Boolean {
  val path = getPath(pathString)
  return Files.exists(path)
}

fun FileSystem.find(path: String): Set<String> {
  val result = mutableSetOf<String>()
  Files.walkFileTree(getPath(path), object : SimpleFileVisitor<Path>() {
    override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
      if (!Files.isDirectory(path)) {
        result.add(path.toString())
      }
      return FileVisitResult.CONTINUE
    }
  })
  return result
}

fun FileSystem.addZip(pathString: String, vararg contents: Pair<String, String>) {
  val path = getPath(pathString)
  if (path.parent != null) {
    Files.createDirectories(path.parent)
  }

  ZipOutputStream(Files.newOutputStream(path)).use { zipOut ->
    val zipSink = zipOut.sink().buffer()
    for ((elementPath, elementContents) in contents) {
      zipOut.putNextEntry(ZipEntry(elementPath))
      zipSink.writeUtf8(elementContents)
      zipSink.flush()
    }
  }
}
