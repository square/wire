/*
 * Copyright (C) 2015 Square, Inc.
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

import com.squareup.wire.ProtoAdapter
import kotlin.collections.set

/**
 * A collection of .proto files that describe a set of messages. A schema is *linked*: each
 * field's type name is resolved to the corresponding type definition.
 *
 * Use [SchemaLoader] to load a schema from source files.
 */
actual class Schema internal actual constructor(protoFiles: Iterable<ProtoFile>) {
  actual val protoFiles: List<ProtoFile> = protoFiles.sortedBy { it.location.path }

  private val protoFilesIndex: Map<ProtoType?, ProtoFile?>
  private val typesIndex: Map<String, Type>
  private val servicesIndex: Map<String, Service>
  init {
    val index = mutableMapOf<ProtoType?, ProtoFile?>()
    typesIndex = buildTypesIndex(protoFiles, index)
    servicesIndex = buildServicesIndex(protoFiles, index)
    protoFilesIndex = index
  }

  /** Returns the proto file at [path], or null if this schema has no such file.  */
  fun protoFile(path: String): ProtoFile? = protoFiles.first { it.location.path == path }

  /** Returns the proto file containing this [protoType], or null if there isn't such file.  */
  fun protoFile(protoType: ProtoType): ProtoFile? = protoFilesIndex[protoType]

  /**
   * Returns a copy of this schema that retains only the types and services selected by
   * [pruningRules], plus their transitive dependencies.
   */
  fun prune(pruningRules: PruningRules): Schema = Pruner(this, pruningRules).prune()

  /**
   * Returns the service with the fully qualified name [name], or null if this schema defines
   * no such service.
   */
  fun getService(name: String): Service? = servicesIndex[name]

  /** Returns the service for [protoType], or null if this schema defines no such service. */
  fun getService(protoType: ProtoType): Service? = getService(protoType.toString())

  /**
   * Returns the type with the fully qualified name [name], or null if this schema defines no
   * such type.
   */
  fun getType(name: String): Type? = typesIndex[name]

  /** Returns the type for [protoType], or null if this schema defines no such type. */
  fun getType(protoType: ProtoType): Type? = getType(protoType.toString())

  /** Returns the field for [protoMember], or null if this schema defines no such field. */
  fun getField(protoMember: ProtoMember): Field? {
    val type = getType(protoMember.type) as? MessageType ?: return null
    return type.field(protoMember.member) ?: type.extensionField(protoMember.member)
  }

  /**
   * Returns the field with the fully qualified [typeName] and [memberName], or null if this schema
   * defines no such field.
   */
  fun getField(typeName: String, memberName: String): Field? {
    return getField(ProtoType.get(typeName), memberName)
  }

  /**
   * Returns the field for [protoType] and [memberName], or null if this schema defines no such
   * field.
   */
  fun getField(protoType: ProtoType, memberName: String): Field? {
    return getField(ProtoMember.get(protoType, memberName))
  }

  /**
   * Returns a wire adapter for the message or enum type named [typeName]. The returned type
   * adapter doesn't have model classes to encode and decode from, so instead it uses scalar types
   * ([String], [ByteString][okio.ByteString], [Integer], etc.),
   * [maps][Map], and [lists][List]. It can both encode and decode
   * these objects. Map keys are field names.
   *
   * @param includeUnknown true to include values for unknown tags in the returned model. Map keys
   * for such values is the unknown value's tag name as a string. Unknown values are decoded to
   * [Long], [Long], [Integer], or [ByteString][okio.ByteString] for
   * [VARINT][com.squareup.wire.FieldEncoding.VARINT],
   * [FIXED64][com.squareup.wire.FieldEncoding.FIXED64],
   * [FIXED32][com.squareup.wire.FieldEncoding.FIXED32], or
   * [LENGTH_DELIMITED][com.squareup.wire.FieldEncoding.LENGTH_DELIMITED], respectively.
   */
  fun protoAdapter(
    typeName: String,
    includeUnknown: Boolean
  ): ProtoAdapter<Any> {
    val type = requireNotNull(getType(typeName)) { "unexpected type $typeName" }
    return SchemaProtoAdapterFactory(this, includeUnknown)[type.type]
  }

  fun isExtensionField(protoMember: ProtoMember): Boolean {
    val type = getType(protoMember.type)
    return type is MessageType && type.extensionField(protoMember.member) != null
  }

  companion object {
    @JvmOverloads
    fun fromFiles(
      sourceProtoFiles: Iterable<ProtoFile>,
      pathFilesLoader: Loader = CoreLoader
    ): Schema {
      return Linker(pathFilesLoader).link(sourceProtoFiles)
    }

    private fun buildTypesIndex(
      protoFiles: Iterable<ProtoFile>,
      protoFilesIndex: MutableMap<ProtoType?, ProtoFile?>
    ): Map<String, Type> {
      val typesByName = mutableMapOf<String, Type>()

      fun index(type: Type, protoFile: ProtoFile) {
        val protoType = type.type
        if (protoType !in protoFilesIndex) {
          protoFilesIndex[protoType] = protoFile
        }
        typesByName[protoType.toString()] = type
        for (nested in type.nestedTypes) {
          index(nested, protoFile)
        }
      }

      for (protoFile in protoFiles) {
        for (type in protoFile.types) {
          index(type, protoFile)
        }
      }
      return typesByName
    }

    private fun buildServicesIndex(
      protoFiles: Iterable<ProtoFile>,
      protoFilesIndex: MutableMap<ProtoType?, ProtoFile?>
    ): Map<String, Service> {
      val result = mutableMapOf<String, Service>()
      for (protoFile in protoFiles) {
        for (service in protoFile.services) {
          result[service.type().toString()] = service
          protoFilesIndex[service.type()] = protoFile
        }
      }
      return result
    }
  }
}
