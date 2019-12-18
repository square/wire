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

import java.util.Collections
import java.util.LinkedHashMap

/**
 * Names a protocol buffer message, enumerated type, service, map, or a scalar. This class models a
 * fully-qualified name using the protocol buffer package.
 */
class ProtoType {
  val isScalar: Boolean

  private val string: String

  val isMap: Boolean

  /** The type of the map's keys. Only present when [isMap] is true.  */
  val keyType: ProtoType?

  /** The type of the map's values. Only present when [isMap] is true.  */
  val valueType: ProtoType?

  val simpleName: String
    get() {
      val dot = string.lastIndexOf('.')
      return string.substring(dot + 1)
    }

  /** Returns the enclosing type, or null if this type is not nested in another type.  */
  val enclosingTypeOrPackage: String?
    get() {
      val dot = string.lastIndexOf('.')
      return if (dot == -1) null else string.substring(0, dot)
    }

  /** Creates a scalar or message type.  */
  private constructor(isScalar: Boolean, string: String) {
    this.isScalar = isScalar
    this.string = string
    this.isMap = false
    this.keyType = null
    this.valueType = null
  }

  /** Creates a map type.  */
  private constructor(keyType: ProtoType, valueType: ProtoType, string: String) {
    require(keyType.isScalar && keyType != BYTES && keyType != DOUBLE && keyType != FLOAT) {
      "map key must be non-byte, non-floating point scalar: $keyType"
    }
    this.isScalar = false
    this.string = string
    this.isMap = true
    this.keyType = keyType // TODO restrict what's allowed here
    this.valueType = valueType
  }

  fun nestedType(name: String?): ProtoType {
    check(!isScalar) { "scalar cannot have a nested type" }
    check(!isMap) { "map cannot have a nested type" }
    require(name != null && !name.contains(".") && name.isNotEmpty()) { "unexpected name: $name" }
    return ProtoType(false, "$string.$name")
  }

  override fun equals(other: Any?) = other is ProtoType && string == other.string

  override fun hashCode() = string.hashCode()

  override fun toString() = string

  companion object {
    @JvmField val BOOL = ProtoType(true, "bool")
    @JvmField val BYTES = ProtoType(true, "bytes")
    @JvmField val DOUBLE = ProtoType(true, "double")
    @JvmField val FLOAT = ProtoType(true, "float")
    @JvmField val FIXED32 = ProtoType(true, "fixed32")
    @JvmField val FIXED64 = ProtoType(true, "fixed64")
    @JvmField val INT32 = ProtoType(true, "int32")
    @JvmField val INT64 = ProtoType(true, "int64")
    @JvmField val SFIXED32 = ProtoType(true, "sfixed32")
    @JvmField val SFIXED64 = ProtoType(true, "sfixed64")
    @JvmField val SINT32 = ProtoType(true, "sint32")
    @JvmField val SINT64 = ProtoType(true, "sint64")
    @JvmField val STRING = ProtoType(true, "string")
    @JvmField val UINT32 = ProtoType(true, "uint32")
    @JvmField val UINT64 = ProtoType(true, "uint64")

    private val SCALAR_TYPES: Map<String, ProtoType>

    init {
      val scalarTypes = LinkedHashMap<String, ProtoType>()
      scalarTypes[BOOL.string] = BOOL
      scalarTypes[BYTES.string] = BYTES
      scalarTypes[DOUBLE.string] = DOUBLE
      scalarTypes[FLOAT.string] = FLOAT
      scalarTypes[FIXED32.string] = FIXED32
      scalarTypes[FIXED64.string] = FIXED64
      scalarTypes[INT32.string] = INT32
      scalarTypes[INT64.string] = INT64
      scalarTypes[SFIXED32.string] = SFIXED32
      scalarTypes[SFIXED64.string] = SFIXED64
      scalarTypes[SINT32.string] = SINT32
      scalarTypes[SINT64.string] = SINT64
      scalarTypes[STRING.string] = STRING
      scalarTypes[UINT32.string] = UINT32
      scalarTypes[UINT64.string] = UINT64
      SCALAR_TYPES = Collections.unmodifiableMap(scalarTypes)
    }

    @JvmStatic
    fun get(enclosingTypeOrPackage: String?, typeName: String): ProtoType {
      return when {
        enclosingTypeOrPackage != null -> get("$enclosingTypeOrPackage.$typeName")
        else -> get(typeName)
      }
    }

    @JvmStatic
    fun get(name: String?): ProtoType {
      val scalar = SCALAR_TYPES[name]
      if (scalar != null) return scalar

      require(name != null && name.isNotEmpty() && !name.contains("#")) { "unexpected name: $name" }

      if (name.startsWith("map<") && name.endsWith(">")) {
        val comma = name.indexOf(',')
        require(comma != -1) { "expected ',' in map type: $name" }
        val key = get(name.substring(4, comma).trim())
        val value = get(name.substring(comma + 1, name.length - 1).trim())
        return ProtoType(key, value, name)
      }

      return ProtoType(false, name)
    }

    @JvmStatic fun get(keyType: ProtoType, valueType: ProtoType, name: String) =
        ProtoType(keyType, valueType, name)
  }
}
