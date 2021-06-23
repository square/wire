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
import com.squareup.kotlinpoet.KModifier.CONST
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
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
import com.squareup.wire.Syntax
import com.squareup.wire.WireEnum
import com.squareup.wire.WireEnumConstant
import com.squareup.wire.WireField
import com.squareup.wire.WireRpc
import com.squareup.wire.internal.boxedOneOfClassName
import com.squareup.wire.internal.boxedOneOfKeyFieldName
import com.squareup.wire.internal.boxedOneOfKeysFieldName
import com.squareup.wire.internal.camelCase
import com.squareup.wire.java.Profile
import com.squareup.wire.kotlin.grpcserver.KotlinGrpcGenerator
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Field.EncodeMode
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.Options.Companion.ENUM_OPTIONS
import com.squareup.wire.schema.Options.Companion.ENUM_VALUE_OPTIONS
import com.squareup.wire.schema.Options.Companion.FIELD_OPTIONS
import com.squareup.wire.schema.Options.Companion.MESSAGE_OPTIONS
import com.squareup.wire.schema.Options.Companion.METHOD_OPTIONS
import com.squareup.wire.schema.Options.Companion.SERVICE_OPTIONS
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.builtInAdapterString
import com.squareup.wire.schema.internal.eligibleAsAnnotationMember
import com.squareup.wire.schema.internal.javaPackage
import com.squareup.wire.schema.internal.optionValueToInt
import com.squareup.wire.schema.internal.optionValueToLong
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import okio.ByteString
import okio.ByteString.Companion.encode
import java.util.Locale

class KotlinGenerator private constructor(
  val schema: Schema,
  private val typeToKotlinName: Map<ProtoType, TypeName>,
  private val memberToKotlinName: Map<ProtoMember, TypeName>,
  private val profile: Profile,
  private val emitAndroid: Boolean,
  private val javaInterOp: Boolean,
  private val emitDeclaredOptions: Boolean,
  private val emitAppliedOptions: Boolean,
  private val rpcCallStyle: RpcCallStyle,
  private val rpcRole: RpcRole,
  private val boxOneOfsMinSize: Int,
  private val grpcServerCompatible: Boolean,
  private val nameSuffix: String?,
) {
  private val nameAllocatorStore = mutableMapOf<Type, NameAllocator>()

  private val ProtoType.typeName: TypeName
    get() {
      val profileTypeName = profile.kotlinTarget(this)
      if (profileTypeName != null) return profileTypeName
      return typeToKotlinName.getValue(this)
    }
  private val ProtoType.isEnum
    get() = schema.getType(this) is EnumType
  private val ProtoType.isMessage
    get() = schema.getType(this) is MessageType
  private val ProtoType.isStruct
    get() = this == ProtoType.STRUCT_MAP
        || this == ProtoType.STRUCT_LIST
        || this == ProtoType.STRUCT_VALUE
        || this == ProtoType.STRUCT_NULL
  private val ProtoType.isStructNull
    get() = this == ProtoType.STRUCT_NULL
  private val Type.typeName
    get() = type.typeName
  private val Service.serviceName
    get() = type.typeName

  /** Returns the full name of the class generated for [type].  */
  fun generatedTypeName(type: Type) = type.typeName as ClassName

  /** Returns the full name of the class generated for [field].  */
  fun generatedTypeName(field: Field) = memberToKotlinName[field.member] as ClassName

  /**
   * Returns the full name of the class generated for [service]#[rpc]. This returns a name like
   * `RouteGuideClient` or `RouteGuideGetFeatureBlockingServer`.
   */
  fun generatedServiceName(
    service: Service,
    rpc: Rpc? = null,
    isImplementation: Boolean = false
  ): ClassName {
    val typeName = service.serviceName as ClassName
    val simpleName = buildString {
      if (isImplementation) {
        append("Grpc")
      }
      append(typeName.simpleName)
      if (rpc != null) {
        append(rpc.name)
      }
      append(serviceNameSuffix)
    }
    return typeName.peerClass(simpleName)
  }

  private val serviceNameSuffix: String
    get() {
      // nameSuffix, if given, overrides both call-style and rpc-role suffixes.
      if (nameSuffix != null) {
        return nameSuffix
      }

      return buildString {
        when (rpcCallStyle) {
          RpcCallStyle.SUSPENDING -> Unit // Suspending is implicit.
          RpcCallStyle.BLOCKING -> {
            if (rpcRole == RpcRole.SERVER) append("Blocking")
          }
        }

        when (rpcRole) {
          RpcRole.CLIENT -> append("Client")
          RpcRole.SERVER -> append("Server")
          RpcRole.NONE   -> Unit
        }
      }
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
  fun generateServiceTypeSpecs(service: Service, onlyRpc: Rpc? = null): Map<ClassName, TypeSpec> {
    val result = mutableMapOf<ClassName, TypeSpec>()

    val (interfaceName, interfaceSpec) = generateService(service, onlyRpc, isImplementation = false)
    result[interfaceName] = interfaceSpec

    if (rpcRole == RpcRole.CLIENT) {
      val (implementationName, implementationSpec) = generateService(service, onlyRpc,
          isImplementation = true)
      result[implementationName] = implementationSpec
    }

    if (grpcServerCompatible) {
      val (grpcClassName, grpcSpec) = KotlinGrpcGenerator(typeToKotlinName)
        .generateGrpcServer(service)
      result[grpcClassName] = grpcSpec
    }

    return result
  }

  private fun generateService(
    service: Service,
    onlyRpc: Rpc?,
    isImplementation: Boolean
  ): Pair<ClassName, TypeSpec> {
    check(rpcRole == RpcRole.CLIENT || !isImplementation) {
      "only clients may generate implementations"
    }
    val interfaceName = generatedServiceName(service, onlyRpc, isImplementation = false)
    val implementationName = generatedServiceName(service, onlyRpc, isImplementation = true)
    val builder = if (!isImplementation) {
      TypeSpec.interfaceBuilder(interfaceName)
          .addSuperinterface(com.squareup.wire.Service::class.java)
    } else {
      TypeSpec.classBuilder(implementationName)
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameter("client", GrpcClient::class)
              .build())
          .addProperty(PropertySpec.builder("client", GrpcClient::class, PRIVATE)
              .initializer("client")
              .build())
          .addSuperinterface(interfaceName)
    }
    builder
        .apply {
          if (service.documentation.isNotBlank()) {
            addKdoc("%L\n", service.documentation.sanitizeKdoc())
          }
          if (!isImplementation) {
            for (annotation in optionAnnotations(service.options)) {
              addAnnotation(annotation)
            }
          }
        }

    val rpcs = if (onlyRpc == null) service.rpcs else listOf(onlyRpc)
    for (rpc in rpcs) {
      builder.addFunction(generateRpcFunction(
          rpc, service.name, service.type.enclosingTypeOrPackage, isImplementation))
    }

    val key = if (isImplementation) implementationName else interfaceName
    return key to builder.build()
  }

  private fun generateRpcFunction(
    rpc: Rpc,
    serviceName: String,
    servicePackageName: String?,
    isImplementation: Boolean
  ): FunSpec {
    val packageName = if (servicePackageName.isNullOrBlank()) "" else "$servicePackageName."
    val funSpecBuilder = FunSpec.builder(rpc.name)
        .apply {
          if (rpc.documentation.isNotBlank()) {
            addKdoc("%L\n", rpc.documentation.sanitizeKdoc())
          }
          if (!isImplementation) {
            for (annotation in optionAnnotations(rpc.options)) {
              addAnnotation(annotation)
            }
          }
        }

    val requestType = rpc.requestType!!.typeName
    val responseType = rpc.responseType!!.typeName

    if (rpcRole == RpcRole.SERVER) {
      val wireRpcAnnotationSpec = AnnotationSpec.builder(WireRpc::class.asClassName())
          .addMember("path = %S", "/$packageName$serviceName/${rpc.name}")
          .addMember("requestAdapter = %S", rpc.requestType!!.adapterString())
          .addMember("responseAdapter = %S", rpc.responseType!!.adapterString())
          .addMember("sourceFile = %S", rpc.location.path)
          .build()
      funSpecBuilder
          .addAnnotation(wireRpcAnnotationSpec)
          .addModifiers(ABSTRACT)
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
          if (isImplementation) {
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
          if (isImplementation) {
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
      RpcCallStyle.SUSPENDING -> SendChannel::class.asClassName().parameterizedBy(typeName)
      RpcCallStyle.BLOCKING -> MessageSink::class.asClassName().parameterizedBy(typeName)
    }
  }

  private fun readableStreamOf(typeName: TypeName): ParameterizedTypeName {
    return when (rpcCallStyle) {
      RpcCallStyle.SUSPENDING -> ReceiveChannel::class.asClassName().parameterizedBy(typeName)
      RpcCallStyle.BLOCKING -> MessageSource::class.asClassName().parameterizedBy(typeName)
    }
  }

  private fun nameAllocator(message: Type): NameAllocator {
    return nameAllocatorStore.getOrPut(message) {
      NameAllocator().apply {
        when (message) {
          is EnumType -> {
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
            for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
              when (fieldOrOneOf) {
                is Field -> {
                  if (fieldOrOneOf.name == fieldOrOneOf.type!!.simpleName ||
                    schema.getType(fieldOrOneOf.qualifiedName) != null
                  ) {
                    newName(fieldOrOneOf.qualifiedName, fieldOrOneOf)
                  } else {
                    newName(fieldOrOneOf.name, fieldOrOneOf)
                  }
                }
                is OneOf -> {
                  val fieldName = newName(fieldOrOneOf.name, fieldOrOneOf)
                  val keysFieldName = boxedOneOfKeysFieldName(fieldName)
                  check(newName(keysFieldName) == keysFieldName) {
                    "unexpected name collision for keys set of boxed one of, ${fieldOrOneOf.name}"
                  }
                  newName(boxedOneOfClassName(fieldOrOneOf.name), boxedOneOfClassName(fieldOrOneOf.name))
                  fieldOrOneOf.fields.forEach { field ->
                    val keyFieldName = boxedOneOfKeyFieldName(fieldOrOneOf.name, field.name)
                    newName(keyFieldName, keyFieldName)
                  }
                }
                else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
              }
            }
          }
        }
      }
    }
  }

  private fun generateMessage(type: MessageType): TypeSpec {
    val className = type.typeName as ClassName
    val builderClassName = className.nestedClass("Builder")
    val nameAllocator = nameAllocator(type)
    val adapterName = nameAllocator["ADAPTER"]
    val unknownFields = nameAllocator["unknownFields"]
    val rawSuperclass = when {
      emitAndroid -> ANDROID_MESSAGE
      else -> MESSAGE
    }
    val superclass = when {
      javaInterOp -> rawSuperclass.parameterizedBy(className, builderClassName)
      else -> rawSuperclass.parameterizedBy(className, NOTHING)
    }

    val companionBuilder = TypeSpec.companionObjectBuilder()

    addDefaultFields(type, companionBuilder, nameAllocator)
    addAdapter(type, companionBuilder)

    val classBuilder = TypeSpec.classBuilder(className)
        .apply {
          if (type.documentation.isNotBlank()) {
            addKdoc("%L\n", type.documentation.sanitizeKdoc())
          }
          for (annotation in optionAnnotations(type.options)) {
            addAnnotation(annotation)
          }
          if (type.isDeprecated) {
            addAnnotation(AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "${className.simpleName} is deprecated")
              .build())
          }
        }
        .superclass(superclass)
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

    if (type.flatOneOfs().isNotEmpty()) {
      classBuilder.addInitializerBlock(generateInitializerFlatOneOfBlock(type))
    }

    for (oneOf in type.boxOneOfs()) {
      val boxClassName = className.nestedClass(nameAllocator[boxedOneOfClassName(oneOf.name)])
      classBuilder.addType(oneOfBoxType(boxClassName, oneOf))
      addOneOfKeys(companionBuilder, oneOf, boxClassName, nameAllocator)
    }

    companionBuilder.addProperty(PropertySpec.builder("serialVersionUID", LONG, PRIVATE, CONST)
      .initializer("0L")
      .build())

    classBuilder.addType(companionBuilder.build())

    type.nestedTypes.forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  private fun generateInitializerFlatOneOfBlock(type: MessageType): CodeBlock {
    return buildCodeBlock {
      val nameAllocator = nameAllocator(type)
      type.flatOneOfs()
          .filter { oneOf -> oneOf.fields.size >= 2 }
          .forEach { oneOf ->
            val countNonNull = MemberName("com.squareup.wire.internal", "countNonNull")
            // FIXME(egor): Revert back to function reference once KotlinPoet compiled with Kotlin
            // 1.4 is released. See https://youtrack.jetbrains.com/issue/KT-37435.
            val fieldNames = oneOf.fields.joinToString(", ") { field -> nameAllocator[field] }
            beginControlFlow("require(%M(%L)·<=·1)", countNonNull, fieldNames)
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
          .addStatement("throw %T(%S)", ClassName("kotlin", "AssertionError"), "Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")
          .build()
    }

    funBuilder.returns(builderClassName)

    val nameAllocator = nameAllocator(type)

    funBuilder.addStatement("val builder = Builder()")

    for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
      when (fieldOrOneOf) {
        is Field -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          funBuilder.addStatement("builder.%1L = %1L", fieldName)
        }
        is OneOf -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          funBuilder.addStatement("builder.%1L = %1L", fieldName)
        }
        else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
      }
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
  //   if (unknownFields != other.unknownFields) return false
  //   if (optional_int32 != other.optional_int32) return false
  //   return true
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
      addStatement("if (%N === this) return·true", otherName)
      addStatement("if (%N !is %T) return·false", otherName, kotlinType)
      addStatement("if (unknownFields != %N.unknownFields) return·false", otherName)

      for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            addStatement("if (%1L != %2N.%1L) return·false", fieldName, otherName)
          }
          is OneOf -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            addStatement("if (%1L != %2N.%1L) return·false", fieldName, otherName)
          }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }
      addStatement("return true")
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

    if (type.fieldsAndOneOfFields.isEmpty()) {
      result.addStatement("return unknownFields.hashCode()")
      return result.build()
    }

    val body = buildCodeBlock {
      addStatement("var %N = super.hashCode", resultName)
      beginControlFlow("if (%N == 0)", resultName)

      addStatement("%N = unknownFields.hashCode()", resultName)

      for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            add("%1N = %1N * 37 + ", resultName)
            if (fieldOrOneOf.isRepeated || fieldOrOneOf.isRequired || fieldOrOneOf.isMap) {
              addStatement("%L.hashCode()", fieldName)
            } else {
              addStatement("(%L?.hashCode() ?: 0)", fieldName)
            }
          }
          is OneOf -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            add("%1N = %1N * 37 + ", resultName)
            addStatement("(%L?.hashCode() ?: 0)", fieldName)
          }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
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
    for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
      when (fieldOrOneOf) {
        is Field -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          result.addParameter(
            ParameterSpec.builder(fieldName, fieldOrOneOf.typeNameForMessageField)
              .defaultValue("this.%N", fieldName)
              .build()
          )
          fieldNames += fieldName
        }
        is OneOf -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          val fieldClass = type.oneOfClassFor(fieldOrOneOf, nameAllocator)
          result.addParameter(
            ParameterSpec.builder(fieldName, fieldClass)
              .defaultValue("this.%N", fieldName)
              .build()
          )
          fieldNames += fieldName
        }
        else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
      }
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
      for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = nameAllocator[fieldOrOneOf]

            val throwExceptionBlock = if (!fieldOrOneOf.isRepeated && fieldOrOneOf.isRequired) {
              CodeBlock.of(" ?: throw %1M(%2L, %2S)", missingRequiredFields, nameAllocator[fieldOrOneOf])
            } else {
              CodeBlock.of("")
            }

            addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
          }
          is OneOf -> {
            val fieldName = nameAllocator[fieldOrOneOf]
            addStatement("%1L = %1L,", fieldName)
          }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }
      add("unknownFields = buildUnknownFields()")
      add("⇤\n)\n") // close the block
    }

    for (fieldOrOneOf in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
      when (fieldOrOneOf) {
        is Field -> {
          val fieldName = nameAllocator[fieldOrOneOf]

          val propertyBuilder = PropertySpec.builder(fieldName, fieldOrOneOf.typeNameForBuilderField)
            .mutable(true)
            .initializer(fieldOrOneOf.identityValue)

          if (javaInterOp) {
            propertyBuilder.addAnnotation(JvmField::class)
          }

          builder.addProperty(propertyBuilder.build())
        }
        is OneOf -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          val fieldClass = type.oneOfClassFor(fieldOrOneOf, nameAllocator)

          val propertyBuilder = PropertySpec.builder(fieldName, fieldClass)
            .mutable(true)
            .initializer(CodeBlock.of("null"))

          if (javaInterOp) {
            propertyBuilder.addAnnotation(JvmField::class)
          }

          builder.addProperty(propertyBuilder.build())
        }
        else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
      }
    }

    // TODO(Benoit) Order builder setters in the same order as the fields.
    type.fields.forEach { field ->
      builder.addFunction(builderSetter(field, nameAllocator, builderClass, oneOf = null))
    }

    type.flatOneOfs().forEach { oneOf ->
      oneOf.fields.forEach { field ->
        builder.addFunction(builderSetter(field, nameAllocator, builderClass, oneOf))
      }
    }
    for (boxOneOf in type.boxOneOfs()) {
      builder.addFunction(
        boxOneOfBuilderSetter(
          type,
          boxOneOf,
          nameAllocator,
          builderClass
        )
      )
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
        .addParameter(fieldName, field.typeNameForBuilderSetter())
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

  private fun boxOneOfBuilderSetter(
    type: MessageType,
    oneOf: OneOf,
    nameAllocator: NameAllocator,
    builderType: TypeName,
  ): FunSpec {
    val fieldClass = type.oneOfClassFor(oneOf, nameAllocator)

    val fieldName = nameAllocator[oneOf]
    val funBuilder = FunSpec.builder(fieldName)
      .addParameter(fieldName, fieldClass)
      .returns(builderType)
    if (oneOf.documentation.isNotBlank()) {
      funBuilder.addKdoc("%L\n", oneOf.documentation.sanitizeKdoc())
    }

    return funBuilder
      .addStatement("this.%1L = %1L", fieldName)
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

    val parametersAndProperties = parametersAndProperties(message, nameAllocator)
    for ((parameter, property) in parametersAndProperties) {
      constructorBuilder.addParameter(parameter)
      classBuilder.addProperty(property)
    }

    val unknownFields = nameAllocator["unknownFields"]
    constructorBuilder.addParameter(
        ParameterSpec.builder(unknownFields, byteClass)
            .defaultValue("%T.EMPTY", byteClass)
            .build())

    classBuilder.primaryConstructor(constructorBuilder.build())
  }

  private fun parametersAndProperties(
    message: MessageType, nameAllocator: NameAllocator
  ): List<Pair<ParameterSpec, PropertySpec>> {
    val result = mutableListOf<Pair<ParameterSpec, PropertySpec>>()

    for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
      when (fieldOrOneOf) {
        is Field -> result.add(
          constructorParameterAndProperty(
            message,
            fieldOrOneOf,
            nameAllocator
          )
        )
        is OneOf -> result.add(
          constructorParameterAndProperty(
            message,
            fieldOrOneOf,
            nameAllocator
          )
        )
        else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
      }
    }

    return result
  }

  private fun constructorParameterAndProperty(
    message: MessageType,
    field: Field,
    nameAllocator: NameAllocator
  ): Pair<ParameterSpec, PropertySpec> {
    val fieldClass = field.typeNameForMessageField
    val fieldName = nameAllocator[field]

    val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
    if (!field.isRequired) {
      parameterSpec.defaultValue(field.identityValue)
    }

    val initializer = when {
      field.type!!.valueType?.isStruct == true -> {
        CodeBlock.of(
          "%M(%S, %N)",
          MemberName("com.squareup.wire.internal", "immutableCopyOfMapWithStructValues"),
          fieldName,
          fieldName
        )
      }
      field.type!!.isStruct -> {
        CodeBlock.of(
          "%M(%S, %N)",
          MemberName("com.squareup.wire.internal", "immutableCopyOfStruct"),
          fieldName,
          fieldName
        )
      }
      field.isRepeated || field.isMap -> {
        CodeBlock.of(
          "%M(%S, %N)",
          MemberName("com.squareup.wire.internal", "immutableCopyOf"),
          fieldName,
          fieldName
        )
      }
      else -> CodeBlock.of("%N", fieldName)
    }

    val propertySpec = PropertySpec.builder(fieldName, fieldClass)
      .initializer(initializer)
      .apply {
        if (field.isDeprecated) {
          addAnnotation(
            AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "$fieldName is deprecated")
              .build()
          )
        }
        for (annotation in optionAnnotations(field.options)) {
          addAnnotation(annotation)
        }
        addAnnotation(wireFieldAnnotation(message, field))
        if (javaInterOp) {
          addAnnotation(JvmField::class)
        }
        if (field.documentation.isNotBlank()) {
          addKdoc("%L\n", field.documentation.sanitizeKdoc())
        }
        if (field.isExtension) {
          addKdoc("Extension source: %L\n", field.location.withPathOnly())
        }
      }
    return parameterSpec.build() to propertySpec.build()
  }

  private fun constructorParameterAndProperty(
    message: MessageType,
    oneOf: OneOf,
    nameAllocator: NameAllocator
  ): Pair<ParameterSpec, PropertySpec> {
    val fieldClass = message.oneOfClassFor(oneOf, nameAllocator)
    val fieldName = oneOf.name

    val parameterSpec = ParameterSpec.builder(fieldName, fieldClass)
    parameterSpec.defaultValue(CodeBlock.of("null"))

    val propertySpec = PropertySpec.builder(fieldName, fieldClass)
      .initializer(CodeBlock.of("%N", fieldName))
      .apply {
        if (javaInterOp) {
          addAnnotation(JvmField::class)
        }
        if (oneOf.documentation.isNotBlank()) {
          addKdoc("%L\n", oneOf.documentation.sanitizeKdoc())
        }
      }

    return parameterSpec.build() to propertySpec.build()
  }

  private fun wireFieldAnnotation(message: MessageType, field: Field): AnnotationSpec {
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
          val wireFieldLabel: WireField.Label? =
            when (field.encodeMode!!) {
              EncodeMode.REQUIRED -> WireField.Label.REQUIRED
              EncodeMode.OMIT_IDENTITY -> WireField.Label.OMIT_IDENTITY
              EncodeMode.REPEATED -> WireField.Label.REPEATED
              EncodeMode.PACKED -> WireField.Label.PACKED
              EncodeMode.MAP,
              EncodeMode.NULL_IF_ABSENT -> null
            }
          if (wireFieldLabel != null) {
            addMember("label = %T.%L", WireField.Label::class, wireFieldLabel)
          }
        }
        .apply { if (field.isRedacted) addMember("redacted = true") }
        .apply {
          val generatedName = nameAllocator(message)[field]
          if (generatedName != field.name) {
            addMember("declaredName = %S", field.name)
          }
        }
        .apply {
          if (field.jsonName != field.name) {
            addMember("jsonName = %S", field.jsonName!!)
          }
        }
        .apply {
          if (field.isOneOf) {
            val oneofName = message.oneOfs.first { it.fields.contains(field) }.name
            addMember("oneofName = %S", oneofName)
          }
        }
        .build()
  }

  private fun wireEnumConstantAnnotation(enum: EnumType, constant: EnumConstant): AnnotationSpec? {
    return AnnotationSpec.builder(WireEnumConstant::class)
        .apply {
          val generatedName = nameAllocator(enum)[constant]
          if (generatedName == constant.name) return null

          addMember("declaredName = %S", constant.name)
        }
        .build()
  }

  private fun generateToStringMethod(type: MessageType, nameAllocator: NameAllocator): FunSpec {
    val sanitizeMember = MemberName("com.squareup.wire.internal", "sanitize")
    val localNameAllocator = nameAllocator.copy()
    val className = generatedTypeName(type)
        val fieldsAndOneOfs = type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()
    val body = buildCodeBlock {
      if (fieldsAndOneOfs.isEmpty()) {
        addStatement("return %S", className.simpleName + "{}")
      } else {
        val resultName = localNameAllocator.newName("result")
        addStatement("val %N = mutableListOf<%T>()", resultName, STRING)

        for (fieldOrOneOf in fieldsAndOneOfs) {
          when (fieldOrOneOf) {
            is Field -> {
              val fieldName = localNameAllocator[fieldOrOneOf]
              if (fieldOrOneOf.isRepeated || fieldOrOneOf.isMap) {
                add("if (%N.isNotEmpty()) ", fieldName)
              } else if (fieldOrOneOf.acceptsNull) {
                add("if (%N != null) ", fieldName)
              }
              addStatement("%N += %P", resultName, buildCodeBlock {
                add(fieldName)
                if (fieldOrOneOf.isRedacted) {
                  add("=██")
                } else {
                  if (fieldOrOneOf.type == ProtoType.STRING) {
                    add("=\${%M($fieldName)}", sanitizeMember)
                  } else {
                    add("=\$")
                    add(fieldName)
                  }
                }
              })
            }
            is OneOf -> {
              val fieldName = localNameAllocator[fieldOrOneOf]
              add("if (%N != null) ", fieldName)
              addStatement("%N += %P", resultName, buildCodeBlock {
                add(fieldName)
                if (fieldOrOneOf.fields.any { it.isRedacted }) {
                  add("=██")
                } else {
                  add("=\$")
                  add(fieldName)
                }
              }
              )
            }
            else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
          }
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
    for (field in type.fieldsAndFlatOneOfFieldsAndBoxedOneOfs().filterIsInstance<Field>()) {
      val default = field.default ?: continue

      val fieldName = "DEFAULT_" + nameAllocator[field].toUpperCase(Locale.US)
      val fieldType = field.typeNameForMessageField.copy(nullable = false)
      val fieldValue = defaultFieldInitializer(field.type!!, default)
      companionBuilder.addProperty(
          PropertySpec.builder(fieldName, fieldType)
              .apply {
                if (field.type!!.isScalar && field.type != ProtoType.BYTES) {
                  addModifiers(CONST)
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
      typeName == FLOAT -> defaultValue.toFloatFieldInitializer()
      typeName == DOUBLE -> defaultValue.toDoubleFieldInitializer()
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
      add("\n⇥%L·= %L⇤", nameAllocator[field], valueInitializer)
    }
    add("\n)")
  }

  private fun Any.toIntFieldInitializer(): CodeBlock = when (val int = optionValueToInt(this)) {
    Int.MIN_VALUE -> CodeBlock.of("%T.MIN_VALUE", INT)
    Int.MAX_VALUE -> CodeBlock.of("%T.MAX_VALUE", INT)
    else -> CodeBlock.of("%L", int)
  }

  private fun Any.toLongFieldInitializer(): CodeBlock = when (val long = optionValueToLong(this)) {
    Long.MIN_VALUE -> CodeBlock.of("%T.MIN_VALUE", LONG)
    Long.MAX_VALUE -> CodeBlock.of("%T.MAX_VALUE", LONG)
    else -> CodeBlock.of("%LL", long)
  }

  private fun Any.toFloatFieldInitializer(): CodeBlock = when(this) {
    "inf" -> CodeBlock.of("Float.POSITIVE_INFINITY")
    "-inf" -> CodeBlock.of("Float.NEGATIVE_INFINITY")
    "nan" -> CodeBlock.of("Float.NaN")
    "-nan" -> CodeBlock.of("Float.NaN")
    else -> CodeBlock.of("%Lf", this.toString())
  }

  private fun Any.toDoubleFieldInitializer(): CodeBlock = when(this) {
    "inf" -> CodeBlock.of("Double.POSITIVE_INFINITY")
    "-inf" -> CodeBlock.of("Double.NEGATIVE_INFINITY")
    "nan" -> CodeBlock.of("Double.NaN")
    "-nan" -> CodeBlock.of("Double.NaN")
    else -> CodeBlock.of("%L", this.toString().toDouble())
  }

  /**
   * Example
   * ```
   * companion object {
   *  @JvmField
   *  val ADAPTER : ProtoAdapter<Person> = object : ProtoAdapter<Person>(
   *    FieldEncoding.LENGTH_DELIMITED,
   *    Person::class,
   *    "square.github.io/wire/unknown",
   *    Syntax.PROTO_3,
   *    null
   *  ) {
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
        .addSuperclassConstructorParameter("\n%S", type.type.typeUrl!!)
        .addSuperclassConstructorParameter("\n%M",
            MemberName(Syntax::class.asClassName(), type.syntax.name))
        .addSuperclassConstructorParameter("\nnull\n⇤")
        .addFunction(encodedSizeFun(type))
        .addFunction(encodeFun(type))
        .addFunction(decodeFun(type))
        .addFunction(redactFun(type))

    for (field in type.fields) {
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

    // Map adapters have to be lazy in order to avoid a circular reference when its value type
    // is the same as its enclosing type.
    return PropertySpec.builder("${name}Adapter", adapterType, PRIVATE)
        .delegate(
            "%M·{ %T.newMapAdapter(%L, %L) }",
            MemberName("kotlin", "lazy"),
            ProtoAdapter::class,
            keyType.getAdapterName(),
            valueType.getAdapterName()
        )
        .build()
  }

  private fun encodedSizeFun(message: MessageType): FunSpec {
    val className = generatedTypeName(message)
    val localNameAllocator = nameAllocator(message).copy()
    val sizeName = localNameAllocator.newName("size")

    val body = buildCodeBlock {
      addStatement("var %N = value.unknownFields.size", sizeName)
      for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            if (fieldOrOneOf.encodeMode == EncodeMode.OMIT_IDENTITY) {
              add("if (value.%1L != %2L) ", fieldName, fieldOrOneOf.identityValue)
            }
            addStatement("%N += %L.encodedSizeWithTag(%L, value.%L)", sizeName, adapterFor(fieldOrOneOf),
              fieldOrOneOf.tag, fieldName)
          }
          is OneOf -> {
            val fieldName = localNameAllocator[fieldOrOneOf]
            add("if (value.%1L != %2L) ", fieldName, "null")
            addStatement("%N += value.%L.encodedSizeWithTag()", sizeName, fieldName)
        }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }
      addStatement("return %N", sizeName)
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

      for (field in message.fields + message.flatOneOfs().flatMap { it.fields }) {
        val fieldName = nameAllocator[field]
        if (field.encodeMode == EncodeMode.OMIT_IDENTITY) {
          add("if (value.%L != %L) ", fieldName, field.identityValue)
        }
        addStatement(
          "%L.encodeWithTag(writer, %L, value.%L)",
          adapterFor(field),
          field.tag,
          fieldName
        )
      }
      for (boxOneOf in message.boxOneOfs()) {
        val fieldName = nameAllocator[boxOneOf]
        add("if (value.%L != %L) ", fieldName, "null")
        addStatement("value.%L.encodeWithTag(writer)", fieldName)
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
    val className = typeToKotlinName.getValue(message.type)
    val nameAllocator = nameAllocator(message).copy()

    val declarationBody = buildCodeBlock {
      for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = nameAllocator[fieldOrOneOf]
            val fieldDeclaration: CodeBlock = fieldOrOneOf.getDeclaration(fieldName)
            addStatement("%L", fieldDeclaration)
          }
          is OneOf -> {
            val fieldName = nameAllocator[fieldOrOneOf]
            val oneOfClass = (message.typeName as ClassName)
              .nestedClass(boxedOneOfClassName(fieldOrOneOf.name))
              .parameterizedBy(STAR)
            val fieldClass = com.squareup.wire.OneOf::class.asClassName()
              .parameterizedBy(oneOfClass, STAR).copy(nullable = true)
            val fieldDeclaration = CodeBlock.of("var $fieldName: %T = %L", fieldClass, "null")
            addStatement("%L", fieldDeclaration)
          }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }
    }

    val returnBody = buildCodeBlock {
      addStatement("return·%T(⇥", className)

      val missingRequiredFields = MemberName("com.squareup.wire.internal", "missingRequiredFields")
      for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
        when (fieldOrOneOf) {
          is Field -> {
            val fieldName = nameAllocator[fieldOrOneOf]

            val throwExceptionBlock =
              if (!fieldOrOneOf.isRepeated && !fieldOrOneOf.isMap && fieldOrOneOf.isRequired) {
                CodeBlock.of(
                  " ?: throw %1M(%2L, %3S)",
                  missingRequiredFields,
                  fieldName,
                  fieldOrOneOf.name
                )
              } else {
                CodeBlock.of("")
              }

            addStatement("%1L = %1L%2L,", fieldName, throwExceptionBlock)
          }
          is OneOf -> {
            val fieldName = nameAllocator[fieldOrOneOf]
            addStatement("%1L = %1L,", fieldName)
          }
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }

      add("unknownFields = unknownFields")
      add("⇤\n)\n") // close the block
    }

    val decodeBlock = buildCodeBlock {
      val fields = message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs().filterIsInstance<Field>()
      val boxOneOfs = message.boxOneOfs()
      if (fields.isEmpty() && boxOneOfs.isEmpty()) {
        addStatement("val unknownFields = reader.forEachTag(reader::readUnknownField)")
      } else {
        val tag = nameAllocator.newName("tag")
        addStatement("val unknownFields = reader.forEachTag { %L ->", tag)
        addStatement("⇥when (%L) {⇥", tag)

        fields.forEach { field ->
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
        if (boxOneOfs.isEmpty()) {
          addStatement("else -> reader.readUnknownField(%L)", tag)
        } else {
          beginControlFlow("else ->")
          val choiceKey = nameAllocator.newName("choiceKey")
          for (boxOneOf in message.boxOneOfs()) {
            val fieldName = nameAllocator[boxOneOf]
            val choiceKeys = boxedOneOfKeysFieldName(fieldName)
            beginControlFlow("for (%L in %L)", choiceKey, choiceKeys)
            beginControlFlow("if (%L == %L.tag)", tag, choiceKey)
            addStatement("%L = %L.decode(reader)", fieldName, choiceKey)
            addStatement("return@forEachTag %T", Unit::class)
            endControlFlow()
            endControlFlow()
          }
          addStatement("reader.readUnknownField(%L)", tag)
          endControlFlow()
        }
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
      else -> "%L·= %L"
    }, fieldName, decode)
  }

  private fun redactFun(message: MessageType): FunSpec {
    val className = typeToKotlinName.getValue(message.type)
    val nameAllocator = nameAllocator(message)
    val result = FunSpec.builder("redact")
        .addModifiers(OVERRIDE)
        .addParameter("value", className)
        .returns(className)

    val redactedMessageFields = message.fields.filter { it.isRedacted }
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
    for (fieldOrOneOf in message.fieldsAndFlatOneOfFieldsAndBoxedOneOfs()) {
      when (fieldOrOneOf) {
        is Field -> {
          val fieldName = nameAllocator[fieldOrOneOf]
          val redactedField = fieldOrOneOf.redact(fieldName)
          if (redactedField != null) {
            redactedFields += CodeBlock.of("%N = %L", fieldName, redactedField)
          }
        }
        is OneOf -> {
          if (fieldOrOneOf.fields.none { it.isRedacted }) continue
          val fieldName = nameAllocator[fieldOrOneOf]
          redactedFields += CodeBlock.of("%N = %L", fieldName, CodeBlock.of("null"))
        }
        else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
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
        encodeMode!! == EncodeMode.NULL_IF_ABSENT -> CodeBlock.of("null")
        isScalar -> PROTOTYPE_TO_IDENTITY_VALUES[type!!]
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
      CodeBlock.of("%N", "${name}Adapter")
    } else {
      type!!.getAdapterName(nameDelimiter)
    }
  }

  private fun ProtoType.getAdapterName(adapterFieldDelimiterName: Char = '.'): CodeBlock {
    val adapterConstant = profile.getAdapter(this)
    return when {
      adapterConstant != null -> {
        CodeBlock.of("%T%L%L",
          adapterConstant.kotlinClassName, adapterFieldDelimiterName, adapterConstant.memberName)
      }
      isScalar -> {
        CodeBlock.of("%T$adapterFieldDelimiterName%L",
            ProtoAdapter::class, simpleName.toUpperCase(Locale.US))
      }
      this == ProtoType.DURATION -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}DURATION", ProtoAdapter::class)
      }
      this == ProtoType.TIMESTAMP -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}INSTANT", ProtoAdapter::class)
      }
      this == ProtoType.EMPTY -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}EMPTY", ProtoAdapter::class)
      }
      this == ProtoType.STRUCT_MAP -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}STRUCT_MAP", ProtoAdapter::class)
      }
      this == ProtoType.STRUCT_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}STRUCT_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.STRUCT_NULL -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}STRUCT_NULL", ProtoAdapter::class)
      }
      this == ProtoType.STRUCT_LIST -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}STRUCT_LIST", ProtoAdapter::class)
      }
      this == ProtoType.DOUBLE_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}DOUBLE_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.FLOAT_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}FLOAT_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.INT64_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}INT64_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.UINT64_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}UINT64_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.INT32_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}INT32_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.UINT32_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}UINT32_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.BOOL_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}BOOL_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.STRING_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}STRING_VALUE", ProtoAdapter::class)
      }
      this == ProtoType.BYTES_VALUE -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}BYTES_VALUE", ProtoAdapter::class)
      }
      isMap -> {
        throw IllegalArgumentException("Can't create single adapter for map type $this")
      }
      else -> {
        CodeBlock.of("%T${adapterFieldDelimiterName}ADAPTER", typeName)
      }
    }
  }

  private fun ProtoType.adapterString(): String {
    val adapterConstant = profile.getAdapter(this)
    if (adapterConstant != null) {
      return "${adapterConstant.javaClassName.reflectionName()}#${adapterConstant.memberName}"
    }

    val builtInAdapterString = builtInAdapterString(this)
    if (builtInAdapterString != null) return builtInAdapterString

    return (typeName as ClassName).reflectionName() + "#ADAPTER"
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
  private fun generateEnum(enum: EnumType): TypeSpec {
    val type = enum.type
    val nameAllocator = nameAllocator(enum)

    // Note that we cannot use nameAllocator for `value` because if we rename it the generated code
    // will not compile for the constructor parameter `override val value_: Int` won't be overriding
    // anything anymore.
    val valueName = "value"

    val primaryConstructor = FunSpec.constructorBuilder()
        .addParameter(valueName, Int::class, OVERRIDE)

    val builder = TypeSpec.enumBuilder(type.simpleName)
        .apply {
          if (enum.documentation.isNotBlank()) {
            addKdoc("%L\n", enum.documentation.sanitizeKdoc())
          }
          for (annotation in optionAnnotations(enum.options)) {
            addAnnotation(annotation)
          }
          if (enum.isDeprecated) {
            addAnnotation(AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "${type.simpleName} is deprecated")
              .build())
          }
        }
        .addSuperinterface(WireEnum::class)
        .addProperty(PropertySpec.builder(valueName, Int::class)
            .initializer(valueName)
            .build())
        .addType(generateEnumCompanion(enum))

    enum.constants.forEach { constant ->
      builder.addEnumConstant(nameAllocator[constant], TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", constant.tag)
          .apply {
            if (constant.documentation.isNotBlank()) {
              addKdoc("%L\n", constant.documentation.sanitizeKdoc())
            }

            for (annotation in optionAnnotations(constant.options)) {
              addAnnotation(annotation)
            }
            val wireEnumConstantAnnotation = wireEnumConstantAnnotation(enum, constant)
            if (wireEnumConstantAnnotation != null) {
              addAnnotation(wireEnumConstantAnnotation)
            }

            if (constant.isDeprecated) {
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
    val parentClassName = typeToKotlinName.getValue(message.type)
    val valueName = "value"
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
    val parentClassName = typeToKotlinName.getValue(message.type)
    val nameAllocator = nameAllocator(message)

    val adapterName = nameAllocator["ADAPTER"]
    val valueName = "value"

    val adapterType = ProtoAdapter::class.asClassName().parameterizedBy(parentClassName)
    val adapterObject = TypeSpec.anonymousClassBuilder()
        .superclass(EnumAdapter::class.asClassName().parameterizedBy(parentClassName))
        .addSuperclassConstructorParameter("\n⇥%T::class", parentClassName)
        .addSuperclassConstructorParameter("\n%M",
            MemberName(Syntax::class.asClassName(), message.syntax.name))
        .addSuperclassConstructorParameter("\n%L\n⇤", message.identity())
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
    val classBuilder = TypeSpec.classBuilder(type.typeName as ClassName)
        .primaryConstructor(FunSpec.constructorBuilder().addModifiers(PRIVATE).build())

    type.nestedTypes.forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  /**
   * Example
   * ```
   * @Retention(AnnotationRetention.RUNTIME)
   * @Target(AnnotationTarget.FIELD)
   * annotation class MyFieldOption(val value: String)
   * ```
   */
  fun generateOptionType(
    extend: Extend,
    field: Field
  ): TypeSpec? {
    require(field in extend.fields)
    val annotationTarget = extend.annotationTarget ?: return null

    if (!emitDeclaredOptions) return null

    if (!eligibleAsAnnotationMember(schema, field)) return null
    val returnType = field.type!!.typeName

    val kotlinType = generatedTypeName(field)

    val builder = TypeSpec.annotationBuilder(kotlinType)
        .addModifiers(PUBLIC)
        .addAnnotation(AnnotationSpec.builder(Retention::class)
            .addMember("%T.%L", AnnotationRetention::class, AnnotationRetention.RUNTIME)
            .build())
        .addAnnotation(AnnotationSpec.builder(Target::class)
            .addMember("%T.%L", AnnotationTarget::class, annotationTarget)
            .build())
    if (field.documentation.isNotEmpty()) {
      builder.addKdoc("%L\n", field.documentation)
    }
    builder.primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("value", returnType)
        .build())
    builder.addProperty(PropertySpec.builder("value", returnType, PUBLIC)
        .initializer("value")
        .build())
    return builder.build()
  }

  private fun Field.getDeclaration(allocatedName: String) = when {
    isRepeated -> CodeBlock.of("val $allocatedName = mutableListOf<%T>()", type!!.typeName)
    isMap -> CodeBlock.of("val $allocatedName = mutableMapOf<%T, %T>()",
        keyType.typeName, valueType.typeName)
    else -> CodeBlock.of("var $allocatedName: %T = %L", typeNameForBuilderField, identityValue)
  }

  private val Field.typeNameForBuilderField: TypeName
    get() {
      val typeNameForBuilderSetter = typeNameForBuilderSetter()
      return when {
        isRepeated || isMap -> typeNameForBuilderSetter
        else -> {
          val nullable = encodeMode != EncodeMode.OMIT_IDENTITY || acceptsNull
          typeNameForBuilderSetter.copy(nullable = nullable)
        }
      }
    }

  private fun ProtoType.asTypeName(): TypeName = when {
    isMap -> Map::class.asTypeName()
        .parameterizedBy(keyType!!.asTypeName(), valueType!!.asTypeName())
    else -> typeToKotlinName.getValue(this)
  }

  private fun EnumType.identity(): CodeBlock {
    val enumConstant = constant(0) ?: return CodeBlock.of("null")
    return CodeBlock.of("%T.%L", type.typeName, nameAllocator(this)[enumConstant])
  }

  private fun Field.typeNameForBuilderSetter(): TypeName {
    val type = type!!
    val baseClass = type.asTypeName()
    return when (encodeMode!!) {
      EncodeMode.REPEATED,
      EncodeMode.PACKED -> List::class.asClassName().parameterizedBy(baseClass)
      EncodeMode.MAP -> baseClass.copy(nullable = false)
      EncodeMode.NULL_IF_ABSENT -> baseClass.copy(nullable = true)
      else -> {
        when {
          type.isMessage -> baseClass.copy(nullable = true)
          else -> baseClass.copy(nullable = false)
        }
      }
    }
  }

  private val Field.typeNameForMessageField: TypeName
    get() {
      val type = type!!
      return when (encodeMode!!) {
        EncodeMode.MAP ->
          Map::class.asTypeName().parameterizedBy(keyType.typeName, valueType.typeName)
        EncodeMode.REPEATED,
        EncodeMode.PACKED -> List::class.asClassName().parameterizedBy(type.typeName)
        EncodeMode.NULL_IF_ABSENT -> type.typeName.copy(nullable = true)
        EncodeMode.REQUIRED -> type.typeName
        EncodeMode.OMIT_IDENTITY -> {
          when {
            type.isStructNull -> type.typeName.copy(nullable = true)
            isOneOf -> type.typeName.copy(nullable = true)
            type.isMessage -> type.typeName.copy(nullable = true)
            else -> type.typeName
          }
        }
      }
    }

  private val Field.identityValue: CodeBlock
    get() {
      return when (encodeMode!!) {
        EncodeMode.MAP -> CodeBlock.of("emptyMap()")
        EncodeMode.REPEATED,
        EncodeMode.PACKED -> CodeBlock.of("emptyList()")
        EncodeMode.NULL_IF_ABSENT -> CodeBlock.of("null")
        EncodeMode.OMIT_IDENTITY -> {
          val protoType = type!!
          val type: Type? = schema.getType(protoType)
          if (protoType.isStructNull) return CodeBlock.of("null")
          if (isOneOf) return CodeBlock.of("null")
          when {
            protoType.isScalar -> PROTOTYPE_TO_IDENTITY_VALUES[protoType]
                ?: throw IllegalArgumentException("Unexpected scalar proto type: $protoType")
            type is MessageType -> CodeBlock.of("null")
            type is EnumType -> type.identity()
            else -> throw IllegalArgumentException(
                "Unexpected type $protoType for IDENTITY_IF_ABSENT")
          }
        }
        // We run this code even if when we're not using the default value so we return something.
        EncodeMode.REQUIRED -> CodeBlock.of("null")
      }
    }

  private val Field.acceptsNull: Boolean
    get() {
      val type = type!!
      return when (encodeMode!!) {
        EncodeMode.MAP,
        EncodeMode.REPEATED,
        EncodeMode.PACKED,
        EncodeMode.REQUIRED -> false
        EncodeMode.NULL_IF_ABSENT -> true
        EncodeMode.OMIT_IDENTITY -> {
          when {
            type.isStructNull -> true
            isOneOf -> true
            type.isMessage -> true
            else -> false
          }
        }
      }
    }

  private fun optionAnnotations(options: Options): List<AnnotationSpec> {
    val result = mutableListOf<AnnotationSpec>()
    for ((key, value) in options.map) {
      val annotationSpec = optionAnnotation(key, value!!)
      if (annotationSpec != null) {
        result.add(annotationSpec)
      }
    }
    return result
  }

  private fun optionAnnotation(protoMember: ProtoMember, value: Any): AnnotationSpec? {
    if (!emitAppliedOptions) return null

    val field: Field = schema.getField(protoMember) ?: return null
    if (!eligibleAsAnnotationMember(schema, field)) return null

    val protoFile: ProtoFile = schema.protoFile(field.location.path) ?: return null
    val simpleName = camelCase(field.name, upperCamel = true) + "Option"
    val type = ClassName(javaPackage(protoFile), simpleName)
    val fieldValue = defaultFieldInitializer(field.type!!, value)

    return AnnotationSpec.builder(type)
        .addMember(fieldValue)
        .build()
  }

  /**
   * Generates a class for this boxed oneof.
   *
   * Example:
   * ```
   * public class Option<T>(
   *   tag: Int,
   *   adapter: ProtoAdapter<T>,
   *   declaredName: String
   * ) : OneOf.Key<T>(tag, adapter, declaredName) {
   *   public fun create(value: T) = OneOf(this, value)
   *
   *   public fun decode(reader: ProtoReader): OneOf<Option<T>, T> = create(adapter.decode(reader))
   * }
   * ```
   */
  private fun oneOfBoxType(boxClassName: ClassName, oneOf: OneOf): TypeSpec {
    val typeVariable = TypeVariableName("T")
    return TypeSpec.classBuilder(boxClassName)
      .addTypeVariable(typeVariable)
      .apply {
        if (oneOf.documentation.isNotBlank()) {
          addKdoc("%L\n", oneOf.documentation.sanitizeKdoc())
        }
      }
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("tag", Int::class)
          .addParameter("adapter", ProtoAdapter::class.asClassName().parameterizedBy(typeVariable))
          .addParameter("declaredName", String::class)
          .build()
      )
      .superclass(
        com.squareup.wire.OneOf.Key::class.asClassName().parameterizedBy(typeVariable)
      )
      .addSuperclassConstructorParameter("%L", "tag")
      .addSuperclassConstructorParameter("%L", "adapter")
      .addSuperclassConstructorParameter("%L", "declaredName")
      .addFunction(
        FunSpec.builder("create")
          .addParameter("value", typeVariable)
          .addStatement("return %T(this, %L)", com.squareup.wire.OneOf::class, "value")
          .build()
      )
      .addFunction(
        FunSpec.builder("decode")
          .addParameter("reader", ProtoReader::class)
          .returns(
            com.squareup.wire.OneOf::class.asClassName().parameterizedBy(
              boxClassName.parameterizedBy(typeVariable),
              typeVariable
            )
          )
          .addStatement("return create(%L.decode(%L))", "adapter", "reader")
          .build()
      )
      .build()
  }

  /**
   * Example:
   * ```
   * @JvmStatic
   * public val CHOICE_KEYS: Set<Choice<*>> = setOf(CHOICE_BUTTON_ELEMENT,
   *     CHOICE_LOCAL_IMAGE_ELEMENT, CHOICE_REMOTE_IMAGE_ELEMENT)
   * ```
   */
  private fun addOneOfKeys(
    companionBuilder: TypeSpec.Builder,
    oneOf: OneOf,
    boxClassName: ClassName,
    nameAllocator: NameAllocator,
  ) {
    val keyFieldNames = mutableListOf<String>()
    for (field in oneOf.fields) {
      val oneOfKey = oneOfKey(oneOf.name, field, boxClassName, nameAllocator)
      keyFieldNames.add(oneOfKey.name)
      companionBuilder.addProperty(oneOfKey)
    }

    val fieldName = nameAllocator[oneOf]
    val keysFieldName = boxedOneOfKeysFieldName(fieldName)
    val allKeys = PropertySpec
      .builder(
        keysFieldName,
        Set::class.asClassName().parameterizedBy(boxClassName.parameterizedBy(STAR))
      )
      .addAnnotation(JvmStatic::class.java)
      .initializer(
        CodeBlock.of(
          """setOf(${keyFieldNames.map { "%L" }.joinToString(", ")})""",
          *keyFieldNames.toTypedArray()
        )
      )
      .build()
    companionBuilder.addProperty(allKeys)
  }

  /**
   * Example:
   * ```
   * public val CHOICE_BUTTON_ELEMENT: Choice<ButtonElement> = Choice<ButtonElement>(tag = 1,
   *     adapter = ButtonElement.ADAPTER, declaredName = "button_element")
   * ```/Users/bquenaudon/workspace/wire/wire-library/wire-tests/src/commonTest/kotlin/com/squareup/wire/BoxOneOfTest.kt
   */
  private fun oneOfKey(
    oneOfName: String,
    field: Field,
    boxClassName: ClassName,
    nameAllocator: NameAllocator,
  ): PropertySpec {
    val name = nameAllocator[boxedOneOfKeyFieldName(oneOfName, field.name)]
    return PropertySpec.builder(name, boxClassName.parameterizedBy(field.type!!.typeName))
      .apply {
        if (field.isDeprecated) {
          addAnnotation(
            AnnotationSpec.builder(Deprecated::class)
              .addMember("message = %S", "${field.name} is deprecated")
              .build()
          )
        }
        for (annotation in optionAnnotations(field.options)) {
          addAnnotation(annotation)
        }
      }
      .initializer(
        CodeBlock.of(
          "%T<%T>(%L = %L, %L = %L, %L = %S)",
          boxClassName,
          field.type!!.asTypeName(),
          "tag",
          field.tag,
          "adapter",
          field.getAdapterName(),
          "declaredName",
          field.name
        )
      )
      .build()
  }

  private fun MessageType.fieldsAndFlatOneOfFieldsAndBoxedOneOfs(): List<Any> {
    val fieldsAndFlatOneOfFields: List<Field> =
      declaredFields + extensionFields + flatOneOfs().flatMap { it.fields }

    return (fieldsAndFlatOneOfFields + boxOneOfs())
      .sortedBy { fieldOrOneOf ->
        when (fieldOrOneOf) {
          is Field -> fieldOrOneOf.location.line
          // TODO(Benoit) If boxed oneofs without fields become a problem, we can add location to
          //  oneofs and use that.
          is OneOf -> fieldOrOneOf.fields.getOrNull(0)?.location?.line ?: 0
          else -> throw IllegalArgumentException("Unexpected element: $fieldOrOneOf")
        }
      }
  }

  private fun MessageType.flatOneOfs(): List<OneOf> {
    val result = mutableListOf<OneOf>()
    for (oneOf in this.oneOfs) {
      if (oneOf.fields.size < boxOneOfsMinSize) {
        result.add(oneOf)
      }
    }
    return result
  }

  private fun MessageType.boxOneOfs(): List<OneOf> {
    return oneOfs.filter { it.fields.size >= boxOneOfsMinSize }
  }

  private fun MessageType.oneOfClassFor(oneOf: OneOf, nameAllocator: NameAllocator): TypeName {
    val oneOfClass = (this.typeName as ClassName)
      .nestedClass(nameAllocator[boxedOneOfClassName(oneOf.name)])
      .parameterizedBy(STAR)
    return com.squareup.wire.OneOf::class.asClassName()
      .parameterizedBy(oneOfClass, STAR).copy(nullable = true)
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
        ProtoType.DURATION to ClassName("com.squareup.wire", "Duration"),
        ProtoType.TIMESTAMP to ClassName("com.squareup.wire", "Instant"),
        ProtoType.EMPTY to ClassName("kotlin", "Unit"),
        ProtoType.STRUCT_MAP to ClassName("kotlin.collections", "Map")
            .parameterizedBy(ClassName("kotlin", "String"), STAR).copy(nullable = true),
        ProtoType.STRUCT_VALUE to ClassName("kotlin", "Any").copy(nullable = true),
        ProtoType.STRUCT_NULL to ClassName("kotlin", "Nothing").copy(nullable = true),
        ProtoType.STRUCT_LIST to ClassName("kotlin.collections", "List")
            .parameterizedBy(STAR).copy(nullable = true),
        ProtoType.DOUBLE_VALUE to DOUBLE.copy(nullable = true),
        ProtoType.FLOAT_VALUE to FLOAT.copy(nullable = true),
        ProtoType.INT64_VALUE to LONG.copy(nullable = true),
        ProtoType.UINT64_VALUE to LONG.copy(nullable = true),
        ProtoType.INT32_VALUE to INT.copy(nullable = true),
        ProtoType.UINT32_VALUE to INT.copy(nullable = true),
        ProtoType.BOOL_VALUE to BOOLEAN.copy(nullable = true),
        ProtoType.STRING_VALUE to String::class.asClassName().copy(nullable = true),
        ProtoType.BYTES_VALUE to ByteString::class.asClassName().copy(nullable = true),
        FIELD_OPTIONS to ClassName("com.google.protobuf", "FieldOptions"),
        MESSAGE_OPTIONS to ClassName("com.google.protobuf", "MessageOptions"),
        ENUM_OPTIONS to ClassName("com.google.protobuf", "EnumOptions")
    )
    private val PROTOTYPE_TO_IDENTITY_VALUES = mapOf(
        ProtoType.BOOL to CodeBlock.of("false"),
        ProtoType.STRING to CodeBlock.of("\"\""),
        ProtoType.BYTES to CodeBlock.of("%T.%L", ByteString::class, "EMPTY"),
        ProtoType.DOUBLE to CodeBlock.of("0.0"),
        ProtoType.FLOAT to CodeBlock.of("0f"),
        ProtoType.FIXED64 to CodeBlock.of("0L"),
        ProtoType.INT64 to CodeBlock.of("0L"),
        ProtoType.SFIXED64 to CodeBlock.of("0L"),
        ProtoType.SINT64 to CodeBlock.of("0L"),
        ProtoType.UINT64 to CodeBlock.of("0L"),
        ProtoType.FIXED32 to CodeBlock.of("0"),
        ProtoType.INT32 to CodeBlock.of("0"),
        ProtoType.SFIXED32 to CodeBlock.of("0"),
        ProtoType.SINT32 to CodeBlock.of("0"),
        ProtoType.UINT32 to CodeBlock.of("0")
    )
    private val MESSAGE = Message::class.asClassName()
    private val ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage")

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      profile: Profile = Profile(),
      emitAndroid: Boolean = false,
      javaInterop: Boolean = false,
      emitDeclaredOptions: Boolean = true,
      emitAppliedOptions: Boolean = false,
      rpcCallStyle: RpcCallStyle = RpcCallStyle.SUSPENDING,
      rpcRole: RpcRole = RpcRole.CLIENT,
      boxOneOfsMinSize: Int = 5_000,
      grpcServerCompatible: Boolean = false,
      nameSuffix: String? = null,
    ): KotlinGenerator {
      val typeToKotlinName = mutableMapOf<ProtoType, TypeName>()
      val memberToKotlinName = mutableMapOf<ProtoMember, TypeName>()

      fun putAll(kotlinPackage: String, enclosingClassName: ClassName?, types: List<Type>) {
        for (type in types) {
          val className = enclosingClassName?.nestedClass(type.type.simpleName)
              ?: ClassName(kotlinPackage, type.type.simpleName)
          typeToKotlinName[type.type] = className
          putAll(kotlinPackage, className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        val kotlinPackage = javaPackage(protoFile)
        putAll(kotlinPackage, null, protoFile.types)

        for (service in protoFile.services) {
          val className = ClassName(kotlinPackage, service.type.simpleName)
          typeToKotlinName[service.type] = className
        }

        for (extend in protoFile.extendList) {
          if (extend.annotationTarget == null) continue
          for (field in extend.fields) {
            if (!eligibleAsAnnotationMember(schema, field)) continue
            val protoMember = field.member
            val simpleName = camelCase(protoMember.simpleName, upperCamel = true) + "Option"
            val className = ClassName(kotlinPackage, simpleName)
            memberToKotlinName[protoMember] = className
          }
        }
      }

      typeToKotlinName.putAll(BUILT_IN_TYPES)

      return KotlinGenerator(
          schema = schema,
          profile = profile,
          typeToKotlinName = typeToKotlinName,
          memberToKotlinName = memberToKotlinName,
          emitAndroid = emitAndroid,
          javaInterOp = javaInterop,
          emitDeclaredOptions = emitDeclaredOptions,
          emitAppliedOptions = emitAppliedOptions,
          rpcCallStyle = rpcCallStyle,
          rpcRole = rpcRole,
          boxOneOfsMinSize = boxOneOfsMinSize,
          grpcServerCompatible = grpcServerCompatible,
          nameSuffix = nameSuffix,
      )
    }

    private val Extend.annotationTarget: AnnotationTarget?
      get() = when (type) {
        MESSAGE_OPTIONS, ENUM_OPTIONS, SERVICE_OPTIONS -> AnnotationTarget.CLASS
        FIELD_OPTIONS, ENUM_VALUE_OPTIONS -> AnnotationTarget.PROPERTY
        METHOD_OPTIONS -> AnnotationTarget.FUNCTION
        else -> null
      }

    internal fun String.sanitizeKdoc(): String {
      return this
          // Remove trailing whitespace on each line.
          .replace("[^\\S\n]+\n".toRegex(), "\n")
          .replace("\\s+$".toRegex(), "")
          .replace("\\*/".toRegex(), "&#42;/")
          .replace("/\\*".toRegex(), "/&#42;")
          .replace("""[""", """\[""")
          .replace("""]""", """\]""")
    }
  }
}

