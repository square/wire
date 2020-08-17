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

/** A generated parameter declaration.  */
class ParameterSpec private constructor(
   builder: ParameterSpec.Builder
) : AttributedSpec(builder.attributes) {
  val argumentLabel = builder.argumentLabel
  val parameterName = builder.parameterName
  val modifiers = builder.modifiers.toImmutableSet()
  val type = builder.type
  val variadic = builder.variadic
  val defaultValue = builder.defaultValue

  internal fun emit(codeWriter: CodeWriter, includeType: Boolean = true) {
    argumentLabel?.let { codeWriter.emitCode("%L ", escapeIfNecessary(it)) }
    codeWriter.emitCode("%L", escapeIfNecessary(parameterName))
    if (includeType) {
      codeWriter.emit(": ")
      codeWriter.emitModifiers(modifiers)
      codeWriter.emitCode("%T", type)
      if (variadic) {
        codeWriter.emit("...")
      }
    }
    emitDefaultValue(codeWriter)
  }

  internal fun emitDefaultValue(codeWriter: CodeWriter) {
    if (defaultValue != null) {
      codeWriter.emitCode(" = %[%L%]", defaultValue)
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

  fun toBuilder(type: TypeName): Builder {
    return toBuilder(argumentLabel, parameterName, type)
  }

  fun toBuilder(labelAndName: String, type: TypeName): Builder {
    return toBuilder(labelAndName, labelAndName, type)
  }

  fun toBuilder(argumentLabel: String?, parameterName: String, type: TypeName): Builder {
    val builder = Builder(argumentLabel, parameterName, type)
    builder.modifiers += modifiers
    builder.defaultValue = defaultValue
    return builder
  }

  class Builder internal constructor(
    internal val argumentLabel: String?,
    internal val parameterName: String,
    internal val type: TypeName
  ) {
    internal val attributes = mutableListOf<AttributeSpec>()
    internal val modifiers = mutableListOf<Modifier>()
    internal var variadic = false
    internal var defaultValue: CodeBlock? = null

    fun variadic(value: Boolean) = apply {
      variadic = value
    }

    fun addModifiers(vararg modifiers: Modifier) = apply {
      addModifiers(modifiers.asList())
    }

    fun addModifiers(modifiers: Iterable<Modifier>) = apply {
      modifiers.forEach { it.checkTarget(Modifier.Target.PARAMETER) }
      requireNoneOrOneOf(setOf(Modifier.OPEN, Modifier.PUBLIC, Modifier.PRIVATE, Modifier.FILEPRIVATE, Modifier.INTERNAL))
      this.modifiers += modifiers
    }

    fun defaultValue(format: String, vararg args: Any?) = defaultValue(CodeBlock.of(format, *args))

    fun defaultValue(codeBlock: CodeBlock) = apply {
      check(this.defaultValue == null) { "initializer was already set" }
      this.defaultValue = codeBlock
    }

    fun build() = ParameterSpec(this)
  }

  companion object {

    @JvmStatic fun builder(argumentLabel: String, parameterName: String, type: TypeName, vararg modifiers: Modifier): Builder {
      return Builder(argumentLabel, parameterName, type).addModifiers(*modifiers)
    }

    @JvmStatic fun builder(parameterName: String, type: TypeName, vararg modifiers: Modifier): Builder {
      return Builder(null, parameterName, type).addModifiers(*modifiers)
    }

    @JvmStatic fun unnamed(typeName: TypeName): ParameterSpec {
      return Builder("", "", typeName).build()
    }

  }
}

internal fun List<ParameterSpec>.emit(
  codeWriter: CodeWriter,
  forceNewLines: Boolean = false,
  emitBlock: (ParameterSpec) -> Unit = { it.emit(codeWriter) }
) = with(codeWriter) {
  val params = this@emit
  emit("(")
  when {
    size > 2 || forceNewLines -> {
      emit("\n")
      indent(1)
      forEachIndexed { index, parameter ->
        if (index > 0) emit(",\n")
        emitBlock(parameter)
      }
      unindent(1)
      emit("\n")
    }
    size == 0 -> emit("")
    size == 1 -> emitBlock(params[0])
    size == 2 -> {
      emitBlock(params[0])
      emit(", ")
      emitBlock(params[1])
    }
  }
  emit(")")
}
