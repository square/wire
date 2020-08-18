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

import io.outfoxx.swiftpoet.Modifier.INTERNAL

/** A generated class, protocol, or enum declaration.  */
class TypeSpec private constructor(
   builder: TypeSpec.Builder
) : AttributedSpec(builder.attributes.toImmutableList()) {
  val kind = builder.kind
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val modifiers = kind.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val associatedTypes = builder.associatedTypes.toImmutableList()

  val isEnum = builder.isEnum

  val superTypes = builder.superTypes.toImmutableSet()
  val enumCases = builder.enumCases.toImmutableMap()
  val propertySpecs = builder.propertySpecs.toImmutableList()
  val funSpecs = builder.functionSpecs.toImmutableList()
  val typeSpecs = builder.typeSpecs.toImmutableList()
  val typeAliasSpecs = builder.typeAliasSpecs.toImmutableList()

  fun toBuilder(): Builder {
    val builder = Builder(kind, name)
    builder.kdoc.add(kdoc)
    builder.attributes += attributes
    builder.typeVariables += typeVariables
    builder.superTypes += superTypes
    builder.enumCases += enumCases
    builder.propertySpecs += propertySpecs
    builder.functionSpecs += funSpecs
    builder.typeSpecs += typeSpecs
    builder.typeAliasSpecs += typeAliasSpecs
    builder.associatedTypes += associatedTypes
    return builder
  }

  internal fun emit(codeWriter: CodeWriter, isNestedExternal: Boolean = false) {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    val previousStatementLine = codeWriter.statementLine
    codeWriter.statementLine = -1

    try {
      codeWriter.emitKdoc(kdoc)
      codeWriter.emitAttributes(attributes)
      codeWriter.emitModifiers(kind.modifiers, setOf(INTERNAL))
      codeWriter.emit(kind.declarationKeyword)
      codeWriter.emitCode(" %L", escapeIfNecessary(name))
      codeWriter.emitTypeVariables(typeVariables)

      val superTypes =
         if (superTypes.contains(CLASS)) {
           listOf(CodeBlock.of("%T", CLASS)) + superTypes.filterNot { it == CLASS }.map { type -> CodeBlock.of("%T", type) }
         }
         else {
           superTypes.map { type -> CodeBlock.of("%T", type) }
         }

      if (superTypes.isNotEmpty()) {
        codeWriter.emitCode(superTypes.joinToCode(separator = ",%W", prefix = " : "))
      }

      codeWriter.emitWhereBlock(typeVariables)
      codeWriter.emit(" {\n")

      codeWriter.pushType(this)
      codeWriter.indent()
      var firstMember = true

      if (associatedTypes.isNotEmpty()) {
        codeWriter.emit("\n")
        firstMember = false
        for (associatedType in associatedTypes) {
          codeWriter.emit("associatedtype ")
          associatedType.emit(codeWriter)
          associatedType.bounds.forEach { it.emit(codeWriter) }
          codeWriter.emit("\n")
        }
      }

      if (enumCases.isNotEmpty()) {
        codeWriter.emit("\n")
        firstMember = false
        val i = enumCases.entries.iterator()
        while (i.hasNext()) {
          val enumCase = i.next()
          val enumCaseValue = enumCase.value
          codeWriter.emitCode("case %L", enumCase.key)
          when (enumCaseValue) {
            is String -> codeWriter.emitCode(" = %L", enumCaseValue)
            is TupleTypeName -> enumCaseValue.emit(codeWriter)
          }
          codeWriter.emit("\n")
        }
      }

      // Properties.
      if (propertySpecs.isNotEmpty()) {
        codeWriter.emit("\n")
        for (propertySpec in propertySpecs) {
          propertySpec.emit(codeWriter, kind.implicitPropertyModifiers)
          codeWriter.emit("\n")
        }
        firstMember = false
      }

      // Constructors.
      val constructors = funSpecs.filter { it.isConstructor }
      if (constructors.isNotEmpty()) {
        firstMember = false
        constructors.forEachIndexed { index, funSpec ->
          codeWriter.emit("\n")
          funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers)
          if (index == constructors.size - 1 && funSpec.body === CodeBlock.ABSTRACT) codeWriter.emit("\n")
        }
      }

      // Functions.
      val functions = funSpecs.filterNot { it.isConstructor }
      if (functions.isNotEmpty()) {
        firstMember = false
        functions.forEachIndexed { index, funSpec ->
          codeWriter.emit("\n")
          funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers)
          if (index == functions.size - 1 && funSpec.body === CodeBlock.ABSTRACT) codeWriter.emit("\n")
        }
      }

      // Types.
      if (typeSpecs.isNotEmpty()) {
        firstMember = false
        typeSpecs.forEach { typeSpec ->
          codeWriter.emit("\n")
          typeSpec.emit(codeWriter, isNestedExternal)
        }
      }

      // Type aliases.
      if (typeAliasSpecs.isNotEmpty()) {
        firstMember = false
        typeAliasSpecs.forEach { typeAliasSpec ->
          codeWriter.emit("\n")
          typeAliasSpec.emit(codeWriter)
        }
      }

      codeWriter.unindent()
      codeWriter.popType()

      if (!firstMember) {
        codeWriter.emit("\n")
      }

      codeWriter.emit("}\n")

    } finally {
      codeWriter.statementLine = previousStatementLine
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { emit(CodeWriter(this)) }

  sealed class Kind(
    internal val declarationKeyword: String,
    internal val defaultImplicitPropertyModifiers: Set<Modifier>,
    internal val defaultImplicitFunctionModifiers: Set<Modifier>,
    internal val modifiers: Set<Modifier> = emptySet()
  ) {

    internal val implicitPropertyModifiers get() = defaultImplicitPropertyModifiers

    internal val implicitFunctionModifiers get() = defaultImplicitFunctionModifiers

    abstract fun plusModifiers(vararg modifiers: Modifier): Kind

    override fun toString() = javaClass.simpleName.toUpperCase()

    class Class(vararg modifiers: Modifier) : Kind(
        "class",
        setOf(INTERNAL),
        setOf(INTERNAL),
        modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: Modifier) =
        Class(*(this.modifiers.toTypedArray() + modifiers))
    }

    class Struct(vararg modifiers: Modifier) : Kind(
        "struct",
        setOf(INTERNAL),
        setOf(INTERNAL),
        modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: Modifier) =
              Struct(*(this.modifiers.toTypedArray() + modifiers))
    }

    class Protocol(vararg modifiers: Modifier): Kind(
            "protocol",
            setOf(INTERNAL),
            setOf(INTERNAL),
            modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: Modifier) =
              Protocol(*(this.modifiers.toTypedArray() + modifiers))
    }

    class Enum(vararg modifiers: Modifier): Kind(
            "enum",
            setOf(INTERNAL),
            setOf(INTERNAL),
            modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: Modifier) =
              Enum(*(this.modifiers.toTypedArray() + modifiers))
    }
  }

  class Builder internal constructor(
    internal var kind: Kind,
    internal val name: String
  ) {
    internal val kdoc = CodeBlock.builder()
    internal val attributes = mutableListOf<AttributeSpec>()
    internal val typeVariables = mutableListOf<TypeVariableName>()
    internal val superTypes = mutableSetOf<TypeName>()
    internal val enumCases = mutableMapOf<String, Any?>()
    internal val propertySpecs = mutableListOf<PropertySpec>()
    internal val functionSpecs = mutableListOf<FunctionSpec>()
    internal val typeSpecs = mutableListOf<TypeSpec>()
    internal val typeAliasSpecs = mutableListOf<TypeAliasSpec>()
    internal val associatedTypes = mutableListOf<TypeVariableName>()
    internal val isEnum get() = kind is Kind.Enum
    internal val isClass = kind is Kind.Class
    internal val isStruct = kind is Kind.Struct
    internal val isProtocol = kind is Kind.Protocol

    init {
      require(name.isName) { "not a valid name: $name" }
    }

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAttribute(attribute: AttributeSpec) = apply {
      this.attributes += attribute
    }

    fun addAttribute(name: String, vararg arguments: String) = apply {
      this.attributes += AttributeSpec.builder(name).addArguments(arguments.toList()).build()
    }

    fun addModifiers(vararg modifiers: Modifier) = apply {
      kind = kind.plusModifiers(*modifiers)
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    fun constrainToClass() = apply {
      check(isProtocol) { "${this.name} is not a protocol" }
      this.superTypes.add(CLASS)
    }

    fun addSuperTypes(superTypes: Iterable<TypeName>) = apply {
      this.superTypes += superTypes
    }

    fun addSuperType(superType: TypeName) = apply {
        this.superTypes += superType
    }

    fun addEnumCase(name: String, type: TupleTypeName) = apply {
      check(isEnum) { "${this.name} is not an enum" }
      require(name.isName) { "not a valid enum case: $name" }
      enumCases[name] = type
    }

    fun addEnumCase(name: String, type: TypeName) = apply {
      check(isEnum) { "${this.name} is not an enum" }
      require(name.isName) { "not a valid enum case: $name" }
      enumCases[name] = TupleTypeName.of("" to type)
    }

    fun addEnumCase(name: String, constant: String) = apply {
      check(isEnum) { "${this.name} is not enum" }
      require(name.isName) { "not a valid enum constant: $name" }
      enumCases[name] = constant
    }

    fun addEnumCase(
       name: String
    ) = apply {
      check(isEnum) { "${this.name} is not enum" }
      require(name.isName) { "not a valid enum constant: $name" }
      enumCases[name] = null
    }

    fun addProperties(propertySpecs: Iterable<PropertySpec>) = apply {
      propertySpecs.map(this::addProperty)
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      propertySpecs += propertySpec
    }

    fun addProperty(name: String, type: TypeName, vararg modifiers: Modifier)
       = addProperty(PropertySpec.builder(name, type, *modifiers).build())

    fun addMutableProperty(name: String, type: TypeName, vararg modifiers: Modifier)
       = addProperty(PropertySpec.varBuilder(name, type, *modifiers).build())

    fun addFunctions(functionSpecs: Iterable<FunctionSpec>) = apply {
      functionSpecs.forEach { addFunction(it) }
    }

    fun addFunction(functionSpec: FunctionSpec) = apply {
      check(!isProtocol || functionSpec.body === CodeBlock.ABSTRACT) { "Protocols require abstract functions; see FunctionSpec.abstractBuilder(...)" }
      requireNoneOrOneOf(functionSpec.modifiers, Modifier.OPEN, Modifier.INTERNAL, Modifier.PUBLIC, Modifier.PRIVATE)
      functionSpecs += functionSpec
    }

    fun addTypes(typeSpecs: Iterable<TypeSpec>) = apply {
      check(!isProtocol) { "${this.name} is a protocol, it cannot contain nested types" }
      this.typeSpecs += typeSpecs
    }

    fun addType(typeSpec: TypeSpec) = apply {
      check(!isProtocol) { "${this.name} is a protocol, it cannot contain nested types" }
      typeSpecs += typeSpec
    }

    fun addTypeAliases(typeAliasSpecs: Iterable<TypeAliasSpec>) = apply {
      check(!isProtocol) { "${this.name} is a protocol, it cannot contain nested types" }
      this.typeAliasSpecs += typeAliasSpecs
    }

    fun addTypeAlias(typeAliasSpec: TypeAliasSpec) = apply {
      check(!isProtocol) { "${this.name} is a protocol, it cannot contain nested types" }
      typeAliasSpecs += typeAliasSpec
    }

    fun addAssociatedType(typeVariable: TypeVariableName) = apply {
      check(isProtocol) { "${this.name} is not a protocol, only protocols can have associated types" }
      associatedTypes += typeVariable
    }

    fun build(): TypeSpec {
      require(!isEnum || enumCases.isNotEmpty()) {
        "at least one enum constant is required for $name"
      }

      return TypeSpec(this)
    }
  }

  companion object {
    @JvmStatic fun classBuilder(name: String) = Builder(Kind.Class(), name)

    @JvmStatic fun classBuilder(className: DeclaredTypeName) = classBuilder(className.simpleName)

    @JvmStatic fun structBuilder(name: String) = Builder(Kind.Struct(), name)

    @JvmStatic fun structBuilder(structName: DeclaredTypeName) = structBuilder(structName.simpleName)

    @JvmStatic fun protocolBuilder(name: String) = Builder(Kind.Protocol(), name)

    @JvmStatic fun protocolBuilder(protocolName: DeclaredTypeName) = protocolBuilder(protocolName.simpleName)

    @JvmStatic fun enumBuilder(name: String) = Builder(Kind.Enum(), name)

    @JvmStatic fun enumBuilder(enumName: DeclaredTypeName) = enumBuilder(enumName.simpleName)
  }
}


private object CLASS : TypeName() {
  override fun emit(out: CodeWriter): CodeWriter {
    return out.emit("class")
  }
}

