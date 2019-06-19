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
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
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
import com.squareup.wire.WireRpc
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Options.ENUM_VALUE_OPTIONS
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import okio.ByteString
import java.util.Locale

class KotlinGenerator private constructor(
  val schema: Schema,
  private val nameToKotlinName: Map<ProtoType, ClassName>,
  private val emitAndroid: Boolean,
  private val javaInterOp: Boolean,
  private val blockingServices: Boolean
) {
  private val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()

  private val ProtoType.typeName
    get() = nameToKotlinName.getValue(this)
  private val ProtoType.isEnum
    get() = schema.getType(this) is EnumType
  private val Type.typeName
    get() = type().typeName
  private val Service.serviceName
    get() = type().typeName

  /** Returns the full name of the class generated for [type].  */
  fun generatedTypeName(type: Type) = type.typeName
  /** Returns the full name of the class generated for [service].  */
  fun generatedServiceName(service: Service) = service.serviceName

  fun generateType(type: Type): TypeSpec = when (type) {
    is MessageType -> generateMessage(type)
    is EnumType -> generateEnum(type)
    is EnclosingType -> generateEnclosing(type)
    else -> error("Unknown type $type")
  }

  /**
   * If [rpc] isn't null, this will generate code only for this rpc; otherwise, all rpcs of the
   * [service] will be code generated.
   */
  fun generateService(service: Service, rpc: Rpc? = null): TypeSpec {
    val (interfaceName, rpcs) =
        if (rpc == null) service.name() to service.rpcs()
        else service.name() + rpc.name() to listOf(rpc)

    val superinterface = com.squareup.wire.Service::class.java

    return TypeSpec.interfaceBuilder(interfaceName)
        .addSuperinterface(superinterface)
        .addFunctions(rpcs.map {
          generateRpcFunction(it, service.name(), service.type().enclosingTypeOrPackage())
        })
        .build()
  }

  private fun generateRpcFunction(
    rpc: Rpc,
    serviceName: String,
    servicePackageName: String?
  ): FunSpec {
    val packageName = if (servicePackageName.isNullOrBlank()) "" else "$servicePackageName."

    val wireRpcAnnotationSpec = AnnotationSpec.builder(WireRpc::class.asClassName())
        .addMember("path = %S", "/$packageName$serviceName/${rpc.name()}")
        // TODO(oldergod|jwilson) Lets' use Profile for this.
        .addMember("requestAdapter = %S", "$packageName${rpc.requestType().simpleName()}#ADAPTER")
        .addMember("responseAdapter = %S", "$packageName${rpc.responseType().simpleName()}#ADAPTER")
        .build()
    val funSpecBuilder = FunSpec.builder(rpc.name())
        .addModifiers(KModifier.ABSTRACT)
        .addAnnotation(wireRpcAnnotationSpec)

    when {
      rpc.requestStreaming() && rpc.responseStreaming() -> {
        val requestChannel =
            SendChannel::class.asClassName().parameterizedBy(rpc.requestType().typeName)
        val responseChannel =
            ReceiveChannel::class.asClassName().parameterizedBy(rpc.responseType().typeName)
        funSpecBuilder
            .returns(Pair::class.asClassName().parameterizedBy(requestChannel, responseChannel))
      }
      rpc.requestStreaming() -> {
        val requestChannel =
            SendChannel::class.asClassName().parameterizedBy(rpc.requestType().typeName)
        val responseDeferred =
            Deferred::class.asClassName().parameterizedBy(rpc.responseType().typeName)
        funSpecBuilder
            .returns(Pair::class.asClassName().parameterizedBy(requestChannel, responseDeferred))
      }
      rpc.responseStreaming() -> {
        funSpecBuilder
            .addParameter("request", rpc.requestType().typeName)
            .returns(
                ReceiveChannel::class.asClassName().parameterizedBy(rpc.responseType().typeName))
      }
      else -> {
        funSpecBuilder
            .addParameter("request", rpc.requestType().typeName)
            .returns(rpc.responseType().typeName)
            .apply {
              if (!blockingServices) addModifiers(KModifier.SUSPEND)
            }
      }
    }

    return funSpecBuilder.build()
  }

  private fun nameAllocator(message: Type): NameAllocator {
    return nameAllocatorStore.getOrPut(message) {
      NameAllocator().apply {
        when (message) {
          is EnumType -> {
            newName("value", "value")
            newName("ADAPTER", "ADAPTER")
            message.constants().forEach { constant ->
              newName(constant.name, constant)
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
        .apply { if (type.documentation().isNotBlank()) addKdoc("%L\n", type.documentation()) }
        .addModifiers(DATA)
        .superclass(if (javaInterOp) {
          superclass.parameterizedBy(className, builderClassName)
        } else {
          superclass.parameterizedBy(className, NOTHING)
        })
        .addSuperclassConstructorParameter(adapterName)
        .addSuperclassConstructorParameter(unknownFields)
        .addFunction(generateNewBuilderMethod(type, builderClassName))
        .apply {
          if (javaInterOp) {
            addType(generateBuilderClass(type, className, builderClassName))
          }
        }

    if (emitAndroid) {
      addAndroidCreator(type, companionObjBuilder)
    }

    if (type.oneOfs().isNotEmpty()) {
      classBuilder.addInitializerBlock(generateInitializerOneOfBlock(type))
    }

    classBuilder.addType(companionObjBuilder.build())

    addMessageConstructor(type, classBuilder)

    if (type.fieldsAndOneOfFields().any { it.isRedacted }) {
      classBuilder.addFunction(generateToStringMethod(type))
    }

    type.nestedTypes().forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  private fun generateInitializerOneOfBlock(type: MessageType): CodeBlock {
    return buildCodeBlock {
      val nameAllocator = nameAllocator(type)
      type.oneOfs()
          .filter { oneOf -> oneOf.fields().size >= 2 }
          .forEach { oneOf ->
            val countNonNull = MemberName("com.squareup.wire.internal", "countNonNull")
            val fieldNames = oneOf.fields().joinToString(", ", transform = nameAllocator::get)
            beginControlFlow("require(%M(%L) <= 1)", countNonNull, fieldNames)
            addStatement("%S", "At most one of $fieldNames may be non-null")
            endControlFlow()
          }
    }
  }

  private fun generateNewBuilderMethod(type: MessageType, builderClassName: ClassName): FunSpec {
    val funBuilder = FunSpec.builder("newBuilder")
        .addModifiers(OVERRIDE)

    if (!javaInterOp) {
      return funBuilder
          .addAnnotation(AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "Shouldn't be used in Kotlin")
              .addMember("level = %T.%L", DeprecationLevel::class, DeprecationLevel.HIDDEN)
              .build())
          .returns(NOTHING)
          .addStatement("throw %T()", ClassName("kotlin", "AssertionError"))
          .build()
    }

    funBuilder.returns(builderClassName)

    val nameAllocator = nameAllocator(type)

    funBuilder.addStatement("val builder = Builder()")

    type.fieldsAndOneOfFields().forEach { field ->
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

    val returnBody = buildCodeBlock {
      add("return %T(⇥\n", className)

      val missingRequiredFields = MemberName("com.squareup.wire.internal", "missingRequiredFields")
      type.fieldsAndOneOfFields().forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && field.isRequired) {
          CodeBlock.of(" ?: throw %1M(%2L, %2S)", missingRequiredFields, field.name())
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }
      add("unknownFields = buildUnknownFields()")
      add("⇤\n)\n") // close the block
    }

    type.fieldsAndOneOfFields().forEach { field ->
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
      val checkElementsNotNull = MemberName("com.squareup.wire.internal", "checkElementsNotNull")
      funBuilder.addStatement("%M(%L)", checkElementsNotNull, fieldName)
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

    val fields = message.fieldsAndOneOfFields()

    fields.forEach { field ->
      val fieldClass = field.typeName
      val fieldName = nameAllocator[field]
      val fieldType = field.type()
      val defaultValue = field.getDefaultValue()

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (!field.isRequired && !fieldType.isMap) {
        parameterSpec.defaultValue(defaultValue)
      }

      if (field.isDeprecated) {
        parameterSpec.addAnnotation(AnnotationSpec.builder(Deprecated::class)
            .addMember("message = %S", "$fieldName is deprecated")
            .build())
      }

      parameterSpec.addAnnotation(wireFieldAnnotation(field))

      if (javaInterOp) {
        parameterSpec.addAnnotation(JvmField::class)
      }

      constructorBuilder.addParameter(parameterSpec.build())
      classBuilder.addProperty(PropertySpec.builder(fieldName, fieldClass)
          .initializer(fieldName)
          .apply {
            if (field.documentation().isNotBlank()) {
              addKdoc("%L\n", field.documentation())
            }
          }
          .build())
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

  private fun wireFieldAnnotation(field: Field): AnnotationSpec {
    return AnnotationSpec.builder(WireField::class)
        .useSiteTarget(FIELD)
        .addMember("tag = %L", field.tag())
        .apply {
          if (field.type().isMap) {
            addMember("keyAdapter = %S", field.type().keyType().adapterString())
            addMember("adapter = %S", field.type().valueType().adapterString())
          } else {
            addMember("adapter = %S", field.type().adapterString())
          }
        }
        .apply {
          if (!field.isOptional) {
            if (field.isPacked) {
              addMember("label = %T.PACKED", WireField.Label::class)
            } else if (field.label() != null) {
              addMember("label = %T.%L", WireField.Label::class, field.label())
            }
          }
        }
        .apply { if (field.isRedacted) addMember("redacted = true") }
        .build()
  }

  private fun ProtoType.adapterString() = when {
    isScalar -> ProtoAdapter::class.qualifiedName + '#' + toString().toUpperCase(Locale.US)
    else -> typeName.reflectionName() + "#ADAPTER"
  }

  private fun generateToStringMethod(type: MessageType): FunSpec {
    val nameAllocator = nameAllocator(type)
    val className = generatedTypeName(type)
    return FunSpec.builder("toString")
        .addModifiers(OVERRIDE)
        .returns(String::class)
        .addCode("return %L", buildCodeBlock {
          beginControlFlow("buildString")
          addStatement("append(%S)", className.simpleName + "(")
          val redactedFields = type.fieldsAndOneOfFields().map { field ->
            nameAllocator[field] to field.isRedacted
          }
          redactedFields.forEachIndexed { index, (name, isRedacted) ->
            addStatement("append(%P)", buildString {
              if (index > 0) append(", ")
              append(name)
              if (isRedacted) {
                append("=██")
              } else {
                append("=\$")
                append(name)
              }
            })
          }
          addStatement("append(%S)", ")")
          endControlFlow()
        })
        .build()
  }

  /**
   * Example
   * ```
   * companion object {
   *  @JvmField
   *  val ADAPTER : ProtoAdapter<Person> =
   *      object : ProtoAdapter<Person>(FieldEncoding.LENGTH_DELIMITED, Person::class) {
   *    override fun encodedSize(value: Person): Int { .. }
   *    override fun encode(writer: ProtoWriter, value: Person) { .. }
   *    override fun decode(reader: ProtoReader): Person { .. }
   *    override fun redact(value: Person): Person? { .. }
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
        .addSuperclassConstructorParameter("\n%T::class\n⇤", parentClassName)
        .addFunction(encodedSizeFun(type))
        .addFunction(encodeFun(type))
        .addFunction(decodeFun(type))
        .addFunction(redactFun(type))

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
      message.fieldsAndOneOfFields().forEach { field ->
        val adapterName = field.getAdapterName()
        val fieldName = nameAllocator[field]
        add("%L.%LencodedSizeWithTag(%L, value.%L) +\n",
            adapterName,
            if (field.isRepeated) "asRepeated()." else "",
            field.tag(),
            fieldName)
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

      message.fieldsAndOneOfFields().forEach { field ->
        val adapterName = field.getAdapterName()
        val fieldName = nameAllocator[field]
        addStatement("%L.%LencodeWithTag(writer, %L, value.%L)",
            adapterName,
            if (field.isRepeated) "asRepeated()." else "",
            field.tag(),
            fieldName)
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

    val declarationBody = buildCodeBlock {
      message.fieldsAndOneOfFields().forEach { field ->
        val fieldName = nameAllocator[field]
        val fieldDeclaration: CodeBlock = field.getDeclaration(fieldName)
        addStatement("%L", fieldDeclaration)
      }
    }

    val returnBody = buildCodeBlock {
      addStatement("return %T(⇥", className)

      val missingRequiredFields = MemberName("com.squareup.wire.internal", "missingRequiredFields")
      message.fieldsAndOneOfFields().forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && !field.isMap && field.isRequired) {
          CodeBlock.of(" ?: throw %1M(%2L, %2S)", missingRequiredFields, field.name())
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }

      add("unknownFields = unknownFields")
      add("⇤\n)\n") // close the block
    }

    val decodeBlock = buildCodeBlock {
      addStatement("val unknownFields = reader.forEachTag { tag ->")
      addStatement("⇥when (tag) {⇥")

      message.fieldsAndOneOfFields().forEach { field ->
        val fieldName = nameAllocator[field]
        val adapterName = field.getAdapterName()

        val decodeBodyTemplate = when {
          field.isRepeated -> "%L -> %L.add(%L.decode(reader))"
          field.isMap -> "%L -> %L.putAll(%L.decode(reader))"
          else -> "%L -> %L = %L.decode(reader)"
        }

        addStatement(decodeBodyTemplate, field.tag(), fieldName, adapterName)
      }
      addStatement("else -> reader.readUnknownField(tag)")
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

  private fun redactFun(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type())
    val nameAllocator = nameAllocator(message)
    val result = FunSpec.builder("redact")
        .addModifiers(OVERRIDE)
        .addParameter("value", className)
        .returns(className)

    val redactedMessageFields = message.fields().filter { it.isRedacted }
    val requiredRedactedMessageFields = redactedMessageFields.filter { it.isRequired }
    if (requiredRedactedMessageFields.isNotEmpty()) {
      result.addStatement(
          "throw %T(%S)",
          UnsupportedOperationException::class,
          requiredRedactedMessageFields.joinToString(
              prefix = if (requiredRedactedMessageFields.size > 1) "Fields [" else "Field '",
              postfix = if (requiredRedactedMessageFields.size > 1) "] are " else "' is " +
                  "required and cannot be redacted.",
              transform = nameAllocator::get
          )
      )
      return result.build()
    }

    val redactedFields = mutableListOf<CodeBlock>()
    for (field in message.fieldsAndOneOfFields()) {
      val fieldName = nameAllocator[field]
      val redactedField = field.redact(fieldName)
      if (redactedField != null) {
        redactedFields += CodeBlock.of("%N = %L", fieldName, redactedField)
      }
    }
    redactedFields += CodeBlock.of("unknownFields = %T.EMPTY", ByteString::class)
    return result
        .addStatement(
            "return %L",
            redactedFields.joinToCode(separator = ",\n", prefix = "value.copy(\n⇥", suffix = "\n⇤)")
        )
        .build()
  }

  private fun Field.redact(fieldName: String): CodeBlock? {
    if (isRedacted) {
      return when {
        isRepeated -> CodeBlock.of("emptyList()")
        isMap -> CodeBlock.of("emptyMap()")
        else -> CodeBlock.of("null")
      }
    } else if (!type().isScalar && !type().isEnum) {
      val redactElements = MemberName("com.squareup.wire.internal", "redactElements")
      if (isRepeated) {
        return CodeBlock.of("value.%N.%M(%L)", fieldName, redactElements, getAdapterName())
      } else if (isMap) {
        // We only need to ask the values to redact themselves if the type is a message.
        if (!valueType.isScalar && !valueType.isEnum) {
          val adapterName = valueType.getAdapterName()
          return CodeBlock.of("value.%N.%M(%L)", fieldName, redactElements, adapterName)
        }
      } else {
        val adapterName = getAdapterName()
        return if (isRequired) {
          CodeBlock.of("%L.redact(value.%N)", adapterName, fieldName)
        } else {
          CodeBlock.of("value.%N?.let(%L::redact)", fieldName, adapterName)
        }
      }
    }
    return null
  }

  // TODO add support for custom adapters.
  private fun Field.getAdapterName(nameDelimiter: Char = '.'): CodeBlock {
    return if (type().isMap) {
      CodeBlock.of("%NAdapter", name())
    } else {
      type().getAdapterName(nameDelimiter)
    }
  }

  private fun ProtoType.getAdapterName(adapterFieldDelimiterName: Char = '.'): CodeBlock {
    return when {
      isScalar -> CodeBlock.of(
          "%T$adapterFieldDelimiterName%L",
          ProtoAdapter::class, simpleName().toUpperCase(Locale.US)
      )
      isMap -> throw IllegalArgumentException("Can't create single adapter for map type $this")
      else -> CodeBlock.of("%T${adapterFieldDelimiterName}ADAPTER", typeName)
    }
  }

  /**
   * Example
   * ```
   * enum class PhoneType(override val value: Int) : WireEnum {
   *     HOME(0),
   *     ...
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
        .apply {
          if (message.documentation().isNotBlank()) {
            addKdoc("%L\n", message.documentation())
          }
        }
        .addSuperinterface(WireEnum::class)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(valueName, Int::class, OVERRIDE)
            .build())
        .addProperty(PropertySpec.builder(valueName, Int::class)
            .initializer(valueName)
            .build())
        .addType(generateEnumCompanion(message))

    message.constants().forEach { constant ->
      builder.addEnumConstant(nameAllocator[constant], TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", constant.tag)
          .apply {
            if (constant.documentation.isNotBlank()) {
              addKdoc("%L\n", constant.documentation)
            }
            if (constant.options.get(ENUM_DEPRECATED) == "true") {
              addAnnotation(AnnotationSpec.builder(Deprecated::class)
                  .addMember("message = %S", "${constant.name} is deprecated")
                  .build())
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
            addCode("%L -> %L\n", constant.tag, nameAllocator[constant])
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
   * val ADAPTER = object : EnumAdapter<PhoneType>(PhoneType::class) {
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
        .addSuperclassConstructorParameter("\n⇥%T::class\n⇤", parentClassName)
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

  private fun generateEnclosing(type: EnclosingType): TypeSpec {
    val classBuilder = TypeSpec.objectBuilder(type.typeName)

    type.nestedTypes()
        .forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
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
      isMap -> getClass()
      else -> getClass().copy(nullable = true)
    }

  private fun ProtoType.asTypeName(): TypeName = when {
    isMap -> Map::class.asTypeName()
        .parameterizedBy(keyType().asTypeName(), valueType().asTypeName())
    else -> nameToKotlinName.getValue(this)
  }

  private fun Field.getClass(baseClass: TypeName = type().asTypeName()) = when {
    isRepeated -> List::class.asClassName().parameterizedBy(baseClass)
    isOptional && default == null -> baseClass.copy(nullable = true)
    else -> baseClass.copy(nullable = false)
  }

  private val Field.typeName: TypeName
    get() = when {
      isRepeated -> List::class.asClassName().parameterizedBy(type().typeName)
      isMap -> Map::class.asTypeName().parameterizedBy(keyType.typeName, valueType.typeName)
      !isRequired || default != null -> type().typeName.copy(nullable = true)
      else -> type().typeName
    }

  private fun Field.getDefaultValue(): CodeBlock {
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

    private val ENUM_DEPRECATED = ProtoMember.get(ENUM_VALUE_OPTIONS, "deprecated")

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      emitAndroid: Boolean = false,
      javaInterop: Boolean = false,
      blockingServices: Boolean = false
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

      return KotlinGenerator(schema, map, emitAndroid, javaInterop, blockingServices)
    }
  }
}

private fun ProtoFile.kotlinPackage() = javaPackage() ?: packageName() ?: ""