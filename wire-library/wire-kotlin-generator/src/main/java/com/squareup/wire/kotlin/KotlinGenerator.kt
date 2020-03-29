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

import com.squareup.kotlinpoet.ANY
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
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
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
import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.Message
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
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
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.Options.Companion.ENUM_OPTIONS
import com.squareup.wire.schema.Options.Companion.ENUM_VALUE_OPTIONS
import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.MESSAGE_OPTIONS
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
import okio.ByteString.Companion.encode
import java.math.BigInteger
import java.util.Locale

class KotlinGenerator private constructor(
  val schema: Schema,
  private val nameToKotlinName: Map<ProtoType, ClassName>,
  private val emitAndroid: Boolean,
  private val javaInterOp: Boolean,
  private val rpcCallStyle: RpcCallStyle,
  private val rpcRole: RpcRole
) {
  private val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()

  private val ProtoType.typeName
    get() = nameToKotlinName.getValue(this)
  private val ProtoType.isEnum
    get() = schema.getType(this) is EnumType
  private val Type.typeName
    get() = type.typeName
  private val Service.serviceName
    get() = type().typeName

  /** Returns the full name of the class generated for [type].  */
  fun generatedTypeName(type: Type) = type.typeName

  /**
   * Returns the full name of the class generated for [service]#[rpc]. This returns a name like
   * `RouteGuideClient` or `RouteGuideGetFeatureBlockingServer`.
   */
  fun generatedServiceName(service: Service, rpc: Rpc? = null): ClassName {
    val typeName = service.serviceName
    val simpleName = buildString {
      append(typeName.simpleName)
      if (rpc != null) {
        append(rpc.name)
      }
      when (rpcCallStyle) {
        RpcCallStyle.SUSPENDING -> Unit // Suspending is implicit.
        RpcCallStyle.BLOCKING -> {
          if (rpcRole == RpcRole.SERVER) append("Blocking")
        }
      }
      when (rpcRole) {
        RpcRole.CLIENT -> append("Client")
        RpcRole.SERVER -> append("Server")
      }
    }
    return typeName.peerClass(simpleName)
  }

  fun generateType(type: Type): TypeSpec {
    check(type.type != ProtoType.ANY)
    return when (type) {
      is MessageType -> generateMessage(type)
      is EnumType -> generateEnum(type)
      is EnclosingType -> generateEnclosing(type)
      else -> error("Unknown type $type")
    }
  }

  /**
   * Generates all [TypeSpec]s for the given [Service].
   *
   * If [onlyRpc] isn't null, this will generate code only for this onlyRpc; otherwise, all RPCs of
   * the [service] will be code generated.
   */
  fun generateServiceTypeSpecs(service: Service, onlyRpc: Rpc? = null): List<TypeSpec> {
    val typeSpecs = mutableListOf<TypeSpec>()
    if (rpcRole == RpcRole.SERVER) {
      typeSpecs.add(generateService(service, onlyRpc))
    } else {
      typeSpecs.add(generateService(service, onlyRpc, generateClientImplementation = false))
      typeSpecs.add(generateService(service, onlyRpc, generateClientImplementation = true))
    }
    return typeSpecs
  }

  private fun generateService(
    service: Service,
    onlyRpc: Rpc?,
    generateClientImplementation: Boolean
  ): TypeSpec {
    val serviceName = generatedServiceName(service, onlyRpc)
    val builder = if (rpcRole == RpcRole.SERVER || !generateClientImplementation) {
      TypeSpec.interfaceBuilder(serviceName)
          .addSuperinterface(com.squareup.wire.Service::class.java)
    } else {
      val serviceImplementationName = ClassName(
          serviceName.packageName,
          "Grpc${serviceName.simpleName}"
      )
      TypeSpec.classBuilder(serviceImplementationName)
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("client", GrpcClient::class)
              .build())
          .addProperty(PropertySpec.builder("client", GrpcClient::class, PRIVATE)
              .initializer("client")
              .build())
          .addSuperinterface(serviceName)
    }
    builder
        .apply {
          if (service.documentation().isNotBlank()) {
            addKdoc("%L\n", service.documentation().sanitizeKdoc())
          }
        }

    val rpcs = if (onlyRpc == null) service.rpcs() else listOf(onlyRpc)
    for (rpc in rpcs) {
      builder.addFunction(generateRpcFunction(
          rpc, service.name(), service.type().enclosingTypeOrPackage, generateClientImplementation))
    }

    return builder.build()
  }

  /**
   * If [onlyRpc] isn't null, this will generate code only for this onlyRpc; otherwise, all RPCs of
   * the [service] will be code generated.
   */
  @Deprecated(
      message = "Client generated code now includes two TypeSpecs - interface and " +
          "implementation. Use generateServiceTypeSpecs to generate correct definitions.",
      replaceWith = ReplaceWith("generateServiceTypeSpecs")
  )
  fun generateService(service: Service, onlyRpc: Rpc? = null): TypeSpec {
    return generateService(service, onlyRpc, generateClientImplementation = true)
  }

  private fun generateRpcFunction(
    rpc: Rpc,
    serviceName: String,
    servicePackageName: String?,
    generateClientImplementation: Boolean
  ): FunSpec {
    val packageName = if (servicePackageName.isNullOrBlank()) "" else "$servicePackageName."
    val funSpecBuilder = FunSpec.builder(rpc.name)
        .apply {
          if (rpc.documentation.isNotBlank()) {
            addKdoc("%L\n", rpc.documentation.sanitizeKdoc())
          }
        }

    val requestType = rpc.requestType!!.typeName
    val responseType = rpc.responseType!!.typeName

    if (rpcRole == RpcRole.SERVER) {
      val wireRpcAnnotationSpec = AnnotationSpec.builder(WireRpc::class.asClassName())
          .addMember("path = %S", "/$packageName$serviceName/${rpc.name}")
          // TODO(oldergod|jwilson) Lets' use Profile for this.
          .addMember("requestAdapter = %S", rpc.requestType!!.adapterString())
          .addMember("responseAdapter = %S", rpc.responseType!!.adapterString())
          .build()
      funSpecBuilder
          .addAnnotation(wireRpcAnnotationSpec)
          .addModifiers(KModifier.ABSTRACT)
      if (rpcCallStyle == RpcCallStyle.SUSPENDING) {
        funSpecBuilder.addModifiers(KModifier.SUSPEND)
      }
      when {
        rpc.requestStreaming && rpc.responseStreaming -> {
          funSpecBuilder
              .addParameter("request", readableStreamOf(requestType))
              .addParameter("response", writableStreamOf(responseType))
        }
        rpc.requestStreaming -> {
          funSpecBuilder
              .addParameter("request", readableStreamOf(requestType))
              .returns(responseType)
        }
        rpc.responseStreaming -> {
          funSpecBuilder
              .addParameter("request", requestType)
              .addParameter("response", writableStreamOf(responseType))
        }
        else -> {
          funSpecBuilder
              .addParameter("request", requestType)
              .returns(responseType)
        }
      }
    } else {
      val grpcMethod = CodeBlock.builder()
          .addStatement("%T(⇥⇥", GrpcMethod::class)
          .addStatement("path = %S,", "/$packageName$serviceName/${rpc.name}")
          .addStatement("requestAdapter = %L,", rpc.requestType!!.getAdapterName())
          .addStatement("responseAdapter = %L", rpc.responseType!!.getAdapterName())
          .add("⇤⇤)")
          .build()
      when {
        rpc.requestStreaming || rpc.responseStreaming -> {
          funSpecBuilder
              .returns(
                  GrpcStreamingCall::class.asClassName().parameterizedBy(requestType, responseType)
              )
          if (generateClientImplementation) {
            funSpecBuilder
                .addModifiers(OVERRIDE)
                .addStatement("return client.newStreamingCall(%L)", grpcMethod)
          } else {
            funSpecBuilder.addModifiers(ABSTRACT)
          }
        }
        else -> {
          funSpecBuilder
              .returns(
                  GrpcCall::class.asClassName().parameterizedBy(requestType, responseType)
              )
          if (generateClientImplementation) {
            funSpecBuilder
                .addModifiers(OVERRIDE)
                .addStatement("return client.newCall(%L)", grpcMethod)
          } else {
            funSpecBuilder.addModifiers(ABSTRACT)
          }
        }
      }
    }

    return funSpecBuilder.build()
  }

  private fun writableStreamOf(typeName: TypeName): ParameterizedTypeName {
    return when (rpcCallStyle) {
      RpcCallStyle.SUSPENDING -> ReceiveChannel::class.asClassName().parameterizedBy(typeName)
      RpcCallStyle.BLOCKING -> MessageSink::class.asClassName().parameterizedBy(typeName)
    }
  }

  private fun readableStreamOf(typeName: TypeName): ParameterizedTypeName {
    return when (rpcCallStyle) {
      RpcCallStyle.SUSPENDING -> SendChannel::class.asClassName().parameterizedBy(typeName)
      RpcCallStyle.BLOCKING -> MessageSource::class.asClassName().parameterizedBy(typeName)
    }
  }

  private fun readableSingleOf(typeName: TypeName): ParameterizedTypeName {
    return when (rpcCallStyle) {
      RpcCallStyle.SUSPENDING -> Deferred::class.asClassName().parameterizedBy(typeName)
      RpcCallStyle.BLOCKING -> MessageSource::class.asClassName().parameterizedBy(typeName)
    }
  }

  private fun pairOf(a: TypeName, b: TypeName) =
      Pair::class.asClassName().parameterizedBy(a, b)

  private fun nameAllocator(message: Type): NameAllocator {
    return nameAllocatorStore.getOrPut(message) {
      NameAllocator().apply {
        when (message) {
          is EnumType -> {
            newName("value", "value")
            newName("ADAPTER", "ADAPTER")
            newName("ENUM_OPTIONS", "ENUM_OPTIONS")
            message.constants.forEach { constant ->
              newName(constant.name, constant)
            }
          }
          is MessageType -> {
            newName("unknownFields", "unknownFields")
            newName("ADAPTER", "ADAPTER")
            newName("adapter", "adapter")
            newName("reader", "reader")
            newName("Builder", "Builder")
            newName("builder", "builder")
            newName("MESSAGE_OPTIONS", "MESSAGE_OPTIONS")

            if (emitAndroid) {
              newName("CREATOR", "CREATOR")
            }
            message.fieldsAndOneOfFields.forEach { field ->
              if (field.name == field.type!!.simpleName ||
                  schema.getType(field.qualifiedName) != null) {
                newName(field.qualifiedName, field)
              } else {
                newName(field.name, field)
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
    val companionBuilder = TypeSpec.companionObjectBuilder()

    addDefaultFields(type, companionBuilder, nameAllocator)
    addAdapter(type, companionBuilder)

    val classBuilder = TypeSpec.classBuilder(className)
        .apply {
          if (type.documentation.isNotBlank()) {
            addKdoc("%L\n", type.documentation.sanitizeKdoc())
          }
        }
        .superclass(if (javaInterOp) {
          superclass.parameterizedBy(className, builderClassName)
        } else {
          superclass.parameterizedBy(className, NOTHING)
        })
        .addSuperclassConstructorParameter(adapterName)
        .addSuperclassConstructorParameter(unknownFields)
        .addFunction(generateNewBuilderMethod(type, builderClassName))
        .addFunction(generateEqualsMethod(type, nameAllocator))
        .addFunction(generateHashCodeMethod(type, nameAllocator))
        .addFunction(generateToStringMethod(type, nameAllocator))
        .addFunction(generateCopyMethod(type, nameAllocator))
        .apply {
          if (javaInterOp) {
            addType(generateBuilderClass(type, className, builderClassName))
          }
        }

    if (emitAndroid) {
      addAndroidCreator(type, companionBuilder)
    }

    addMessageConstructor(type, classBuilder)

    if (type.oneOfs.isNotEmpty()) {
      classBuilder.addInitializerBlock(generateInitializerOneOfBlock(type))
    }

    classBuilder.addType(companionBuilder.build())

    type.nestedTypes.forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  private fun generateInitializerOneOfBlock(type: MessageType): CodeBlock {
    return buildCodeBlock {
      val nameAllocator = nameAllocator(type)
      type.oneOfs
          .filter { oneOf -> oneOf.fields.size >= 2 }
          .forEach { oneOf ->
            val countNonNull = MemberName("com.squareup.wire.internal", "countNonNull")
            val fieldNames = oneOf.fields.joinToString(", ", transform = nameAllocator::get)
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

    type.fieldsAndOneOfFields.forEach { field ->
      val fieldName = nameAllocator[field]
      funBuilder.addStatement("builder.%1L = %1L", fieldName)
    }

    return funBuilder
        .addStatement("builder.addUnknownFields(unknownFields)")
        .addStatement("return builder")
        .build()
  }

  // Example:
  //
  // override fun equals(other: Any?): Boolean {
  //   if (other === this) return true
  //   if (other !is SimpleMessage) return false
  //   return unknownFields == other.unknownFields
  //       && optional_int32 == other.optional_int32
  // }
  private fun generateEqualsMethod(type: MessageType, nameAllocator: NameAllocator): FunSpec {
    val localNameAllocator = nameAllocator.copy()
    val otherName = localNameAllocator.newName("other")
    val kotlinType = type.typeName
    val result = FunSpec.builder("equals")
        .addModifiers(OVERRIDE)
        .addParameter(otherName, ANY.copy(nullable = true))
        .returns(BOOLEAN)

    val body = buildCodeBlock {
      addStatement("if (%N === this) return true", otherName)
      addStatement("if (%N !is %T) return false", otherName, kotlinType)
      add("«return unknownFields == %N.unknownFields", otherName)
      val fields = type.fieldsAndOneOfFields
      for (field in fields) {
        val fieldName = localNameAllocator[field]
        add("\n&& %1L == %2N.%1L", fieldName, otherName)
      }
      add("\n»")
    }
    result.addCode(body)

    return result.build()
  }

  // Example:
  //
  // override fun hashCode(): Int {
  //   var result = super.hashCode
  //   if (result == 0) {
  //     result = unknownFields.hashCode()
  //     result = result * 37 + f?.hashCode()
  //     super.hashCode = result
  //   }
  //   return result
  // }
  //
  // For repeated fields, the final "0" in the example above changes to a "1"
  // in order to be the same as the system hash code for an empty list.
  //
  private fun generateHashCodeMethod(type: MessageType, nameAllocator: NameAllocator): FunSpec {
    val localNameAllocator = nameAllocator.copy()
    val resultName = localNameAllocator.newName("result")
    val result = FunSpec.builder("hashCode")
        .addModifiers(OVERRIDE)
        .returns(INT)

    val fields = type.fieldsAndOneOfFields
    if (fields.isEmpty()) {
      result.addStatement("return unknownFields.hashCode()")
      return result.build()
    }

    val body = buildCodeBlock {
      addStatement("var %N = super.hashCode", resultName)
      beginControlFlow("if (%N == 0)", resultName)

      val hashCode = MemberName("kotlin", "hashCode")
      addStatement("%N = unknownFields.hashCode()", resultName)
      for (field in fields) {
        val fieldName = localNameAllocator[field]
        add("%1N = %1N * 37 + ", resultName)
        if (field.isRepeated || field.isRequired || field.isMap) {
          addStatement("%L.hashCode()", fieldName)
        } else {
          addStatement("%L.%M()", fieldName, hashCode)
        }
      }

      addStatement("super.hashCode = %N", resultName)
      endControlFlow()
      addStatement("return %N", resultName)
    }
    result.addCode(body)

    return result.build()
  }

  // Example:
  //
  // fun copy(
  //   name: String = this.name,
  //   id: Int = this.id,
  //   email: String? = this.email,
  //   phone: List<PhoneNumber> = this.phone,
  //   unknownFields: ByteString = this.unknownFields
  // ): Person {
  //   return Person(name, id, email, phone, unknownFields)
  // }
  private fun generateCopyMethod(type: MessageType, nameAllocator: NameAllocator): FunSpec {
    val className = generatedTypeName(type)
    val result = FunSpec.builder("copy")
        .returns(type.typeName)
    val fieldNames = mutableListOf<String>()
    for (field in type.fieldsAndOneOfFields) {
      val fieldName = nameAllocator[field]
      result.addParameter(ParameterSpec.builder(fieldName, field.typeName)
          .defaultValue("this.%N", fieldName)
          .build())
      fieldNames += fieldName
    }
    result.addParameter(ParameterSpec.builder("unknownFields", ByteString::class)
        .defaultValue("this.unknownFields")
        .build())
    fieldNames += "unknownFields"
    result.addStatement("return %L", fieldNames
        .joinToString(prefix = className.simpleName + "(", postfix = ")"))
    return result.build()
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
      type.fieldsAndOneOfFields.forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && field.isRequired) {
          CodeBlock.of(" ?: throw %1M(%2L, %2S)", missingRequiredFields, field.name)
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }
      add("unknownFields = buildUnknownFields()")
      add("⇤\n)\n") // close the block
    }

    type.fieldsAndOneOfFields.forEach { field ->
      val fieldName = nameAllocator[field]

      val propertyBuilder = PropertySpec.builder(fieldName, field.declarationClass)
          .mutable(true)
          .initializer(field.defaultValue)

      if (javaInterOp) {
        propertyBuilder.addAnnotation(JvmField::class)
      }

      builder.addProperty(propertyBuilder.build())
    }

    type.fields().forEach { field ->
      builder.addFunction(builderSetter(field, nameAllocator, builderClass, oneOf = null))
    }

    type.oneOfs.forEach { oneOf ->
      oneOf.fields.forEach { field ->
        builder.addFunction(builderSetter(field, nameAllocator, builderClass, oneOf))
      }
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
    builderType: TypeName,
    oneOf: OneOf?
  ): FunSpec {
    val fieldName = nameAllocator[field]
    val funBuilder = FunSpec.builder(fieldName)
        .addParameter(fieldName, field.getClass())
        .returns(builderType)
    if (field.documentation.isNotBlank()) {
      funBuilder.addKdoc("%L\n", field.documentation.sanitizeKdoc())
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
        .apply {
          if (oneOf != null) {
            for (other in oneOf.fields) {
              if (field != other) {
                addStatement("this.%L = null", nameAllocator[other])
              }
            }
          }
        }
        .addStatement("return this")
        .build()
  }

  /**
   * Example
   * ```
   * class Person(
   *   val name: String,
   *   val email: String? = null,
   *   val phone: List<PhoneNumber> = emptyList(),
   *   unknownFields: ByteString = ByteString.EMPTY
   * )
   * ```
   */
  private fun addMessageConstructor(message: MessageType, classBuilder: TypeSpec.Builder) {
    val constructorBuilder = FunSpec.constructorBuilder()
    val nameAllocator = nameAllocator(message)
    val byteClass = ProtoType.BYTES.typeName

    val fields = message.fieldsAndOneOfFields

    fields.forEach { field ->
      val fieldClass = field.typeName
      val fieldName = nameAllocator[field]

      val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
      if (!field.isRequired) {
        parameterSpec.defaultValue(field.defaultValue)
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
            if (field.documentation.isNotBlank()) {
              addKdoc("%L\n", field.documentation.sanitizeKdoc())
            }
            if (field.isExtension) {
              addKdoc("Extension source: %L\n", field.location.withPathOnly())
            }
          }
          .build())
    }

    val unknownFields = nameAllocator["unknownFields"]
    constructorBuilder.addParameter(
        ParameterSpec.builder(unknownFields, byteClass)
            .defaultValue("%T.EMPTY", byteClass)
            .build())

    classBuilder.primaryConstructor(constructorBuilder.build())
  }

  private fun wireFieldAnnotation(field: Field): AnnotationSpec {
    return AnnotationSpec.builder(WireField::class)
        .useSiteTarget(FIELD)
        .addMember("tag = %L", field.tag)
        .apply {
          if (field.type!!.isMap) {
            addMember("keyAdapter = %S", field.type!!.keyType!!.adapterString())
            addMember("adapter = %S", field.type!!.valueType!!.adapterString())
          } else {
            addMember("adapter = %S", field.type!!.adapterString())
          }
        }
        .apply {
          if (!field.isOptional) {
            if (field.isPacked) {
              addMember("label = %T.PACKED", WireField.Label::class)
            } else if (field.label != null) {
              addMember("label = %T.%L", WireField.Label::class, field.label!!)
            }
          }
        }
        .apply { if (field.isRedacted) addMember("redacted = true") }
        .build()
  }

  private fun ProtoType.adapterString() = when {
    isScalar -> ProtoAdapter::class.java.name + '#' + toString().toUpperCase(Locale.US)
    else -> typeName.reflectionName() + "#ADAPTER"
  }

  private fun generateToStringMethod(type: MessageType, nameAllocator: NameAllocator): FunSpec {
    val localNameAllocator = nameAllocator.copy()
    val className = generatedTypeName(type)
    val fields = type.fieldsAndOneOfFields
    val body = buildCodeBlock {
      if (fields.isEmpty()) {
        addStatement("return %S", className.simpleName + "{}")
      } else {
        val resultName = localNameAllocator.newName("result")
        addStatement("val %N = mutableListOf<%T>()", resultName, STRING)
        for (field in fields) {
          val fieldName = localNameAllocator[field]
          if (field.isRepeated || field.isMap) {
            add("if (%N.isNotEmpty()) ", fieldName)
          } else if (!field.isRequired) {
            add("if (%N != null) ", fieldName)
          }
          addStatement("%N += %P", resultName, buildString {
            append(fieldName)
            if (field.isRedacted) {
              append("=██")
            } else {
              append("=\$")
              append(fieldName)
            }
          })
        }
        addStatement(
            "return %N.joinToString(prefix = %S, separator = %S, postfix = %S)",
            resultName,
            className.simpleName + "{",
            ", ",
            "}"
        )
      }
    }
    return FunSpec.builder("toString")
        .addModifiers(OVERRIDE)
        .returns(String::class)
        .addCode(body)
        .build()
  }

  private fun addDefaultFields(
    type: MessageType,
    companionBuilder: TypeSpec.Builder,
    nameAllocator: NameAllocator
  ) {
    for (field in type.fieldsAndOneOfFields) {
      val default = field.default ?: continue

      val fieldName = "DEFAULT_" + nameAllocator[field].toUpperCase(Locale.US)
      val fieldType = field.getClass().copy(nullable = false)
      val fieldValue = defaultFieldInitializer(field.type!!, default)
      companionBuilder.addProperty(
          PropertySpec.builder(fieldName, fieldType)
              .apply {
                if (field.type!!.isScalar && field.type != ProtoType.BYTES) {
                  addModifiers(KModifier.CONST)
                } else {
                  jvmField()
                }
              }
              .initializer(fieldValue)
              .build())
    }
  }

  private fun defaultFieldInitializer(protoType: ProtoType, defaultValue: Any): CodeBlock {
    val typeName = protoType.typeName
    return when {
      defaultValue is List<*> -> defaultValue.toListFieldInitializer(protoType)
      defaultValue is Map<*, *> -> defaultValue.toMapFieldInitializer(protoType)
      typeName == BOOLEAN -> CodeBlock.of("%L", defaultValue)
      typeName == INT -> defaultValue.toIntFieldInitializer()
      typeName == LONG -> defaultValue.toLongFieldInitializer()
      typeName == FLOAT -> CodeBlock.of("%Lf", defaultValue.toString())
      typeName == DOUBLE -> CodeBlock.of("%L", defaultValue.toString())
      typeName == STRING -> CodeBlock.of("%S", defaultValue)
      typeName == ByteString::class.asTypeName() -> CodeBlock.of("%S.%M()!!",
          defaultValue.toString().encode(charset = Charsets.ISO_8859_1).base64(),
          ByteString.Companion::class.asClassName().member("decodeBase64"))
      protoType.isEnum -> CodeBlock.of("%T.%L", typeName, defaultValue)
      else -> throw IllegalStateException("$protoType is not an allowed scalar type")
    }
  }

  private fun List<*>.toListFieldInitializer(protoType: ProtoType): CodeBlock = buildCodeBlock {
    add("listOf(")
    var first = true
    forEach {
      if (!first) add(",")
      first = false
      add("\n⇥%L⇤", defaultFieldInitializer(protoType, it!!))
    }
    add("\n)")
  }

  private fun Map<*, *>.toMapFieldInitializer(protoType: ProtoType): CodeBlock = buildCodeBlock {
    add("%T(", protoType.typeName)
    var first = true
    entries.forEach { entry ->
      val field = schema.getField(entry.key as ProtoMember)!!
      val valueInitializer = defaultFieldInitializer(field.type!!, entry.value!!)
      val nameAllocator = nameAllocator(schema.getType(protoType)!!)
      if (!first) add(",")
      first = false
      add("\n⇥%L = %L⇤", nameAllocator[field], valueInitializer)
    }
    add("\n)")
  }

  private fun Any.toIntFieldInitializer(): CodeBlock = when (val int = valueToInt()) {
    Int.MIN_VALUE -> CodeBlock.of("%T.MIN_VALUE", INT)
    Int.MAX_VALUE -> CodeBlock.of("%T.MAX_VALUE", INT)
    else -> CodeBlock.of("%L", int)
  }

  private fun Any.valueToInt(): Int {
    val string = toString()
    return when {
      string.startsWith("0x") || string.startsWith("0X") ->
        string.substring("0x".length).toInt(radix = 16) // Hexadecimal.
      string.startsWith("0") && string != "0" ->
        throw IllegalStateException("Octal literal unsupported $this")
      else -> BigInteger(string).toInt()
    }
  }

  private fun Any.toLongFieldInitializer(): CodeBlock = when (val long = valueToLong()) {
    Long.MIN_VALUE -> CodeBlock.of("%T.MIN_VALUE", LONG)
    Long.MAX_VALUE -> CodeBlock.of("%T.MAX_VALUE", LONG)
    else -> CodeBlock.of("%LL", long)
  }

  private fun Any.valueToLong(): Long {
    val string = toString()
    return when {
      string.startsWith("0x") || string.startsWith("0X") ->
        string.substring("0x".length).toLong(radix = 16) // Hexadecimal.
      string.startsWith("0") && string != "0" ->
        throw IllegalStateException("Octal literal unsupported $this")
      else -> BigInteger(string).toLong()
    }
  }

  /**
   * Example
   * ```
   * companion object {
   *  @JvmField
   *  val ADAPTER : ProtoAdapter<Person> =
   *      object : ProtoAdapter<Person>(FieldEncoding.LENGTH_DELIMITED, Person::class, "square.github.io/wire/unknown") {
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
        .addSuperclassConstructorParameter("\n%T::class", parentClassName)
        .addSuperclassConstructorParameter("\n%S\n⇤", type.type.typeUrl!!)
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

    return PropertySpec.builder("${name}Adapter", adapterType, PRIVATE)
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
      message.fieldsAndOneOfFields.forEach { field ->
        val fieldName = nameAllocator[field]
        add("%L.encodedSizeWithTag(%L, value.%L) +\n", adapterFor(field), field.tag, fieldName)
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

  private fun adapterFor(field: Field) = buildCodeBlock {
    add("%L", field.getAdapterName())
    if (field.isPacked) {
      add(".asPacked()")
    } else if (field.isRepeated) {
      add(".asRepeated()")
    }
  }

  private fun encodeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val body = buildCodeBlock {
      val nameAllocator = nameAllocator(message)

      message.fieldsAndOneOfFields.forEach { field ->
        val fieldName = nameAllocator[field]
        addStatement("%L.encodeWithTag(writer, %L, value.%L)",
            adapterFor(field),
            field.tag,
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
    val className = nameToKotlinName.getValue(message.type)
    val nameAllocator = nameAllocator(message).copy()

    val declarationBody = buildCodeBlock {
      message.fieldsAndOneOfFields.forEach { field ->
        val fieldName = nameAllocator[field]
        val fieldDeclaration: CodeBlock = field.getDeclaration(fieldName)
        addStatement("%L", fieldDeclaration)
      }
    }

    val returnBody = buildCodeBlock {
      addStatement("return %T(⇥", className)

      val missingRequiredFields = MemberName("com.squareup.wire.internal", "missingRequiredFields")
      message.fieldsAndOneOfFields.forEach { field ->
        val fieldName = nameAllocator[field]

        val throwExceptionBlock = if (!field.isRepeated && !field.isMap && field.isRequired) {
          CodeBlock.of(" ?: throw %1M(%2L, %3S)", missingRequiredFields, fieldName, field.name)
        } else {
          CodeBlock.of("")
        }

        addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
      }

      add("unknownFields = unknownFields")
      add("⇤\n)\n") // close the block
    }

    val decodeBlock = buildCodeBlock {
      val fields = message.fieldsAndOneOfFields
      if (fields.isEmpty()) {
        addStatement("val unknownFields = reader.forEachTag(reader::readUnknownField)")
      } else {
        val tag = nameAllocator.newName("tag")
        addStatement("val unknownFields = reader.forEachTag { %L ->", tag)
        addStatement("⇥when (%L) {⇥", tag)

        message.fieldsAndOneOfFields.forEach { field ->
          val fieldName = nameAllocator[field]
          val adapterName = field.getAdapterName()

          if (field.type!!.isEnum) {
            beginControlFlow("%L -> try", field.tag)
            addStatement("%L", decodeAndAssign(field, fieldName, adapterName))
            nextControlFlow("catch (e: %T)", ProtoAdapter.EnumConstantNotFoundException::class)
            addStatement("reader.addUnknownField(%L, %T.VARINT, e.value.toLong())", tag,
                FieldEncoding::class)
            endControlFlow()
          } else {
            addStatement("%L -> %L", field.tag, decodeAndAssign(field, fieldName, adapterName))
          }
        }
        addStatement("else -> reader.readUnknownField(%L)", tag)
        add("⇤}\n⇤}\n") // close the block
      }
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

  private fun decodeAndAssign(field: Field, fieldName: String, adapterName: CodeBlock): CodeBlock {
    val decode = CodeBlock.of("%L.decode(reader)", adapterName)
    return CodeBlock.of(when {
      field.isRepeated -> "%L.add(%L)"
      field.isMap -> "%L.putAll(%L)"
      else -> "%L = %L"
    }, fieldName, decode)
  }

  private fun redactFun(message: MessageType): FunSpec {
    val className = nameToKotlinName.getValue(message.type)
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
          ClassName("kotlin", "UnsupportedOperationException"),
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
    for (field in message.fieldsAndOneOfFields) {
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
    } else if (!type!!.isScalar && !type!!.isEnum) {
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
    return if (type!!.isMap) {
      CodeBlock.of("%NAdapter", name)
    } else {
      type!!.getAdapterName(nameDelimiter)
    }
  }

  private fun ProtoType.getAdapterName(adapterFieldDelimiterName: Char = '.'): CodeBlock {
    return when {
      isScalar -> CodeBlock.of(
          "%T$adapterFieldDelimiterName%L",
          ProtoAdapter::class, simpleName.toUpperCase(Locale.US)
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
    val type = message.type
    val nameAllocator = nameAllocator(message)

    val valueName = nameAllocator["value"]

    val primaryConstructor = FunSpec.constructorBuilder()
        .addParameter(valueName, Int::class, OVERRIDE)

    val builder = TypeSpec.enumBuilder(type.simpleName)
        .apply {
          if (message.documentation.isNotBlank()) {
            addKdoc("%L\n", message.documentation.sanitizeKdoc())
          }
        }
        .addSuperinterface(WireEnum::class)
        .addProperty(PropertySpec.builder(valueName, Int::class)
            .initializer(valueName)
            .build())
        .addType(generateEnumCompanion(message))

    val allOptionFieldsBuilder = mutableSetOf<ProtoMember>()

    // Each enum constant option requires a constructor parameter and property
    message.constants.forEach { constant ->
      constant.options.map.keys.forEach { protoMember ->
        if (allOptionFieldsBuilder.add(protoMember)) {
          val optionField = schema.getField(protoMember)!!
          primaryConstructor.addParameter(optionField.name, optionField.typeName)
          builder.addProperty(PropertySpec.builder(optionField.name, optionField.typeName)
              .initializer(optionField.name)
              .build())
        }
      }
    }

    message.constants.forEach { constant ->
      builder.addEnumConstant(nameAllocator[constant], TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", constant.tag)
          .apply {
            allOptionFieldsBuilder.toList().forEach { protoMember ->
              val field = schema.getField(protoMember)!!
              val fieldValue = constant.options.map[protoMember]
              if (fieldValue != null) {
                addSuperclassConstructorParameter("%L",
                    defaultFieldInitializer(field.type!!, fieldValue))
              } else {
                addSuperclassConstructorParameter("null")
              }
            }
            if (constant.documentation.isNotBlank()) {
              addKdoc("%L\n", constant.documentation.sanitizeKdoc())
            }
            if (constant.options.get(ENUM_DEPRECATED) == "true") {
              addAnnotation(AnnotationSpec.builder(Deprecated::class)
                  .addMember("message = %S", "${constant.name} is deprecated")
                  .build())
            }
          }
          .build())
    }

    return builder.primaryConstructor(primaryConstructor.build())
        .build()
  }

  private fun generateEnumCompanion(message: EnumType): TypeSpec {
    val nameAllocator = nameAllocator(message)
    val companionObjectBuilder = TypeSpec.companionObjectBuilder()
    val parentClassName = nameToKotlinName.getValue(message.type)
    val valueName = nameAllocator["value"]
    val fromValue = FunSpec.builder("fromValue")
        .jvmStatic()
        .addParameter(valueName, Int::class)
        .returns(parentClassName.copy(nullable = true))
        .apply {
          addCode("return when (value) {\n⇥")
          message.constants.forEach { constant ->
            addCode("%L -> %L\n", constant.tag, nameAllocator[constant])
          }
          addCode("else -> null")
          addCode("\n⇤}\n") // close the block
        }
        .build()

    return companionObjectBuilder.addFunction(fromValue)
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
    val parentClassName = nameToKotlinName.getValue(message.type)
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
            .returns(parentClassName.copy(nullable = true))
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
    val classBuilder = TypeSpec.classBuilder(type.typeName)
        .primaryConstructor(FunSpec.constructorBuilder().addModifiers(PRIVATE).build())

    type.nestedTypes.forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  private fun Field.getDeclaration(allocatedName: String) = when {
    isRepeated -> CodeBlock.of("val $allocatedName = mutableListOf<%T>()", type!!.typeName)
    isMap -> CodeBlock.of("val $allocatedName = mutableMapOf<%T, %T>()",
        keyType.typeName, valueType.typeName)
    else -> CodeBlock.of("var $allocatedName: %T = %L", declarationClass, defaultValue)
  }

  private val Field.declarationClass: TypeName
    get() = when {
      isRepeated || isMap -> getClass()
      else -> getClass().copy(nullable = true)
    }

  private fun ProtoType.asTypeName(): TypeName = when {
    isMap -> Map::class.asTypeName()
        .parameterizedBy(keyType!!.asTypeName(), valueType!!.asTypeName())
    else -> nameToKotlinName.getValue(this)
  }

  private fun Field.getClass(baseClass: TypeName = type!!.asTypeName()) = when {
    isRepeated -> List::class.asClassName().parameterizedBy(baseClass)
    isOptional -> baseClass.copy(nullable = true)
    else -> baseClass.copy(nullable = false)
  }

  private val Field.typeName: TypeName
    get() = when {
      isRepeated -> List::class.asClassName().parameterizedBy(type!!.typeName)
      isMap -> Map::class.asTypeName().parameterizedBy(keyType.typeName, valueType.typeName)
      !isRequired -> type!!.typeName.copy(nullable = true)
      else -> type!!.typeName
    }

  private val Field.defaultValue: CodeBlock
    get() = when {
      isRepeated -> CodeBlock.of("emptyList()")
      isMap -> CodeBlock.of("emptyMap()")
      else -> CodeBlock.of("null")
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
        ProtoType.UINT64 to LONG,
        ProtoType.ANY to ClassName("com.squareup.wire", "AnyMessage"),
        FIELD_OPTIONS to ClassName("com.google.protobuf", "FieldOptions"),
        MESSAGE_OPTIONS to ClassName("com.google.protobuf", "MessageOptions"),
        ENUM_OPTIONS to ClassName("com.google.protobuf", "EnumOptions")
    )
    private val MESSAGE = Message::class.asClassName()
    private val ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage")

    private val ENUM_DEPRECATED = ProtoMember.get(ENUM_VALUE_OPTIONS, "deprecated")

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      emitAndroid: Boolean = false,
      javaInterop: Boolean = false,
      rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,
      rpcRole: RpcRole = RpcRole.CLIENT
    ): KotlinGenerator {
      val map = mutableMapOf<ProtoType, ClassName>()

      fun putAll(kotlinPackage: String, enclosingClassName: ClassName?, types: List<Type>) {
        for (type in types) {
          val className = enclosingClassName?.nestedClass(type.type.simpleName)
              ?: ClassName(kotlinPackage, type.type.simpleName)
          map[type.type] = className
          putAll(kotlinPackage, className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        val kotlinPackage = protoFile.kotlinPackage()
        putAll(kotlinPackage, null, protoFile.types)

        for (service in protoFile.services) {
          val className = ClassName(kotlinPackage, service.type().simpleName)
          map[service.type()] = className
        }
      }

      map.putAll(BUILT_IN_TYPES)

      return KotlinGenerator(schema, map, emitAndroid, javaInterop, rpcCallStyle, rpcRole)
    }

    internal fun String.sanitizeKdoc(): String {
      return this
          // Remove trailing whitespace on each line.
          .replace("[^\\S\n]+\n".toRegex(), "\n")
          .replace("\\s+$".toRegex(), "")
          .replace("\\*/".toRegex(), "&#42;/")
          .replace("/\\*".toRegex(), "/&#42;")
    }
  }
}

private fun ProtoFile.kotlinPackage() = javaPackage() ?: packageName ?: ""
