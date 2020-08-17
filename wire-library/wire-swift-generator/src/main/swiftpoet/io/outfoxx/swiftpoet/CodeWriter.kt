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

/** Sentinel value that indicates that no user-provided package has been set.  */
private val NO_MODULE = String()

/**
 * Converts a [FileSpec] to a string suitable to both human- and kotlinc-consumption. This honors
 * imports, indentation, and deferred variable names.
 */
internal class CodeWriter constructor(
  out: Appendable,
  private val indent: String = DEFAULT_INDENT,
  internal val importedTypes: Map<String, DeclaredTypeName> = emptyMap(),
  private val importedModules: Set<String> = emptySet()
) {
  private val out = LineWrapper(out, indent, 100)
  private var indentLevel = 0

  private var kdoc = false
  private var comment = false
  private var moduleName = NO_MODULE
  private val typeSpecStack = mutableListOf<TypeSpec>()
  private val importableTypes = mutableMapOf<String, DeclaredTypeName>()
  private var trailingNewline = false

  /**
   * When emitting a statement, this is the line of the statement currently being written. The first
   * line of a statement is indented normally and subsequent wrapped lines are double-indented. This
   * is -1 when the currently-written line isn't part of a statement.
   */
  var statementLine = -1

  fun indent(levels: Int = 1) = apply {
    indentLevel += levels
  }

  fun unindent(levels: Int = 1) = apply {
    require(indentLevel - levels >= 0) { "cannot unindent $levels from $indentLevel" }
    indentLevel -= levels
  }

  fun pushModule(moduleName: String) = apply {
    require(this.moduleName === NO_MODULE) { "module already set: ${this.moduleName}" }
    this.moduleName = moduleName
  }

  fun popModule() = apply {
    require(this.moduleName !== NO_MODULE) { "module already set: ${this.moduleName}" }
    this.moduleName = NO_MODULE
  }

  fun pushType(type: TypeSpec) = apply {
    this.typeSpecStack.add(type)
  }

  fun popType() = apply {
    this.typeSpecStack.removeAt(typeSpecStack.size - 1)
  }

  fun emitComment(codeBlock: CodeBlock) {
    trailingNewline = true // Force the '//' prefix for the comment.
    comment = true
    try {
      emitCode(codeBlock)
      emit("\n")
    } finally {
      comment = false
    }
  }

  fun emitKdoc(kdocCodeBlock: CodeBlock) {
    if (kdocCodeBlock.isEmpty()) return

    emit("/**\n")
    kdoc = true
    try {
      emitCode(kdocCodeBlock)
    } finally {
      kdoc = false
    }
    emit(" */\n")
  }

  /**
   * Emits `attributes` in declaration order.
   */
  fun emitAttributes(
     attributes: List<AttributeSpec>,
     separator: String = "\n",
     suffix: String = "\n"
  ) {
    if (attributes.isNotEmpty()) {
      var firstAttribute = true
      attributes.forEach {
        if (!firstAttribute) emit(separator)
        firstAttribute = false
        it.emit(this)
      }
      emit(suffix)
    }
  }

  /**
   * Emits `modifiers` in the standard order. Modifiers in `implicitModifiers` will not
   * be emitted.
   */
  fun emitModifiers(
    modifiers: Set<Modifier>,
    implicitModifiers: Set<Modifier> = emptySet()
  ) {
    if (modifiers.isEmpty()) return
    for (modifier in modifiers.toEnumSet()) {
      if (implicitModifiers.contains(modifier)) continue
      emit(modifier.keyword)
      emit(" ")
    }
  }

  /**
   * Emit type variables with their bounds. If a type variable has more than a single bound - call
   * [emitWhereBlock] with same input to produce an additional `where` block.
   *
   * This should only be used when declaring type variables; everywhere else bounds are omitted.
   */
  fun emitTypeVariables(typeVariables: List<TypeVariableName>) {
    if (typeVariables.isEmpty()) return

    emit("<")
    typeVariables.forEachIndexed { index, typeVariable ->
      if (index > 0) emit(", ")
      emitCode("%L", typeVariable.name)
      if (typeVariables.size <= 2 && typeVariables.all { it.bounds.size == 1 }) {
        typeVariable.bounds[0].emit(this)
      }
    }
    emit(">")
  }

  /**
   * Emit a `where` block containing type bounds for each type variable that has at least two
   * bounds.
   */
  fun emitWhereBlock(typeVariables: List<TypeVariableName>, forceOutput: Boolean = false) {
    if (typeVariables.isEmpty()) return

    typeVariables.forEachIndexed { index, typeVariable ->
      if (forceOutput || typeVariables.size > 2 || typeVariables.any { it.bounds.size > 1 }) {
        for (bound in typeVariable.bounds) {
          if (index > 0) emitCode(",%W") else emitCode("%Wwhere ")
          emitCode("%T", typeVariable)
          bound.emit(this)
        }
      }
    }
  }

  fun emitCode(s: String) = emitCode(CodeBlock.of(s))

  fun emitCode(format: String, vararg args: Any?) = emitCode(CodeBlock.of(format, *args))

  fun emitCode(codeBlock: CodeBlock) = apply {
    var a = 0
    val partIterator = codeBlock.formatParts.listIterator()
    while (partIterator.hasNext()) {
      val part = partIterator.next()
      when (part) {
        "%L" -> emitLiteral(codeBlock.args[a++])

        "%N" -> emit(codeBlock.args[a++] as String)

        "%S" -> {
          val string = codeBlock.args[a++] as String?
          // Emit null as a literal null: no quotes.
          emit(if (string != null)
            stringLiteralWithQuotes(string) else
            "null")
        }

        "%T" -> {
          val typeName = codeBlock.args[a++] as TypeName
          typeName.emit(this)
        }

        "%%" -> emit("%")

        "%>" -> indent()

        "%<" -> unindent()

        "%[" -> {
          check(statementLine == -1) { "statement enter %[ followed by statement enter %[" }
          statementLine = 0
        }

        "%]" -> {
          check(statementLine != -1) { "statement exit %] has no matching statement enter %[" }
          if (statementLine > 0) {
            unindent(2) // End a multi-line statement. Decrease the indentation level.
          }
          statementLine = -1
        }

        "%W" -> out.wrappingSpace(indentLevel + 2)

        else -> emit(part)
      }
    }
  }

  fun emitWrappingSpace() = apply {
    out.wrappingSpace(indentLevel + 2)
  }

  private fun emitLiteral(o: Any?) {
    when (o) {
      is TypeSpec -> o.emit(this)
      is PropertySpec -> o.emit(this, emptySet())
      is CodeBlock -> emitCode(o)
      else -> emit(o.toString())
    }
  }

  /**
   * Returns the best name to identify `typeName` with in the current context. This uses the
   * available imports and the current scope to find the shortest name available. It does not honor
   * names visible due to inheritance.
   */
  fun lookupName(typeName: DeclaredTypeName): String {
    // Find the shortest suffix of typeName that resolves to typeName. This uses both local type
    // names (so `Entry` in `Map` refers to `Map.Entry`). Also uses imports.
    var nameResolved = false
    var c: DeclaredTypeName? = typeName
    while (c != null) {
      val simpleName = c.simpleName
      val resolved = resolve(simpleName)
      nameResolved = resolved != null

      if (resolved == c.unwrapOptional()) {
        val suffixOffset = c.simpleNames.size - 1
        return typeName.simpleNames.subList(suffixOffset, typeName.simpleNames.size).joinToString(".")
      }

      c = c.enclosingTypeName()
    }

    // If the name resolved but wasn't a match, we're stuck with the fully qualified name.
    if (nameResolved) {
      return typeName.canonicalName
    }

    // If the type is in the same module, we're done.
    if (moduleName == typeName.moduleName) {
      return typeName.simpleNames.joinToString(".")
    }

    // If the type is in a manually imported module and doesn't clash, use an unqualified type
    if (importedModules.contains(typeName.moduleName) && !importableTypes.containsKey(typeName.simpleName)) {
      return typeName.simpleName
    }

    // We'll have to use the fully-qualified name. Mark the type as importable for a future pass.
    if (!kdoc) {
      importableType(typeName)
    }

    return typeName.canonicalName
  }

  private fun importableType(typeName: DeclaredTypeName) {
    if (typeName.moduleName.isEmpty()) {
      return
    }
    val topLevelTypeName = typeName.topLevelTypeName()
    val simpleName = topLevelTypeName.simpleName
    val replaced = importableTypes.put(simpleName, topLevelTypeName)
    if (replaced != null) {
      importableTypes[simpleName] = replaced // On collision, prefer the first inserted.
    }
  }

  /**
   * Returns the type referenced by `simpleName`, using the current nesting context.
   */
  // TODO(jwilson): also honor supertype members when resolving names.
  private fun resolve(simpleName: String): DeclaredTypeName? {
    // Match a child of the current (potentially nested) type.
    for (i in typeSpecStack.indices.reversed()) {
      val typeSpec = typeSpecStack[i]
      for (visibleChild in typeSpec.typeSpecs) {
        if (visibleChild.name == simpleName) {
          return stackTypeName(i, simpleName)
        }
      }
    }

    // Match the top-level type.
    if (typeSpecStack.size > 0 && typeSpecStack[0].name == simpleName) {
      return DeclaredTypeName(moduleName, simpleName)
    }

    // Match an imported type.
    val importedType = importedTypes[simpleName]
    if (importedType != null) return importedType

    // No match.
    return null
  }

  /** Returns the type named `simpleName` when nested in the type at `stackDepth`.  */
  private fun stackTypeName(stackDepth: Int, simpleName: String): DeclaredTypeName {
    var typeName = DeclaredTypeName(moduleName, typeSpecStack[0].name)
    for (i in 1..stackDepth) {
      typeName = typeName.nestedType(typeSpecStack[i].name)
    }
    return typeName.nestedType(simpleName)
  }

  /**
   * Emits `s` with indentation as required. It's important that all code that writes to
   * [CodeWriter.out] does it through here, since we emit indentation lazily in order to avoid
   * unnecessary trailing whitespace.
   */
  fun emit(s: String) = apply {
    var first = true
    for (line in s.split('\n')) {
      // Emit a newline character. Make sure blank lines in KDoc & comments look good.
      if (!first) {
        if ((kdoc || comment) && trailingNewline) {
          emitIndentation()
          out.append(if (kdoc) " *" else "//")
        }
        out.append("\n")
        trailingNewline = true
        if (statementLine != -1) {
          if (statementLine == 0) {
            indent(2) // Begin multiple-line statement. Increase the indentation level.
          }
          statementLine++
        }
      }

      first = false
      if (line.isEmpty()) continue // Don't indent empty lines.

      // Emit indentation and comment prefix if necessary.
      if (trailingNewline) {
        emitIndentation()
        if (kdoc) {
          out.append(" * ")
        } else if (comment) {
          out.append("// ")
        }
      }

      out.append(line)
      trailingNewline = false
    }
  }

  private fun emitIndentation() {
    for (j in 0 until indentLevel) {
      out.append(indent)
    }
  }

  /**
   * Returns the modules that should have been imported for this code.
   */
  fun suggestedImports(): Map<String, DeclaredTypeName> {
    return importableTypes
  }
}
