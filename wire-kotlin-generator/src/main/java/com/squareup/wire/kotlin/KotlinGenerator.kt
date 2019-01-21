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
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FIELD
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
import com.squareup.kotlinpoet.KModifier.SEALED
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.kotlinpoet.jvm.jvmStatic
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
import com.squareup.wire.schema.OneOf
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
  private val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()

  private val ProtoType.typeName
      get() = nameToKotlinName.getValue(this)
  private val Type.typeName
      get() = type().typeName

  /** Returns the full name of the class generated for [type].  */
  fun generatedTypeName(type: Type) = type.typeName

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
            message.fieldsAndOneOfFields().forEach { field ->
              newName(field.name(), field)
            }

            if (!javaInterOp) {
              message.oneOfs().forEach { oneOf ->
                newName(oneOf.name(), oneOf)
              }
            }
          }
        }
      }
    }
  }

  private fun generateMessage(type: MessageType): TypeSpec {
    val className = type.typeName
    val builderClassName = className.nestedClass("Builder")
    val nameAllocator = nameAllocator(type)
    val adapterName = nameAllocator["ADAPTER"]
    val unknownFields = nameAllocator["unknownFields"]
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

    if (type.oneOfs().isNotEmpty()) {
      if (javaInterOp) {
        classBuilder.addInitializerBlock(generateInitializerOneOfBlock(type))
      } else {
        // TODO emit oneofs using sealed classes.
      }
    }

    classBuilder.addType(companionObjBuilder.build())

    addMessageConstructor(type, classBuilder)

    type.nestedTypes().forEach { classBuilder.addType(generateType(it)) }

    if (!javaInterOp) {
      type.oneOfs().forEach { classBuilder.addType(generateOneOfClass(type, it)) }
    }

    return classBuilder.build()
  }

  private fun generateOneOfClass(type: MessageType, oneOf: OneOf): TypeSpec {
    val nameAllocator = nameAllocator(type)
    val oneOfClassName = nameAllocator[oneOf].capitalize()
    val oneOfClassType = type.oneOfClass(oneOf)
    val builder = TypeSpec.classBuilder(oneOfClassName)
        .addModifiers(SEALED)
    oneOf.fields().forEach { oneOfField ->
      val name = nameAllocator[oneOfField]
      val className = name.capitalize()
      val fieldClass = oneOfField.type().typeName

      builder.addType(TypeSpec.classBuilder(className)
          .addModifiers(DATA)
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

  private fun generateInitializerOneOfBlock(type: MessageType): CodeBlock {
    return buildCodeBlock {
      val nameAllocator = nameAllocator(type)
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

    type.fieldsWithJavaInteropOneOfs().forEach { field ->
      val fieldName = nameAllocator[field]
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

      builder
          .addProperty(PropertySpec.builder(fieldName, field.declarationClass)
              .mutable(true)
              .initializer(field.getDefaultValue())
              .addModifiers(INTERNAL)
              .build())
          .addFunction(builderSetter(field, nameAllocator, builderClass))
    }

    val buildFunction = FunSpec.builder("build")
        .addModifiers(OVERRIDE)
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
    val byteClass = ProtoType.BYTES.typeName

    val fields = message.fieldsWithJavaInteropOneOfs()

    fields.forEach { field ->
      var fieldClass: TypeName
      val fieldName = nameAllocator[field]
      val fieldType = field.type()
      var defaultValue = CodeBlock.of("null")

      when {
        field.isRepeated -> {
          fieldClass = List::class.asClassName().parameterizedBy(fieldType.typeName)
          defaultValue = CodeBlock.of("emptyList()")
        }
        fieldType.isMap -> {
          fieldClass = Map::class.asTypeName()
              .parameterizedBy(fieldType.keyType().typeName, fieldType.valueType().typeName)
          defaultValue = CodeBlock.of("emptyMap()")
        }
        !field.isRequired -> fieldClass = fieldType.typeName.copy(nullable = true)
        else -> fieldClass = fieldType.typeName
      }

      if (field.default != null) {
        defaultValue = field.getDefaultValue()
        fieldClass = fieldType.typeName.copy(nullable = false)
      }

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (!field.isRequired && !fieldType.isMap) {
        parameterSpec.defaultValue(defaultValue)
      }

      parameterSpec.addAnnotation(AnnotationSpec.builder(WireField::class)
          .useSiteTarget(FIELD)
          .addMember("tag = %L", field.tag())
          .addMember("adapter = %S", field.getAdapterName())
          .build())

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

    companionObjBuilder.addProperty(PropertySpec.builder(adapterName, adapterType)
        .jvmField()
        .initializer("%L", adapterObject.build())
        .build())
  }

  private fun Field.toProtoAdapterPropertySpec(): PropertySpec {
    val adapterType = ProtoAdapter::class.asTypeName()
        .parameterizedBy(Map::class.asTypeName()
            .parameterizedBy(keyType.typeName, valueType.typeName))

    return PropertySpec.builder("${name()}Adapter", adapterType, PRIVATE)
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
    val nameAllocator = nameAllocator(message)
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
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun encodeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = buildCodeBlock {
      val nameAllocator = nameAllocator(message)

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
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun decodeFun(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator(message)
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
        .addModifiers(OVERRIDE)
        .build()
  }

  // TODO add support for custom adapters.
  private fun Field.getAdapterName(): CodeBlock {
    return if (type().isMap) {
      CodeBlock.of("%NAdapter", name())
    } else {
      type().getAdapterName()
    }
  }

  private fun ProtoType.getAdapterName(): CodeBlock {
    return when {
      isScalar -> CodeBlock.of("%T.%L", ProtoAdapter::class, simpleName().toUpperCase(Locale.US))
      isMap -> throw IllegalArgumentException("Can't create single adapter for map type $this")
      else -> CodeBlock.of("%L.ADAPTER", typeName.simpleName)
    }
  }

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
  private fun generateEnum(message: EnumType): TypeSpec {
    val type = message.type()
    val nameAllocator = nameAllocator(message)

    val valueName = nameAllocator["value"]

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
    val nameAllocator = nameAllocator(message)
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
    val nameAllocator = nameAllocator(message)

    val adapterName = nameAllocator["ADAPTER"]
    val valueName = nameAllocator["value"]

    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)
    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(EnumAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("\n⇥%T::class.java\n⇤", parentClassName)
        .addFunction(FunSpec.builder("fromValue")
            .addModifiers(OVERRIDE)
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
    val creatorName = nameAllocator["CREATOR"]
    val creatorTypeName = ClassName("android.os", "Parcelable", "Creator")
        .parameterizedBy(parentClassName)

    companionObjBuilder.addProperty(PropertySpec.builder(creatorName, creatorTypeName)
        .jvmField()
        .initializer("%T.newCreator(ADAPTER)", ANDROID_MESSAGE)
        .build())
  }

  private val Field.isEnum: Boolean
    get() = schema.getType(type()) is EnumType

  private fun Field.getDeclaration(allocatedName: String) = when {
    isRepeated -> CodeBlock.of("val $allocatedName = mutableListOf<%T>()", type().typeName)
    isMap -> CodeBlock.of("val $allocatedName = mutableMapOf<%T, %T>()",
        keyType.typeName, valueType.typeName)
    else -> CodeBlock.of("var $allocatedName: %T = %L", declarationClass, getDefaultValue())
  }

  private val Field.declarationClass: TypeName
    get() = when {
      isRepeated || default != null -> getClass()
      else -> getClass().copy(nullable = true)
    }

  private fun Field.getClass(baseClass: TypeName = nameToKotlinName.getValue(type())) = when {
    isRepeated -> List::class.asClassName().parameterizedBy(baseClass)
    isOptional && default == null -> baseClass.copy(nullable = true)
    else -> baseClass.copy(nullable = false)
  }

  private fun Field.getDefaultValue(): CodeBlock {
    return when {
      isRepeated -> {
        if (javaInterOp) {
          val internalClass = ClassName("com.squareup.wire.internal", "Internal")
          CodeBlock.of("%T.newMutableList()", internalClass)
        } else {
          CodeBlock.of("emptyList()")
        }
      }
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

  private fun MessageType.fieldsWithJavaInteropOneOfs(): List<Field> {
    return if (javaInterOp)
      fieldsAndOneOfFields()
    else
      fields()
  }

  private fun MessageType.oneOfClass(oneOf: OneOf): ClassName {
    val name = oneOf.name().capitalize()
    return typeName.nestedClass(name)
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