/*
 * Copyright 2020 Square Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.EnumAdapter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireEnumConstant

/**
 * Encodes enums using their declared names as defined in [WireEnumConstant] or their generated
 * names if the declared name is empty. Decodes using either their declared names, their generated
 * names, or their tags.
 */
class EnumJsonFormatter<E : WireEnum>(
  adapter: EnumAdapter<E>
) : JsonFormatter<E> {
  private val stringToValue: Map<String, E>
  private val valueToString: Map<E, String>

  init {
    val mutableStringToValue = mutableMapOf<String, E>()
    val mutableValueToString = mutableMapOf<E, String>()

    // E is a subtype of Enum<*>, but we don't know that statically.
    val enumType = adapter.type!!.java as Class<E>
    for (constant in enumType.enumConstants) {
      val name = (constant as Enum<*>).name

      mutableStringToValue[name] = constant
      mutableStringToValue[constant.value.toString()] = constant

      mutableValueToString[constant] = name

      val constantField = enumType.getDeclaredField(name)
      val wireEnumConstant = constantField.getAnnotation(WireEnumConstant::class.java)
      if (wireEnumConstant != null && wireEnumConstant.declaredName.isNotEmpty()) {
        mutableStringToValue[wireEnumConstant.declaredName] = constant
        mutableValueToString[constant] = wireEnumConstant.declaredName
      }
    }

    stringToValue = mutableStringToValue
    valueToString = mutableValueToString
  }

  override fun fromString(value: String): E? {
    return stringToValue[value]
  }

  override fun toStringOrNumber(value: E): String {
    return valueToString[value]!!
  }
}
