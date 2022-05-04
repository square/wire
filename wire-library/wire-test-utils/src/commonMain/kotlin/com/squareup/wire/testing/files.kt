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

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

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

fun FileSystem.add(
  pathString: String,
  contents: String,
) {
  val path = pathString.toPath()
  if (path.parent != null) {
    createDirectories(path.parent!!)
  }
  write(path) {
    writeUtf8(contents)
  }
}

internal val slash = Path.DIRECTORY_SEPARATOR
internal val otherSlash = if (slash == "/") "\\" else "/"

/**
 * This returns a string where all other slashes are replaced with the slash of the local platform.
 * On Windows, `/` will be replaced with `\`. On other platforms, `\` will be replaced with `/`.
 */
fun String.withPlatformSlashes(): String {
  return replace(otherSlash, slash)
}
