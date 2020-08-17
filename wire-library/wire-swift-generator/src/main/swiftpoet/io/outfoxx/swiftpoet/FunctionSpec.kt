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

/** A generated function declaration.  */
class FunctionSpec private constructor(
   builder: Builder
) : AttributedSpec(builder.attributes) {
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val returnType = builder.returnType
  val parameters = builder.parameters.toImmutableList()
  val throws = builder.throws
  val failable = builder.failable
  val body = if (builder.abstract) CodeBlock.ABSTRACT else builder.body.build()

  init {
    require(name != SETTER || parameters.size <= 1) {
      "$name must have zero or one parameter"
    }
  }

  internal fun parameter(name: String) = parameters.firstOrNull { it.parameterName == name }

  internal fun emit(
    codeWriter: CodeWriter,
    enclosingName: String?,
    implicitModifiers: Set<Modifier>
  ) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAttributes(attributes)
    codeWriter.emitModifiers(modifiers, implicitModifiers)

    if (!isConstructor && !name.isAccessor) {
      codeWriter.emit("func ")
    }

    emitSignature(codeWriter, enclosingName)
    codeWriter.emitWhereBlock(typeVariables)

    if (body !== CodeBlock.ABSTRACT) {
      codeWriter.emit(" {\n")
      codeWriter.indent()
      codeWriter.emitCode(body)
      codeWriter.unindent()
      codeWriter.emit("}\n")
    }
  }

  private fun emitSignature(codeWriter: CodeWriter, enclosingName: String?) {
    if (isConstructor) {
      codeWriter.emitCode(CONSTRUCTOR, enclosingName)
      if (failable) {
        codeWriter.emit("?")
      }
    } else if (name == GETTER) {
      codeWriter.emitCode(GETTER)
      return
    } else if (name == SETTER) {
      codeWriter.emitCode(SETTER)
      if (parameters.isEmpty()) {
        return
      }
    } else {
      codeWriter.emitCode("%L", escapeIfNecessary(name))
    }

    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
    }

    parameters.emit(codeWriter) { param ->
      param.emit(codeWriter, includeType = name != SETTER)
    }

    if (throws) {
      codeWriter.emit(" throws")
    }

    if (returnType != null && returnType != VOID) {
      codeWriter.emitCode(" -> %T", returnType)
    }
  }

  val isConstructor get() = name.isConstructor

  val isAccessor get() = name.isAccessor

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString {
    emit(CodeWriter(this), "Constructor", TypeSpec.Kind.Class().implicitFunctionModifiers)
  }

  fun toBuilder(): Builder {
    val builder = Builder(name)
    builder.kdoc.add(kdoc)
    builder.attributes += attributes
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.returnType = returnType
    builder.parameters += parameters
    builder.body.add(body)
    return builder
  }

  class Builder internal constructor(internal val name: String) {
    internal val kdoc = CodeBlock.builder()
    internal val attributes = mutableListOf<AttributeSpec>()
    internal val modifiers = mutableListOf<Modifier>()
    internal val typeVariables = mutableListOf<TypeVariableName>()
    internal var returnType: TypeName? = null
    internal val parameters = mutableListOf<ParameterSpec>()
    internal var throws = false
    internal var failable = false
    internal val body: CodeBlock.Builder = CodeBlock.builder()
    internal var abstract = false

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
      this.modifiers += modifiers
    }

    fun addModifiers(modifiers: Iterable<Modifier>) = apply {
      this.modifiers += modifiers
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      check(!name.isAccessor) { "$name cannot have type variables" }
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      check(!name.isAccessor) { "$name cannot have type variables" }
      typeVariables += typeVariable
    }

    fun returns(returnType: TypeName) = apply {
      check(!name.isConstructor && !name.isAccessor) { "$name cannot have a return type" }
      this.returnType = returnType
    }

    fun addParameters(parameterSpecs: Iterable<ParameterSpec>) = apply {
      for (parameterSpec in parameterSpecs) {
        addParameter(parameterSpec)
      }
    }

    fun addParameter(parameterSpec: ParameterSpec) = apply {
      check(name != GETTER) { "$name cannot have parameters" }
      check(name != SETTER || parameters.size == 0) { "$name can have only one parameter" }
      parameters += parameterSpec
    }

    fun addParameter(name: String, type: TypeName, vararg modifiers: Modifier)
       = addParameter(ParameterSpec.builder(name, type, *modifiers).build())

    fun addParameter(label: String, name: String, type: TypeName, vararg modifiers: Modifier)
       = addParameter(ParameterSpec.builder(label, name, type, *modifiers).build())

    fun addCode(format: String, vararg args: Any) = apply {
      body.add(format, *args)
    }

    fun abstract(value: Boolean) = apply {
      check(body.isEmpty()) { "function with code cannot be abstract" }
      abstract = value
    }

    fun failable(value: Boolean) = apply {
      check(name.isConstructor) { "only constructors can be failable" }
      failable = value
    }

    fun throws(value: Boolean) = apply {
      throws = value
    }

    fun addNamedCode(format: String, args: Map<String, *>) = apply {
      check(!abstract) { "abstract functions cannot have code" }
      body.addNamed(format, args)
    }

    fun addCode(codeBlock: CodeBlock) = apply {
      check(!abstract) { "abstract functions cannot have code" }
      body.add(codeBlock)
    }

    fun addComment(format: String, vararg args: Any) = apply {
      body.add("// " + format + "\n", *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * * Shouldn't contain braces or newline characters.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any) = apply {
      body.beginControlFlow(controlFlow, *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     * *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any) = apply {
      body.nextControlFlow(controlFlow, *args)
    }

    fun endControlFlow() = apply {
      body.endControlFlow()
    }

    fun addStatement(format: String, vararg args: Any) = apply {
      body.addStatement(format, *args)
    }

    fun build() = FunctionSpec(this)
  }

  companion object {
    private const val CONSTRUCTOR = "init"
    internal const val GETTER = "get"
    internal const val SETTER = "set"

    internal val String.isConstructor get() = this == CONSTRUCTOR
    internal val String.isAccessor get() = this.isOneOf(GETTER, SETTER)

    @JvmStatic fun builder(name: String) = Builder(name)

    @JvmStatic fun abstractBuilder(name: String) = Builder(name).abstract(true)

    @JvmStatic fun constructorBuilder() = Builder(CONSTRUCTOR)

    @JvmStatic fun getterBuilder() = Builder(GETTER)

    @JvmStatic fun setterBuilder() = Builder(SETTER)
  }
}
