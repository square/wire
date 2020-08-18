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

import io.outfoxx.swiftpoet.CodeBlock.Companion.ABSTRACT
import io.outfoxx.swiftpoet.FunctionSpec.Companion.GETTER
import io.outfoxx.swiftpoet.FunctionSpec.Companion.SETTER

/** A generated property declaration.  */
class PropertySpec private constructor(
   builder: Builder
) : AttributedSpec(builder.attributes) {
  val mutable = builder.mutable
  val name = builder.name
  val type = builder.type
  val kdoc = builder.kdoc.build()
  val modifiers = builder.modifiers.toImmutableSet()
  val initializer = builder.initializer
  val getter = builder.getter
  val setter = builder.setter

  internal fun emit(
    codeWriter: CodeWriter,
    implicitModifiers: Set<Modifier>,
    withInitializer: Boolean = true
  ) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAttributes(attributes)
    codeWriter.emitModifiers(modifiers, implicitModifiers)
    codeWriter.emit(if (mutable || getter != null || setter != null) "var " else "let ")
    codeWriter.emitCode("%L: %T", escapeIfNecessary(name), type)
    if (withInitializer && initializer != null) {
      codeWriter.emitCode(" = %[%L%]", initializer)
    }

    if (getter != null || setter != null) {

      // Support concise syntax (e.g. "{ get set }") for protocol property declarations
      if ((getter == null || getter.body == ABSTRACT) &&
         (setter == null || setter.body == ABSTRACT)) {
        codeWriter.emit(" { ")
        if (getter != null) codeWriter.emit("${getter.name} ")
        if (setter != null) codeWriter.emit("${setter.name} ")
        codeWriter.emit("}")
        return
      }

      codeWriter.emit(" {\n")
      if (getter != null) {
        codeWriter.emitCode("%>")
        getter.emit(codeWriter, null, implicitModifiers)
        codeWriter.emitCode("%<")
      }
      if (setter != null) {
        codeWriter.emitCode("%>")
        setter.emit(codeWriter, null, implicitModifiers)
        codeWriter.emitCode("%<")
      }

      codeWriter.emit("}")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { emit(CodeWriter(this), emptySet()) }

  fun toBuilder(): Builder {
    val builder = Builder(name, type)
    builder.mutable = mutable
    builder.kdoc.add(kdoc)
    builder.modifiers += modifiers
    builder.initializer = initializer
    builder.setter = setter
    builder.getter = getter
    return builder
  }

  class Builder internal constructor(internal val name: String, internal val type: TypeName) {
    internal var mutable = false
    internal val kdoc = CodeBlock.builder()
    internal val attributes = mutableListOf<AttributeSpec>()
    internal val modifiers = mutableListOf<Modifier>()
    internal var initializer: CodeBlock? = null
    internal var getter: FunctionSpec? = null
    internal var setter: FunctionSpec? = null

    fun mutable(mutable: Boolean) = apply {
      this.mutable = mutable
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
      modifiers.forEach { it.checkTarget(Modifier.Target.PROPERTY) }
      this.modifiers += modifiers
    }

    fun initializer(format: String, vararg args: Any?) = initializer(CodeBlock.of(format, *args))

    fun initializer(codeBlock: CodeBlock) = apply {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
    }

    fun getter(getter: FunctionSpec) = apply {
      require(getter.name == GETTER) { "${getter.name} is not a getter" }
      check(this.getter == null) { "getter was already set" }
      this.getter = getter
    }

    fun setter(setter: FunctionSpec) = apply {
      require(setter.name == SETTER) { "${setter.name} is not a setter" }
      check(this.setter == null) { "setter was already set" }
      this.setter = setter
    }

    fun abstractGetter() = apply {
      this.getter = FunctionSpec.getterBuilder().abstract(true).build()
    }

    fun abstractSetter() = apply {
      this.setter = FunctionSpec.setterBuilder().abstract(true).build()
    }

    fun build() = PropertySpec(this)
  }

  companion object {
    @JvmStatic fun builder(name: String, type: TypeName, vararg modifiers: Modifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun varBuilder(name: String, type: TypeName, vararg modifiers: Modifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type)
          .mutable(true)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun abstractBuilder(name: String, type: TypeName, vararg modifiers: Modifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type)
         .mutable(true)
         .addModifiers(*modifiers)
    }
  }
}
