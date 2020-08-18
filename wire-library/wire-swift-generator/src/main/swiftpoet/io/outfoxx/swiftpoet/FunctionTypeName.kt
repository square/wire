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

class FunctionTypeName internal constructor(
  parameters: List<ParameterSpec> = emptyList(),
  val returnType: TypeName = VOID,
  val escaping: Boolean = false
) : TypeName() {
  val parameters = parameters.toImmutableList()

  init {
    for (param in parameters) {
      require(param.modifiers.isEmpty()) { "Parameters with modifiers are not allowed" }
      require(param.defaultValue == null) { "Parameters with default values are not allowed" }
    }
  }

  override fun emit(out: CodeWriter): CodeWriter {
    if (escaping) {
      out.emit("@escaping ")
    }

    parameters.emit(out)
    out.emitCode(" -> %T", returnType)

    return out
  }

  companion object {
    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      parameters: List<ParameterSpec> = emptyList(),
      returnType: TypeName
    ) = FunctionTypeName(parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      vararg parameters: TypeName = emptyArray(),
      returnType: TypeName
    ): FunctionTypeName {
      return FunctionTypeName(
          parameters.toList().map { ParameterSpec.unnamed(it) },
          returnType)
    }

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
      vararg parameters: ParameterSpec = emptyArray(),
      returnType: TypeName
    ) = FunctionTypeName(parameters.toList(), returnType)
  }
}
