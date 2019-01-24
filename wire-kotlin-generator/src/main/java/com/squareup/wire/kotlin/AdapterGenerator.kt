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
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema

/**
 * Example
 * ```
 * companion object {
 *  @JvmField
 *  val ADAPTER : ProtoAdapter<Person> =
 *      object : ProtoAdapter<Person>(FieldEncoding.LENGTH_DELIMITED, Person::class.java) {
 *    override fun encodedSize(value: Person): Int { .. }
 *    override fun encode(writer: ProtoWriter, value: Person) { .. }
 *    override fun decode(reader: ProtoReader): Person { .. }
 *  }
 * }
 * ```
 */
internal class AdapterGenerator(
    schema: Schema,
    nameToKotlinName: Map<ProtoType, ClassName>,
    emitAndroid: Boolean,
    javaInterOp: Boolean,
    nameAllocator: KotlinWireNameAllocator
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator) {


  internal fun generate(type: MessageType): PropertySpec {
    val nameAllocator = nameAllocator.create(type)
    val parentClassName = generatedTypeName(type)
    val adapterName = nameAllocator["ADAPTER"]

    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(ProtoAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("\n⇥%T.LENGTH_DELIMITED",
            FieldEncoding::class.asClassName())
        .addSuperclassConstructorParameter("\n%T::class.java\n⇤", parentClassName)
        .addFunction(encodedSizeFun(type))
        .addFunction(encodeFun(type))
        .addFunction(decodeFun(type))

    for (field in type.fields()) {
      if (field.isMap) {
        adapterObject.addProperty(field.toProtoAdapterPropertySpec())
      }
    }

    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)

    return PropertySpec.builder(adapterName, adapterType)
        .jvmField()
        .initializer("%L", adapterObject.build())
        .build()
  }


  private fun Field.toProtoAdapterPropertySpec(): PropertySpec {
    val adapterType = ProtoAdapter::class.asTypeName()
        .parameterizedBy(Map::class.asTypeName()
            .parameterizedBy(keyType.typeName, valueType.typeName))

    return PropertySpec.builder("${name()}Adapter", adapterType, KModifier.PRIVATE)
        .initializer(
            "%T.newMapAdapter(%L, %L)",
            ProtoAdapter::class,
            keyType.getAdapterName(),
            valueType.getAdapterName()
        )
        .build()
  }

  private fun encodedSizeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val nameAllocator = nameAllocator.create(message)
    val body = buildCodeBlock {
      add("return \n⇥")
      message.fieldsWithJavaInteropOneOfs().forEach { field ->
        val adapterName = field.getAdapterName()
        val fieldName = nameAllocator[field]
        add("%L.%LencodedSizeWithTag(%L, value.%L) +\n",
            adapterName,
            if (field.isRepeated) "asRepeated()." else "",
            field.tag(),
            fieldName)
      }

      if (!javaInterOp) {
        message.oneOfs().forEach { oneOf ->
          val oneOfName = nameAllocator[oneOf]
          val oneOfClass = message.oneOfClass(oneOf)
          addStatement("when (value.$oneOfName) {⇥")
          oneOf.fields().forEach { field ->
            val fieldName = nameAllocator[field]
            val adapterName = field.getAdapterName()
            addStatement(
                "is %T -> %L.encodedSizeWithTag(${field.tag()}, value.$oneOfName.$fieldName)",
                oneOfClass.nestedClass(fieldName.capitalize()),
                adapterName)
          }
          addStatement("else -> 0")
          addStatement("⇤} +")
        }
      }

      add("value.unknownFields.size⇤\n")
    }
    return FunSpec.builder("encodedSize")
        .addParameter("value", className)
        .returns(Int::class)
        .addCode(body)
        .addModifiers(KModifier.OVERRIDE)
        .build()
  }

  private fun encodeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = buildCodeBlock {
      val nameAllocator = nameAllocator.create(message)

      message.fieldsWithJavaInteropOneOfs().forEach { field ->
        val adapterName = field.getAdapterName()
        val fieldName = nameAllocator[field]
        addStatement("%L.%LencodeWithTag(writer, %L, value.%L)",
            adapterName,
            if (field.isRepeated) "asRepeated()." else "",
            field.tag(),
            fieldName)
      }

      if (!javaInterOp) {
        message.oneOfs().forEach { oneOf ->
          val oneOfName = nameAllocator[oneOf]
          val oneOfClass = message.oneOfClass(oneOf)
          addStatement("when (value.$oneOfName) {⇥")
          oneOf.fields().forEach { field ->
            val fieldName = nameAllocator[field]
            val adapterName = field.getAdapterName()
            addStatement(
                "is %T -> %L.encodeWithTag(writer, ${field.tag()}, value.$oneOfName.$fieldName)",
                oneOfClass.nestedClass(fieldName.capitalize()),
                adapterName)
          }
          addStatement("⇤}")
        }
      }

      addStatement("writer.writeBytes(value.unknownFields)")
    }

    return FunSpec.builder("encode")
        .addParameter("writer", ProtoWriter::class)
        .addParameter("value", className)
        .addCode(body)
        .addModifiers(KModifier.OVERRIDE)
        .build()
  }

  private fun decodeFun(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator.create(message)
    val internalClass = ClassName("com.squareup.wire.internal", "Internal")

    val declarationBody = buildCodeBlock {
      message.fieldsWithJavaInteropOneOfs().forEach { field ->
        val fieldName = nameAllocator[field]
        val fieldDeclaration: CodeBlock = field.getDeclaration(fieldName)
        addStatement("%L", fieldDeclaration)
      }

      if (!javaInterOp) {
        message.oneOfs().forEach { oneOf ->
          val name = oneOf.name()
          val oneOfClass = message.oneOfClass(oneOf)
          addStatement("var $name: %T = null", oneOfClass.copy(nullable = true))
        }
      }
    }

    val returnBody = buildCodeBlock {
      addStatement("return %T(⇥", className)

      message.fieldsWithJavaInteropOneOfs().forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && !field.isMap && field.isRequired) {
          CodeBlock.of(" ?: throw %1T.missingRequiredFields(%2L, %2S)",
              internalClass,
              field.name())
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }

      if (!javaInterOp) {
        message.oneOfs().forEach { oneOf ->
          addStatement("%1L = %1L,", oneOf.name())
        }
      }

      add("unknownFields = unknownFields")
      add("⇤\n)\n") // close the block
    }

    val decodeBlock = buildCodeBlock {
      addStatement("val unknownFields = reader.forEachTag { tag ->")
      addStatement("⇥when (tag) {⇥")

      message.fieldsWithJavaInteropOneOfs().forEach { field ->
        val fieldName = nameAllocator[field]
        val adapterName = field.getAdapterName()

        val decodeBodyTemplate = when {
          field.isRepeated -> "%L -> %L.add(%L.decode(reader))"
          field.isMap -> "%L -> %L.putAll(%L.decode(reader))"
          else -> "%L -> %L = %L.decode(reader)"
        }

        addStatement(decodeBodyTemplate, field.tag(), fieldName, adapterName)
      }

      if (!javaInterOp) {
        message.oneOfs().forEach { oneOf ->
          val name = oneOf.name()
          val oneOfClass = message.oneOfClass(oneOf)

          oneOf.fields().forEach { field ->
            val adapterName = field.getAdapterName()
            val fieldName = field.name().capitalize()
            val fieldClass = oneOfClass.nestedClass(fieldName)

            addStatement("${field.tag()} -> $name = %T(%L.decode(reader))",
                fieldClass,
                adapterName)
          }
        }
      }

      val tagHandlerClass = ClassName("com.squareup.wire", "TagHandler")

      addStatement("else -> %T.%L", tagHandlerClass, "UNKNOWN_TAG")
      add("⇤}\n⇤}\n") // close the block
    }

    return FunSpec.builder("decode")
        .addParameter("reader", ProtoReader::class)
        .returns(className)
        .addCode(declarationBody)
        .addCode(decodeBlock)
        .addCode(returnBody)
        .addModifiers(KModifier.OVERRIDE)
        .build()
  }

  private fun Field.getDeclaration(allocatedName: String) = when {
    isRepeated -> CodeBlock.of("val $allocatedName = mutableListOf<%T>()", type().typeName)
    isMap -> CodeBlock.of("val $allocatedName = mutableMapOf<%T, %T>()",
        keyType.typeName, valueType.typeName)
    else -> CodeBlock.of("var $allocatedName: %T = %L", declarationClass, getDefaultValue())
  }

}