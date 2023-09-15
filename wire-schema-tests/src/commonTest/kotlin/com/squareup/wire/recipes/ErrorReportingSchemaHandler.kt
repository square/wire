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
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.Path

/** Sample schema validator that enforces a field naming pattern. */
class ErrorReportingSchemaHandler : SchemaHandler() {
  override fun handle(type: Type, context: SchemaHandler.Context): Path? {
    val errorCollector = context.errorCollector

    if ("descriptor.proto" in type.location.path) return null // Don't report errors on built-in stuff.
    if (type is MessageType) {
      for (field in type.fields) {
        if (field.name.startsWith("a")) {
          errorCollector.at(field) += "field starts with 'a'"
        }
      }
    }
    return null
  }

  override fun handle(service: Service, context: SchemaHandler.Context): List<Path> = emptyList()

  override fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path? = null
}
