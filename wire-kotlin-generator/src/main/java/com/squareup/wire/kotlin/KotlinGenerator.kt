/*
 * Copyright 2018 Square Inc.
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

import android.os.Parcel
import android.os.Parcelable
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import okio.ByteString
import java.util.Locale

class KotlinGenerator private constructor(
  val schema: Schema,
  private val nameToKotlinName: Map<ProtoType, ClassName>,
  private val emitAndroid: Boolean
) {
  val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()

  /** Returns the Kotlin type for [protoType]. */
  fun typeName(protoType: ProtoType) = nameToKotlinName.getValue(protoType)

  /** Returns the full name of the class generated for `type`.  */
  fun generatedTypeName(type: Type) = typeName(type.type())

  fun generateType(type: Type): TypeSpec = when (type) {
    is MessageType -> generateMessage(type)
    is EnumType -> generateEnum(type)
    else -> error("Unknown type $type")
  }

  private fun nameAllocator(message: Type): NameAllocator {
    return nameAllocatorStore.getOrPut(message) {
      NameAllocator().apply {
        when (message) {
          is EnumType -> {
            newName("value", "value")
            newName("ADAPTER", "ADAPTER")
            message.constants().forEach { constant ->
              newName(constant.name(), constant)
            }
          }
          is MessageType -> {
            newName("unknownFields", "unknownFields")
            newName("ADAPTER", "ADAPTER")
            newName("reader", "reader")
            if (emitAndroid) {
              newName("CREATOR", "CREATOR")
            }
            message.fields().forEach { field ->
              newName(field.name(), field)
            }
          }
        }
      }
    }
  }

  private fun generateMessage(type: MessageType): TypeSpec {
    val className = typeName(type.type())

    val classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(DATA)
        .addType(generateAdapter(type))

    if (emitAndroid) {
      addAndroidCodeToMessage(classBuilder, type)
    }

    addMessageConstructor(type, classBuilder)

    type.nestedTypes().forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
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
    val nameAllocator = nameAllocator(message)
    val byteClass = typeName(ProtoType.BYTES)

    message.fields().forEach { field ->
      var fieldClass: TypeName = typeName(field.type())!!
      val fieldName = nameAllocator.get(field)
      var defaultValue = CodeBlock.of("null")

      when {
        field.isOptional -> {
          fieldClass = fieldClass.asNullable()
        }
        field.isRepeated -> {
          fieldClass = List::class.asClassName().parameterizedBy(fieldClass)
          defaultValue = CodeBlock.of("emptyList()")
        }
      }

      if (field.default != null) {
        defaultValue = getDefaultValue(field)
        fieldClass = fieldClass.asNonNullable()
      }

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (field.isOptional || field.isRepeated) {
        parameterSpec.defaultValue(defaultValue)
      }

      constructorBuilder.addParameter(parameterSpec.build())
      classBuilder.addProperty(PropertySpec.builder(fieldName, fieldClass)
          .initializer(fieldName)
          .build())
    }

    val unknownFields = nameAllocator.get("unknownFields")
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
   * object ADAPTER : ProtoAdapter<Person>(FieldEncoding.LENGTH_DELIMITED, Person::class.java) {
   *     override fun encodedSize(value: Person): Int { .. }
   *     override fun encode(writer: ProtoWriter, value: Person) { .. }
   *     override fun decode(reader: ProtoReader): Person { .. }
   * }
   * ```
   */
  private fun generateAdapter(type: MessageType): TypeSpec {
    val nameAllocator = nameAllocator(type)
    val parentClassName = generatedTypeName(type)
    val adapterName = nameAllocator.get("ADAPTER")

    return TypeSpec.objectBuilder(adapterName)
        .superclass(ProtoAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("%T.LENGTH_DELIMITED",
            FieldEncoding::class.asClassName())
        .addSuperclassConstructorParameter("%T::class.java", parentClassName)
        .addFunction(encodedSizeFun(type))
        .addFunction(encodeFun(type))
        .addFunction(decodeFun(type))
        .build()
  }

  private fun encodedSizeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = CodeBlock.builder()
        .add("return ")

    val indentation = " ".repeat(4)

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)

      body.add("%L.%LencodedSizeWithTag(%L, value.%L) +\n",
          adapterName,
          if (field.isRepeated) "asRepeated()." else "",
          field.tag(),
          field.name())
      body.add(indentation)
    }

    return FunSpec.builder("encodedSize")
        .addParameter("value", className)
        .returns(Int::class)
        .addCode(body
            .add("value.unknownFields.size()\n")
            .build())
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun encodeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    var body = CodeBlock.builder()

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      body.addStatement("%L.%LencodeWithTag(writer, %L, value.%L)",
          adapterName,
          if (field.isRepeated) "asRepeated()." else "",
          field.tag(),
          field.name())
    }

    body.addStatement("writer.writeBytes(value.unknownFields)")

    return FunSpec.builder("encode")
        .addParameter("writer", ProtoWriter::class)
        .addParameter("value", className)
        .addCode(body.build())
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun decodeFun(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator(message)
    val indentation = " ".repeat(4)
    val internalClass = ClassName("com.squareup.wire.internal", "Internal")

    var declarationBody = CodeBlock.builder()

    var returnBody = CodeBlock.builder()
    returnBody.add("return %T(\n", className)

    var decodeBlock = CodeBlock.builder()
    decodeBlock.addStatement(
      "val unknownFields = UnknownFieldsBuilder.decodeMessage(reader) { tag ->")

    // Indent manually as code generator doesn't handle this block gracefully.
    decodeBlock.addStatement("%Lwhen (tag) {", indentation)

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      val fieldName = nameAllocator.get(field)

      var throwExceptionBlock = CodeBlock.of("")
      var decodeBodyTemplate: String

      var fieldClass: TypeName = nameToKotlinName.getValue(field.type())
      var fieldDeclaration: CodeBlock

      if (field.isRepeated) {
        fieldDeclaration = CodeBlock.of("var %L = mutableListOf<%T>()", fieldName, fieldClass)
        decodeBodyTemplate = "%L%L -> %L.add(%L.decode(reader))"
        fieldClass = List::class.asClassName().parameterizedBy(fieldClass)
      } else {
        fieldClass = fieldClass.asNullable()
        fieldDeclaration = CodeBlock.of("var %L: %T = null", fieldName, fieldClass)
        decodeBodyTemplate = "%L%L -> %L = %L.decode(reader)"

        if (!field.isOptional && field.default == null) {
          throwExceptionBlock = CodeBlock.of(" ?: throw %T.%L(%L, %S)",
              internalClass, "missingRequiredFields", field.name(), field.name())
        }
      }

      if (field.default != null) {
        fieldClass = fieldClass.asNonNullable()
        fieldDeclaration = CodeBlock.of("var %L: %T = %L", fieldName,
            fieldClass, getDefaultValue(field))
      }

      declarationBody.addStatement("%L", fieldDeclaration)
      decodeBlock.addStatement(decodeBodyTemplate, indentation.repeat(2), field.tag(), fieldName,
          adapterName)
      returnBody.add("%L%L = %L%L,\n", indentation, fieldName, fieldName, throwExceptionBlock)
    }

    val unknownFieldsBuilder = ClassName("com.squareup.wire", "UnknownFieldsBuilder")

    decodeBlock.addStatement("%Lelse -> %T.%L", indentation.repeat(2), unknownFieldsBuilder,
        "UNKNOWN_FIELD")
    decodeBlock.addStatement("%L}", indentation)
    decodeBlock.addStatement("}")

    returnBody.add("%LunknownFields = unknownFields\n)\n", indentation)

    return FunSpec.builder("decode")
        .addParameter("reader", ProtoReader::class)
        .returns(className)
        .addCode(declarationBody.build())
        .addCode(decodeBlock.build())
        .addCode(returnBody.build())
        .addModifiers(OVERRIDE)
        .build()
  }

  // TODO add support for custom adapters.
  private fun getAdapterName(field: Field): CodeBlock {
    if (field.type().isScalar) {
      return CodeBlock.of("%T.%L", ProtoAdapter::class,
          field.type().simpleName().toUpperCase(Locale.US))
    }

    return CodeBlock.of("%L.ADAPTER", typeName(field.type()).simpleName)
  }

  /**
   * Example
   * ```
   * enum class PhoneType(private val value: Int) : WireEnum {
   *     HOME(0),
   *     ...
   *     override fun getValue(): Int = value
   *     object ADAPTER { ... }
   * ```
   * }
   */
  private fun generateEnum(message: EnumType): TypeSpec {
    val type = message.type()
    val nameAllocator = nameAllocator(message)

    val valueName = nameAllocator.get("value")

    var builder = TypeSpec.enumBuilder(type.simpleName())
        .addSuperinterface(WireEnum::class)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(valueName, Int::class, PRIVATE)
            .build())
        .addProperty(PropertySpec.builder(valueName, Int::class, PRIVATE)
            .initializer(valueName)
            .build())
        .addFunction(FunSpec.builder("getValue")
            .returns(Int::class)
            .addModifiers(OVERRIDE)
            .addStatement("return $valueName")
            .build())
        .addType(generateEnumAdapter(message))

    message.constants().forEach { constant ->
      builder.addEnumConstant(nameAllocator.get(constant), TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", constant.tag())
          .addKdoc(constant.documentation())
          .build())
    }

    return builder.build()
  }

  /**
   * Example
   * ```
   * object ADAPTER : EnumAdapter<PhoneType>(PhoneType::class.java) {
   *     override fun fromValue(value: Int): PhoneType? = values().find { it.value == value }
   * }
   * ```
   */

  private fun generateEnumAdapter(message: EnumType): TypeSpec {
    val parentClassName = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator(message)

    val adapterName = nameAllocator.get("ADAPTER")
    val valueName = nameAllocator.get("value")

    return TypeSpec.objectBuilder(adapterName)
        .superclass(EnumAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("%T::class.java", parentClassName)
        .addFunction(FunSpec.builder("fromValue")
            .addModifiers(OVERRIDE)
            .addParameter(valueName, Int::class)
            .returns(parentClassName.asNullable())
            .addStatement("return values().find { it.value == value }")
            .build())
        .build()
  }

  /**
   * Adds code to help message class implement Parcelable.
   */
  private fun addAndroidCodeToMessage(classBuilder: TypeSpec.Builder, type: MessageType) {
    classBuilder.addFunction(FunSpec.builder("writeToParcel")
        .addStatement("return destination.writeByteArray(ADAPTER.encode(this))")
        .addParameter("destination", Parcel::class)
        .addParameter("flags", Int::class)
        .addModifiers(OVERRIDE)
        .build())
    classBuilder.addFunction(FunSpec.builder("describeContents")
        .addStatement("return 0")
        .addModifiers(OVERRIDE)
        .build())
    classBuilder.addSuperinterface(Parcelable::class)
    classBuilder.addType(generateAndroidCreator(type))
  }

  /**
   * Example
   * ```
   * object CREATOR : Parcelable.Creator<Person> {
   *     override fun createFromParcel(input: Parcel) = ADAPTER.decode(input.createByteArray())
   *     override fun newArray(size: Int): Array<Person?> = arrayOfNulls(size)
   * }
   * ```
   */
  private fun generateAndroidCreator(type: MessageType): TypeSpec {
    val nameAllocator = nameAllocator(type)
    val parentClassName = generatedTypeName(type)
    val creatorName = nameAllocator.get("CREATOR")

    return TypeSpec.objectBuilder(creatorName)
        .addSuperinterface(Parcelable.Creator::class.asClassName().parameterizedBy(parentClassName))
        .addFunction(FunSpec.builder("createFromParcel")
            .addParameter("input", Parcel::class)
            .addStatement("return ADAPTER.decode(input.createByteArray())")
            .addModifiers(OVERRIDE)
            .build())
        .addFunction(FunSpec.builder("newArray")
            .addParameter("size", Int::class)
            .returns(ARRAY.parameterizedBy(parentClassName.asNullable()))
            .addStatement("return arrayOfNulls(size)")
            .addModifiers(OVERRIDE)
            .build())
        .build()
  }

  private fun getDefaultValue(field: Field): CodeBlock {
    return when {
      isEnum(field) ->
        CodeBlock.of("%T.%L", typeName(field.type()), field.default)
      else -> CodeBlock.of("%L", field.default)
    }
  }

  private fun isEnum(field: Field): Boolean = schema.getType(field.type()) is EnumType

  companion object {
    private val BUILT_IN_TYPES = mapOf(
        ProtoType.BOOL to BOOLEAN,
        ProtoType.BYTES to ByteString::class.asClassName(),
        ProtoType.DOUBLE to DOUBLE,
        ProtoType.FLOAT to FLOAT,
        ProtoType.FIXED32 to INT,
        ProtoType.FIXED64 to LONG,
        ProtoType.INT32 to INT,
        ProtoType.INT64 to LONG,
        ProtoType.SFIXED32 to INT,
        ProtoType.SFIXED64 to LONG,
        ProtoType.SINT32 to INT,
        ProtoType.SINT64 to LONG,
        ProtoType.STRING to String::class.asClassName(),
        ProtoType.UINT32 to INT,
        ProtoType.UINT64 to LONG
    )

    @JvmStatic @JvmName("get")
    operator fun invoke(schema: Schema, emitAndroid: Boolean = false): KotlinGenerator {
      val map = BUILT_IN_TYPES.toMutableMap()

      fun putAll(kotlinPackage: String, enclosingClassName: ClassName?, types: List<Type>) {
        for (type in types) {
          val className = enclosingClassName?.nestedClass(type.type().simpleName())
              ?: ClassName(kotlinPackage, type.type().simpleName())
          map[type.type()] = className
          putAll(kotlinPackage, className, type.nestedTypes())
        }
      }

      for (protoFile in schema.protoFiles()) {
        val kotlinPackage = protoFile.kotlinPackage()
        putAll(kotlinPackage, null, protoFile.types())

        for (service in protoFile.services()) {
          val className = ClassName(kotlinPackage, service.type().simpleName())
          map[service.type()] = className
        }
      }

      return KotlinGenerator(schema, map, emitAndroid)
    }
  }
}

private fun ProtoFile.kotlinPackage() = javaPackage() ?: packageName() ?: ""