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

import com.squareup.wire.schema.ProtoType.Companion.get
import com.squareup.wire.schema.internal.isValidTag
import java.util.ArrayDeque

/** Links local field types and option types to the corresponding declarations. */
class Linker {
  private val loader: Loader
  private val fileLinkers: MutableMap<String, FileLinker>
  private val fileOptionsQueue: ArrayDeque<FileLinker>
  private val protoTypeNames: MutableMap<String, Type>
  private val errors: MutableList<String>
  private val contextStack: List<Any>
  private val requestedTypes: MutableSet<ProtoType?>

  internal constructor(loader: Loader) {
    this.loader = loader
    fileLinkers = mutableMapOf()
    fileOptionsQueue = ArrayDeque()
    protoTypeNames = mutableMapOf()
    contextStack = emptyList()
    errors = mutableListOf()
    requestedTypes = mutableSetOf()
  }

  private constructor(enclosing: Linker, additionalContext: Any) {
    loader = enclosing.loader
    fileLinkers = enclosing.fileLinkers
    fileOptionsQueue = enclosing.fileOptionsQueue
    protoTypeNames = enclosing.protoTypeNames
    contextStack = enclosing.contextStack + additionalContext
    errors = enclosing.errors
    requestedTypes = enclosing.requestedTypes
  }

  /** Returns a linker for `path`, loading the file if necessary. */
  internal fun getFileLinker(path: String): FileLinker {
    val existing = fileLinkers[path]
    if (existing != null) return existing

    val protoFile = loader.load(path)
    val result = FileLinker(protoFile, withContext(protoFile))
    fileLinkers[path] = result
    fileOptionsQueue += result
    return result
  }

  /**
   * Link all features of all files in `sourceProtoFiles` to create a schema. This will also
   * partially link any imported files necessary.
   */
  fun link(sourceProtoFiles: Iterable<ProtoFile>): Schema {
    val sourceFiles = sourceProtoFiles.map { sourceFile ->
      FileLinker(sourceFile, withContext(sourceFile))
          .also { fileLinker ->
            fileLinkers[sourceFile.location.path] = fileLinker
          }
    }

    for (fileLinker in sourceFiles) {
      fileLinker.requireTypesRegistered()
    }

    // Also link descriptor.proto's types, which are necessary for options.
    val descriptorProto = getFileLinker("google/protobuf/descriptor.proto")
    descriptorProto.requireTypesRegistered()

    for (fileLinker in sourceFiles) {
      fileLinker.requireExtensionsLinked()
    }

    for (fileLinker in sourceFiles) {
      fileLinker.requireImportedExtensionsRegistered()
    }

    for (fileLinker in sourceFiles) {
      fileLinker.linkMembers()
    }

    for (fileLinker in sourceFiles) {
      fileLinker.linkOptions()
    }

    // For compactness we'd prefer to link the options of source files only. But we link file
    // options on referenced files to make sure that java_package is populated.
    while (fileOptionsQueue.isNotEmpty()) {
      val fileLinker = fileOptionsQueue.poll()
      fileLinker.requireFileOptionsLinked()
    }

    for (fileLinker in sourceFiles) {
      fileLinker.validate()
    }

    if (errors.isNotEmpty()) {
      throw SchemaException(errors)
    }

    val result = mutableListOf<ProtoFile>()
    for (fileLinker in fileLinkers.values) {
      if (sourceFiles.contains(fileLinker)) {
        result.add(fileLinker.protoFile)
        continue
      }

      // Retain this type if it's used by anything in the source path.
      val anyTypeIsUsed = fileLinker.protoFile.typesAndNestedTypes()
          .any { type ->
            requestedTypes.contains(type.type)
          }
      if (anyTypeIsUsed) {
        result.add(fileLinker.protoFile.retainLinked(requestedTypes as Set<ProtoType>))
      }
    }

    return Schema(result)
  }

  /** Returns the type name for the scalar, relative or fully-qualified name `name`. */
  fun resolveType(name: String): ProtoType {
    return resolveType(name, false)
  }

  /** Returns the type name for the relative or fully-qualified name `name`. */
  fun resolveMessageType(name: String): ProtoType {
    return resolveType(name, true)
  }

  private fun resolveType(name: String, messageOnly: Boolean): ProtoType {
    val type = get(name)

    if (type.isScalar) {
      if (messageOnly) {
        addError("expected a message but was %s", name)
      }
      return type
    }

    if (type.isMap) {
      if (messageOnly) {
        addError("expected a message but was %s", name)
      }
      val keyType = resolveType(type.keyType.toString(), false)
      val valueType = resolveType(type.valueType.toString(), false)
      return get(keyType, valueType, name)
    }

    var resolved: Type? = resolve(name, protoTypeNames)
    // If no type could be resolved, load imported files and try again.
    if (resolved == null) {
      for (fileLinker in contextImportedTypes()) {
        fileLinker.requireTypesRegistered()
      }
      resolved = resolve(name, protoTypeNames)
    }

    if (resolved == null) {
      addError("unable to resolve %s", name)
      return ProtoType.BYTES // Just return any placeholder.
    }

    if (messageOnly && resolved !is MessageType) {
      addError("expected a message but was %s", name)
      return ProtoType.BYTES // Just return any placeholder.
    }

    requestedTypes.add(resolved.type)

    return resolved.type!!
  }

  fun <T> resolve(name: String, map: Map<String, T>): T? {
    if (name.startsWith(".")) {
      // If name starts with a '.', the rest of it is fully qualified.
      val result = map[name.substring(1)]
      if (result != null) return result
    } else {
      // We've got a name suffix, like 'Person' or 'protos.Person'. Start the search from with the
      // longest prefix like foo.bar.Baz.Quux, shortening the prefix until we find a match.
      var prefix = resolveContext()
      while (prefix.isNotEmpty()) {
        val result = map["$prefix.$name"]
        if (result != null) return result

        // Strip the last nested class name or package name from the end and try again.
        val dot = prefix.lastIndexOf('.')
        prefix = if (dot != -1) prefix.substring(0, dot) else ""
      }
      val result = map[name]
      if (result != null) return result
    }
    return null
  }

  private fun resolveContext(): String {
    for (i in contextStack.indices.reversed()) {
      val context = contextStack[i]
      when {
        context is Type -> {
          return context.type.toString()
        }
        context is ProtoFile -> {
          val packageName = context.packageName
          return packageName ?: ""
        }
        context is Field && context.isExtension -> {
          return context.packageName ?: ""
        }
      }
    }
    throw IllegalStateException()
  }

  /** Returns the current package name from the context stack. */
  fun packageName(): String? {
    for (context in contextStack) {
      if (context is ProtoFile) return context.packageName
    }
    return null
  }

  /**
   * Returns the files imported in the current context. These files declare the types that may be
   * resolved.
   */
  internal fun contextImportedTypes(): List<FileLinker> {
    val result = mutableListOf<FileLinker>()
    for (i in contextStack.indices.reversed()) {
      val context = contextStack[i]
      if (context is ProtoFile) {
        val path = context.location.path
        val fileLinker = getFileLinker(path)
        for (effectiveImport in fileLinker.effectiveImports()) {
          result.add(getFileLinker(effectiveImport))
        }
      }
    }
    return result
  }

  /** Adds `type`. */
  fun addType(protoType: ProtoType, type: Type) {
    protoTypeNames[protoType.toString()] = type
  }

  /** Returns the type or null if it doesn't exist. */
  operator fun get(protoType: ProtoType): Type? {
    var result = protoTypeNames[protoType.toString()]

    // If no type could be resolved, load imported files and try again.
    if (result == null) {
      for (fileLinker in contextImportedTypes()) {
        fileLinker.requireTypesRegistered()
      }
      result = protoTypeNames[protoType.toString()]
    }

    if (result != null) {
      requestedTypes.add(protoType)
    }

    return result
  }

  /**
   * Returns the type or null if it doesn't exist. Before this returns it ensures members are linked
   * so that options may dereference them.
   */
  fun getForOptions(protoType: ProtoType): Type? {
    val result = get(protoType) ?: return null

    val fileLinker = getFileLinker(result.location.path)
    fileLinker.requireMembersLinked(result)
    return result
  }

  /** Returns the field named `field` on the message type of `self`. */
  fun dereference(
    self: Field,
    field: String
  ): Field? {
    @Suppress("NAME_SHADOWING") var field = field
    if (field.startsWith("[") && field.endsWith("]")) {
      field = field.substring(1, field.length - 1)
    }

    val type = get(self.type!!)
    if (type is MessageType) {
      val messageField = type.field(field)
      if (messageField != null) return messageField

      val typeExtensions = type.extensionFieldsMap()
      val extensionField = resolve(field, typeExtensions)
      if (extensionField != null) return extensionField
    }

    return null // Unable to traverse this field path.
  }

  /** Validate that the tags of `fields` are unique and in range. */
  fun validateFields(
    fields: Iterable<Field>,
    reserveds: List<Reserved>
  ) {
    val tagToField = linkedMapOf<Int, MutableSet<Field>>()
    val nameToField = linkedMapOf<String, MutableSet<Field>>()

    for (field in fields) {
      val tag = field.tag
      if (!tag.isValidTag()) {
        withContext(field).addError("tag is out of range: %s", tag)
      }

      for (reserved in reserveds) {
        if (reserved.matchesTag(tag)) {
          withContext(field).addError("tag %s is reserved (%s)", tag, reserved.location)
        }
        if (reserved.matchesName(field.name)) {
          withContext(field).addError("name '%s' is reserved (%s)", field.name, reserved.location)
        }
      }

      tagToField.getOrPut(tag, { mutableSetOf() }).also { it += field }
      nameToField.getOrPut(field.qualifiedName, { mutableSetOf() }).also { it += field }
    }

    for ((key, value) in tagToField) {
      if (value.size > 1) {
        val error = StringBuilder()
        error.append("multiple fields share tag $key:")
        var index = 1
        for (field in value) {
          error.append(String.format("\n  %s. %s (%s)", index++, field.name, field.location))
        }
        addError("%s", error)
      }
    }

    for (collidingFields in nameToField.values) {
      if (collidingFields.size > 1) {
        val first = collidingFields.iterator().next()
        val error = StringBuilder()
        error.append(String.format("multiple fields share name %s:", first.name))
        var index = 1
        for (field in collidingFields) {
          error.append(String.format("\n  %s. %s (%s)", index++, field.name, field.location))
        }
        addError("%s", error)
      }
    }
  }

  fun validateEnumConstantNameUniqueness(nestedTypes: Iterable<Type>) {
    val nameToType = mutableMapOf<String, MutableSet<EnumType>>()
    for (type in nestedTypes) {
      if (type is EnumType) {
        for (enumConstant in type.constants) {
          nameToType.getOrPut(enumConstant.name, { mutableSetOf() } ).also { it += type }
        }
      }
    }

    for ((constant, value) in nameToType) {
      if (value.size > 1) {
        val error = buildString {
          var index = 1
          append("multiple enums share constant $constant:")
          for (enumType in value) {
            append(String.format("\n  %s. %s.%s (%s)",
                index++, enumType.type, constant, enumType.constant(constant)!!.location))
          }
        }
        addError("%s", error)
      }
    }
  }

  fun validateImport(
    location: Location,
    type: ProtoType
  ) {
    @Suppress("NAME_SHADOWING") var type = type

    // Map key type is always scalar. No need to validate it.
    if (type.isMap) type = type.valueType!!

    if (type.isScalar) return

    val path = location.path
    val requiredImport = get(type)!!.location.path
    val fileLinker = getFileLinker(path)
    if (path != requiredImport && !fileLinker.effectiveImports().contains(requiredImport)) {
      addError("%s needs to import %s", path, requiredImport)
    }
  }

  /** Returns a new linker that uses `context` to resolve type names and report errors. */
  fun withContext(context: Any): Linker {
    return Linker(this, context)
  }

  fun addError(format: String, vararg args: Any) {
    val error = StringBuilder()
    error.append(String.format(format, *args))

    for (i in contextStack.indices.reversed()) {
      val context = contextStack[i]
      val prefix = if (i == contextStack.size - 1) "\n  for" else "\n  in"

      when (context) {
        is Rpc -> {
          error.append(String.format("%s rpc %s (%s)", prefix, context.name, context.location))
        }

        is Extend -> {
          val type = context.type
          error.append(
              if (type != null) String.format("%s extend %s (%s)", prefix, type, context.location)
              else String.format("%s extend (%s)", prefix, context.location))
        }

        is Field -> {
          error.append(String.format("%s field %s (%s)", prefix, context.name, context.location))
        }

        is MessageType -> {
          error.append(String.format("%s message %s (%s)", prefix, context.type, context.location))
        }

        is EnumType -> {
          error.append(String.format("%s enum %s (%s)", prefix, context.type, context.location))
        }

        is Service -> {
          error.append(
              String.format("%s service %s (%s)", prefix, context.type(), context.location()))
        }

        is Extensions -> {
          error.append(String.format("%s extensions (%s)", prefix, context.location))
        }
      }
    }
    errors.add(error.toString())
  }
}
