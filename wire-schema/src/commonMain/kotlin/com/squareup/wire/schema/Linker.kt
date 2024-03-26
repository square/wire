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

import com.squareup.wire.schema.ProtoType.Companion.BYTES
import com.squareup.wire.schema.ProtoType.Companion.get
import com.squareup.wire.schema.internal.MutableQueue
import com.squareup.wire.schema.internal.isValidTag
import com.squareup.wire.schema.internal.mutableQueueOf

/** Links local field types and option types to the corresponding declarations. */
class Linker {
  private val loader: Loader
  private val fileLinkers: MutableMap<String, FileLinker>
  private val fileOptionsQueue: MutableQueue<FileLinker>
  private val protoTypeNames: MutableMap<String, Type>
  private val contextStack: List<Any>
  private val requestedTypes: MutableSet<ProtoType>
  private val requestedFields: MutableSet<Field>
  private val permitPackageCycles: Boolean
  private val opaqueTypes: List<ProtoType>
  val loadExhaustively: Boolean

  /** Errors accumulated by this load. */
  val errors: ErrorCollector

  constructor(
    loader: Loader,
    errors: ErrorCollector,
    permitPackageCycles: Boolean,
    loadExhaustively: Boolean,
    opaqueTypes: List<ProtoType>,
  ) {
    this.loader = loader
    this.fileLinkers = mutableMapOf()
    this.fileOptionsQueue = mutableQueueOf()
    this.protoTypeNames = mutableMapOf()
    this.contextStack = emptyList()
    this.requestedTypes = mutableSetOf()
    this.requestedFields = mutableSetOf()
    this.errors = errors
    this.permitPackageCycles = permitPackageCycles
    this.loadExhaustively = loadExhaustively
    this.opaqueTypes = opaqueTypes
  }

  constructor(
    loader: Loader,
    errors: ErrorCollector,
    permitPackageCycles: Boolean,
    loadExhaustively: Boolean,
  ) : this(
    loader = loader,
    errors = errors,
    permitPackageCycles = permitPackageCycles,
    loadExhaustively = loadExhaustively,
    opaqueTypes = listOf(),
  )

  private constructor(
    enclosing: Linker,
    additionalContext: Any,
  ) {
    this.loader = enclosing.loader
    this.fileLinkers = enclosing.fileLinkers
    this.fileOptionsQueue = enclosing.fileOptionsQueue
    this.protoTypeNames = enclosing.protoTypeNames
    this.contextStack = enclosing.contextStack + additionalContext
    this.requestedTypes = enclosing.requestedTypes
    this.requestedFields = enclosing.requestedFields
    this.errors = enclosing.errors.at(additionalContext)
    this.permitPackageCycles = false
    this.loadExhaustively = enclosing.loadExhaustively
    this.opaqueTypes = enclosing.opaqueTypes
  }

  /** Returns a linker for [path], loading the file if necessary. */
  internal fun getFileLinker(path: String): FileLinker {
    val existing = fileLinkers[path]
    if (existing != null) return existing

    val protoFile = loader.withErrors(errors).load(path)
    val result = FileLinker(protoFile, withContext(protoFile))
    fileLinkers[path] = result
    fileOptionsQueue += result
    return result
  }

  /**
   * Link all features of all files in [sourceProtoFiles] to create a schema. This will also
   * partially link any imported files necessary.
   */
  fun link(sourceProtoFiles: Iterable<ProtoFile>): Schema {
    val sourceFiles = mutableListOf<FileLinker>()
    for (sourceFile in sourceProtoFiles) {
      val fileLinker = FileLinker(sourceFile, withContext(sourceFile))
      fileLinkers[sourceFile.location.path] = fileLinker
      sourceFiles += fileLinker
    }

    // Ensure linking the descriptor.proto and wire_options.proto, if not provided. This ensures
    // we can resolve our java_package and wire_package options.
    if (fileLinkers[DESCRIPTOR_PROTO] == null) {
      sourceFiles += getFileLinker(DESCRIPTOR_PROTO)
    }
    if (fileLinkers[WIRE_EXTENSIONS_PROTO] == null) {
      sourceFiles += getFileLinker(WIRE_EXTENSIONS_PROTO)
    }

    // When loading exhaustively, every import (and transitive import!) is a source file.
    if (loadExhaustively) {
      val queue = mutableQueueOf<FileLinker>()
      queue += fileLinkers.values
      while (true) {
        val fileLinker = queue.poll() ?: break
        for (importPath in fileLinker.protoFile.imports + fileLinker.protoFile.publicImports) {
          if (importPath !in fileLinkers) {
            val imported = withContext(fileLinker.protoFile).getFileLinker(importPath)
            sourceFiles += imported
            queue += imported
          }
        }
      }
    }

    // The order of the input files shows up in the order of extension fields in the output files.
    // Sort the inputs to get consistent output even when the order of input files is inconsistent.
    sourceFiles.sortBy { it.protoFile.location.path }

    for (fileLinker in sourceFiles) {
      fileLinker.requireTypesRegistered()
    }

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
      val syntaxRules = SyntaxRules.get(fileLinker.protoFile.syntax)
      fileLinker.linkOptions(syntaxRules, validate = true)
    }

    for (fileLinker in sourceFiles) {
      fileLinker.requireImportedExtensionOptionsLinked(validate = false)
    }

    // For compactness we'd prefer to link the options of source files only. But we link file
    // options on referenced files to make sure that java_package is populated.
    while (fileOptionsQueue.isNotEmpty()) {
      val fileLinker = fileOptionsQueue.poll()!!
      fileLinker.requireFileOptionsLinked(validate = false)
    }

    validatePackages()

    for (fileLinker in sourceFiles) {
      val syntaxRules = SyntaxRules.get(fileLinker.protoFile.syntax)
      fileLinker.validate(syntaxRules)
    }

    val cycleChecker = CycleChecker(fileLinkers, errors)
    cycleChecker.checkForImportCycles()
    if (!permitPackageCycles) {
      cycleChecker.checkForPackageCycles()
    }

    errors.throwIfNonEmpty()

    val result = mutableListOf<ProtoFile>()
    for (fileLinker in fileLinkers.values) {
      if (fileLinker in sourceFiles) {
        result.add(fileLinker.protoFile)
        continue
      }

      // Retain this type if it's used by anything in the source path.
      val anyTypeIsUsed = fileLinker.protoFile.typesAndNestedTypes()
        .any { type ->
          requestedTypes.contains(type.type)
        }
      val anyFieldIsUsed = fileLinker.protoFile.extendList
        .any { extend ->
          extend.fields.any { it in requestedFields }
        }
      if (anyTypeIsUsed || anyFieldIsUsed) {
        result.add(fileLinker.protoFile.retainLinked(requestedTypes.toSet(), requestedFields))
      }
    }

    return Schema(result)
  }

  /** Returns the type name for the scalar, relative or fully-qualified name [name]. */
  fun resolveType(name: String): ProtoType {
    return resolveType(name, false)
  }

  /** Returns the type name for the relative or fully-qualified name [name]. */
  fun resolveMessageType(name: String): ProtoType {
    return resolveType(name, true)
  }

  private fun resolveType(
    name: String,
    messageOnly: Boolean,
  ): ProtoType {
    val type = get(name)

    if (type.isScalar) {
      if (messageOnly) {
        errors += "expected a message but was $name"
      }
      if (type in opaqueTypes) {
        errors += "Scalar types like $type cannot be opaqued"
      }
      return type
    }

    if (type.isMap) {
      if (messageOnly) {
        errors += "expected a message but was $name"
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
      errors += "unable to resolve $name"
      return BYTES // Just return any placeholder.
    }

    if (messageOnly && resolved !is MessageType) {
      errors += "expected a message but was $name"
      return BYTES // Just return any placeholder.
    }

    when (resolved.type) {
      in opaqueTypes -> {
        when (resolved) {
          is EnumType -> {
            errors += "Enums like ${resolved.type} cannot be opaqued"
            return resolved.type
          }

          else -> return BYTES
        }
      }
      else -> {
        requestedTypes.add(resolved.type)
        return resolved.type
      }
    }
  }

  fun <T> resolve(
    name: String,
    map: Map<String, T>,
  ): T? {
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

  fun resolveContext(): String {
    for (i in contextStack.indices.reversed()) {
      when (val context = contextStack[i]) {
        is Type -> {
          return context.type.toString()
        }

        is ProtoFile -> {
          val packageName = context.packageName
          return packageName ?: ""
        }
      }
    }
    throw IllegalStateException()
  }

  /**
   * Returns the files imported in the current context. These files declare the types that may be
   * resolved.
   */
  internal fun contextImportedTypes(): List<FileLinker> {
    val result = mutableListOf<FileLinker>()
    for (i in contextStack.indices.reversed()) {
      val context = contextStack[i]

      val location = when {
        context is ProtoFile -> context.location
        context is Field && context.isExtension -> context.location
        else -> null
      }

      if (location != null) {
        val path = location.path
        val fileLinker = getFileLinker(path)
        for (effectiveImport in fileLinker.effectiveImports()) {
          result.add(getFileLinker(effectiveImport))
        }
      }
    }
    return result
  }

  /** Adds [type]. */
  fun addType(
    protoType: ProtoType,
    type: Type,
  ) {
    protoTypeNames[protoType.toString()] = type
  }

  /** Returns the type or null if it doesn't exist. */
  fun get(protoType: ProtoType): Type? {
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

  /** Mark a field as used in an option so its file is retained in the schema. */
  fun request(field: Field) {
    requestedFields.add(field)
  }

  /** Returns the field named [field] on the message type of [protoType]. */
  fun dereference(
    protoType: ProtoType,
    field: String,
  ): Field? {
    @Suppress("NAME_SHADOWING")
    var field = field
    if (field.startsWith("[") && field.endsWith("]")) {
      field = field.substring(1, field.length - 1)
    }

    val type = getForOptions(protoType)
    if (type is MessageType) {
      val messageField = type.field(field)
      if (messageField != null) return messageField

      val typeExtensions = type.extensionFieldsMap()
      val extensionField = resolve(field, typeExtensions)
      if (extensionField != null) return extensionField
    }

    return null // Unable to traverse this field path.
  }

  /**
   * Validate that the tags of [fields] are unique and in range, that proto3 message cannot
   * reference proto2 enums.
   */
  @Suppress("NAME_SHADOWING")
  fun validateFields(
    fields: Iterable<Field>,
    reserveds: List<Reserved>,
    syntaxRules: SyntaxRules,
  ) {
    val tagToField = linkedMapOf<Int, MutableSet<Field>>()
    val nameToField = linkedMapOf<String, MutableSet<Field>>()
    val jsonNameToField = linkedMapOf<String, MutableSet<Field>>()

    for (field in fields) {
      val tag = field.tag
      if (!tag.isValidTag()) {
        errors.at(field) += "tag is out of range: $tag"
      }

      for (reserved in reserveds) {
        if (reserved.matchesTag(tag)) {
          errors.at(field) += "tag $tag is reserved (${reserved.location})"
        }
        if (reserved.matchesName(field.name)) {
          errors.at(field) += "name '${field.name}' is reserved (${reserved.location})"
        }
      }

      tagToField.getOrPut(tag) { mutableSetOf() }.add(field)
      nameToField.getOrPut(field.qualifiedName) { mutableSetOf() }.add(field)
      // We allow JSON collisions for extensions.
      if (!field.isExtension) {
        jsonNameToField
          .getOrPut(syntaxRules.jsonName(field.name, field.declaredJsonName)) { mutableSetOf() }
          .add(field)
      }

      syntaxRules.validateTypeReference(get(field.type!!), errors.at(field))
    }

    for ((key, values) in tagToField) {
      if (values.size > 1) {
        val error = StringBuilder()
        error.append("multiple fields share tag $key:")
        values.forEachIndexed { index, field ->
          error.append("\n  ${index + 1}. ${field.name} (${field.location})")
        }
        errors += error.toString()
      }
    }

    var hasCollidingFields = false
    for (collidingFields in nameToField.values) {
      if (collidingFields.size > 1) {
        hasCollidingFields = true
        val first = collidingFields.iterator().next()
        val error = StringBuilder()
        error.append("multiple fields share name ${first.name}:")
        collidingFields.forEachIndexed { index, field ->
          error.append("\n  ${index + 1}. ${field.name} (${field.location})")
        }
        errors += error.toString()
      }
    }

    if (!hasCollidingFields) {
      for ((jsonName, fields) in jsonNameToField) {
        if (fields.size > 1) {
          val error = StringBuilder()
          error.append("multiple fields share same JSON name '$jsonName':")
          fields.forEachIndexed { index, field ->
            error.append("\n  ${index + 1}. ${field.name} (${field.location})")
          }
          errors += error.toString()
        }
      }
    }
  }

  private fun validatePackages() {
    val filesByPackageName: Map<String?, List<FileLinker>> =
      fileLinkers.values.groupBy { it.protoFile.packageName }

    for (fileLinkers in filesByPackageName.values) {
      validateTypeUniqueness(fileLinkers)

      // Enum constants must be unique within each package.
      val types = fileLinkers.flatMap { it.protoFile.types }
      withContext(fileLinkers[0].protoFile).validateEnumConstantNameUniqueness(types)
    }
  }

  private fun validateTypeUniqueness(fileLinkers: List<FileLinker>) {
    val conflictingTypes = fileLinkers
      .flatMap { linker -> linker.protoFile.types.map { it.type to it.location } }
      .groupBy { (type, typeLocation) -> type to typeLocation.toString() }
      .values.filter { it.size > 1 }

    for (typesAndLocations in conflictingTypes) {
      val type = typesAndLocations.first().first
      val error = buildString {
        append("same type '$type' from the same file loaded from different paths:")
        typesAndLocations.forEachIndexed { index, (_, typeLocation) ->
          append("\n  ${index + 1}. base:${typeLocation.base}, path:${typeLocation.copy(base = "")}")
        }
      }
      errors += error
    }
  }

  fun validateEnumConstantNameUniqueness(nestedTypes: Iterable<Type>) {
    val nameToType = mutableMapOf<String, MutableSet<EnumType>>()
    for (type in nestedTypes) {
      if (type is EnumType) {
        for (enumConstant in type.constants) {
          nameToType.getOrPut(enumConstant.name) { mutableSetOf() }.also { it += type }
        }
      }
    }

    for ((constant, values) in nameToType) {
      if (values.size > 1) {
        val error = buildString {
          append("multiple enums share constant $constant:")
          values.forEachIndexed { index, enumType ->
            append(
              "\n  ${index + 1}. ${enumType.type}.$constant " +
                "(${enumType.constant(constant)!!.location})",
            )
          }
        }
        errors += error
      }
    }
  }

  fun validateImportForType(
    location: Location,
    type: ProtoType,
  ) {
    @Suppress("NAME_SHADOWING")
    var type = type

    // Map key type is always scalar. No need to validate it.
    if (type.isMap) type = type.valueType!!

    if (type.isScalar) return

    val path = location.path
    val requiredImport = get(type)!!.location.path
    val fileLinker = getFileLinker(path)
    if (path != requiredImport && !fileLinker.effectiveImports().contains(requiredImport)) {
      errors += "$path needs to import $requiredImport"
    }
  }

  fun validateImportForPath(
    location: Location,
    requiredImport: String,
  ) {
    val path = location.path
    val fileLinker = getFileLinker(path)
    if (path != requiredImport && !fileLinker.effectiveImports().contains(requiredImport)) {
      errors += "$path needs to import $requiredImport"
    }
  }

  /** Returns a new linker that uses [context] to resolve type names and report errors. */
  fun withContext(context: Any) = Linker(this, context)
}
