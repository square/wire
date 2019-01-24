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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.Message
import com.squareup.wire.WireField
import com.squareup.wire.internal.Internal
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
internal class MessageGenerator(
    schema: Schema,
    nameToKotlinName: Map<ProtoType, ClassName>,
    emitAndroid: Boolean,
    javaInterOp: Boolean,
    nameAllocator: KotlinWireNameAllocator,
    private val kotlinGenerator: KotlinGenerator
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator) {
  private val adapterGenerator =
      AdapterGenerator(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator)
  private val builderClassGenerator =
      BuilderClassGenerator(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator)
  private val oneOfClassGenerator =
      OneOfClassGenerator(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator)

  internal fun generate(type: MessageType): TypeSpec {
    val className = type.typeName
    val builderClassName = className.nestedClass("Builder")
    val nameAllocator = nameAllocator.create(type)
    val adapterName = nameAllocator["ADAPTER"]
    val unknownFields = nameAllocator["unknownFields"]
    val superclass = if (emitAndroid) ANDROID_MESSAGE else MESSAGE
    val companionObjBuilder = TypeSpec.companionObjectBuilder()

    companionObjBuilder.addProperty(adapterGenerator.generate(type))

    val classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(KModifier.DATA)
        .superclass(superclass.parameterizedBy(className, builderClassName))
        .addSuperclassConstructorParameter(adapterName)
        .addSuperclassConstructorParameter(unknownFields)
        .addFunction(generateNewBuilderMethod(type, builderClassName))
        .addType(builderClassGenerator.generate(type, className, builderClassName))

    if (emitAndroid) {
      addAndroidCreator(type, companionObjBuilder)
    }

    if (type.oneOfs().isNotEmpty()) {
      if (javaInterOp) {
        classBuilder.addInitializerBlock(generateInitializerOneOfBlock(type))
      } else {
        // TODO emit oneofs using sealed classes.
      }
    }

    classBuilder.addType(companionObjBuilder.build())

    addMessageConstructor(type, classBuilder)

    type.nestedTypes().forEach { classBuilder.addType(kotlinGenerator.generateType(it)) }

    if (!javaInterOp) {
      type.oneOfs().forEach { classBuilder.addType(oneOfClassGenerator.generate(type, it)) }
    }

    return classBuilder.build()
  }


  private fun generateInitializerOneOfBlock(type: MessageType): CodeBlock {
    return buildCodeBlock {
      val nameAllocator = nameAllocator.create(type)
      type.oneOfs()
          .filter { oneOf -> oneOf.fields().size >= 2 }
          .forEach { oneOf ->
            val fieldNames = oneOf.fields().joinToString(", ", transform = nameAllocator::get)
            beginControlFlow("require (%T.countNonNull(%L) > 1)",
                Internal::class, fieldNames)
            addStatement("\"At most one of $fieldNames may be non-null\"")
            endControlFlow()
          }
    }
  }

  private fun generateNewBuilderMethod(type: MessageType, builderClassName: ClassName): FunSpec {
    val funBuilder = FunSpec.builder("newBuilder")
        .addModifiers(KModifier.OVERRIDE)
        .returns(builderClassName)

    if (!javaInterOp) {
      return funBuilder
          .addAnnotation(AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "Shouldn't be used in Kotlin")
              .addMember("level = %T.%L", DeprecationLevel::class, DeprecationLevel.HIDDEN)
              .build())
          .addStatement("return %T(this.copy())", builderClassName)
          .build()
    }

    val nameAllocator = nameAllocator.create(type)

    funBuilder.addStatement("val builder = Builder()")

    type.fieldsWithJavaInteropOneOfs().forEach { field ->
      val fieldName = nameAllocator[field]
      funBuilder.addStatement("builder.%1L = %1L", fieldName)
    }

    return funBuilder
        .addStatement("builder.addUnknownFields(unknownFields())")
        .addStatement("return builder")
        .build()
  }

  /**
   * Example
   * ```
   * data class Person(
   *   val name: String,
   *   val email: String? = null,
   *   val phone: List<PhoneNumber> = emptyList(),
   *   val unknownFields: ByteString = ByteString.EMPTY
   * )
   * ```
   */
  private fun addMessageConstructor(message: MessageType, classBuilder: TypeSpec.Builder) {
    val constructorBuilder = FunSpec.constructorBuilder()
    val nameAllocator = nameAllocator.create(message)
    val byteClass = ProtoType.BYTES.typeName

    val fields = message.fieldsWithJavaInteropOneOfs()

    fields.forEach { field ->
      val fieldClass = field.typeName
      val fieldName = nameAllocator[field]
      val fieldType = field.type()
      val defaultValue = field.getDefaultValue()

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (!field.isRequired && !fieldType.isMap) {
        parameterSpec.defaultValue(defaultValue)
      }

      parameterSpec.addAnnotation(AnnotationSpec.builder(WireField::class)
          .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
          .addMember("tag = %L", field.tag())
          .addMember("adapter = %S", field.getAdapterName(nameDelimiter = '#'))
          .build())

      if (javaInterOp) {
        parameterSpec.addAnnotation(JvmField::class)
      }

      constructorBuilder.addParameter(parameterSpec.build())
      classBuilder.addProperty(PropertySpec.builder(fieldName, fieldClass)
          .initializer(fieldName)
          .build())
    }

    if (!javaInterOp) {
      message.oneOfs().forEach { oneOf ->
        val name = nameAllocator[oneOf]
        val fieldType = message.oneOfClass(oneOf).copy(nullable = true)
        constructorBuilder.addParameter(ParameterSpec.builder(name, fieldType)
            .defaultValue("null")
            .build())
        classBuilder.addProperty(PropertySpec.builder(name, fieldType)
            .initializer(name)
            .build())
      }
    }

    val unknownFields = nameAllocator["unknownFields"]
    constructorBuilder.addParameter(
        ParameterSpec.builder(unknownFields, byteClass)
            .defaultValue("%T.EMPTY", byteClass)
            .build())
    classBuilder.addProperty(PropertySpec.builder(unknownFields, byteClass)
        .initializer(unknownFields)
        .build())

    classBuilder.primaryConstructor(constructorBuilder.build())
  }

  /**
   * Example
   * ```
   * companion object {
   *     @JvmStatic
   *     val CREATOR: Parcelable.Creator<Person> = AndroidMessage.newCreator(ADAPTER)
   * }
   * ```
   */
  private fun addAndroidCreator(type: MessageType, companionObjBuilder: TypeSpec.Builder) {
    val nameAllocator = nameAllocator.create(type)
    val parentClassName = generatedTypeName(type)
    val creatorName = nameAllocator["CREATOR"]
    val creatorTypeName = ClassName("android.os", "Parcelable", "Creator")
        .parameterizedBy(parentClassName)

    companionObjBuilder.addProperty(PropertySpec.builder(creatorName, creatorTypeName)
        .jvmField()
        .initializer("%T.newCreator(ADAPTER)", ANDROID_MESSAGE)
        .build())
  }

  private val Field.typeName: TypeName
    get() = when {
      isRepeated -> List::class.asClassName().parameterizedBy(type().typeName)
      isMap -> Map::class.asTypeName().parameterizedBy(keyType.typeName, valueType.typeName)
      !isRequired || default != null -> type().typeName.copy(nullable = true)
      else -> type().typeName
    }

  companion object {
    private val MESSAGE = Message::class.asClassName()
    private val ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage")
  }
}