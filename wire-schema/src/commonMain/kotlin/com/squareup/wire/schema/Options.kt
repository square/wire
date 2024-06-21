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

import com.squareup.wire.schema.ProtoMember.Companion.get
import com.squareup.wire.schema.internal.parser.OptionElement
import kotlin.jvm.JvmField

/**
 * A set of options declared on a message declaration, field declaration, enum declaration, enum
 * constant declaration, service declaration, RPC method declaration, or proto file declaration.
 * Options values may be arbitrary protocol buffer messages, but must be valid protocol buffer
 * messages.
 */
class Options(
  private val optionType: ProtoType,
  private val optionElements: List<OptionElement>,
) {
  // Null until this options is linked.
  private var entries: List<LinkedOptionEntry>? = null

  val elements: List<OptionElement>
    get() {
      return entries?.map {
        // TODO(Benoit) this property is used to go from `Options` to `List<OptionElement>` but this
        //  doesn't take into account what has been pruned. We should consume `it.value`  somehow and
        //  select the option elements we are to fetch, or not.
        it.optionElement
      } ?: optionElements
    }

  val map: Map<ProtoMember, Any?>
    get() = entries?.toMap() ?: emptyMap()

  fun retainLinked() = Options(optionType, emptyList())

  fun get(protoMember: ProtoMember): Any? {
    return entries?.find { it.protoMember == protoMember }?.value
  }

  /**
   * Returns true if any of the options in [entries] matches both of the regular expressions
   * provided: its name matches the option's name and its value matches the option's value.
   */
  fun optionMatches(namePattern: String, valuePattern: String): Boolean {
    val nameRegex = namePattern.toRegex()
    val valueRegex = valuePattern.toRegex()

    return entries!!.any { entry ->
      nameRegex.matchEntire(entry.protoMember.member) != null &&
        valueRegex.matchEntire(entry.value.toString()) != null
    }
  }

  fun link(linker: Linker, location: Location, validate: Boolean) {
    var entries: List<LinkedOptionEntry> = emptyList()

    for (option in optionElements) {
      val canonicalOption: List<LinkedOptionEntry> =
        canonicalizeOption(linker, optionType, option, validate, location)
          ?: continue

      entries = union(linker, entries, canonicalOption)
    }
    this.entries = entries
  }

  private fun canonicalizeOption(
    linker: Linker,
    extensionType: ProtoType,
    option: OptionElement,
    validate: Boolean,
    location: Location,
  ): List<LinkedOptionEntry>? {
    val type = linker.getForOptions(extensionType) as? MessageType
      ?: return null // No known extensions for the given extension type.

    var path: Array<String>?
    var field = type.field(option.name)

    if (field != null) {
      // This is an option declared by descriptor.proto.
      path = arrayOf(option.name)
    } else {
      // This is an option declared by an extension.
      val extensionsForType = type.extensionFieldsMap()
      path = resolveFieldPath(option.name, extensionsForType.keys)
      var namespace = linker.resolveContext()
      while (path == null && namespace.isNotBlank()) {
        // If the path couldn't be resolved, attempt again by prefixing it with the package name.
        path = resolveFieldPath("$namespace.${option.name}", extensionsForType.keys)
        // Retry with one upper level package to resolve relative paths.
        if (path == null) {
          namespace = namespace.substringBeforeLast(".", missingDelimiterValue = "")
        }
      }
      if (path == null) {
        if (validate) {
          linker.errors += "unable to resolve option ${option.name}"
        }
        return null // Unable to find the root of this field path.
      }
      field = extensionsForType[path[0]]
      if (validate) {
        linker.withContext(field!!).validateImportForPath(location, field.location.path)
      }
    }
    linker.request(field!!)

    val result = mutableMapOf<ProtoMember, Any>()
    var last = result
    var lastProtoType: ProtoType? = type.type
    for (i in 1 until path.size) {
      val nested = mutableMapOf<ProtoMember, Any>()
      last[get(lastProtoType!!, field!!)] = nested
      lastProtoType = field.type

      // Force members linking.
      if (lastProtoType != null) {
        linker.getForOptions(lastProtoType)
      }

      last = nested
      field =
        linker.dereference(field.type!!, path[i]) ?: return null // Unable to dereference segment.
      linker.request(field)
    }

    last[get(lastProtoType!!, field!!)] =
      canonicalizeValue(linker, field.type!!, field.isRepeated, option.value)

    check(result.size == 1) // TODO(benoit) might be safe to remove
    val (protoMember, value) = result.entries.first()
    return listOf(LinkedOptionEntry(option, protoMember, value))
  }

  private fun canonicalizeValue(
    linker: Linker,
    context: ProtoType,
    isRepeated: Boolean,
    value: Any,
  ): Any {
    when (value) {
      is OptionElement -> {
        val result = mutableMapOf<ProtoMember, Any>()
        val field = linker.dereference(context, value.name)
        if (field == null) {
          linker.errors += "unable to resolve option ${value.name} on $context"
        } else {
          val protoMember = get(context, field)
          result[protoMember] =
            canonicalizeValue(linker, field.type!!, field.isRepeated, value.value)
        }
        return coerceValueForField(context, result, isRepeated)
      }

      is Map<*, *> -> {
        if (context.isMap) {
          // Map fields are defined with two optional entries: `key` and 'value'.
          val mapFieldKeyAsString = value["key"]
          val mapFieldValueAsString = value["value"]
          val mapFieldKey =
            if (mapFieldKeyAsString == null) {
              null
            } else {
              canonicalizeValue(
                linker,
                context.keyType!!,
                isRepeated = false,
                mapFieldKeyAsString,
              )
            }
          val mapFieldValue =
            if (mapFieldValueAsString == null) {
              null
            } else {
              canonicalizeValue(
                linker,
                context.valueType!!,
                isRepeated = false,
                mapFieldValueAsString,
              )
            }
          return coerceValueForField(context, mapOf(mapFieldKey to mapFieldValue), isRepeated)
        } else {
          val result = mutableMapOf<ProtoMember, Any>()
          for (entry in value) {
            val name = entry.key as String
            val field = linker.dereference(context, name)
            if (field == null) {
              linker.errors += "unable to resolve option $name on $context"
            } else {
              val protoMember = get(context, field)
              result[protoMember] =
                canonicalizeValue(linker, field.type!!, field.isRepeated, entry.value!!)
            }
          }
          return coerceValueForField(context, result, isRepeated)
        }
      }

      is List<*> -> {
        val result = mutableListOf<Any>()
        for (element in value) {
          @Suppress("UNCHECKED_CAST")
          result.addAll(canonicalizeValue(linker, context, isRepeated, element!!) as List<Any>)
        }
        return coerceValueForField(context, result, isRepeated)
      }

      is String -> {
        return coerceValueForField(context, value, isRepeated)
      }

      is OptionElement.OptionPrimitive -> {
        return canonicalizeValue(linker, context, isRepeated, value.value)
      }

      else -> {
        throw IllegalArgumentException("Unexpected option value: $value")
      }
    }
  }

  private fun coerceValueForField(context: ProtoType, value: Any, isRepeated: Boolean): Any {
    return when {
      isRepeated || context.isMap -> value as? List<*> ?: listOf(value)
      value is List<*> -> value.single()!!
      else -> value
    }
  }

  /** Combine values for the same key, resolving conflicts based on their type.  */
  private fun union(linker: Linker, a: Any, b: Any): Any {
    return when (a) {
      is List<*> -> a + b as List<*>

      is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST") // All maps have this type.
        union(linker, a as Map<ProtoMember, Any?>, b as Map<ProtoMember, Any>)
      }

      else -> {
        linker.errors += "conflicting options: $a, $b"
        a // Just return any placeholder.
      }
    }
  }

  private fun union(
    linker: Linker,
    a: List<LinkedOptionEntry>,
    b: List<LinkedOptionEntry>,
  ): List<LinkedOptionEntry> {
    val aMap: Map<ProtoMember, Any?> = a.toMap()

    @Suppress("UNCHECKED_CAST")
    val bMap: Map<ProtoMember, Any> = b.toMap() as Map<ProtoMember, Any>

    val valuesMap: MutableMap<ProtoMember, Any?> = LinkedHashMap(aMap)
    for (entry in bMap) {
      val bValue = entry.value
      val aValue = valuesMap[entry.key]
      valuesMap[entry.key] = when {
        aValue != null -> union(linker, aValue, bValue)
        else -> bValue
      }
    }

    return a.map { it.optionElement to it.protoMember }
      .union(b.map { it.optionElement to it.protoMember })
      .map { (optionElement, protoMember) ->
        LinkedOptionEntry(
          optionElement,
          protoMember,
          valuesMap[protoMember],
        )
      }
  }

  private fun union(
    linker: Linker,
    a: Map<ProtoMember, Any?>,
    b: Map<ProtoMember, Any>,
  ): Map<ProtoMember, Any?> {
    val result: MutableMap<ProtoMember, Any?> = LinkedHashMap(a)
    for (entry in b) {
      val bValue = entry.value
      val aValue = result[entry.key]
      result[entry.key] = when {
        aValue != null -> union(linker, aValue, bValue)
        else -> bValue
      }
    }
    return result
  }

  fun fields(): Multimap<ProtoType, ProtoMember> {
    return fields(PruningRules.Builder().build())
  }

  fun fields(pruningRules: PruningRules): Multimap<ProtoType, ProtoMember> {
    return mutableMapOf<ProtoType, MutableCollection<ProtoMember>>().also {
      gatherFields(it, optionType, entries?.toMap(), pruningRules)
    }.toMultimap()
  }

  private fun gatherFields(
    sink: MutableMap<ProtoType, MutableCollection<ProtoMember>>,
    type: ProtoType,
    o: Any?,
    pruningRules: PruningRules,
  ) {
    when (o) {
      is Map<*, *> -> {
        for ((key, value) in o) {
          val protoMember = when (key) {
            is ProtoMember -> key
            else -> {
              // When the key isn't a `ProtoMember`, this key/value pair is a inlined value of a map
              // field. We don't need to track the key type in map fields for they are always of
              // scalar types. We however have to check the value type.
              gatherFields(sink, type, value!!, pruningRules)
              continue
            }
          }
          if (pruningRules.prunes(protoMember)) continue
          sink.getOrPut(type, ::ArrayList).add(protoMember)
          gatherFields(sink, protoMember.type, value!!, pruningRules)
        }
      }
      is List<*> -> {
        for (e in o) {
          gatherFields(sink, type, e!!, pruningRules)
        }
      }
    }
  }

  fun retainAll(schema: Schema, markSet: MarkSet): Options {
    if (entries.isNullOrEmpty()) return this // Nothing to prune.

    val result = Options(optionType, optionElements)

    @Suppress("UNCHECKED_CAST") // All maps have these type parameters.
    val map = retainAll(schema, markSet, optionType, entries!!.toMap()) as Map<ProtoMember, Any?>?
      ?: emptyMap<ProtoMember, Any>()

    result.entries = entries
      ?.filter { map.containsKey(it.protoMember) }
      ?.map { entry ->
        entry.copy(value = map.getValue(entry.protoMember))
      }

    return result
  }

  /** Returns an object of the same type as [o], or null if it is not retained.  */
  private fun retainAll(
    schema: Schema,
    markSet: MarkSet,
    type: ProtoType?,
    o: Any,
  ): Any? {
    return when {
      o is Map<*, *> -> {
        val map = mutableMapOf<ProtoMember, Any>()
        for ((key, value) in o) {
          val protoMember = when (key) {
            is ProtoMember -> key
            else -> {
              // When the key isn't a `ProtoMember`, this key/value pair is a inlined value of a map
              // field.
              val retainedValue = retainAll(schema, markSet, type, value!!)
              // if `retainedValue` is a map, its value represents an inline message, and we need to
              // mark the proto member.
              if (retainedValue is Map<*, *>) {
                val (k, v) = retainedValue.entries.single()
                map[k as ProtoMember] = v!!
              }
              continue
            }
          }
          val isCoreMemberOfGoogleProtobuf =
            protoMember.type in GOOGLE_PROTOBUF_OPTION_TYPES &&
              !schema.isExtensionField(protoMember)
          if (!markSet.contains(protoMember) && !isCoreMemberOfGoogleProtobuf) {
            continue // Prune this field.
          }

          val field = schema.getField(protoMember)!!
          val retainedValue = retainAll(schema, markSet, field.type, value!!)
          if (retainedValue != null) {
            map[protoMember] = retainedValue // This retained field is non-empty.
          } else if (isCoreMemberOfGoogleProtobuf) {
            map[protoMember] = value
          }
        }
        map.ifEmpty { null }
      }

      o is List<*> -> {
        val list = mutableListOf<Any>()
        for (value in o) {
          val retainedValue = retainAll(schema, markSet, type, value!!)
          if (retainedValue != null) {
            list.add(retainedValue) // This retained value is non-empty.
          }
        }
        list.ifEmpty { null }
      }

      !markSet.contains(type!!) -> null // Prune this type.

      else -> o
    }
  }

  companion object {
    @JvmField val FILE_OPTIONS = ProtoType.get("google.protobuf.FileOptions")
    private val FILE_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.FileOptions")

    @JvmField val MESSAGE_OPTIONS = ProtoType.get("google.protobuf.MessageOptions")
    private val MESSAGE_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.MessageOptions")

    @JvmField val FIELD_OPTIONS = ProtoType.get("google.protobuf.FieldOptions")
    private val FIELD_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.FieldOptions")

    @JvmField val ONEOF_OPTIONS = ProtoType.get("google.protobuf.OneofOptions")
    private val ONEOF_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.OneofOptions")

    @JvmField val ENUM_OPTIONS = ProtoType.get("google.protobuf.EnumOptions")
    private val ENUM_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.EnumOptions")

    @JvmField val ENUM_VALUE_OPTIONS = ProtoType.get("google.protobuf.EnumValueOptions")
    private val ENUM_VALUE_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.EnumValueOptions")

    @JvmField val SERVICE_OPTIONS = ProtoType.get("google.protobuf.ServiceOptions")
    private val SERVICE_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.ServiceOptions")

    @JvmField val METHOD_OPTIONS = ProtoType.get("google.protobuf.MethodOptions")
    private val METHOD_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.MethodOptions")

    @JvmField val EXTENSION_RANGE_OPTIONS = ProtoType.get("google.protobuf.ExtensionRangeOptions")
    private val EXTENSION_RANGE_OPTIONS_VARIANT = ProtoType.get(".google.protobuf.ExtensionRangeOptions")

    // Protobuf allows leading dots when referencing a type. We add the variants to make sure our
    // equality check match them too when we need to know that a ProtoType is a Protobuf option.
    val GOOGLE_PROTOBUF_OPTION_TYPES = arrayOf(
      FILE_OPTIONS,
      MESSAGE_OPTIONS,
      FIELD_OPTIONS,
      ONEOF_OPTIONS,
      ENUM_OPTIONS,
      ENUM_VALUE_OPTIONS,
      SERVICE_OPTIONS,
      METHOD_OPTIONS,
      EXTENSION_RANGE_OPTIONS,
      FILE_OPTIONS_VARIANT,
      MESSAGE_OPTIONS_VARIANT,
      FIELD_OPTIONS_VARIANT,
      ONEOF_OPTIONS_VARIANT,
      ENUM_OPTIONS_VARIANT,
      ENUM_VALUE_OPTIONS_VARIANT,
      SERVICE_OPTIONS_VARIANT,
      METHOD_OPTIONS_VARIANT,
      EXTENSION_RANGE_OPTIONS_VARIANT,
    )

    /**
     * Given a path like `a.b.c.d` and a set of paths like `{a.b.c, a.f.g, h.j}`, this returns the
     * original path split on dots such that the first element is in the set. For the above example
     * it would return the array `[a.b.c, d]`.
     *
     * Typically, the input path is a package name like `a.b`, followed by a dot and a sequence of
     * field names. The first field name is an extension field; subsequent field names make a path
     * within that extension.
     *
     * https://developers.google.com/protocol-buffers/docs/overview?hl=en#packages_and_name_resolution
     * Names can be prefixed with a `.` when the search should start from the outermost scope.
     *
     * Note that a single input may yield multiple possible answers, such as when package names
     * and field names collide. This method prefers shorter package names though that is an
     * implementation detail.
     */
    fun resolveFieldPath(
      name: String,
      fullyQualifiedNames: Set<String?>,
    ): Array<String>? { // Try to resolve a local name.
      var pos = 0
      val chompedName = name.removePrefix(".")
      while (pos < chompedName.length) {
        pos = chompedName.indexOf('.', pos)
        if (pos == -1) pos = chompedName.length
        val candidate = chompedName.substring(0, pos)
        if (fullyQualifiedNames.contains(candidate)) {
          val path = chompedName.substring(pos).split('.').toTypedArray()
          path[0] = chompedName.substring(0, pos)
          return path
        }
        pos++
      }
      return null
    }
  }

  private fun List<LinkedOptionEntry>.toMap(): Map<ProtoMember, Any?> {
    return associate { it.protoMember to it.value }
  }
}
