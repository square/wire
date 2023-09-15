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
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** Sample schema handler which writes to disk generated artifacts. */
class LogToFileHandler : SchemaHandler() {
  private val filePath = "log.txt".toPath()

  override fun handle(type: Type, context: SchemaHandler.Context): Path? {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating type: ${type.type}\n")
    }

    return null
  }

  override fun handle(service: Service, context: SchemaHandler.Context): List<Path> {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating service: ${service.type}\n")
    }

    return listOf()
  }

  override fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path? {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating ${extend.type} on ${field.location}\n")
    }

    return null
  }
}
