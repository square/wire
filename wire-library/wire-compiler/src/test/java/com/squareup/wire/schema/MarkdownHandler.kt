/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.WireLogger
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * This is a sample handler that writes text files that describe types.
 */
class MarkdownHandler : CustomHandlerBeta {
  override fun newHandler(
    schema: Schema,
    fs: FileSystem,
    outDirectory: String,
    logger: WireLogger,
    profileLoader: ProfileLoader
  ): Target.SchemaHandler {
    return object : Target.SchemaHandler {
      override fun handle(type: Type): Path? {
        return writeMarkdownFile(type.type, toMarkdown(type))
      }

      override fun handle(service: Service): List<Path> {
        return listOf(writeMarkdownFile(service.type, toMarkdown(service)))
      }

      override fun handle(extend: Extend, field: Field): Path? {
        return null
      }

      private fun writeMarkdownFile(protoType: ProtoType, markdown: String): Path {
        val path = outDirectory.toPath() / toPath(protoType).joinToString(separator = "/")
        fs.createDirectories(path.parent!!)
        fs.write(path) {
          writeUtf8(markdown)
        }
        return path
      }
    }
  }

  /** Returns a path like `squareup/colors/Blue.md`. */
  private fun toPath(protoType: ProtoType): List<String> {
    val result = mutableListOf<String>()
    for (part in protoType.toString().split(".")) {
      result += part
    }
    result[result.size - 1] = (result[result.size - 1]) + ".md"
    return result
  }

  private fun toMarkdown(type: Type): String {
    return """
        |# ${type.type.simpleName}
        |
        |${type.documentation}
        |""".trimMargin()
  }

  private fun toMarkdown(service: Service): String {
    return """
        |# ${service.type.simpleName}
        |
        |${service.documentation}
        |""".trimMargin()
  }
}
