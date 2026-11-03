/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.parser.OptionElement
import okio.Path
import okio.Path.Companion.toPath

/** Keeps source paths inside a single generated line comment without changing input paths. */
internal fun Location.sourcePathForComment(): String = buildString {
  for (character in withPathOnly().toString()) {
    append(
      when (character) {
        '\r', '\n', '\u000b', '\u000c', '\u0085', '\u2028', '\u2029' -> ' '
        else -> character
      },
    )
  }
}

// TODO internal and friend for wire-java-generator: https://youtrack.jetbrains.com/issue/KT-34102
fun StringBuilder.appendDocumentation(
  documentation: String,
) {
  if (documentation.isEmpty()) {
    return
  }
  var lines = documentation.split("\n")
  if (lines.count() > 1 && lines.last().isEmpty()) {
    lines = lines.dropLast(1)
  }
  for (line in lines) {
    append("// ")
      .append(line)
      .append('\n')
  }
}

internal fun StringBuilder.appendOptions(
  options: List<OptionElement>,
) {
  val count = options.size
  if (count == 1) {
    append('[')
      .append(options[0].toSchema())
      .append(']')
    return
  }
  append("[\n")
  for (i in 0 until count) {
    val endl = if (i < count - 1) "," else ""
    appendIndented(options[i].toSchema() + endl)
  }
  append(']')
}

// TODO internal and friend for wire-java-generator: https://youtrack.jetbrains.com/issue/KT-34102
fun StringBuilder.appendIndented(
  value: String,
) {
  var lines = value.split("\n")
  if (lines.count() > 1 && lines.last().isEmpty()) {
    lines = lines.dropLast(1)
  }
  for (line in lines) {
    append("  ")
      .append(line)
      .append('\n')
  }
}

internal const val MIN_TAG_VALUE = 1
internal const val MAX_TAG_VALUE = (1 shl 29) - 1 // 536,870,911

private const val RESERVED_TAG_VALUE_START = 19000
private const val RESERVED_TAG_VALUE_END = 19999

/** True if the supplied value is in the valid tag range and not reserved.  */
internal fun Int.isValidTag() = this in MIN_TAG_VALUE until RESERVED_TAG_VALUE_START ||
  this in (RESERVED_TAG_VALUE_END + 1) until MAX_TAG_VALUE + 1

internal expect fun Char.isDigit(): Boolean

internal expect fun String.toEnglishLowerCase(): String

expect interface MutableQueue<T : Any> : MutableCollection<T> {
  fun poll(): T?
}

internal expect fun <T : Any> mutableQueueOf(): MutableQueue<T>

// TODO internal and friend for wire-compiler: https://youtrack.jetbrains.com/issue/KT-34102
/**
 * Replace types in this schema which are present in [typesToStub] with empty shells that have no
 * outward references. This has to be done in this module so that we can access the internal
 * constructor to avoid re-linking.
 *
 * @param upstreamEnums the enums in [typesToStub] as pruned by the module that owns them. An enum
 *     stub keeps only the zero value found here, so it never has one its owner doesn't generate.
 */
fun Schema.withStubs(
  typesToStub: Set<ProtoType>,
  upstreamEnums: Map<ProtoType, EnumType>,
): Schema {
  if (typesToStub.isEmpty()) {
    return this
  }
  return Schema(
    protoFiles.map { protoFile ->
      val result = protoFile.copy(
        types = protoFile.types.map { type ->
          if (type.type in typesToStub) type.asStub(upstreamEnums) else type
        },
        services = protoFile.services.map { service ->
          if (service.type in typesToStub) service.asStub() else service
        },
      )
      return@map result
    },
  )
}

/** Return a copy of this type with all possible type references removed. */
private fun Type.asStub(upstreamEnums: Map<ProtoType, EnumType>): Type = when {
  // Don't stub the built-in protobuf types which model concepts like options.
  type.toString().startsWith("google.protobuf.") -> this

  this is MessageType -> copy(
    declaredFields = emptyList(),
    extensionFields = mutableListOf(),
    nestedTypes = nestedTypes.map { it.asStub(upstreamEnums) },
    options = Options(Options.MESSAGE_OPTIONS, emptyList()),
  )

  this is EnumType -> copy(
    // Keep the owner's zero value so proto3 defaults resolve, but drop its options and their type
    // references.
    constants = upstreamEnums[type]?.constants.orEmpty()
      .filter { it.tag == 0 }
      .map { it.copy(options = Options(Options.ENUM_VALUE_OPTIONS, emptyList())) },
    options = Options(Options.ENUM_OPTIONS, emptyList()),
  )

  this is EnclosingType -> copy(
    nestedTypes = nestedTypes.map { it.asStub(upstreamEnums) },
  )

  else -> throw AssertionError("Unknown type $type")
}

/** Return a copy of this service with all possible type references removed. */
private fun Service.asStub() = copy(
  rpcs = emptyList(),
  options = Options(Options.SERVICE_OPTIONS, emptyList()),
)

fun Path.withUnixSlashes(): Path = toString().replace('\\', '/').toPath()
