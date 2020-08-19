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

import java.io.StringWriter

/**
 * Any type in Swift's type system. This class identifies simple types like `Int` and `String`,
 * composite types like `Optional<String>` and `Set<String>`, and unassignable types like `Void`.
 *
 * Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for `Swift.Array` doesn't know about the `count` property, the fact that arrays are
 * collections, or even that it accepts a single type parameter.
 *
 * Instances of this class are immutable value objects that implement `equals()` and `hashCode()`
 * properly.
 *
 * Defining new types
 * ------------------
 *
 * Create new reference types like `MyModule.HelloWorld` with [DeclaredTypeName.typeName]. To build composite
 * types like `Set<Long>`, use the factory methods on [ParameterizedTypeName] and [TypeVariableName].
 */
abstract class TypeName internal constructor() {

  /** Lazily-initialized toString of this type name.  */
  private val cachedString: String by lazy {
    buildString {
      val codeWriter = CodeWriter(this)
      emit(codeWriter)
    }
  }

  open val optional: Boolean = false

  open fun makeOptional(): ParameterizedTypeName = wrapOptional()
  open fun makeNonOptional(): TypeName = unwrapOptional()

  open fun wrapOptional(): ParameterizedTypeName = OPTIONAL.parameterizedBy(this)
  open fun unwrapOptional(): TypeName = this

  open val name: String
    get() {
      val out = StringWriter()
      emit(CodeWriter(out))
      return out.toString()
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = cachedString

  internal abstract fun emit(out: CodeWriter): CodeWriter

}
