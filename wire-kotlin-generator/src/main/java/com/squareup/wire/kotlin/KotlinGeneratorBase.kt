/*
 * Copyright 2019 Square Inc.
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
package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import java.util.Locale

abstract class KotlinGeneratorBase internal constructor(
    internal val schema: Schema,
    internal val nameToKotlinName: Map<ProtoType, ClassName>,
    internal val emitAndroid: Boolean,
    internal val javaInterOp: Boolean,
    internal val nameAllocator: KotlinWireNameAllocator = KotlinWireNameAllocator(emitAndroid, javaInterOp)
) {
  /** Returns the full name of the class generated for [type].  */
  fun generatedTypeName(type: Type) = type.typeName

  internal val ProtoType.typeName
    get() = nameToKotlinName.getValue(this)
  internal val Type.typeName
    get() = type().typeName

  internal fun MessageType.fieldsWithJavaInteropOneOfs(): List<Field> {
    return if (javaInterOp)
      fieldsAndOneOfFields()
    else
      fields()
  }

  internal fun MessageType.oneOfClass(oneOf: OneOf): ClassName {
    val name = oneOf.name().capitalize()
    return typeName.nestedClass(name)
  }


  internal fun Field.getDefaultValue(): CodeBlock {
    return when {
      isRepeated -> CodeBlock.of("emptyList()")
      isMap -> CodeBlock.of("emptyMap()")
      default != null -> {
        if (isEnum) {
          CodeBlock.of("%T.%L", type().typeName, default)
        } else {
          CodeBlock.of("%L", default)
        }
      }
      else -> CodeBlock.of("null")
    }
  }

  private val Field.isEnum: Boolean
    get() = schema.getType(type()) is EnumType

  internal val Field.declarationClass: TypeName
    get() = when {
      isRepeated || default != null -> getClass()
      else -> getClass().copy(nullable = true)
    }

  internal fun Field.getClass(baseClass: TypeName = nameToKotlinName.getValue(type())) = when {
    isRepeated -> List::class.asClassName().parameterizedBy(baseClass)
    isOptional && default == null -> baseClass.copy(nullable = true)
    else -> baseClass.copy(nullable = false)
  }

  // TODO add support for custom adapters.
  internal fun Field.getAdapterName(nameDelimiter: Char = '.'): CodeBlock {
    return if (type().isMap) {
      CodeBlock.of("%NAdapter", name())
    } else {
      type().getAdapterName(nameDelimiter)
    }
  }

  internal fun ProtoType.getAdapterName(adapterFieldDelimiterName: Char = '.'): CodeBlock {
    return when {
      isScalar -> CodeBlock.of(
          "%T$adapterFieldDelimiterName%L",
          ProtoAdapter::class, simpleName().toUpperCase(Locale.US)
      )
      isMap -> throw IllegalArgumentException("Can't create single adapter for map type $this")
      else -> CodeBlock.of("%T${adapterFieldDelimiterName}ADAPTER", typeName)
    }
  }
}