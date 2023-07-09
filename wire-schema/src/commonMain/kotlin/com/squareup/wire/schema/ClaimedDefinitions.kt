/*
 * Copyright (C) 2020 Square, Inc.
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

/**
 * [ClaimedDefinitions] tracks handled objects: [Type]s, [Service]s, and [Field]s. A
 * [SchemaHandler] is to first check if an object has already been claimed; if yes, it is not to
 * handle it. Otherwise, the [SchemaHandler] is to handle the object and [claim] it. It is an error
 * for a [SchemaHandler] to handle an object which has already been claimed.
 */
class ClaimedDefinitions {
  private val types = mutableSetOf<ProtoType>()
  private val members = mutableSetOf<ProtoMember>()

  /** Tracks that [type] has been handled. */
  fun claim(type: ProtoType) {
    check(types.add(type)) { "Type $type already claimed" }
  }

  /** Tracks that [member] has been handled. */
  fun claim(member: ProtoMember) {
    check(members.add(member)) { "member $member already claimed" }
  }

  /** Tracks that [type] has been handled. */
  fun claim(type: Type) {
    claim(type.type)
  }

  /** Tracks that [service] has been handled. */
  fun claim(service: Service) {
    claim(service.type)
  }

  /** Returns true if [type] has already been handled. */
  operator fun contains(type: ProtoType) = type in types

  /** Returns true if [member] has already been handled. */
  operator fun contains(member: ProtoMember) = member in members

  /** Returns true if [type] has already been handled. */
  operator fun contains(type: Type) = contains(type.type)

  /** Returns true if [service] has already been handled. */
  operator fun contains(service: Service) = contains(service.type)
}
