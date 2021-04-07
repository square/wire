/*
 * Copyright (C) 2021 Square, Inc.
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

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.Options
import okio.Path

/** Visit [path] and all its children recursively, if it has any. */
internal fun okio.FileSystem.visitAll(path: Path, block: (Path) -> Unit) {
  block(path)

  val toVisit: List<Path> = try {
    list(path)
  } catch (e: IOException) {
    listOf()
  }

  for (child in toVisit) {
    visitAll(child, block)
  }
}

internal fun FileSystem.toOkioFileSystem(): okio.FileSystem {
  return when {
    this == FileSystems.getDefault() -> okio.FileSystem.SYSTEM
    else -> error("Wire doesn't support non-default file system: $this")
  }
}

/**
 * Given directory `src/main/proto` and path `src/main/proto/squareup/dinosaurs/dino.proto`,
 * this returns `squareup/dinosaurs/dino.proto`.
 */
internal fun Path.relativize(descendant: Path): String {
  if (this == descendant) return ""

  val descendantString = descendant.toString()
  val thisString = toString()

  val slashIndex = when (thisString) {
    "/" , "\\" -> 0
    else -> thisString.length
  }

  require(descendantString.startsWith(thisString) &&
      (descendantString[slashIndex] == '/' || descendantString[slashIndex] == '\\')) {
    "not a descendant of $thisString: $descendantString"
  }

  return descendantString.substring(slashIndex + 1)
}

internal fun Path.endsWithDotProto() = toString().endsWith(".proto")

private val UNICODE_BOMS = Options.of(
  "efbbbf".decodeHex(), // UTF-8
  "feff".decodeHex(), // UTF-16BE
  "fffe".decodeHex(), // UTF-16LE
  "0000ffff".decodeHex(), // UTF-32BE
  "ffff0000".decodeHex() // UTF-32LE
)

internal fun BufferedSource.readBomAsCharset(default: Charset = Charsets.UTF_8): Charset {
  return when (select(UNICODE_BOMS)) {
    0 -> Charsets.UTF_8
    1 -> Charsets.UTF_16BE
    2 -> Charsets.UTF_16LE
    3 -> Charsets.UTF_32BE
    4 -> Charsets.UTF_32LE
    -1 -> default
    else -> throw AssertionError()
  }
}
