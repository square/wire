/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.javaPackage

// TODO this should go elsewhere
internal fun buildClassMap(schema: Schema, service: Service): Map<ProtoType, ClassName> =
  schema.protoFiles
    .map { Triple(it, it.services, it.types) }
    .flatMap { (schema, services, types) ->
      val servicesToName =
        services.map { it.type to ClassName(javaPackage(schema), service.name) }
      val typesToName =
        types.flatMap { getNestedTypes(javaPackage(schema), null, listOf(it)) }
      servicesToName + typesToName
    }
    .toMap() + mapOf(
    ProtoType.EMPTY to ClassName("kotlin", "Unit"),
  )

private fun getNestedTypes(
  kotlinPackage: String,
  enclosingClassName: ClassName?,
  types: List<Type>,
): List<Pair<ProtoType, ClassName>> {
  return types.map { type ->
    val className = enclosingClassName?.nestedClass(type.type.simpleName)
      ?: ClassName(kotlinPackage, type.type.simpleName)
    return listOf(type.type to className) +
      getNestedTypes(kotlinPackage, className, type.nestedTypes)
  }
}
