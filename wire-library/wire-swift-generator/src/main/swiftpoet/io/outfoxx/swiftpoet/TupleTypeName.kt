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

class TupleTypeName internal constructor(
  internal val types: Map<String, TypeName>
) : TypeName() {

  override fun emit(out: CodeWriter): CodeWriter {
    out.emit("(")
    var first = true
    for ((name, type) in types) {
      if (!first) {
        out.emit(", ")
      }
      first = false
      if (name.isEmpty())
        out.emitCode("%T", type)
      else
        out.emitCode("%L: %T", name, type)
    }
    out.emit(")")
    return out
  }

  companion object {
    fun of(vararg types: Pair<String, TypeName>): TupleTypeName {
      return TupleTypeName(mapOf(*types))
    }

    fun from(vararg types: Pair<String, String>): TupleTypeName {
      return TupleTypeName(mapOf(*types).mapValues { DeclaredTypeName.typeName(it.value) })
    }
  }
}
