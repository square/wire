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

import io.outfoxx.swiftpoet.TypeVariableName.Bound.Constraint.SAME_TYPE

class TypeVariableName private constructor(
  override val name: String,
  val bounds: List<Bound>
) : TypeName() {

  class Bound internal constructor(val constraint: Constraint? = null, val type: TypeName) {

    constructor(type: TypeName)
            : this(null, type)

    constructor(constraint: Constraint, type: String)
            : this(constraint, DeclaredTypeName.typeName(type))

    constructor(type: String)
            : this(null, DeclaredTypeName.typeName(type))

    enum class Constraint {
      CONFORMS_TO,
      SAME_TYPE
    }

    internal fun emit(out: CodeWriter) =
        out.emitCode(" %L %T", if (constraint == SAME_TYPE) "==" else ":", type)

  }

  fun withBounds(vararg bounds: Bound) = withBounds(bounds.toList())

  fun withBounds(bounds: List<Bound>) = TypeVariableName(name, this.bounds + bounds)

  override fun emit(out: CodeWriter) = out.emit(name)

  companion object {

    fun typeVariable(name: String, bounds: List<Bound>): TypeVariableName {
      return TypeVariableName(name, bounds)
    }

    fun typeVariable(name: String, vararg bounds: Bound): TypeVariableName {
      return TypeVariableName(name, bounds.toList())
    }

    fun bound(constraint: Bound.Constraint, name: String): Bound {
      return Bound(constraint, name)
    }

    fun bound(constraint: Bound.Constraint, typeName: TypeName): Bound {
      return Bound(constraint, typeName)
    }

    fun bound(type: TypeName): Bound {
      return Bound(type)
    }

    fun bound(name: String): Bound {
      return Bound(name)
    }

    /** Returns type variable named `name` with `variance` and without bounds.  */
    @JvmStatic @JvmName("get")
    operator fun invoke(name: String) =
        TypeVariableName.typeVariable(name, listOf())

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get")
    operator fun invoke(name: String, vararg bounds: Bound) =
        TypeVariableName.typeVariable(name, bounds.toList())
  }
}
