/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema

import kotlin.jvm.JvmStatic

/**
 * Identifies a field, enum or RPC on a declaring type. Members are encoded as strings containing a
 * type name, a hash, and a member name, like `squareup.dinosaurs.Dinosaur#length_meters`.
 *
 * A member's name is typically a simple name like "length_meters" or "packed". If the member
 * field is an extension to its type, that name is prefixed with its enclosing package. This yields
 * a member name with two packages, like `google.protobuf.FieldOptions#squareup.units.unit`.
 */
class ProtoMember private constructor(
  val type: ProtoType,
  val member: String,
) {
  val simpleName: String
    get() = member.substringAfterLast('.') // Strip package prefix for extension fields.

  init {
    require(!type.isScalar) { "scalars cannot have members" }
  }

  override fun equals(other: Any?) =
    other is ProtoMember && type == other.type && member == other.member

  override fun hashCode() = type.hashCode() * 37 + member.hashCode()

  override fun toString() = "$type#$member"

  companion object {
    @JvmStatic
    fun get(typeAndMember: String): ProtoMember {
      val hash = typeAndMember.indexOf('#')
      require(hash != -1) { "expected a '#' in $typeAndMember" }
      val type = ProtoType.get(typeAndMember.substring(0, hash))
      val member = typeAndMember.substring(hash + 1)
      return ProtoMember(type, member)
    }

    @JvmStatic
    fun get(type: ProtoType, member: String) = ProtoMember(type, member)

    @JvmStatic
    fun get(type: ProtoType, field: Field): ProtoMember {
      val member = if (field.isExtension) field.qualifiedName else field.name
      return ProtoMember(type, member)
    }
  }
}
