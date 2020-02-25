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
  private val optionElements: List<OptionElement>
) {
  // Null until this options is linked.
  private var entries: List<LinkedOptionEntry>? = null

  val elements: List<OptionElement>
    get() {
      return entries?.map { it.optionElement } ?: optionElements
    }

  val map: Map<ProtoMember, Any?>
    get() = entries!!.toMap()

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

  fun link(linker: Linker) {
    var entries: List<LinkedOptionEntry> = emptyList()

    for (option in optionElements) {
      val canonicalOption: List<LinkedOptionEntry> = canonicalizeOption(linker, optionType, option)
          ?: continue

      entries = union(linker, entries, canonicalOption)
    }
    this.entries = entries
  }

  private fun canonicalizeOption(
    linker: Linker,
    extensionType: ProtoType,
    option: OptionElement
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
      val packageName = linker.packageName()
      if (path == null && packageName != null) {
        // If the path couldn't be resolved, attempt again by prefixing it with the package name.
        path = resolveFieldPath(packageName + "." + option.name, extensionsForType.keys)
      }
      if (path == null) {
        return null // Unable to find the root of this field path.
      }
      field = extensionsForType[path[0]]
    }

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
      field = linker.dereference(field, path[i]) ?: return null // Unable to dereference segment.
    }

    last[get(lastProtoType!!, field!!)] = canonicalizeValue(linker, field, option.value)

    check(result.size == 1) // TODO(benoit) might be safe to remove
    val (protoMember, value) = result.entries.first()
    return listOf(LinkedOptionEntry(option, protoMember, value))
  }

  private fun canonicalizeValue(
    linker: Linker,
    context: Field,
    value: Any
  ): Any {
    when (value) {
      is OptionElement -> {
        val result = mutableMapOf<ProtoMember, Any>()
        val field = linker.dereference(context, value.name)
        if (field == null) {
          linker.addError("unable to resolve option ${value.name} on ${context.type}")
        } else {
          val protoMember = get(context.type!!, field)
          result[protoMember] = canonicalizeValue(linker, field, value.value)
        }
        return coerceValueForField(context, result)
      }

      is Map<*, *> -> {
        val result = mutableMapOf<ProtoMember, Any>()
        for (entry in value) {
          val name = entry.key as String
          val field = linker.dereference(context, name)
          if (field == null) {
            linker.addError("unable to resolve option $name on ${context.type}")
          } else {
            val protoMember = get(context.type!!, field)
            result[protoMember] = canonicalizeValue(linker, field, entry.value!!)
          }
        }
        return coerceValueForField(context, result)
      }

      is List<*> -> {
        val result = mutableListOf<Any>()
        for (element in value) {
          result.addAll(canonicalizeValue(linker, context, element!!) as List<Any>)
        }
        return coerceValueForField(context, result)
      }

      is String -> {
        return coerceValueForField(context, value)
      }

      else -> {
        throw IllegalArgumentException("Unexpected option value: $value")
      }
    }
  }

  private fun coerceValueForField(context: Field, value: Any): Any {
    return when {
      context.isRepeated -> value as? List<*> ?: listOf(value)
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
        linker.addError("conflicting options: $a, $b")
        a // Just return any placeholder.
      }
    }
  }

  private fun union(
    linker: Linker, a: List<LinkedOptionEntry>, b: List<LinkedOptionEntry>
  ): List<LinkedOptionEntry> {
    val aMap: Map<ProtoMember, Any?> = a.toMap()
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
              valuesMap[protoMember]
          )
        }
  }

  private fun union(
    linker: Linker, a: Map<ProtoMember, Any?>, b: Map<ProtoMember, Any>
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
    pruningRules: PruningRules
  ) {
    when (o) {
      is Map<*, *> -> {
        for ((key, value) in o) {
          val protoMember = key as ProtoMember
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
    o: Any
  ): Any? {
    return when {
      o is Map<*, *> -> {
        val map = mutableMapOf<ProtoMember, Any>()
        for ((key, value) in o) {
          val protoMember = key as ProtoMember
          val isCoreMemberOfGoogleProtobuf =
              protoMember.type in GOOGLE_PROTOBUF_OPTION_TYPES &&
                  !schema.isExtensionField(protoMember)
          if (!markSet.contains(protoMember) && !isCoreMemberOfGoogleProtobuf) {
            continue  // Prune this field.
          }

          val field = schema.getField(protoMember)!!
          val retainedValue = retainAll(schema, markSet, field.type, value!!)
          if (retainedValue != null) {
            map[protoMember] = retainedValue // This retained field is non-empty.
          } else if (isCoreMemberOfGoogleProtobuf) {
            map[protoMember] = value
          }
        }
        if (map.isNotEmpty()) map else null
      }

      o is List<*> -> {
        val list = mutableListOf<Any>()
        for (value in o) {
          val retainedValue = retainAll(schema, markSet, type, value!!)
          if (retainedValue != null) {
            list.add(retainedValue) // This retained value is non-empty.
          }
        }
        if (list.isNotEmpty()) list else null
      }

      !markSet.contains(type!!) -> null // Prune this type.

      else -> o
    }
  }

  /** Returns true if these options assigns a value to [protoMember].  */
  fun assignsMember(protoMember: ProtoMember?): Boolean {
    // TODO(jwilson): remove the null check; this shouldn't be called until linking completes.
    return entries?.any { it.protoMember == protoMember } ?: false
  }

  companion object {
    @JvmField val FILE_OPTIONS = ProtoType.get("google.protobuf.FileOptions")
    @JvmField val MESSAGE_OPTIONS = ProtoType.get("google.protobuf.MessageOptions")
    @JvmField val FIELD_OPTIONS = ProtoType.get("google.protobuf.FieldOptions")
    @JvmField val ENUM_OPTIONS = ProtoType.get("google.protobuf.EnumOptions")
    @JvmField val ENUM_VALUE_OPTIONS = ProtoType.get("google.protobuf.EnumValueOptions")
    @JvmField val SERVICE_OPTIONS = ProtoType.get("google.protobuf.ServiceOptions")
    @JvmField val METHOD_OPTIONS = ProtoType.get("google.protobuf.MethodOptions")
    val GOOGLE_PROTOBUF_OPTION_TYPES = arrayOf(
        FILE_OPTIONS,
        MESSAGE_OPTIONS,
        FIELD_OPTIONS,
        ENUM_OPTIONS,
        ENUM_VALUE_OPTIONS,
        SERVICE_OPTIONS,
        METHOD_OPTIONS
    )

    /**
     * Given a path like `a.b.c.d` and a set of paths like `{a.b.c, a.f.g, h.j}`, this returns the
     * original path split on dots such that the first element is in the set. For the above example
     * it would return the array `[a.b.c, d]`.
     *
     * Typically the input path is a package name like `a.b`, followed by a dot and a sequence of
     * field names. The first field name is an extension field; subsequent field names make a path
     * within that extension.
     *
     * Note that a single input may yield multiple possible answers, such as when package names
     * and field names collide. This method prefers shorter package names though that is an
     * implementation detail.
     */
    fun resolveFieldPath(
      name: String,
      fullyQualifiedNames: Set<String?>
    ): Array<String>? { // Try to resolve a local name.
      var pos = 0
      while (pos < name.length) {
        pos = name.indexOf('.', pos)
        if (pos == -1) pos = name.length
        val candidate = name.substring(0, pos)
        if (fullyQualifiedNames.contains(candidate)) {
          val path = name.substring(pos).split('.').toTypedArray()
          path[0] = name.substring(0, pos)
          return path
        }
        pos++
      }
      return null
    }
  }

  private fun List<LinkedOptionEntry>.toMap(): Map<ProtoMember, Any?> {
    return map { it.protoMember to it.value }.toMap()
  }
}
