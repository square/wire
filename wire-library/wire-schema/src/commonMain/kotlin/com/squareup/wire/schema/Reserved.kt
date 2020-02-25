/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.ReservedElement
import kotlin.jvm.JvmStatic

class Reserved(
  val location: Location,
  val documentation: String,
  val values: List<Any>
) {
  fun matchesTag(tag: Int) = values.any {
    it is Int && tag == it || (it as? IntRange)?.contains(tag) == true
  }

  fun matchesName(name: String) = values.any { it is String && name == it }

  companion object {
    @JvmStatic
    fun fromElements(elements: List<ReservedElement>) =
      elements.map { Reserved(it.location, it.documentation, it.values) }

    @JvmStatic
    fun toElements(reserveds: List<Reserved>) =
      reserveds.map { ReservedElement(it.location, it.documentation, it.values) }
  }
}
