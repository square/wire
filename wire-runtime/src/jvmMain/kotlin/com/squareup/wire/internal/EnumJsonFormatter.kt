/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.EnumAdapter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireEnumConstant
import java.lang.reflect.Constructor

/**
 * Encodes enums using their declared names as defined in [WireEnumConstant] or their generated
 * names if the declared name is empty. Decodes using either their declared names, their generated
 * names, or their tags.
 */
class EnumJsonFormatter<E : WireEnum>(
  adapter: EnumAdapter<E>,
) : JsonFormatter<E> {
  private val stringToValue: Map<String, E>
  private val valueToString: Map<E, String>

  /**
   * The `Unrecognized(value: Int) class that might have been generated. See [EnumMode][com.squareup.wire.kotlin.EnumMode].
   */
  private var unrecognizedClassConstructor: Constructor<E>? = null

  init {
    val mutableStringToValue = mutableMapOf<String, E>()
    val mutableValueToString = mutableMapOf<E, String>()

    // E is a subtype of Enum<*>, but we don't know that statically.
    @Suppress("UNCHECKED_CAST")
    val enumType = adapter.type!!.java as Class<E>
    val enumConstants = enumType.enumConstants
    if (enumConstants == null) {
      // The enum has been generated as a sealed class.
      for (subClass in enumType.declaredClasses) {
        val name = subClass.simpleName
        if (name == "Companion") continue

        val field = subClass.declaredFields.first()
        if (field.name == "INSTANCE") {
          @Suppress("UNCHECKED_CAST") // We know it's a E since we generated it.
          val subClassInstance = field.get(null) as E
          mutableStringToValue[subClass.simpleName] = subClassInstance
          mutableStringToValue[subClassInstance.value.toString()] = subClassInstance

          mutableValueToString[subClassInstance] = name

          val wireEnumConstant = subClass.annotations.firstOrNull { it is WireEnumConstant } as? WireEnumConstant
          if (wireEnumConstant != null && wireEnumConstant.declaredName.isNotEmpty()) {
            mutableStringToValue[wireEnumConstant.declaredName] = subClassInstance
            mutableValueToString[subClassInstance] = wireEnumConstant.declaredName
          }
        } else {
          @Suppress("UNCHECKED_CAST") // We know it's a constructor of E since we generated it.
          unrecognizedClassConstructor = subClass.constructors.first() as Constructor<E>
        }
      }
    } else {
      // The enum has been generated as an enum class.
      for (constant in enumConstants) {
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
    }

    stringToValue = mutableStringToValue
    valueToString = mutableValueToString
  }

  override fun fromString(value: String): E? {
    return stringToValue[value]
      // If the constant is unknown to our runtime, we return a `Unrecognized` instance if it has
      // been generated.
      ?: unrecognizedClassConstructor?.newInstance(value.toInt())
  }

  override fun toStringOrNumber(value: E): Any {
    return valueToString[value] ?: value.value
  }
}
