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
    .toMap()

private fun getNestedTypes(
  kotlinPackage: String,
  enclosingClassName: ClassName?,
  types: List<Type>
): List<Pair<ProtoType, ClassName>> {
  return types.map { type ->
    val className = enclosingClassName?.nestedClass(type.type.simpleName)
      ?: ClassName(kotlinPackage, type.type.simpleName)
    return listOf(type.type to className) +
      getNestedTypes(kotlinPackage, className, type.nestedTypes)
  }
}
