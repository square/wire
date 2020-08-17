/*
 * Copyright 2018 Outfox, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.outfoxx.swiftpoet

import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

/**
 * A Kotlin file containing top level objects like classes, objects, functions, properties, and type
 * aliases.
 *
 * Items are output in the following order:
 * - Comment
 * - Annotations
 * - Package
 * - Imports
 * - Members
 */
class FileSpec private constructor(builder: FileSpec.Builder) {
  val comment = builder.comment.build()
  val moduleName = builder.moduleName
  val name = builder.name
  val members = builder.members.toList()
  private val moduleImports = builder.moduleImports
  private val indent = builder.indent

  @Throws(IOException::class)
  fun writeTo(out: Appendable) {
    // First pass: emit the entire class, just to collect the modules we'll need to import.
    val importsCollector = CodeWriter(NullAppendable, indent)
    emit(importsCollector)
    val suggestedImports = importsCollector.suggestedImports()

    // Second pass: write the code, taking advantage of the imports.
    val codeWriter = CodeWriter(out, indent, suggestedImports, moduleImports.map { it.name }.toSet())
    emit(codeWriter)
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  fun writeTo(directory: Path) {
    require(Files.notExists(directory) || Files.isDirectory(directory)) {
      "path $directory exists but is not a directory."
    }
    val outputPath = directory.resolve("$name.swift")
    OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8).use { writer -> writeTo(writer) }
  }

  /** Writes this to `directory` as UTF-8 using the standard directory structure.  */
  @Throws(IOException::class)
  fun writeTo(directory: File) = writeTo(directory.toPath())

  private fun emit(codeWriter: CodeWriter) {
    if (comment.isNotEmpty()) {
      codeWriter.emitComment(comment)
    }

    codeWriter.pushModule(moduleName)

    val allImports = moduleImports + codeWriter.importedTypes.map { ImportSpec(it.value.moduleName) }
    val imports = allImports.filter { it.name != "Swift" }

    if (imports.isNotEmpty()) {
      for (import in imports.toSortedSet()) {
        import.emit(codeWriter)
        codeWriter.emit("\n")
      }
      codeWriter.emit("\n")
    }

    members.forEachIndexed { index, member ->
      if (index > 0) codeWriter.emit("\n")
      when (member) {
        is TypeSpec -> member.emit(codeWriter)
        is FunctionSpec -> member.emit(codeWriter, null, setOf(Modifier.PUBLIC))
        is PropertySpec -> member.emit(codeWriter, setOf(Modifier.PUBLIC))
        is TypeAliasSpec -> member.emit(codeWriter)
        is ExtensionSpec -> member.emit(codeWriter)
        else -> throw AssertionError()
      }
    }

    codeWriter.popModule()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { writeTo(this) }

  fun toBuilder(): Builder {
    val builder = Builder(moduleName, name)
    builder.comment.add(comment)
    builder.members.addAll(this.members)
    builder.indent = indent
    builder.moduleImports.addAll(moduleImports)
    return builder
  }

  class Builder internal constructor(
    val moduleName: String,
    val name: String
  ) {
    internal val comment = CodeBlock.builder()
    internal val moduleImports = sortedSetOf<ImportSpec>()
    internal var indent = DEFAULT_INDENT
    internal val members = mutableListOf<Any>()

    init {
      require(name.isName) { "not a valid file name: $name" }
    }

    fun addComment(format: String, vararg args: Any) = apply {
      comment.add(format, *args)
    }

    fun addType(typeSpec: TypeSpec) = apply {
      members += typeSpec
    }

    fun addFunction(functionSpec: FunctionSpec) = apply {
      require(!functionSpec.isConstructor && !functionSpec.isAccessor) {
        "cannot add ${functionSpec.name} to file $name"
      }
      members += functionSpec
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      members += propertySpec
    }

    fun addTypeAlias(typeAliasSpec: TypeAliasSpec) = apply {
      members += typeAliasSpec
    }

    fun addExtension(extensionSpec: ExtensionSpec) = apply {
      members += extensionSpec
    }

    fun addImport(moduleName: String, vararg attributes: AttributeSpec) = apply {
      moduleImports += ImportSpec(moduleName, attributes.toList())
    }

    fun indent(indent: String) = apply {
      this.indent = indent
    }

    fun build() = FileSpec(this)
  }

  companion object {
    @JvmStatic fun get(moduleName: String, typeSpec: TypeSpec): FileSpec {
      return builder(moduleName, typeSpec.name).addType(typeSpec).build()
    }

    @JvmStatic fun builder(moduleName: String, fileName: String) = Builder(moduleName, fileName)

    @JvmStatic fun builder(fileName: String) = Builder("", fileName)
  }
}

internal const val DEFAULT_INDENT = "  "
