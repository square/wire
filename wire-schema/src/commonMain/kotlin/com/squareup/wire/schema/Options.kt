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

import com.squareup.wire.schema.internal.parser.OptionElement

expect class Options(
  optionType: ProtoType,
  optionElements: List<OptionElement>
) {
  val elements: List<OptionElement>

  fun retainLinked(): Options
  operator fun get(protoMember: ProtoMember): Any?
  fun optionMatches(namePattern: String, valuePattern: String): Boolean
  fun link(linker: Linker)
  fun retainAll(schema: Schema, markSet: MarkSet): Options
  fun assignsMember(protoMember: ProtoMember?): Boolean

  companion object {
    val FILE_OPTIONS: ProtoType
    val MESSAGE_OPTIONS: ProtoType
    val FIELD_OPTIONS: ProtoType
    val ENUM_OPTIONS: ProtoType
    val ENUM_VALUE_OPTIONS: ProtoType
    val SERVICE_OPTIONS: ProtoType
    val METHOD_OPTIONS: ProtoType
    val GOOGLE_PROTOBUF_OPTION_TYPES: Array<ProtoType>
  }
}
