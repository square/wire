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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.kotlinpoet.jvm.jvmStatic
import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema

/**
 * Example
 * ```
 * enum class PhoneType(private val value: Int) : WireEnum {
 *     HOME(0),
 *     ...
 *     override fun getValue(): Int = value
 *
 *     companion object {
 *       fun fromValue(value: Int): PhoneType = ...
 *
 *       val ADAPTER: ProtoAdapter<PhoneType> = ...
 *     }
 * ```
 * }
 */
internal class EnumGenerator(
    schema: Schema,
    nameToKotlinName: Map<ProtoType, ClassName>,
    emitAndroid: Boolean,
    javaInterOp: Boolean,
    nameAllocator: KotlinWireNameAllocator
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator) {

  internal fun generate(message: EnumType): TypeSpec {
    val type = message.type()
    val nameAllocator = nameAllocator.create(message)

    val valueName = nameAllocator["value"]

    val builder = TypeSpec.enumBuilder(type.simpleName())
        .addSuperinterface(WireEnum::class)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(valueName, Int::class, KModifier.PRIVATE)
            .build())
        .addProperty(PropertySpec.builder(valueName, Int::class, KModifier.PRIVATE)
            .initializer(valueName)
            .build())
        .addFunction(FunSpec.builder("getValue")
            .returns(Int::class)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return $valueName")
            .build())
        .addType(generateEnumCompanion(message))

    message.constants().forEach { constant ->
      builder.addEnumConstant(nameAllocator[constant], TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", constant.tag())
          .apply {
            if (constant.documentation().isNotBlank()) {
              addKdoc("%L\n", constant.documentation())
            }
          }
          .build())
    }

    return builder.build()
  }

  private fun generateEnumCompanion(message: EnumType): TypeSpec {
    val parentClassName = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator.create(message)
    val valueName = nameAllocator["value"]
    val fromValue = FunSpec.builder("fromValue")
        .jvmStatic()
        .addParameter(valueName, Int::class)
        .returns(parentClassName)
        .apply {
          addCode("return when (value) {\n⇥")
          message.constants().forEach { constant ->
            addCode("%L -> %L\n", constant.tag(), nameAllocator[constant])
          }
          addCode("else -> throw IllegalArgumentException(%P)", "Unexpected value: \$value")
          addCode("\n⇤}\n") // close the block
        }
        .build()
    return TypeSpec.companionObjectBuilder()
        .addFunction(fromValue)
        .addProperty(generateEnumAdapter(message))
        .build()
  }

  /**
   * Example
   * ```
   * @JvmField
   * val ADAPTER = object : EnumAdapter<PhoneType>(PhoneType::class.java) {
   *     override fun fromValue(value: Int): PhoneType = PhoneType.fromValue(value)
   * }
   * ```
   */
  private fun generateEnumAdapter(message: EnumType): PropertySpec {
    val parentClassName = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator.create(message)

    val adapterName = nameAllocator["ADAPTER"]
    val valueName = nameAllocator["value"]

    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)
    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(EnumAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("\n⇥%T::class.java\n⇤", parentClassName)
        .addFunction(FunSpec.builder("fromValue")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(valueName, Int::class)
            .returns(parentClassName)
            .addStatement("return %T.fromValue(value)", parentClassName)
            .build())
        .build()

    return PropertySpec.builder(adapterName, adapterType)
        .jvmField()
        .initializer("%L", adapterObject)
        .build()
  }

}