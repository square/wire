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
package com.squareup.wire.schema

import com.palantir.javapoet.ClassName
import com.squareup.kotlinpoet.ClassName as KClassName

/**
 * A constant field that identifies a [ProtoAdapter]. This should be a string like like
 * `com.squareup.dinosaurs.Dinosaur#ADAPTER` with a fully qualified class name, a `#`, and a
 * field name.
 */
data class AdapterConstant(
  @JvmField val javaClassName: ClassName,
  @JvmField val kotlinClassName: KClassName,
  @JvmField val memberName: String,
) {
  companion object {
    operator fun invoke(adapter: String): AdapterConstant {
      val names = adapter.split("#").toTypedArray()
      require(names.size == 2) { "Illegally formatted adapter: $adapter." }
      return AdapterConstant(
        javaClassName = ClassName.bestGuess(names[0]),
        kotlinClassName = KClassName.bestGuess(names[0]),
        memberName = names[1],
      )
    }
  }
}
