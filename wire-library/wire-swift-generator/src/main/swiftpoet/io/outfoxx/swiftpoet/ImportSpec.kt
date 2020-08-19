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

class ImportSpec internal constructor(
   internal val name: String,
   attributes: List<AttributeSpec> = listOf()
) : AttributedSpec(attributes), Comparable<ImportSpec> {

  private val importString = buildString {
    append(name)
  }

  internal fun emit(out: CodeWriter): CodeWriter {
    out.emitAttributes(attributes, suffix = "")
    out.emit("import $name")
    return out
  }

  override fun toString() = importString

  override fun compareTo(other: ImportSpec) = importString.compareTo(other.importString)

}
