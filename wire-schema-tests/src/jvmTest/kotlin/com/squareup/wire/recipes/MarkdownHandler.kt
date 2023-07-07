/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.recipes

import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.Path

/** Sample schema handler which generate Markdown files for types and services. */
class MarkdownHandler : SchemaHandler() {
  override fun handle(type: Type, context: SchemaHandler.Context): Path {
    return writeMarkdownFile(type.type, toMarkdown(type), context)
  }

  override fun handle(service: Service, context: SchemaHandler.Context): List<Path> {
    return listOf(writeMarkdownFile(service.type, toMarkdown(service), context))
  }

  override fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path? {
    return null
  }

  private fun writeMarkdownFile(
    protoType: ProtoType,
    markdown: String,
    context: SchemaHandler.Context,
  ): Path {
    val path = context.outDirectory / toPath(protoType).joinToString(separator = "/")
    context.fileSystem.createDirectories(path.parent!!)
    context.fileSystem.write(path) {
      writeUtf8(markdown)
    }
    return path
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
        |
    """.trimMargin()
  }

  private fun toMarkdown(service: Service): String {
    return """
        |# ${service.type.simpleName}
        |
        |${service.documentation}
        |
    """.trimMargin()
  }
}
