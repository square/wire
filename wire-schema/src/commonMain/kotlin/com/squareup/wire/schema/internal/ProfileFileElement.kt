/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal

import com.squareup.wire.schema.Location

/**
 * A single `.wire` file. This file is structured similarly to a `.proto` file, but with
 * different elements.
 *
 * File Structure
 * --------------
 *
 * A project may have 0 or more `.wire` files. These files should be in the same directory as
 * the `.proto` files so they may be automatically discovered by Wire.
 *
 *
 * Each file starts with a syntax declaration. The syntax must be "wire2". This is followed by an
 * optional package declaration, which should match to the package declarations of the `.proto` files in the directory.
 *
 *
 * Profiles may import any number of proto files. Note that it is an error to import `.wire` files.
 * These imports are used to resolve types specified later in the file.
 *
 *
 * Profiles may specify any number of type configurations. These specify a fully qualified
 * type, its target Java type, and an adapter to do the encoding and decoding.
 *
 * ```
 * syntax = "wire2";
 * package squareup.dinosaurs;
 *
 * import "squareup/geology/period.proto";
 *
 * // Roar!
 * type squareup.dinosaurs.Dinosaur {
 * target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;
 * }
 * ```
 */
data class ProfileFileElement(
  val location: Location,
  val packageName: String? = null,
  val imports: List<String> = emptyList(),
  val typeConfigs: List<TypeConfigElement> = emptyList(),
) {

  fun toSchema() = buildString {
    append("// $location\n")
    append("syntax = \"wire2\";\n")
    if (packageName != null) {
      append("package $packageName;\n")
    }
    if (!imports.isEmpty()) {
      append('\n')
      for (file in imports) {
        append("import \"$file\";\n")
      }
    }
    if (!typeConfigs.isEmpty()) {
      append('\n')
      for (typeConfigElement in typeConfigs) {
        append(typeConfigElement.toSchema())
      }
    }
  }
}
