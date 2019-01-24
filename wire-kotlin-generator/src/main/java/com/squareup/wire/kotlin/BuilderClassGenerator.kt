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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.wire.Message
import com.squareup.wire.internal.Internal
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema

internal class BuilderClassGenerator(
    schema: Schema,
    nameToKotlinName: Map<ProtoType, ClassName>,
    emitAndroid: Boolean,
    javaInterOp: Boolean,
    nameAllocator: KotlinWireNameAllocator
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator) {

  internal fun generate(
      type: MessageType,
      className: ClassName,
      builderClassName: ClassName
  ): TypeSpec {
    val builder = TypeSpec.classBuilder("Builder")
        .superclass(Message.Builder::class.asTypeName()
            .parameterizedBy(className, builderClassName))

    if (!javaInterOp) {
      return builder
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("message", className)
              .build())
          .addProperty(PropertySpec.builder("message", className)
              .addModifiers(KModifier.PRIVATE)
              .initializer("message")
              .build())
          .addFunction(FunSpec.builder("build")
              .addModifiers(KModifier.OVERRIDE)
              .returns(className)
              .addStatement("return message")
              .build())
          .build()
    }

    val nameAllocator = nameAllocator.create(type)
    val builderClass = className.nestedClass("Builder")
    val internalClass = ClassName("com.squareup.wire.internal", "Internal")

    val returnBody = buildCodeBlock {
      add("return %T(⇥\n", className)

      type.fieldsWithJavaInteropOneOfs().forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && field.isRequired) {
          CodeBlock.of(" ?: throw %1T.%2L(%3L, %3S)",
              internalClass,
              "missingRequiredFields",
              field.name())
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }
      add("unknownFields = buildUnknownFields()")
      add("⇤\n)\n") // close the block
    }

    type.fieldsWithJavaInteropOneOfs().forEach { field ->
      val fieldName = nameAllocator[field]

      val propertyBuilder = PropertySpec.builder(fieldName, field.declarationClass)
          .mutable(true)
          .initializer(field.getDefaultValue())

      if (javaInterOp) {
        propertyBuilder.addAnnotation(JvmField::class)
      }

      builder
          .addProperty(propertyBuilder.build())
          .addFunction(builderSetter(field, nameAllocator, builderClass))
    }

    val buildFunction = FunSpec.builder("build")
        .addModifiers(KModifier.OVERRIDE)
        .returns(className)
        .addCode(returnBody)
        .build()

    return builder.addFunction(buildFunction)
        .build()
  }

  private fun builderSetter(
      field: Field,
      nameAllocator: NameAllocator,
      builderType: TypeName
  ): FunSpec {
    val fieldName = nameAllocator[field]
    val funBuilder = FunSpec.builder(fieldName)
        .addParameter(fieldName, field.getClass())
        .returns(builderType)
    if (field.documentation().isNotBlank()) {
      funBuilder.addKdoc("%L\n", field.documentation())
    }
    if (field.isDeprecated) {
      funBuilder.addAnnotation(AnnotationSpec.builder(Deprecated::class)
          .addMember("message = %S", "$fieldName is deprecated")
          .build())
    }
    if (field.isRepeated) {
      funBuilder.addStatement("%T.checkElementsNotNull(%L)", Internal::class, fieldName)
    }

    return funBuilder
        .addStatement("this.%1L = %1L", fieldName)
        .addStatement("return this")
        .build()
  }
}