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

class ParameterizedTypeName internal constructor(
  private val enclosingType: TypeName?,
  val rawType: DeclaredTypeName,
  typeArguments: List<TypeName>
) : TypeName() {
  val typeArguments = typeArguments.toImmutableList()

  init {
    require(typeArguments.isNotEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override val optional get() = rawType == OPTIONAL

  override fun makeOptional() =
     if (optional)
       this
     else
       wrapOptional()

  override fun makeNonOptional() =
     if (optional)
       typeArguments[0].makeNonOptional()
     else
       this

  override fun unwrapOptional() =
     if (optional)
       typeArguments[0]
     else
       this

  override fun emit(out: CodeWriter): CodeWriter {
    when (rawType) {
      OPTIONAL -> out.emitCode("%T?", typeArguments[0])
      ARRAY -> out.emitCode("[%T]", typeArguments[0])
      DICTIONARY -> out.emitCode("[%T : %T]", typeArguments[0], typeArguments[1])
      else -> {
        if (enclosingType != null) {
          enclosingType.emit(out)
          out.emit("." + rawType.simpleName)
        }
        else {
          rawType.emit(out)
        }
        if (typeArguments.isNotEmpty()) {
          out.emit("<")
          typeArguments.forEachIndexed { index, parameter ->
            if (index > 0) out.emit(", ")
            parameter.emit(out)
          }
          out.emit(">")
        }
      }
    }
    return out
  }

  /**
   * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested inside this
   * type, with the specified `typeArguments`.
   */
  fun nestedType(name: String, typeArguments: List<TypeName>)
      = ParameterizedTypeName(this, rawType.nestedType(name), typeArguments)

  companion object {
    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
  }
}

fun DeclaredTypeName.parameterizedBy(vararg typeArguments: TypeName)
   = ParameterizedTypeName(null, this, typeArguments.toList())
