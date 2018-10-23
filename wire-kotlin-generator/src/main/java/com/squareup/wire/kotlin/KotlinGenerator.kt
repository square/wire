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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.INTERNAL
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
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import com.squareup.wire.internal.Internal
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
  private val emitAndroid: Boolean,
  private val javaInterOp: Boolean
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
            newName("Builder", "Builder")
            newName("builder", "builder")

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
    val builderClassName = className.nestedClass("Builder")
    val nameAllocator = nameAllocator(type)
    val adapterName = nameAllocator.get("ADAPTER")
    val unknownFields = nameAllocator.get("unknownFields")
    val superclass = if (emitAndroid) ANDROID_MESSAGE else MESSAGE
    val companionObjBuilder = TypeSpec.companionObjectBuilder()

    addAdapter(type, companionObjBuilder)

    val classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(DATA)
        .superclass(superclass.parameterizedBy(className, builderClassName))
        .addSuperclassConstructorParameter(adapterName)
        .addSuperclassConstructorParameter(unknownFields)
        .addFunction(generateNewBuilderMethod(type, builderClassName))
        .addType(generateBuilderClass(type, className, builderClassName))

    if (emitAndroid) {
      addAndroidCreator(type, companionObjBuilder)
    }

    classBuilder.addType(companionObjBuilder.build())

    addMessageConstructor(type, classBuilder)

    type.nestedTypes().forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  private fun generateNewBuilderMethod(type: MessageType, builderClassName: ClassName): FunSpec {
    val funBuilder = FunSpec.builder("newBuilder")
        .addModifiers(OVERRIDE)
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

    val nameAllocator = nameAllocator(type)

    funBuilder.addStatement("val builder = Builder()")

    type.fields().forEach { field ->
      val fieldName = nameAllocator.get(field)
      funBuilder.addStatement("builder.%1L = %1L", fieldName)
    }

    return funBuilder
        .addStatement("builder.addUnknownFields(unknownFields())")
        .addStatement("return builder")
        .build()
  }

  private fun generateBuilderClass(
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
              .addModifiers(PRIVATE)
              .initializer("message")
              .build())
          .addFunction(FunSpec.builder("build")
              .addModifiers(OVERRIDE)
              .returns(className)
              .addStatement("return message")
              .build())
          .build()
    }

    val nameAllocator = nameAllocator(type)
    val builderClass = className.nestedClass("Builder")
    val indentation = " ".repeat(4)
    val internalClass = ClassName("com.squareup.wire.internal", "Internal")

    val returnBody = CodeBlock.builder()
        .add("return %T(\n", className)

    type.fields().forEach { field ->
      val fieldName = nameAllocator.get(field)

      val throwExceptionBlock = if (!field.isRepeated
          && !field.isOptional
          && field.default == null) {
        CodeBlock.of(" ?: throw %1T.%2L(%3L, %3S)",
            internalClass,
            "missingRequiredFields",
            field.name())
      } else {
        CodeBlock.of("")
      }

      builder
          .addProperty(PropertySpec.builder(fieldName, field.declarationClass())
              .mutable(true)
              .initializer(getDefaultValue(field))
              .addModifiers(INTERNAL)
              .build())
          .addFunction(builderSetter(field, nameAllocator, builderClass))

      returnBody.add("%1L%2L = %2L%3L,\n",
          indentation,
          fieldName,
          throwExceptionBlock)
    }

    val buildFunction = FunSpec.builder("build")
        .addModifiers(OVERRIDE)
        .returns(className)
        .addCode(returnBody
            .add("%LunknownFields = buildUnknownFields()\n)\n", indentation)
            .build())
        .build()

    return builder.addFunction(buildFunction)
        .build()
  }

  private fun builderSetter(
    field: Field,
    nameAllocator: NameAllocator,
    builderType: TypeName
  ): FunSpec {
    val fieldName = nameAllocator.get(field)
    val funBuilder = FunSpec.builder(fieldName)
        .addParameter(fieldName, field.getClass())
        .returns(builderType)
    if (field.documentation().isNotEmpty()) {
      funBuilder.addKdoc(field.documentation())
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
      var fieldClass: TypeName = typeName(field.type())
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
        fieldClass = fieldClass.asNonNull()
      }

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (field.isOptional || field.isRepeated) {
        parameterSpec.defaultValue(defaultValue)
      }

      if (javaInterOp) {
        parameterSpec.addAnnotation(AnnotationSpec.builder(WireField::class)
            .addMember("tag = %L", field.tag())
            .addMember("adapter = %S", getAdapterName(field))
            .build())
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
  private fun addAdapter(type: MessageType, companionObjBuilder: TypeSpec.Builder) {
    val nameAllocator = nameAllocator(type)
    val parentClassName = generatedTypeName(type)
    val adapterName = nameAllocator.get("ADAPTER")

    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(ProtoAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("%T.LENGTH_DELIMITED",
            FieldEncoding::class.asClassName())
        .addSuperclassConstructorParameter("%T::class.java", parentClassName)
        .addFunction(encodedSizeFun(type))
        .addFunction(encodeFun(type))
        .addFunction(decodeFun(type))
        .build()

    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)

    companionObjBuilder.addProperty(PropertySpec.builder(adapterName, adapterType)
        .jvmField()
        .initializer("%L", adapterObject)
        .build())
  }

  private fun encodedSizeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = CodeBlock.builder()
        .add("return ")

    val indentation = " ".repeat(4)
    val nameAllocator = nameAllocator(message)

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      val fieldName = nameAllocator.get(field)

      body.add("%L.%LencodedSizeWithTag(%L, value.%L) +\n",
          adapterName,
          if (field.isRepeated) "asRepeated()." else "",
          field.tag(),
          fieldName)
      body.add(indentation)
    }

    return FunSpec.builder("encodedSize")
        .addParameter("value", className)
        .returns(Int::class)
        .addCode(body
            .add("value.unknownFields.size\n")
            .build())
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun encodeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = CodeBlock.builder()
    val nameAllocator = nameAllocator(message)

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      val fieldName = nameAllocator.get(field)
      body.addStatement("%L.%LencodeWithTag(writer, %L, value.%L)",
          adapterName,
          if (field.isRepeated) "asRepeated()." else "",
          field.tag(),
          fieldName)
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

    val declarationBody = CodeBlock.builder()

    val returnBody = CodeBlock.builder()
    returnBody.add("return %T(\n", className)

    val decodeBlock = CodeBlock.builder()
    decodeBlock.addStatement(
        "val unknownFields = reader.forEachTag { tag ->")

    // Indent manually as code generator doesn't handle this block gracefully.
    decodeBlock.addStatement("%Lwhen (tag) {", indentation)

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      val fieldName = nameAllocator.get(field)

      var throwExceptionBlock = CodeBlock.of("")
      val decodeBodyTemplate: String
      val fieldDeclaration: CodeBlock = field.getDeclaration(fieldName)

      if (field.isRepeated) {
        decodeBodyTemplate = "%L%L -> %L.add(%L.decode(reader))"
      } else {
        decodeBodyTemplate = "%L%L -> %L = %L.decode(reader)"

        if (!field.isOptional && field.default == null) {
          throwExceptionBlock = CodeBlock.of(" ?: throw %1T.missingRequiredFields(%2L, %2S)",
              internalClass,
              field.name())
        }
      }

      declarationBody.addStatement("%L", fieldDeclaration)
      decodeBlock.addStatement(decodeBodyTemplate, indentation.repeat(2), field.tag(), fieldName,
          adapterName)
      returnBody.add("%L%L = %L%L,\n", indentation, fieldName, fieldName, throwExceptionBlock)
    }

    val tagHandlerClass = ClassName("com.squareup.wire", "TagHandler")

    decodeBlock.addStatement("%Lelse -> %T.%L", indentation.repeat(2), tagHandlerClass,
        "UNKNOWN_TAG")
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

    val builder = TypeSpec.enumBuilder(type.simpleName())
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
   * companion object {
   *     @JvmField
   *     val ADAPTER = object : EnumAdapter<PhoneType>(PhoneType::class.java) {
   *         override fun fromValue(value: Int): PhoneType? = values().find { it.value == value }
   *     }
   * }
   * ```
   */

  private fun generateEnumAdapter(message: EnumType): TypeSpec {
    val parentClassName = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator(message)

    val adapterName = nameAllocator.get("ADAPTER")
    val valueName = nameAllocator.get("value")

    val companionObjBuilder = TypeSpec.companionObjectBuilder()
    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)
    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(EnumAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("%T::class.java", parentClassName)
        .addFunction(FunSpec.builder("fromValue")
            .addModifiers(OVERRIDE)
            .addParameter(valueName, Int::class)
            .returns(parentClassName.asNullable())
            .addStatement("return values().find { it.value == value }")
            .build())
        .build()

    return companionObjBuilder.addProperty(
        PropertySpec.builder(adapterName, adapterType)
            .jvmField()
            .initializer("%L", adapterObject)
            .build())
        .build()
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
    val nameAllocator = nameAllocator(type)
    val parentClassName = generatedTypeName(type)
    val creatorName = nameAllocator.get("CREATOR")
    val creatorTypeName = ClassName("android.os", "Parcelable", "Creator")
        .parameterizedBy(parentClassName)

    companionObjBuilder.addProperty(PropertySpec.builder(creatorName, creatorTypeName)
        .jvmField()
        .initializer("%T.newCreator(ADAPTER)", ANDROID_MESSAGE)
        .build())
  }

  private fun getDefaultValue(field: Field): CodeBlock {
    val internalClass = ClassName("com.squareup.wire.internal", "Internal")
    return when {
      isEnum(field) -> if (field.default != null) {
        CodeBlock.of("%T.%L", typeName(field.type()), field.default)
      } else {
        CodeBlock.of("null")
      }
      field.isRepeated -> CodeBlock.of("%T.newMutableList()", internalClass)
      else -> CodeBlock.of("%L", field.default)
    }
  }

  private fun isEnum(field: Field): Boolean = schema.getType(field.type()) is EnumType

  private fun Field.getDeclaration(allocatedName: String): CodeBlock {
    val baseClass = nameToKotlinName.getValue(type())
    val fieldClass = declarationClass()
    val default = getDefaultValue(this)
    return when {
      isRepeated -> CodeBlock.of("var $allocatedName = mutableListOf<%T>()", baseClass)
      else -> CodeBlock.of("var $allocatedName: %T = %L", fieldClass, default)
    }
  }

  private fun Field.declarationClass(): TypeName {
    val fieldClass = getClass()
    if (isRepeated || default != null) return fieldClass
    return fieldClass.asNullable()
  }

  private fun Field.getClass(baseClass: TypeName = nameToKotlinName.getValue(type())) = when {
    isRepeated -> List::class.asClassName().parameterizedBy(baseClass)
    isOptional && default == null -> baseClass.asNullable()
    else -> baseClass.asNonNull()
  }

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
    private val MESSAGE = Message::class.asClassName()
    private val ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage")

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      emitAndroid: Boolean = false,
      javaInterop: Boolean = false
    ): KotlinGenerator {
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

      return KotlinGenerator(schema, map, emitAndroid, javaInterop)
    }
  }
}

private fun ProtoFile.kotlinPackage() = javaPackage() ?: packageName() ?: ""
