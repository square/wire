package com.squareup.wire.kotlin

import android.os.Parcel
import android.os.Parcelable
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.FINAL
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
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import okio.ByteString
import java.util.*

class KotlinGenerator private constructor(
    val schema: Schema,
    private val nameToKotlinName: Map<ProtoType, ClassName>,
    private val emitAndroid: Boolean
) {
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
    val allocator = NameAllocator()
    when (message) {
      is EnumType -> {
        allocator.newName("value", "value")
        allocator.newName("ADAPTER", "ADAPTER")
        message.constants().forEach { constant ->
          allocator.newName(constant.name(), constant)
        }
      }
      is MessageType -> {
        allocator.newName("unknownFields", "unknownFields")
        allocator.newName("ADAPTER", "ADAPTER")
        if (emitAndroid) {
          allocator.newName("CREATOR", "CREATOR")
        }
        message.fields().forEach { field ->
          allocator.newName(field.name(), field)
        }
      }
    }
    return allocator
  }

  private fun generateMessage(type: MessageType): TypeSpec {
    val className = typeName(type.type())

    val classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(DATA)
        .addType(generateAdapter(type))

    if (emitAndroid) {
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

    addMessageConstructor(type, classBuilder)

    type.nestedTypes().forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

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

  private fun generateAdapter(type: MessageType): TypeSpec {
    val nameAllocator = nameAllocator(type)
    val parentClassName = generatedTypeName(type)
    val adapterName = nameAllocator.get("ADAPTER")

    return TypeSpec.objectBuilder(adapterName)
        .superclass(ProtoAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("%T.%L", FieldEncoding::class.asClassName(),
            "LENGTH_DELIMITED")
        .addSuperclassConstructorParameter("%T::class.java", parentClassName)
        .addFunction(encodedSizeFunc(type))
        .addFunction(encodeFunc(type))
        .addFunction(decodeFunc(type))
        .build()
  }

  private fun addMessageConstructor(message: MessageType, classBuilder: TypeSpec.Builder) {
    val constructorBuilder = FunSpec.constructorBuilder()
    val nameAllocator = nameAllocator(message)

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
        ParameterSpec.builder(unknownFields, ByteString::class)
            .defaultValue("%T.EMPTY", ByteString::class)
            .build())
    classBuilder.addProperty(PropertySpec.builder(unknownFields, ByteString::class)
        .initializer(unknownFields)
        .build())

    classBuilder.primaryConstructor(constructorBuilder.build())
  }

  private fun encodedSizeFunc(message: MessageType): FunSpec {
    // TODO Fix indentation here.
    val className = generatedTypeName(message)
    val body = CodeBlock.builder()
    body.add("%[")

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)

      body.add("%L.%LencodedSizeWithTag(%L, value.%L) + \n",
          adapterName, if (field.isRepeated) "asRepeated()." else "", field.tag(), field.name())
    }

    body.add("value.unknownFields.size()%]\n")

    return FunSpec.builder("encodedSize")
        .addParameter("value", className)
        .returns(Int::class)
        .addCode("return %L", body.build())
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun encodeFunc(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    var body = CodeBlock.builder()

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      body.addStatement("%L.%LencodeWithTag(writer, %L, value.%L)",
          adapterName, if (field.isRepeated) "asRepeated()." else "", field.tag(), field.name())
    }

    body.addStatement("writer.writeBytes(value.unknownFields)")

    return FunSpec.builder("encode")
        .addParameter("writer", ProtoWriter::class)
        .addParameter("value", className)
        .addCode(body.build())
        .addModifiers(OVERRIDE)
        .build()
  }

  private fun decodeFunc(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type())

    var declarationBody = CodeBlock.builder()

    var returnBody = CodeBlock.builder()
    returnBody.add("return %L(\n", className.simpleName)

    var decodeBlock = CodeBlock.builder()
    decodeBlock.addStatement("val unknownFields = reader.decodeMessage { tag -> ")

    val INDENTATION = " ".repeat(4)

    // Indent manually as code generator doesn't handle this block gracefully.
    decodeBlock.addStatement("%Lwhen (tag) {", INDENTATION)
    val nameAllocator = nameAllocator(message)

    val missingRequiredFields = ClassName("com.squareup.wire.internal", "Internal")

    message.fields().forEach { field ->
      val adapterName = getAdapterName(field)
      var throwExceptionBlock = CodeBlock.of("")
      var decodeBodyTemplate: String
      val fieldName = nameAllocator.get(field)
      var fieldClass: TypeName = nameToKotlinName.getValue(field.type())
      var fieldDeclaration: CodeBlock;

      if (field.isRepeated) {
        fieldDeclaration = CodeBlock.of("var %L = mutableListOf<%T>()", fieldName, fieldClass)
        decodeBodyTemplate = "%L%L -> %L.add(%L.decode(reader))"
        fieldClass = List::class.asClassName().parameterizedBy(fieldClass)
      } else {
        fieldClass = fieldClass.asNullable()
        fieldDeclaration = CodeBlock.of("var %L: %T = null", fieldName, fieldClass)
        decodeBodyTemplate = "%L%L -> %L = %L.decode(reader)"

        if (!field.isOptional) {
          throwExceptionBlock = CodeBlock.of(" ?: throw %T.%L(%L, \"%L\")",
              missingRequiredFields, "missingRequiredFields", field.name(), field.name())
        }
      }

      if (field.default != null) {
        fieldClass = fieldClass.asNonNullable()
        fieldDeclaration = CodeBlock.of("var %L: %T = %L", fieldName,
            fieldClass, getDefaultValue(field))
        throwExceptionBlock = CodeBlock.of("")
      }

      declarationBody.addStatement("%L", fieldDeclaration)
      decodeBlock.addStatement(decodeBodyTemplate, INDENTATION.repeat(2), field.tag(), fieldName,
          adapterName)
      returnBody.add("%L%L = %L%L,\n", INDENTATION, fieldName, fieldName, throwExceptionBlock)
    }

    val unknownFieldsBuilder = ClassName("com.squareup.wire.kotlin", "UnknownFieldsBuilder")

    decodeBlock.addStatement("%Lelse -> %T.%L", INDENTATION.repeat(2), unknownFieldsBuilder,
        "UNKNOWN_FIELD")
    decodeBlock.addStatement("%L}", INDENTATION)
    decodeBlock.addStatement("}")

    returnBody.add("%LunknownFields = unknownFields\n)\n", INDENTATION)

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

  private fun getDefaultValue(field: Field): CodeBlock {
    return when {
      isEnum(field) -> {
        CodeBlock.of("%T.%L", typeName(field.type()), field.default)
      }
      else -> CodeBlock.of("%L", field.default)
    }
  }

  private fun isEnum(field: Field) : Boolean {
    return schema.getType(field.type()) is EnumType
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
