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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.squareup.wire.schema.ProtoMember.Companion.get
import com.squareup.wire.schema.internal.parser.OptionElement
import java.util.LinkedHashMap
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * A set of options declared on a message declaration, field declaration, enum declaration, enum
 * constant declaration, service declaration, RPC method declaration, or proto file declaration.
 * Options values may be arbitrary protocol buffer messages, but must be valid protocol buffer
 * messages.
 */
class Options(
  private val optionType: ProtoType,
  elements: List<OptionElement>
) {
  val elements: ImmutableList<OptionElement> = ImmutableList.copyOf(elements)

  // Null until this options is linked.
  var map: ImmutableMap<ProtoMember, Any?>? = null
    private set

  fun retainLinked() = Options(optionType, ImmutableList.of())

  operator fun get(protoMember: ProtoMember): Any? = map!![protoMember]

  /**
   * Returns true if any of the options in `options` matches both of the regular expressions
   * provided: its name matches the option's name and its value matches the option's value.
   */
  fun optionMatches(namePattern: String, valuePattern: String): Boolean {
    val nameMatcher = Pattern.compile(namePattern).matcher("")
    val valueMatcher = Pattern.compile(valuePattern).matcher("")
    return map!!.any { entry ->
      nameMatcher.reset(entry.key.member).matches() &&
          valueMatcher.reset(entry.value.toString()).matches()
    }
  }

  fun link(linker: Linker) {
    var map = ImmutableMap.of<ProtoMember, Any?>()
    for (option in elements) {
      val canonicalOption = canonicalizeOption(linker, optionType, option) ?: continue
      map = union(linker, map, canonicalOption)
    }
    this.map = map
  }

  private fun canonicalizeOption(
    linker: Linker,
    extensionType: ProtoType,
    option: OptionElement
  ): Map<ProtoMember, Any>? {
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
    return result
  }

  private fun canonicalizeValue(
    linker: Linker,
    context: Field,
    value: Any
  ): Any {
    when (value) {
      is OptionElement -> {
        val result = ImmutableMap.builder<ProtoMember, Any>()
        val field = linker.dereference(context, value.name)
        if (field == null) {
          linker.addError("unable to resolve option %s on %s", value.name, context.type!!)
        } else {
          val protoMember = get(context.type!!, field)
          result.put(protoMember, canonicalizeValue(linker, field, value.value))
        }
        return coerceValueForField(context, result.build())
      }

      is Map<*, *> -> {
        val result = ImmutableMap.builder<ProtoMember, Any>()
        for (entry in value) {
          val name = entry.key as String
          val field = linker.dereference(context, name)
          if (field == null) {
            linker.addError("unable to resolve option %s on %s", name, context.type!!)
          } else {
            val protoMember = get(context.type!!, field)
            result.put(protoMember, canonicalizeValue(linker, field, entry.value!!))
          }
        }
        return coerceValueForField(context, result.build())
      }

      is List<*> -> {
        val result = ImmutableList.builder<Any>()
        for (element in value) {
          result.addAll(canonicalizeValue(linker, context, element!!) as List<*>)
        }
        return coerceValueForField(context, result.build())
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
      context.isRepeated -> value as? List<*> ?: ImmutableList.of(value)
      value is List<*> -> Iterables.getOnlyElement<Any>(value)
      else -> value
    }
  }

  /** Combine values for the same key, resolving conflicts based on their type.  */
  private fun union(linker: Linker, a: Any, b: Any): Any {
    return when (a) {
      is List<*> -> {
        union(a, b as List<*>)
      }

      is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST") // All maps have this type.
        union(linker, a as Map<ProtoMember, Any?>, b as Map<ProtoMember, Any>)
      }

      else -> {
        linker.addError("conflicting options: %s, %s", a, b)
        a // Just return any placeholder.
      }
    }
  }

  private fun union(
    linker: Linker, a: Map<ProtoMember, Any?>, b: Map<ProtoMember, Any>
  ): ImmutableMap<ProtoMember, Any?> {
    val result: MutableMap<ProtoMember, Any?> = LinkedHashMap(a)
    for (entry in b) {
      val bValue = entry.value
      val aValue = result[entry.key]
      result[entry.key] = when {
        aValue != null -> union(linker, aValue, bValue)
        else -> bValue
      }
    }
    return ImmutableMap.copyOf(result)
  }

  private fun union(a: List<*>, b: List<*>): ImmutableList<Any> {
    return ImmutableList.builder<Any>()
        .addAll(a)
        .addAll(b)
        .build()
  }

  fun fields(): Multimap<ProtoType, ProtoMember> {
    return LinkedHashMultimap.create<ProtoType, ProtoMember>().also {
      gatherFields(it, optionType, map, IdentifierSet.Builder().build())
    }
  }

  fun fields(identifierSet: IdentifierSet): Multimap<ProtoType, ProtoMember> {
    return LinkedHashMultimap.create<ProtoType, ProtoMember>().also {
      gatherFields(it, optionType, map, identifierSet)
    }
  }

  private fun gatherFields(
    sink: Multimap<ProtoType, ProtoMember>,
    type: ProtoType,
    o: Any?,
    identifierSet: IdentifierSet
  ) {
    when (o) {
      is Map<*, *> -> {
        for ((key, value) in o) {
          val protoMember = key as ProtoMember
          if (identifierSet.excludes(protoMember)) continue
          sink.put(type, protoMember)
          gatherFields(sink, protoMember.type, value!!, identifierSet)
        }
      }
      is List<*> -> {
        for (e in o) {
          gatherFields(sink, type, e!!, identifierSet)
        }
      }
    }
  }

  fun retainAll(schema: Schema, markSet: MarkSet): Options {
    if (map == null || map!!.isEmpty()) return this // Nothing to prune.

    val result = Options(optionType, elements)
    @Suppress("UNCHECKED_CAST") // All maps have these type parameters.
    result.map = retainAll(schema, markSet, optionType, map!!) as ImmutableMap<ProtoMember, Any?>?
        ?: ImmutableMap.of<ProtoMember, Any>()
    return result
  }

  /** Returns an object of the same type as `o`, or null if it is not retained.  */
  private fun retainAll(
    schema: Schema,
    markSet: MarkSet,
    type: ProtoType?,
    o: Any
  ): Any? {
    return when {
      !markSet.contains(type!!) -> null // Prune this type.

      o is Map<*, *> -> {
        val builder = ImmutableMap.builder<ProtoMember, Any>()
        for ((key, value) in o) {
          val protoMember = key as ProtoMember
          if (!markSet.contains(protoMember)) continue  // Prune this field.
          val field = schema.getField(protoMember)
          val retainedValue = retainAll(schema, markSet, field.type, value!!)
          if (retainedValue != null) {
            builder.put(protoMember, retainedValue) // This retained field is non-empty.
          }
        }
        val map = builder.build()
        if (map.isNotEmpty()) map else null
      }

      o is List<*> -> {
        val builder = ImmutableList.builder<Any>()
        for (value in o) {
          val retainedValue = retainAll(schema, markSet, type, value!!)
          if (retainedValue != null) {
            builder.add(retainedValue) // This retained value is non-empty.
          }
        }
        val list = builder.build()
        if (list.isNotEmpty()) list else null
      }

      else -> o
    }
  }

  /** Returns true if these options assigns a value to `protoMember`.  */
  fun assignsMember(protoMember: ProtoMember?): Boolean {
    // TODO(jwilson): remove the null check; this shouldn't be called until linking completes.
    return map != null && map!!.containsKey(protoMember)
  }

  companion object {
    @JvmField val FILE_OPTIONS = ProtoType.get("google.protobuf.FileOptions")
    @JvmField val MESSAGE_OPTIONS = ProtoType.get("google.protobuf.MessageOptions")
    @JvmField val FIELD_OPTIONS = ProtoType.get("google.protobuf.FieldOptions")
    @JvmField val ENUM_OPTIONS = ProtoType.get("google.protobuf.EnumOptions")
    @JvmField val ENUM_VALUE_OPTIONS = ProtoType.get("google.protobuf.EnumValueOptions")
    @JvmField val SERVICE_OPTIONS = ProtoType.get("google.protobuf.ServiceOptions")
    @JvmField val METHOD_OPTIONS = ProtoType.get("google.protobuf.MethodOptions")

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
}
