/*
 * Copyright (C) 2019 Square, Inc.
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

internal class FileLinker(
  val protoFile: ProtoFile,
  private val linker: Linker,
) {
  /** Lazily computed set of files used to reference other types and options. */
  private var effectiveImports: Set<String>? = null

  /** True once this linker has registered its types with the enclosing linker. */
  private var typesRegistered = false
  private var extensionsLinked = false
  private var importedExtensionsRegistered = false
  private var extensionOptionsLinked = false
  private var importedExtensionOptionsLinked = false
  private var fileOptionsLinked = false

  /** The set of types defined in this file whose members have been linked. */
  private val typesWithMembersLinked: MutableSet<ProtoType> = LinkedHashSet()

  /**
   * Returns all effective imports. This is computed on-demand by unioning all direct imports plus
   * the recursive set of all public imports.
   */
  fun effectiveImports(): Set<String> {
    if (effectiveImports == null) {
      val sink: MutableSet<String> = LinkedHashSet()
      addImportsRecursive(sink, protoFile.imports)
      addImportsRecursive(sink, protoFile.publicImports)
      effectiveImports = LinkedHashSet(sink)
    }
    return effectiveImports!!
  }

  private fun addImportsRecursive(
    sink: MutableSet<String>,
    paths: Collection<String>,
  ) {
    for (path in paths) {
      if (sink.add(path)) {
        val fileLinker = linker.getFileLinker(path)
        addImportsRecursive(sink, fileLinker.protoFile.publicImports)

        if (linker.loadExhaustively) {
          addImportsRecursive(sink, fileLinker.protoFile.imports)
        }
      }
    }
  }

  fun requireTypesRegistered() {
    if (typesRegistered) return
    typesRegistered = true

    for (type in protoFile.types) {
      addTypes(type)
    }
  }

  private fun addTypes(type: Type) {
    linker.addType(type.type, type)
    for (nestedType in type.nestedTypes) {
      addTypes(nestedType)
    }
  }

  fun requireExtensionsLinked() {
    if (extensionsLinked) return
    extensionsLinked = true

    requireTypesRegistered()
    for (extend in protoFile.extendList) {
      extend.link(linker)
    }
    for (type in protoFile.types) {
      linkNestedExtensions(type, linker)
    }
  }

  private fun linkNestedExtensions(type: Type, linker: Linker) {
    val typeLinker = linker.withContext(type)
    for (extend in type.nestedExtendList) {
      extend.link(typeLinker)
    }
    for (nested in type.nestedTypes) {
      linkNestedExtensions(nested, typeLinker)
    }
  }

  /**
   * This file might use extensions defined on one of the files we import. Make sure those
   * extensions are registered before we try to use our extensions.
   */
  fun requireImportedExtensionsRegistered() {
    if (importedExtensionsRegistered) return
    importedExtensionsRegistered = true

    for (importedFileLinker in linker.contextImportedTypes()) {
      importedFileLinker.requireExtensionsLinked()
    }
  }

  fun requireExtensionOptionsLinked(validate: Boolean) {
    if (extensionOptionsLinked) return
    extensionOptionsLinked = true

    val syntaxRules = SyntaxRules.get(protoFile.syntax)
    for (extend in protoFile.extendList) {
      extend.linkOptions(linker, syntaxRules, validate)
    }
    for (type in protoFile.types) {
      linkNestedExtensionOptions(type, linker, syntaxRules, validate)
    }
  }

  private fun linkNestedExtensionOptions(type: Type, linker: Linker, syntaxRules: SyntaxRules, validate: Boolean) {
    val typeLinker = linker.withContext(type)
    for (extend in type.nestedExtendList) {
      extend.linkOptions(typeLinker, syntaxRules, validate)
    }
    for (nested in type.nestedTypes) {
      linkNestedExtensionOptions(nested, typeLinker, syntaxRules, validate)
    }
  }

  fun requireImportedExtensionOptionsLinked(validate: Boolean) {
    if (importedExtensionOptionsLinked) return
    importedExtensionOptionsLinked = true

    for (importedFileLinker in linker.contextImportedTypes()) {
      importedFileLinker.requireExtensionOptionsLinked(validate)
    }
  }

  fun linkMembers() {
    linkMembersRecursive(protoFile.types)
    for (service in protoFile.services) {
      service.link(linker)
    }
  }

  /** Link the members of [types] and their nested types. */
  private fun linkMembersRecursive(types: List<Type>) {
    for (type in types) {
      requireMembersLinked(type)
      linkMembersRecursive(type.nestedTypes)
    }
  }

  /** Link the members of [type] that haven't been linked already. */
  fun requireMembersLinked(type: Type) {
    if (typesWithMembersLinked.add(type.type)) {
      type.linkMembers(linker)
    }
  }

  /**
   * This requires traversal of members of imported types! This may potentially include non-direct
   * dependencies!
   */
  fun linkOptions(syntaxRules: SyntaxRules, validate: Boolean) {
    requireFileOptionsLinked(validate)
    requireExtensionOptionsLinked(validate)
    for (type in protoFile.types) {
      type.linkOptions(linker, syntaxRules, validate)
    }
    for (service in protoFile.services) {
      service.linkOptions(linker, validate)
    }
  }

  fun requireFileOptionsLinked(validate: Boolean) {
    if (fileOptionsLinked) return
    fileOptionsLinked = true

    protoFile.linkOptions(linker, validate)
  }

  fun validate(syntaxRules: SyntaxRules) {
    for (type in protoFile.types) {
      type.validate(linker, syntaxRules)
    }
    for (service in protoFile.services) {
      service.validate(linker)
    }
    for (extend in protoFile.extendList) {
      extend.validate(linker, syntaxRules)
    }
  }
}
