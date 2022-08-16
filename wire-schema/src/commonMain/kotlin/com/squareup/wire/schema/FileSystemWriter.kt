/*
 * Copyright (C) 2019 Square, Inc.
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

import okio.FileSystem
import okio.Path

class FileSystemWriter constructor(
  /** To be used by the [SchemaHandler] for reading/writing operations on disk. */
  private val fileSystem: FileSystem,
  /** Location on [fileSystem] where the [SchemaHandler] is to write files, if it needs to. */
  private val outDirectory: Path,
): FileWriter {

  override fun write(file: Path, content: String): Path {
    val output = outDirectory / file
    fileSystem.createDirectories(outDirectory / file.parent!!)
    fileSystem.write(output, false) {
      writeUtf8(content)
    }
    return output
  }
}
