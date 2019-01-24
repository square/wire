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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema

internal class OneOfClassGenerator(
    schema: Schema,
    nameToKotlinName: Map<ProtoType, ClassName>,
    emitAndroid: Boolean,
    javaInterOp: Boolean,
    nameAllocator: KotlinWireNameAllocator
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator) {

  internal fun generate(type: MessageType, oneOf: OneOf): TypeSpec {
    val nameAllocator = nameAllocator.create(type)
    val oneOfClassName = nameAllocator[oneOf].capitalize()
    val oneOfClassType = type.oneOfClass(oneOf)
    val builder = TypeSpec.classBuilder(oneOfClassName)
        .addModifiers(KModifier.SEALED)
    oneOf.fields().forEach { oneOfField ->
      val name = nameAllocator[oneOfField]
      val className = name.capitalize()
      val fieldClass = oneOfField.type().typeName

      builder.addType(TypeSpec.classBuilder(className)
          .addModifiers(KModifier.DATA)
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter(ParameterSpec.builder(name, fieldClass)
                  .build())
              .build())
          .addProperty(PropertySpec.builder(name, fieldClass)
              .initializer(name)
              .build())
          .superclass(oneOfClassType)
          .build())
    }
    return builder.build()
  }
}