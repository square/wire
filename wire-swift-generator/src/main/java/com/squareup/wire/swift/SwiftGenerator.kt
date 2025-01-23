/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.swift

import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.internal.camelCase
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Field.EncodeMode
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.DagChecker
import com.squareup.wire.schema.internal.optionValueToInt
import com.squareup.wire.schema.internal.optionValueToLong
import io.outfoxx.swiftpoet.ARRAY
import io.outfoxx.swiftpoet.AttributeSpec
import io.outfoxx.swiftpoet.BOOL
import io.outfoxx.swiftpoet.CASE_ITERABLE
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DATA
import io.outfoxx.swiftpoet.DICTIONARY
import io.outfoxx.swiftpoet.DOUBLE
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.EnumerationCaseSpec
import io.outfoxx.swiftpoet.ExtensionSpec
import io.outfoxx.swiftpoet.FLOAT
import io.outfoxx.swiftpoet.FileMemberSpec
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSignatureSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.FunctionTypeName
import io.outfoxx.swiftpoet.INT
import io.outfoxx.swiftpoet.INT32
import io.outfoxx.swiftpoet.INT64
import io.outfoxx.swiftpoet.Modifier.FILEPRIVATE
import io.outfoxx.swiftpoet.Modifier.PRIVATE
import io.outfoxx.swiftpoet.Modifier.PUBLIC
import io.outfoxx.swiftpoet.Modifier.STATIC
import io.outfoxx.swiftpoet.OPTIONAL
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.ParameterizedTypeName
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.SelfTypeName
import io.outfoxx.swiftpoet.TypeAliasSpec
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.TypeSpec
import io.outfoxx.swiftpoet.TypeVariableName
import io.outfoxx.swiftpoet.UINT32
import io.outfoxx.swiftpoet.UINT64
import io.outfoxx.swiftpoet.joinToCode
import io.outfoxx.swiftpoet.parameterizedBy
import okio.ByteString.Companion.encode

class SwiftGenerator private constructor(
  val schema: Schema,
  private val nameToTypeName: Map<ProtoType, DeclaredTypeName>,
  private val referenceCycleIndirections: Map<ProtoType, Set<ProtoType>>,
) {
  private val proto2Codable = DeclaredTypeName.typeName("Wire.Proto2Codable")
  private val proto3Codable = DeclaredTypeName.typeName("Wire.Proto3Codable")
  private val proto2Enum = DeclaredTypeName.typeName("Wire.Proto2Enum")
  private val proto3Enum = DeclaredTypeName.typeName("Wire.Proto3Enum")
  private val protoMessage = DeclaredTypeName.typeName("Wire.ProtoMessage")
  private val protoReader = DeclaredTypeName.typeName("Wire.ProtoReader")
  private val protoWriter = DeclaredTypeName.typeName("Wire.ProtoWriter")
  private val protoDefaultedValue = DeclaredTypeName.typeName("Wire.ProtoDefaultedValue")

  private val heap = DeclaredTypeName.typeName("Wire.CopyOnWrite")
  private val indirect = DeclaredTypeName.typeName("Wire.Indirect")
  private val redactable = DeclaredTypeName.typeName("Wire.Redactable")
  private val redactedKey = DeclaredTypeName.typeName("Wire.RedactedKey")
  private val customDefaulted = DeclaredTypeName.typeName("Wire.CustomDefaulted")
  private val protoDefaulted = DeclaredTypeName.typeName("Wire.ProtoDefaulted")
  private val unknownFields = DeclaredTypeName.typeName("Wire.UnknownFields")
  private val extensibleUnknownFields = DeclaredTypeName.typeName("Wire.ExtensibleUnknownFields")
  private val protoExtensible = DeclaredTypeName.typeName("Wire.ProtoExtensible")

  private val stringLiteralCodingKeys = DeclaredTypeName.typeName("Wire.StringLiteralCodingKeys")

  private val equatable = DeclaredTypeName.typeName("Swift.Equatable")
  private val hashable = DeclaredTypeName.typeName("Swift.Hashable")
  private val sendable = DeclaredTypeName.typeName("Swift.Sendable")
  private val void = DeclaredTypeName.typeName("Swift.Void", true)

  private val codable = DeclaredTypeName.typeName("Swift.Codable")
  private val codingKey = DeclaredTypeName.typeName("Swift.CodingKey")
  private val decoder = DeclaredTypeName.typeName("Swift.Decoder")
  private val encoder = DeclaredTypeName.typeName("Swift.Encoder")
  private val writableKeyPath = DeclaredTypeName.typeName("Swift.WritableKeyPath")

  private val deprecated = AttributeSpec.builder("available").addArguments("*", "deprecated").build()

  private val ProtoType.typeName
    get() = nameToTypeName.getValue(this)
  private val ProtoType.isMessage
    get() = schema.getType(this) is MessageType
  private val ProtoType.isEnum
    get() = schema.getType(this) is EnumType
  private val Type.typeName
    get() = type.typeName

  private val MessageType.needsCustomCodable: Boolean
    get() = type.enclosingTypeOrPackage == "google.protobuf" && NEEDS_CUSTOM_CODABLE.contains(type.simpleName)

  private val MessageType.unknownFieldsType: DeclaredTypeName
    get() = if (isExtensible) {
      extensibleUnknownFields
    } else {
      unknownFields
    }

  private val TypeName.isStringEncoded
    get() = when (this.makeNonOptional()) {
      INT64, UINT64 -> true
      DATA -> true
      else -> false
    }

  fun generatedTypeName(type: Type) = type.typeName

  internal val Field.isMessage
    get() = type!!.isMessage
  internal val Field.isEnum
    get() = type!!.isEnum
  internal val Field.isOptional: Boolean
    get() = typeName.optional
  internal val Field.isMap: Boolean
    get() = type!!.isMap
  internal val Field.keyType: ProtoType
    get() = type!!.keyType!!
  internal val Field.valueType: ProtoType
    get() = type!!.valueType!!

  private val Field.isRequiredParameter: Boolean
    get() = !isOptional && !isRepeated && !isMap

  private val Field.isCollection: Boolean
    get() = when (typeName.makeNonOptional()) {
      DATA, STRING -> true
      else -> isMap || isRepeated
    }

  private val Field.codableDefaultValue: String?
    get() = default?.let {
      return it
    } ?: when (typeName.makeNonOptional()) {
      BOOL -> "false"
      DOUBLE, FLOAT -> "0"
      INT32, UINT32, INT64, UINT64 -> "0"
      else -> null
    }

  private val MessageType.supportsEmptyInitialization: Boolean
    get() = if (needsCustomCodable) {
      // These types' fields aren't properly loaded
      false
    } else {
      fields.isEmpty() || fields.all { !it.isRequiredParameter }
    }

  private val EnumType.supportsEmptyInitialization: Boolean
    get() = protoDefaultedName != null

  private val EnumType.protoDefaultedName: String?
    get() = constants.takeIf {
      // Proto2 allows for non-zero default values
      // We can only handle them if the enum has no pruned values
      syntax == PROTO_3
    }?.firstOrNull { it.tag == 0 }?.name

  private val Field.isProtoDefaulted: Boolean
    get() {
      if (isRequiredParameter || isMap || isRepeated) {
        return false
      }
      if (type == ProtoType.ANY) return false
      if (isMessage) {
        val messageType = schema.getType(type!!) as MessageType

        return messageType.supportsEmptyInitialization && messageType.declaredFields.isNotEmpty()
      }
      if (isEnum) {
        val enumType = schema.getType(type!!) as EnumType
        return enumType.supportsEmptyInitialization
      }
      return true
    }

  private val Field.defaultedValue: CodeBlock?
    get() = default?.let {
      defaultFieldInitializer(type!!, it)
    }

  // see https://protobuf.dev/programming-guides/proto3/#default
  private val Field.proto3InitialValue: String
    get() = when {
      isMap -> "[:]"
      isRepeated -> "[]"
      isOptional -> "nil"
      else -> when (typeName.makeNonOptional()) {
        BOOL -> "false"
        DOUBLE, FLOAT -> "0"
        INT32, UINT32, INT64, UINT64 -> "0"
        STRING -> """""""" // evaluates to the empty string
        DATA -> ".init()"
        else -> "nil"
      }
    }

  private val Field.codableName: String?
    get() = jsonName?.takeIf { it != name } ?: camelCase(name).takeIf { it != name }

  private val Field.typeName: TypeName
    get() = when (encodeMode!!) {
      EncodeMode.MAP -> DICTIONARY.parameterizedBy(keyType.typeName, valueType.typeName)
      EncodeMode.REPEATED,
      EncodeMode.PACKED,
      -> ARRAY.parameterizedBy(type!!.typeName)
      EncodeMode.NULL_IF_ABSENT -> OPTIONAL.parameterizedBy(type!!.typeName)
      EncodeMode.REQUIRED -> type!!.typeName
      EncodeMode.OMIT_IDENTITY -> {
        when {
          isOneOf || isMessage -> OPTIONAL.parameterizedBy(type!!.typeName)
          else -> type!!.typeName
        }
      }
    }

  val Type.safeDeclaredTypeName: DeclaredTypeName
    get() = generatedTypeName(this).let { result ->
      if (result.simpleName == "Error") result.peerType("Error_") else result
    }

  fun generateTypeTo(type: Type, builder: FileSpec.Builder) {
    val fileMembers = mutableListOf<FileMemberSpec>()

    generateType(type, fileMembers).forEach {
      builder.addType(it)
    }

    for (extension in fileMembers) {
      builder.addMember(extension)
    }
  }

  private fun generateType(
    type: Type,
    fileMembers: MutableList<FileMemberSpec>,
  ) = when (type) {
    is MessageType -> generateMessage(type, fileMembers)
    is EnumType -> listOf(generateEnum(type, fileMembers))
    is EnclosingType -> listOf(generateEnclosing(type, fileMembers))
    else -> error("Unknown type $type")
  }

  private fun String.sanitizeDoc(): String {
    return this
      // Remove trailing whitespace on each line.
      .replace("[^\\S\n]+\n".toRegex(), "\n")
      .replace("\\s+$".toRegex(), "")
      .replace("\\*/".toRegex(), "&#42;/")
      .replace("/\\*".toRegex(), "/&#42;")
  }

  /**
   * Returns true if [field] inside [type] forms a cycle and needs an indirection to allow it
   * to compile.
   */
  private fun isIndirect(type: MessageType, field: Field): Boolean {
    if (field.isRepeated) {
      return false // Arrays are heap-allocated.
    }
    return field.type in referenceCycleIndirections.getOrDefault(type.type, emptySet())
  }

  /**
   * Checks that every enum in a proto3 message contains a value with tag 0.
   *
   * @throws NoSuchElementException if the case doesn't exist
   */
  @Throws(NoSuchElementException::class)
  private fun validateProto3DefaultsExist(type: MessageType) {
    // TODO: Remove when we support unknown cases
    if (type.syntax == PROTO_2) { return }

    // validate each enum field
    type
      .fields
      .mapNotNull { schema.getType(it.type!!) as? EnumType }
      .forEach { enum ->
        // ensure that a protoDefaultedName case exists
        if (enum.protoDefaultedName == null) {
          throw NoSuchElementException("Missing a zero value for ${enum.name}")
        }
      }
  }

  @OptIn(ExperimentalStdlibApi::class) // TODO move to build flag
  private fun generateMessage(
    type: MessageType,
    fileMembers: MutableList<FileMemberSpec>,
  ): List<TypeSpec> {
    val structType = type.typeName
    val oneOfEnumNames = type.oneOfs.associateWith {
      structType.nestedType(it.name.replaceFirstChar(Char::uppercase))
    }

    // TODO use a NameAllocator
    val propertyNames = type.declaredFields.map { it.name } + type.oneOfs.map { it.name }

    val storageType = structType.nestedType("Storage")
    val storageName = if ("storage" in propertyNames) "_storage" else "storage"

    val typeSpecs = mutableListOf<TypeSpec>()

    validateProto3DefaultsExist(type)

    typeSpecs += TypeSpec.structBuilder(structType)
      .addModifiers(PUBLIC)
      .apply {
        if (type.documentation.isNotBlank()) {
          addDoc("%L\n", type.documentation.sanitizeDoc())
        }

        if (type.isHeapAllocated) {
          addAttribute(
            AttributeSpec.builder("dynamicMemberLookup").build(),
          )

          addProperty(
            PropertySpec.varBuilder(storageName, storageType, PRIVATE)
              .addAttribute(AttributeSpec.builder(heap).build())
              .build(),
          )

          generateMessageStoragePropertyDelegates(type, storageName, storageType, oneOfEnumNames)
          generateMessageStorageDelegateConstructor(type, storageName, storageType, oneOfEnumNames)
          generateMessageExtensions(type, storageType, fileMembers, forStorageType = true)
          generateMessageExtensionStorageDelegates(type, storageName, structType, fileMembers)
        } else {
          generateMessageProperties(type, oneOfEnumNames)
          generateMessageConstructor(type, oneOfEnumNames)
          generateMessageExtensions(type, structType, fileMembers)
        }
      }
      .build()

    // Generate common Foundation types
    val structEquatableExtension = ExtensionSpec.builder(structType)
      .addSuperType(equatable)
      .build()
    fileMembers += FileMemberSpec.builder(structEquatableExtension)
      .addGuard("!$FLAG_REMOVE_EQUATABLE")
      .build()

    val structHashableExtension = ExtensionSpec.builder(structType)
      .addSuperType(hashable)
      .build()
    fileMembers += FileMemberSpec.builder(structHashableExtension)
      .addGuard("!$FLAG_REMOVE_HASHABLE")
      .build()

    val structSendableExtension = ExtensionSpec.builder(structType)
      .addSuperType(sendable)
      .build()
    fileMembers += FileMemberSpec.builder(structSendableExtension)
      .build()

    // Add proto defaulted value

    if (type.supportsEmptyInitialization) {
      val defaultedValueExtension = ExtensionSpec.builder(structType)
        .addSuperType(protoDefaultedValue)
        .addProperty(
          PropertySpec.varBuilder("defaultedValue", SelfTypeName.INSTANCE.makeNonOptional(), PUBLIC, STATIC).getter(
            FunctionSpec.getterBuilder().addCode(".init()\n").build(),
          ).build(),
        ).build()

      fileMembers += FileMemberSpec.builder(defaultedValueExtension).build()
    }

    // Add redaction, which is potentially delegated
    val requiresRedaction = type.fields.any { it.isRedacted }

    if (requiresRedaction) {
      val redactionExtension = ExtensionSpec.builder(structType)
        .addSuperType(redactable)
        .addType(
          TypeSpec.enumBuilder("RedactedKeys")
            .addModifiers(PUBLIC)
            .addSuperType(STRING)
            .addSuperType(redactedKey)
            .apply {
              type.fields.forEach { field ->
                if (field.isRedacted) {
                  addEnumCase(field.name)
                }
              }
            }
            .build(),
        )

      if (type.isHeapAllocated) {
        redactionExtension.addProperty(
          PropertySpec.varBuilder("description", STRING)
            .addModifiers(PUBLIC)
            .getter(
              FunctionSpec.getterBuilder()
                .addStatement("return %N.description", storageName)
                .build(),
            )
            .build(),
        )
      }

      fileMembers += FileMemberSpec.builder(redactionExtension.build())
        .addGuard("!$FLAG_REMOVE_REDACTABLE")
        .build()
    }

    // Add {Proto/Foundation} Codable methods, which may delegate to Storage
    if (type.isHeapAllocated) {
      val structProtoCodableExtension = ExtensionSpec.builder(structType)
        .addSuperType(type.protoCodableType)
        .addFunction(
          FunctionSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter("from", "protoReader", protoReader)
            .throws(true)
            .addStatement("self.%N = try %T(from: protoReader)", storageName, storageType)
            .build(),
        )
        .addFunction(
          FunctionSpec.builder("encode")
            .addModifiers(PUBLIC)
            .addParameter("to", "protoWriter", protoWriter)
            .throws(true)
            .addStatement("try %N.encode(to: protoWriter)", storageName)
            .build(),
        )
        .build()
      fileMembers += FileMemberSpec.builder(structProtoCodableExtension).build()

      val structMessageConformanceExtension = ExtensionSpec.builder(structType)
        .messageConformanceExtension(type)
        .build()
      fileMembers += FileMemberSpec.builder(structMessageConformanceExtension).build()

      val structCodableExtension = heapCodableExtension(structType, storageName, storageType)
      fileMembers += FileMemberSpec.builder(structCodableExtension)
        .addGuard("!$FLAG_REMOVE_CODABLE")
        .build()
    } else {
      val messageConformanceExtension = ExtensionSpec.builder(structType)
        .messageConformanceExtension(type)
        .build()
      fileMembers += FileMemberSpec.builder(messageConformanceExtension).build()

      val protoCodableExtension = ExtensionSpec.builder(structType)
        .messageProtoCodableExtension(type, structType, oneOfEnumNames, propertyNames)
        .build()
      fileMembers += FileMemberSpec.builder(protoCodableExtension).build()

      if (!type.needsCustomCodable) {
        fileMembers += FileMemberSpec.builder(messageCodableExtension(type, structType))
          .addGuard("!$FLAG_REMOVE_CODABLE")
          .build()
      }
    }

    // Generate One-Ofs and Nested Types
    if (type.oneOfs.isNotEmpty() || type.nestedTypes.isNotEmpty()) {
      val nestedFileMembers = mutableListOf<FileMemberSpec>()

      val nestedExtension = ExtensionSpec.builder(structType)
        .addDoc("Subtypes within %T\n", structType)
        .apply {
          generateMessageOneOfs(type, oneOfEnumNames, nestedFileMembers)
        }
        .apply {
          type.nestedTypes.forEach { nestedType ->
            generateType(nestedType, nestedFileMembers).forEach {
              addType(it)
            }
          }
        }
        .build()

      fileMembers += FileMemberSpec.builder(nestedExtension).build()
      fileMembers += nestedFileMembers
    }

    // Generate Storage
    if (type.isHeapAllocated) {
      val storageExtension = ExtensionSpec.builder(structType)
        .addType(
          TypeSpec.structBuilder(storageType)
            .addDoc("Underlying storage for %T\n", structType)
            .addModifiers(PUBLIC)
            .apply {
              generateMessageProperties(type, oneOfEnumNames, forStorageType = true)
              generateMessageConstructor(type, oneOfEnumNames)
            }
            .build(),
        )
        .build()
      fileMembers += FileMemberSpec.builder(storageExtension).build()

      // Generate common Foundation types
      val storageEquatableExtension = ExtensionSpec.builder(storageType)
        .addSuperType(equatable)
        .build()
      fileMembers += FileMemberSpec.builder(storageEquatableExtension)
        .addGuard("!$FLAG_REMOVE_EQUATABLE")
        .build()

      val storageHashableExtension = ExtensionSpec.builder(storageType)
        .addSuperType(hashable)
        .build()
      fileMembers += FileMemberSpec.builder(storageHashableExtension)
        .addGuard("!$FLAG_REMOVE_HASHABLE")
        .build()

      val storageSendableExtension = ExtensionSpec.builder(storageType)
        .addSuperType(sendable)
        .build()
      fileMembers += FileMemberSpec.builder(storageSendableExtension)
        .build()

      // Add redaction
      if (requiresRedaction) {
        val storageRedactableExtension = ExtensionSpec.builder(storageType)
          .addSuperType(redactable)
          .addType(
            TypeAliasSpec.builder("RedactedKeys", structType.nestedType("RedactedKeys"))
              .addModifiers(PUBLIC)
              .build(),
          )
          .build()
        fileMembers += FileMemberSpec.builder(storageRedactableExtension)
          .addGuard("!$FLAG_REMOVE_REDACTABLE")
          .build()
      }

      // Add {Proto/Foundation} Codable methods
      val storageMessageConformanceExtension = ExtensionSpec.builder(storageType)
        .messageConformanceExtension(type)
        .build()
      fileMembers += FileMemberSpec.builder(storageMessageConformanceExtension).build()

      val storageProtoCodableExtension = ExtensionSpec.builder(storageType)
        .messageProtoCodableExtension(type, structType, oneOfEnumNames, propertyNames)
        .build()
      fileMembers += FileMemberSpec.builder(storageProtoCodableExtension).build()

      val storageCodableExtension = messageCodableExtension(type, storageType)
      fileMembers += FileMemberSpec.builder(storageCodableExtension)
        .addGuard("!$FLAG_REMOVE_CODABLE")
        .build()
    }

    return typeSpecs
  }

  private fun ExtensionSpec.Builder.messageProtoCodableExtension(
    type: MessageType,
    structType: DeclaredTypeName,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    propertyNames: Collection<String>,
  ): ExtensionSpec.Builder = apply {
    addSuperType(type.protoCodableType)

    val reader = if ("protoReader" in propertyNames) "_protoReader" else "protoReader"
    val token = if ("token" in propertyNames) "_token" else "token"
    val tag = if ("tag" in propertyNames) "_tag" else "tag"
    addFunction(
      FunctionSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter("from", reader, protoReader)
        .throws(true)
        .apply {
          // Declare locals into which everything is written before promoting to members.
          type.declaredFields.forEach { field ->
            val localType = when (type.syntax) {
              PROTO_2 -> if (field.isRepeated || field.isMap) {
                field.typeName
              } else {
                field.typeName.makeOptional()
              }
              PROTO_3 -> if (field.isOptional || (field.isEnum && !field.isRepeated)) {
                field.typeName.makeOptional()
              } else {
                field.typeName
              }
            }

            val initializer = when (type.syntax) {
              PROTO_2 -> when {
                field.isMap -> "[:]"
                field.isRepeated -> "[]"
                else -> "nil"
              }
              PROTO_3 -> field.proto3InitialValue
            }

            addStatement("var %N: %T = %L", field.safeName, localType, initializer)
          }
          type.oneOfs.forEach { oneOf ->
            val enumName = oneOfEnumNames.getValue(oneOf)
            addStatement("var %N: %T = nil", oneOf.name, enumName.makeOptional())
          }
          if (type.declaredFieldsAndOneOfFields.isNotEmpty()) {
            addStatement("")
          }

          addStatement("let $token = try $reader.beginMessage()")
          beginControlFlow("while", "let $tag = try $reader.nextTag(token: $token)")
          beginControlFlow("switch", tag)
          type.declaredFields.forEach { field ->
            val decoder = CodeBlock.Builder()
            if (field.isMap) {
              decoder.add("try $reader.decode(into: &%N", field.safeName)
              field.keyType.encoding?.let { keyEncoding ->
                decoder.add(", keyEncoding: .%N", keyEncoding)
              }
              field.valueType.encoding?.let { valueEncoding ->
                decoder.add(", valueEncoding: .%N", valueEncoding)
              }
            } else {
              if (field.isRepeated) {
                decoder.add("try $reader.decode(into: &%N", field.safeName)
              } else {
                val typeName = field.typeName.makeNonOptional()

                decoder.add("%N = try $reader.decode(%T.self", field.safeName, typeName)
              }
              field.type!!.encoding?.let { encoding ->
                decoder.add(", encoding: .%N", encoding)
              }
            }
            decoder.add(")")
            addStatement("case %L: %L", field.tag, decoder.build())
          }
          type.oneOfs.forEach { oneOf ->
            oneOf.fields.forEach { field ->
              when {
                // ProtoReader.decode() return optional for enums. Handle that specially.
                field.isEnum -> {
                  addStatement(
                    "case %1L: %2N = (try $reader.decode(%4T.self)).flatMap { .%3N(\$0) }",
                    field.tag,
                    oneOf.name,
                    field.safeName,
                    field.typeName.makeNonOptional(),
                  )
                }
                else -> {
                  addStatement(
                    "case %1L: %2N = .%3N(try $reader.decode(%4T.self))",
                    field.tag,
                    oneOf.name,
                    field.safeName,
                    field.typeName.makeNonOptional(),
                  )
                }
              }
            }
          }
          addStatement("default: try $reader.readUnknownField(tag: $tag)")
          endControlFlow("switch")
          endControlFlow("while")
          addStatement("self.unknownFields = try $reader.endMessage(token: $token)")

          // Check required and bind members.
          addStatement("")
          type.declaredFields.forEach { field ->
            val hasPropertyWrapper = !isIndirect(type, field) && (field.defaultedValue != null || field.isProtoDefaulted)

            val initializer = when (type.syntax) {
              PROTO_2 -> if (field.isOptional || field.isRepeated || field.isMap) {
                CodeBlock.of("%N", field.safeName)
              } else {
                CodeBlock.of("try %1T.checkIfMissing(%2N, %3S)", structType, field.safeName, field.name)
              }
              PROTO_3 -> if (field.isEnum && !field.isRepeated) {
                CodeBlock.of("try %1T.defaultIfMissing(%2N)", field.typeName.makeNonOptional(), field.safeName)
              } else {
                CodeBlock.of("%N", field.safeName)
              }
            }

            val fieldName = if (hasPropertyWrapper) { "_${field.safeName}" } else { field.safeName }
            addStatement(
              if (hasPropertyWrapper) {
                "self.%N.wrappedValue = %L"
              } else {
                "self.%N = %L"
              },
              fieldName,
              initializer,
            )
          }
          type.oneOfs.forEach { oneOf ->
            addStatement("self.%1N = %1N", oneOf.name)
          }
        }
        .build(),
    )

    val writer = if ("protoWriter" in propertyNames) "_protoWriter" else "protoWriter"
    addFunction(
      FunctionSpec.builder("encode")
        .addModifiers(PUBLIC)
        .addParameter("to", writer, protoWriter)
        .throws(true)
        .apply {
          type.declaredFields.forEach { field ->
            if (field.isMap) {
              addCode("try $writer.encode(tag: %L, value: self.%N", field.tag, field.safeName)
              field.keyType.encoding?.let { keyEncoding ->
                addCode(", keyEncoding: .%N", keyEncoding)
              }
              field.valueType.encoding?.let { valueEncoding ->
                addCode(", valueEncoding: .%N", valueEncoding)
              }
              addCode(")\n")
            } else {
              addCode("try $writer.encode(tag: %L, value: self.%N", field.tag, field.safeName)
              field.type!!.encoding?.let { encoding ->
                addCode(", encoding: .%N", encoding)
              }
              if (field.isPacked) {
                addCode(", packed: true")
              }
              addCode(")\n")
            }
          }
          type.oneOfs.forEach { oneOf ->
            beginControlFlow("if", "let %1N = self.%1N", oneOf.name)
            addStatement("try %N.encode(to: $writer)", oneOf.name)
            endControlFlow("if")
          }
        }
        .addStatement("try $writer.writeUnknownFields(unknownFields)")
        .build(),
    )
  }

  private fun ExtensionSpec.Builder.messageConformanceExtension(
    type: MessageType,
  ): ExtensionSpec.Builder = apply {
    addSuperType(protoMessage)
    addFunction(
      FunctionSpec.builder("protoMessageTypeURL")
        .returns(STRING)
        .addModifiers(PUBLIC, STATIC)
        .addStatement("return \"%N\"", type.type.typeUrl!!)
        .build(),
    )
  }

  private val MessageType.protoCodableType: DeclaredTypeName
    get() = when (syntax) {
      PROTO_2 -> proto2Codable
      PROTO_3 -> proto3Codable
    }

  private val EnumType.protoCodableType: DeclaredTypeName
    get() = when (syntax) {
      PROTO_2 -> proto2Enum
      PROTO_3 -> proto3Enum
    }

  private fun heapCodableExtension(
    structType: DeclaredTypeName,
    storageName: String,
    storageType: DeclaredTypeName,
  ): ExtensionSpec {
    return ExtensionSpec.builder(structType)
      .addSuperType(codable)
      .apply {
        addFunction(
          FunctionSpec.constructorBuilder()
            .addParameter("from", "decoder", decoder)
            .addModifiers(PUBLIC)
            .throws(true)
            .addStatement("let container = try decoder.singleValueContainer()")
            .addStatement("self.%N = try container.decode(%T.self)", storageName, storageType)
            .build(),
        )
        addFunction(
          FunctionSpec.builder("encode")
            .addParameter("to", "encoder", encoder)
            .addModifiers(PUBLIC)
            .throws(true)
            .addStatement("var container = encoder.singleValueContainer()")
            .addStatement("try container.encode(%N)", storageName)
            .build(),
        )
      }
      .build()
  }

  private fun messageCodableExtension(
    type: MessageType,
    structType: DeclaredTypeName,
  ): ExtensionSpec {
    return ExtensionSpec.builder(structType)
      .addSuperType(codable)
      .apply {
        val codingKeys = if (type.declaredFieldsAndOneOfFields.isEmpty()) {
          structType.nestedType("CodingKeys")
        } else {
          stringLiteralCodingKeys
        }

        if (type.declaredFieldsAndOneOfFields.isEmpty()) {
          addType(
            // Coding keys still need to be specified on empty messages in order to prevent `unknownFields` from
            // getting serialized via JSON/Codable. In this case, the keys cannot conform to `RawRepresentable`
            // in order to compile.

            TypeSpec.enumBuilder(codingKeys)
              .addModifiers(PUBLIC)
              .addSuperType(codingKey)
              .build(),
          )
        }

        // We cannot rely upon built-in Codable support since we need to support multiple keys.
        if (type.declaredFieldsAndOneOfFields.isNotEmpty()) {
          addFunction(
            FunctionSpec.constructorBuilder()
              .addParameter("from", "decoder", decoder)
              .addModifiers(PUBLIC)
              .throws(true)
              .addStatement("let container = try decoder.container(keyedBy: %T.self)", codingKeys)
              .apply {
                type.declaredFields.forEach { field ->
                  val hasPropertyWrapper = !isIndirect(type, field) && (field.defaultedValue != null || field.isProtoDefaulted)

                  var typeName: TypeName = field.typeName.makeNonOptional()
                  if (field.isRepeated && typeName is ParameterizedTypeName) {
                    typeName = typeName.typeArguments[0]
                  }

                  var decode = "decode"
                  if (field.isRepeated) {
                    decode += "ProtoArray"
                  } else if (field.isMap) {
                    decode += "ProtoMap"
                  } else if (field.isOptional) {
                    decode += "IfPresent"
                  }

                  val typeArg = if (field.typeName.isStringEncoded) {
                    "stringEncoded: "
                  } else {
                    ""
                  }

                  val forKeys = field.codableName?.let {
                    "firstOfKeys"
                  } ?: "forKey"

                  val keys = listOf(field.codableName, field.name)
                    .filterNotNull()
                    .map { CodeBlock.of("%S", it) }
                    .joinToCode()

                  val fieldName = if (hasPropertyWrapper) { "_${field.safeName}" } else { field.safeName }
                  val prefix = if (hasPropertyWrapper) { "self.%1N.wrappedValue" } else { "self.%1N" }
                  addStatement(
                    "$prefix = try container.$decode($typeArg%2T.self, $forKeys: $keys)",
                    fieldName,
                    typeName,
                  )
                }

                type.oneOfs.forEach { oneOf ->
                  oneOf.fields.fold(mutableListOf<Pair<Field, String>>()) { memo, field ->
                    field.codableName?.let { codableName ->
                      memo.add(Pair(field, codableName))
                    }
                    memo.add(Pair(field, field.name))

                    memo
                  }.forEachIndexed { index, pair ->
                    val (field, keyName) = pair

                    val typeName = field.typeName.makeNonOptional()

                    if (index == 0) {
                      beginControlFlow(
                        "if",
                        "let %1N = try container.decodeIfPresent(%2T.self, forKey: %3S)",
                        field.safeName,
                        typeName,
                        keyName,
                      )
                    } else {
                      nextControlFlow(
                        "else if",
                        "let %1N = try container.decodeIfPresent(%2T.self, forKey: %3S)",
                        field.safeName,
                        typeName,
                        keyName,
                      )
                    }
                    addStatement("self.%1N = .%2N(%2N)", oneOf.name, field.safeName)
                  }
                  nextControlFlow("else", "")
                  addStatement("self.%N = nil", oneOf.name)
                  endControlFlow("if")
                }
              }
              .build(),
          )
          addFunction(
            FunctionSpec.builder("encode")
              .addParameter("to", "encoder", encoder)
              .addModifiers(PUBLIC)
              .throws(true)
              .addStatement("var container = encoder.container(keyedBy: %T.self)", codingKeys)
              .apply {
                if (type.declaredFieldsAndOneOfFields.any { it.codableName != null }) {
                  addStatement("let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase")
                }
                if (type.declaredFields.any { !it.isOptional && (it.isCollection || it.isEnum || it.codableDefaultValue != null) }) {
                  addStatement("let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include")
                }
                addStatement("")

                type.declaredFields.forEach { field ->
                  fun addEncode() {
                    var encode = "encode"
                    if (field.isRepeated) {
                      encode += "ProtoArray"
                    } else if (field.isMap) {
                      encode += "ProtoMap"
                    } else if (field.isOptional) {
                      encode += "IfPresent"
                    }

                    val typeArg = if (field.typeName.isStringEncoded) {
                      "stringEncoded: "
                    } else {
                      ""
                    }

                    val (keys, args) = field.codableName?.let { codableName ->
                      Pair(
                        "preferCamelCase ? %3S : %2S",
                        arrayOf(field.name, codableName),
                      )
                    } ?: Pair("%2S", arrayOf(field.name))

                    addStatement(
                      "try container.$encode(${typeArg}self.%1N, forKey: $keys)",
                      field.safeName,
                      *args,
                    )
                  }

                  val defaultValue = field.codableDefaultValue

                  if (field.isOptional) {
                    // A proto3 field that is defined with the optional keyword supports field presence.
                    // Fields that have a value set and that support field presence always include the field value
                    // in the JSON-encoded output, even if it is the default value.
                    addEncode()
                  } else if (field.isCollection) {
                    beginControlFlow("if", "includeDefaults || !self.%N.isEmpty", field.safeName)
                    addEncode()
                    endControlFlow("if")
                  } else if (field.isEnum) {
                    beginControlFlow("if", "includeDefaults || self.%N.rawValue != 0", field.safeName)
                    addEncode()
                    endControlFlow("if")
                  } else if (defaultValue != null) {
                    beginControlFlow("if", "includeDefaults || self.%N != $defaultValue", field.safeName)
                    addEncode()
                    endControlFlow("if")
                  } else {
                    // Message is fundamentally broken right now when it comes to evaluating "isDefault"
                    // We would need to check _every_ value and/or keep track of mutations
                    addEncode()
                  }
                }

                type.oneOfs.forEach { oneOf ->
                  beginControlFlow("switch", "self.%N", oneOf.name)
                  oneOf.fields.forEach { field ->
                    val (keys, args) = field.codableName?.let { codableName ->
                      Pair(
                        "preferCamelCase ? %3S : %2S",
                        arrayOf(field.name, codableName),
                      )
                    } ?: Pair("%2S", arrayOf(field.name))

                    addStatement(
                      "case .%1N(let %1N): try container.encode(%1N, forKey: $keys)",
                      field.safeName,
                      *args,
                    )
                  }
                  addStatement("case %T.none: break", OPTIONAL)
                  endControlFlow("switch")
                }
              }
              .build(),
          )
        }
      }
      .build()
  }

  private fun ParameterSpec.Builder.withFieldDefault(field: Field) = apply {
    when {
      field.isMap -> defaultValue("[:]")
      field.isRepeated -> defaultValue("[]")
      field.isOptional -> defaultValue("nil")
    }
  }

  private fun FunctionSpec.Builder.addParameters(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    includeDefaults: Boolean = true,
    includeOneOfs: Boolean = true,
    fieldsFilter: (Field) -> Boolean = { true },
  ) = apply {
    type.declaredFields.filter(fieldsFilter).forEach { field ->
      addParameter(
        ParameterSpec.builder(field.safeName, field.typeName)
          .apply {
            if (includeDefaults) {
              withFieldDefault(field)
            }
          }
          .build(),
      )
    }
    if (includeOneOfs) {
      type.oneOfs.forEach { oneOf ->
        val enumName = oneOfEnumNames.getValue(oneOf).makeOptional()
        addParameter(
          ParameterSpec.builder(oneOf.name, enumName)
            .defaultValue("nil")
            .build(),
        )
      }
    }
  }

  private fun TypeSpec.Builder.generateMessageStorageDelegateConstructor(
    type: MessageType,
    storageName: String,
    storageType: DeclaredTypeName,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
  ) {
    val needsConfigure = type.declaredFields.any { !it.isRequiredParameter } || type.oneOfs.isNotEmpty() || type.isExtensible

    addFunction(
      FunctionSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameters(
          type,
          oneOfEnumNames,
          includeDefaults = false,
          includeOneOfs = false,
          fieldsFilter = { it.isRequiredParameter },
        )
        .apply {
          if (needsConfigure) {
            val closureType = FunctionTypeName.get(
              TypeVariableName.typeVariable("inout Self.${storageType.simpleName}"),
              returnType = void,
            )

            addParameter(
              ParameterSpec.builder("configure", closureType)
                .defaultValue("{ _ in }")
                .build(),
            )
          }
        }
        .apply {
          val storageParams = mutableListOf<CodeBlock>()
          type.declaredFields.filter { it.isRequiredParameter }.forEach { field ->
            storageParams += CodeBlock.of("%1N: %1N", field.safeName)
          }

          if (needsConfigure) {
            storageParams += CodeBlock.of("%1N: %1N", "configure")
          }

          addStatement(
            "self.%N = %T(\n%L\n)",
            storageName,
            storageType,
            storageParams.joinToCode(separator = ",\n"),
          )
        }
        .build(),
    )
  }

  private fun TypeSpec.Builder.generateMessageConstructor(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
  ) {
    val needsConfigure = type.declaredFields.any { !it.isRequiredParameter } || type.oneOfs.isNotEmpty() || type.isExtensible

    addFunction(
      FunctionSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameters(
          type,
          oneOfEnumNames,
          includeDefaults = false,
          includeOneOfs = false,
          fieldsFilter = { it.isRequiredParameter },
        )
        .apply {
          if (needsConfigure) {
            val closureType = FunctionTypeName.get(
              TypeVariableName.typeVariable("inout Self"),
              returnType = void,
            )

            addParameter(
              ParameterSpec.builder("configure", closureType)
                .defaultValue("{ _ in }")
                .build(),
            )
          }
        }
        .apply {
          type.declaredFields.filter { it.isRequiredParameter }.forEach { field ->
            val hasPropertyWrapper = !isIndirect(type, field) && (field.defaultedValue != null || field.isProtoDefaulted)
            val fieldName = if (hasPropertyWrapper) { "_${field.safeName}" } else { field.safeName }
            addStatement(
              if (hasPropertyWrapper) {
                "self.%1N.wrappedValue = %2N"
              } else {
                "self.%1N = %2N"
              },
              fieldName,
              field.safeName,
            )
          }
          if (needsConfigure) {
            addStatement("configure(&self)")
          }
        }
        .build(),
    )
  }

  private fun TypeSpec.Builder.generateMessageExtensionStorageDelegates(
    type: MessageType,
    storageName: String,
    structType: DeclaredTypeName,
    fileMembers: MutableList<FileMemberSpec>,
  ) {
    if (!type.isHeapAllocated) {
      println("Generating storage property delegates for a non-heap allocated type?!")
    }

    if (type.isExtensible) {
      val extensibleExtension = ExtensionSpec.builder(structType)
        .addSuperType(protoExtensible)
        .addDoc("Extensions of %T\n", structType)
        .apply {
          type.extensionFields.forEach { field ->
            val property = PropertySpec.varBuilder(field.safeName, field.typeName, PUBLIC)
              .apply {
                if (field.documentation.isNotBlank()) {
                  addDoc("\n%L\n", field.documentation.sanitizeDoc())
                }
                addDoc("\nSource: %L\n", field.location.withPathOnly())

                if (field.isDeprecated) {
                  addAttribute(deprecated)
                }
              }
              .getter(
                FunctionSpec.getterBuilder()
                  .addStatement("%N.%N", storageName, field.safeName)
                  .build(),
              )
              .setter(
                FunctionSpec.setterBuilder()
                  .addStatement("%N.%N = newValue", storageName, field.safeName)
                  .build(),
              )
              .build()
            addProperty(property)

            if (!field.isMap && !field.isRepeated && (field.defaultedValue != null || field.isProtoDefaulted)) {
              // Look into AccessorMacros in the future when CocoaPods has better support.
              val defaultProperty =
                PropertySpec.varBuilder("default_${field.safeName}", field.typeName.makeNonOptional(), PUBLIC, STATIC)
                  .addDoc("Default value for %L extension field.\n", field.safeName)
                  .mutable(false)
                  .initializer(field.defaultedValue ?: CodeBlock.of(".defaultedValue"))
                  .build()

              addProperty(defaultProperty)
            }
          }
        }
        .build()

      fileMembers += FileMemberSpec.builder(extensibleExtension)
        .build()
    }
  }

  private fun generateMessageExtensions(
    type: MessageType,
    structType: DeclaredTypeName,
    fileMembers: MutableList<FileMemberSpec>,
    forStorageType: Boolean = false,
  ) {
    if (type.isExtensible) {
      val extensibleExtension = ExtensionSpec.builder(structType)
        .addSuperType(protoExtensible)
        .build()
      fileMembers += FileMemberSpec.builder(extensibleExtension)
        .build()
    }

    if (type.extensionFields.isNotEmpty()) {
      val extendedFieldsExtension = ExtensionSpec.builder(structType)
        .apply {
          if (!forStorageType) {
            addDoc("Extensions of %T\n", structType)
          }
          type.extensionFields.forEach { field ->
            val property = PropertySpec.varBuilder(field.safeName, field.typeName, PUBLIC)
              .apply {
                if (!forStorageType && field.documentation.isNotBlank()) {
                  addDoc("\n%L\n", field.documentation.sanitizeDoc())
                }
                if (!forStorageType) {
                  addDoc("\nSource: %L\n", field.location.withPathOnly())
                }

                if (field.isDeprecated) {
                  addAttribute(deprecated)
                }

                val getterFunctionSpec = FunctionSpec.getterBuilder()
                var parseMethod = "self.parseUnknownField(fieldNumber: %L"
                val args = mutableListOf<Any>(field.tag)
                if (!field.isRepeated) {
                  parseMethod += ", type: %T.self"
                  args.add(field.typeName.makeNonOptional())
                }
                val encoding = field.type!!.encoding
                if (encoding != null) {
                  parseMethod += ", encoding: .%N"
                  args.add(encoding)
                }
                parseMethod += ")\n"
                getterFunctionSpec.addCode(
                  parseMethod,
                  *args.toTypedArray(),
                )
                getter(getterFunctionSpec.build())

                val setterFunctionSpec = FunctionSpec.setterBuilder()
                if (encoding != null) {
                  setterFunctionSpec.addCode(
                    "self.setUnknownField(fieldNumber: %L, newValue: newValue, encoding: .%N)\n",
                    field.tag,
                    encoding,
                  )
                } else {
                  setterFunctionSpec.addCode("self.setUnknownField(fieldNumber: %L, newValue: newValue)\n", field.tag)
                }
                setter(setterFunctionSpec.build())
              }
              .build()
            addProperty(property)

            if (!forStorageType && !field.isMap && !field.isRepeated && (field.defaultedValue != null || field.isProtoDefaulted)) {
              // Look into AccessorMacros in the future when CocoaPods has better support.
              val defaultProperty =
                PropertySpec.varBuilder("default_${field.safeName}", field.typeName.makeNonOptional(), PUBLIC, STATIC)
                  .addDoc("Default value for %L extension field.\n", field.safeName)
                  .mutable(false)
                  .initializer(field.defaultedValue ?: CodeBlock.of(".defaultedValue"))
                  .build()

              addProperty(defaultProperty)
            }
          }
        }
        .build()
      fileMembers += FileMemberSpec.builder(extendedFieldsExtension)
        .build()
    }
  }

  private fun TypeSpec.Builder.generateMessageProperties(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    forStorageType: Boolean = false,
  ) {
    type.declaredFields.forEach { field ->
      val property = PropertySpec.varBuilder(field.safeName, field.typeName, PUBLIC)
      if (!forStorageType && field.documentation.isNotBlank()) {
        property.addDoc("%L\n", field.documentation.sanitizeDoc())
      }
      if (!forStorageType && field.isDeprecated) {
        property.addAttribute(deprecated)
      }
      if (isIndirect(type, field)) {
        property.addAttribute(AttributeSpec.builder(indirect).build())
      } else {
        val defaultedValue = field.defaultedValue

        if (defaultedValue != null) {
          property.addAttribute(AttributeSpec.builder(customDefaulted).addArgument(CodeBlock.of("defaultValue: %L", defaultedValue)).build())
        } else if (field.isProtoDefaulted) {
          property.addAttribute(AttributeSpec.builder(protoDefaulted).build())
        }
      }

      if (field.isMap) {
        property.initializer("[:]")
      } else if (field.isRepeated) {
        property.initializer("[]")
      }

      addProperty(property.build())
    }

    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      addProperty(
        PropertySpec.varBuilder(oneOf.name, enumName.makeOptional(), PUBLIC)
          .apply {
            if (oneOf.documentation.isNotBlank()) {
              addDoc("%N\n", oneOf.documentation.sanitizeDoc())
            }
            if (oneOf.fields.any { oneOfField -> isIndirect(type, oneOfField) }) {
              addAttribute(AttributeSpec.builder(indirect).build())
            }
          }
          .build(),
      )
    }

    addProperty(
      PropertySpec.varBuilder("unknownFields", type.unknownFieldsType, PUBLIC)
        .initializer(".init()")
        .build(),
    )
  }

  private fun defaultFieldInitializer(
    protoType: ProtoType,
    defaultValue: Any,
  ): CodeBlock {
    val typeName = protoType.typeName
    return when {
      typeName == BOOL -> CodeBlock.of("%L", defaultValue)
      typeName == INT -> defaultValue.toIntFieldInitializer()
      typeName == INT32 -> defaultValue.toInt32FieldInitializer()
      typeName == INT64 -> defaultValue.toInt64FieldInitializer()
      typeName == UINT32 -> defaultValue.toUInt32FieldInitializer()
      typeName == UINT64 -> defaultValue.toUInt64FieldInitializer()
      typeName == FLOAT -> defaultValue.toFloatFieldInitializer()
      typeName == DOUBLE -> defaultValue.toDoubleFieldInitializer()
      typeName == STRING -> CodeBlock.of("%S", defaultValue)
      typeName == DATA -> CodeBlock.of(
        "%T(base64Encoded: %S)!",
        FOUNDATION_DATA,
        defaultValue.toString().encode(charset = Charsets.ISO_8859_1).base64(),
      )
      protoType.isEnum -> CodeBlock.of("%T.%L", typeName, defaultValue)
      else -> throw IllegalStateException("$protoType is not an allowed scalar type")
    }
  }

  private fun Any.toIntFieldInitializer(): CodeBlock = when (val int = optionValueToInt(this)) {
    Int.MIN_VALUE -> CodeBlock.of("%T.min", INT)
    Int.MAX_VALUE -> CodeBlock.of("%T.max", INT)
    else -> CodeBlock.of("%L", int)
  }

  private fun Any.toInt32FieldInitializer(): CodeBlock = when (val int = optionValueToInt(this)) {
    Int.MIN_VALUE -> CodeBlock.of("%T.min", INT32)
    Int.MAX_VALUE -> CodeBlock.of("%T.max", INT32)
    else -> CodeBlock.of("%L", int)
  }

  private fun Any.toInt64FieldInitializer(): CodeBlock = when (val long = optionValueToLong(this)) {
    Long.MIN_VALUE -> CodeBlock.of("%T.min", INT64)
    Long.MAX_VALUE -> CodeBlock.of("%T.max", INT64)
    else -> CodeBlock.of("%L", long)
  }

  private fun Any.toUInt32FieldInitializer(): CodeBlock = when (val int = optionValueToInt(this)) {
    0 -> CodeBlock.of("%T.min", UINT32)
    -1 -> CodeBlock.of("%T.max", UINT32)
    else -> CodeBlock.of("%L", int)
  }

  private fun Any.toUInt64FieldInitializer(): CodeBlock = when (val long = optionValueToLong(this)) {
    0L -> CodeBlock.of("%T.min", UINT64)
    -1L -> CodeBlock.of("%T.max", UINT64)
    else -> CodeBlock.of("%L", long)
  }

  private fun Any.toFloatFieldInitializer(): CodeBlock = when (this) {
    "inf" -> CodeBlock.of("Float.infinity")
    "-inf" -> CodeBlock.of("-Float.infinity")
    "nan" -> CodeBlock.of("Float.nan")
    "-nan" -> CodeBlock.of("Float.nan * -1")
    else -> CodeBlock.of("%L", this.toString())
  }

  private fun Any.toDoubleFieldInitializer(): CodeBlock = when (this) {
    "inf" -> CodeBlock.of("Double.infinity")
    "-inf" -> CodeBlock.of("-Double.infinity")
    "nan" -> CodeBlock.of("Double.nan")
    "-nan" -> CodeBlock.of("Double.nan * -1")
    else -> CodeBlock.of("%L", this.toString().toDouble())
  }

  private fun TypeSpec.Builder.generateMessageStoragePropertyDelegates(
    type: MessageType,
    storageName: String,
    storageType: DeclaredTypeName,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
  ) {
    if (!type.isHeapAllocated) {
      println("Generating storage property delegates for a non-heap allocated type?!")
    }

    val propertyVariable = TypeVariableName.typeVariable("Property")
    val signature = FunctionSignatureSpec.builder()
      .addTypeVariable(propertyVariable)
      .addParameter(
        "dynamicMember",
        "keyPath",
        writableKeyPath.parameterizedBy(storageType, propertyVariable),
      )
      .returns(propertyVariable)
      .build()

    val subscript = PropertySpec.subscriptBuilder(signature)
      .addDoc("Access the underlying storage\n")
      .addModifiers(PUBLIC)
      .getter(
        FunctionSpec.getterBuilder()
          .addStatement("%N[keyPath: keyPath]", storageName)
          .build(),
      )
      .setter(
        FunctionSpec.setterBuilder()
          .addStatement("%N[keyPath: keyPath] = newValue", storageName)
          .build(),
      )
      .build()
    addProperty(subscript)

    type.declaredFields.forEach { field ->
      val property = PropertySpec.varBuilder(field.safeName, field.typeName, PUBLIC)
        .getter(
          FunctionSpec.getterBuilder()
            .addStatement("%N.%N", storageName, field.safeName)
            .build(),
        )
        .setter(
          FunctionSpec.setterBuilder()
            .addStatement("%N.%N = newValue", storageName, field.safeName)
            .build(),
        )

      if (field.documentation.isNotBlank()) {
        property.addDoc("%L\n", field.documentation.sanitizeDoc())
      }
      if (field.isDeprecated) {
        property.addAttribute(deprecated)
      }

      addProperty(property.build())
    }

    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      addProperty(
        PropertySpec.varBuilder(oneOf.name, enumName.makeOptional(), PUBLIC)
          .getter(
            FunctionSpec.getterBuilder()
              .addStatement("%N.%N", storageName, oneOf.name)
              .build(),
          )
          .setter(
            FunctionSpec.setterBuilder()
              .addStatement("%N.%N = newValue", storageName, oneOf.name)
              .build(),
          )
          .apply {
            if (oneOf.documentation.isNotBlank()) {
              addDoc("%N\n", oneOf.documentation.sanitizeDoc())
            }
          }
          .build(),
      )
    }

    addProperty(
      PropertySpec.varBuilder("unknownFields", type.unknownFieldsType, PUBLIC)
        .getter(
          FunctionSpec.getterBuilder()
            .addStatement("%N.unknownFields", storageName)
            .build(),
        )
        .setter(
          FunctionSpec.setterBuilder()
            .addStatement("%N.unknownFields = newValue", storageName)
            .build(),
        )
        .build(),
    )
  }

  private fun ExtensionSpec.Builder.generateMessageOneOfs(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    fileMembers: MutableList<FileMemberSpec>,
  ) {
    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      // TODO use a NameAllocator
      val writer = if (oneOf.fields.any { it.name == "protoWriter" }) "_protoWriter" else "protoWriter"
      addType(
        TypeSpec.enumBuilder(enumName)
          .addModifiers(PUBLIC)
          .apply {
            oneOf.fields.forEach { oneOfField ->
              addEnumCase(
                EnumerationCaseSpec.builder(oneOfField.safeName, oneOfField.typeName.makeNonOptional())
                  .apply {
                    if (oneOfField.documentation.isNotBlank()) {
                      addDoc("%L\n", oneOfField.documentation.sanitizeDoc())
                    }
                    if (oneOfField.isDeprecated) {
                      addAttribute(deprecated)
                    }
                  }
                  .build(),
              )
            }
          }
          .addFunction(
            FunctionSpec.builder("encode")
              .addParameter("to", writer, protoWriter)
              .addModifiers(FILEPRIVATE)
              .throws(true)
              .beginControlFlow("switch", "self")
              .apply {
                oneOf.fields.forEach { field ->
                  addStatement(
                    "case .%1N(let %1N): try $writer.encode(tag: %2L, value: %1N)",
                    field.safeName,
                    field.tag,
                  )
                }
              }
              .endControlFlow("switch")
              .build(),
          )
          .build(),
      )

      val equatableExtension = ExtensionSpec.builder(enumName)
        .addSuperType(equatable)
        .build()
      fileMembers += FileMemberSpec.builder(equatableExtension)
        .addGuard("!$FLAG_REMOVE_EQUATABLE")
        .build()

      val hashableExtension = ExtensionSpec.builder(enumName)
        .addSuperType(hashable)
        .build()
      fileMembers += FileMemberSpec.builder(hashableExtension)
        .addGuard("!$FLAG_REMOVE_HASHABLE")
        .build()

      val sendableExtension = ExtensionSpec.builder(enumName)
        .addSuperType(sendable)
        .build()
      fileMembers += FileMemberSpec.builder(sendableExtension)
        .build()

      if (oneOf.fields.any { it.isRedacted }) {
        val redactableExtension = ExtensionSpec.builder(enumName)
          .addSuperType(redactable)
          .addType(
            TypeSpec.enumBuilder("RedactedKeys")
              .addModifiers(PUBLIC)
              .addSuperType(STRING)
              .addSuperType(redactedKey)
              .apply {
                oneOf.fields.forEach { field ->
                  if (field.isRedacted) {
                    addEnumCase(field.name)
                  }
                }
              }
              .build(),
          )
          .build()
        fileMembers += FileMemberSpec.builder(redactableExtension)
          .addGuard("!$FLAG_REMOVE_REDACTABLE")
          .build()
      }
    }
  }

  private val ProtoType.encoding: String?
    get() = when (this) {
      ProtoType.SINT32, ProtoType.SINT64 -> "signed"
      ProtoType.FIXED32, ProtoType.FIXED64 -> "fixed"
      ProtoType.SFIXED32, ProtoType.SFIXED64 -> "fixed"
      ProtoType.INT32, ProtoType.INT64 -> "variable"
      ProtoType.UINT32, ProtoType.UINT64 -> "variable"
      else -> null
    }

  private fun generateEnum(
    type: EnumType,
    fileMembers: MutableList<FileMemberSpec>,
  ): TypeSpec {
    val enumName = type.typeName
    return TypeSpec.enumBuilder(enumName)
      .addModifiers(PUBLIC)
      .addSuperTypes(listOf(INT32, CASE_ITERABLE, type.protoCodableType))
      .apply {
        type.protoDefaultedName?.let { protoDefaultedName ->
          addSuperType(protoDefaultedValue)
          addProperty(
            PropertySpec.varBuilder("defaultedValue", enumName, PUBLIC, STATIC).getter(
              FunctionSpec.getterBuilder().addCode(
                CodeBlock.of("%T.%L\n", type.typeName.makeNonOptional(), protoDefaultedName),
              ).build(),
            ).build(),
          )
        }
      }
      .apply {
        val sendableExtension = ExtensionSpec.builder(enumName)
          .addSuperType(sendable)
          .build()
        fileMembers += FileMemberSpec.builder(sendableExtension)
          .build()

        if (type.documentation.isNotBlank()) {
          addDoc("%L\n", type.documentation.sanitizeDoc())
        }
        type.constants.forEach { constant ->
          addEnumCase(
            EnumerationCaseSpec.builder(constant.name, constant.tag)
              .apply {
                if (constant.documentation.isNotBlank()) {
                  addDoc("%L\n", constant.documentation.sanitizeDoc())
                }
                if (constant.isDeprecated) {
                  addAttribute(deprecated)
                }
              }
              .build(),
          )
        }

        addProperty(
          PropertySpec.varBuilder("description", STRING)
            .addModifiers(PUBLIC)
            .getter(
              FunctionSpec.getterBuilder()
                .beginControlFlow("switch", "self")
                .apply {
                  type.constants.forEach { constant ->
                    addStatement("case .%1N: return \"%1N\"", constant.name)
                  }
                }
                .endControlFlow("switch")
                .build(),
            )
            .build(),
        )

        // Swift won't synthesize CaseIterable conformance if any constants contain an availability
        // attribute. https://bugs.swift.org/browse/SR-7151
        if (type.constants.any { it.isDeprecated }) {
          addProperty(
            PropertySpec.varBuilder("allCases", ARRAY.parameterizedBy(enumName))
              .addModifiers(PUBLIC, STATIC)
              .getter(
                FunctionSpec.getterBuilder()
                  .addStatement(
                    "return [%L]",
                    type.constants.map { CodeBlock.of(".%N", it.name) }
                      .joinToCode(",%W"),
                  )
                  .build(),
              )
              .build(),
          )
        }
        type.nestedTypes.forEach { nestedType ->
          generateType(nestedType, fileMembers).forEach {
            addType(it)
          }
        }
      }
      .build()
  }

  private fun generateEnclosing(
    type: EnclosingType,
    fileMembers: MutableList<FileMemberSpec>,
  ): TypeSpec {
    return TypeSpec.enumBuilder(type.typeName)
      .addModifiers(PUBLIC)
      .addDoc(
        "%N\n",
        "*Note:* This type only exists to maintain class structure for its nested types and " +
          "is not an actual message.",
      )
      .apply {
        type.nestedTypes.forEach { nestedType ->
          generateType(nestedType, fileMembers).forEach {
            addType(it)
          }
        }
      }
      .build()
  }

  companion object {
    // Always fully-qualified in case a message type is named `Data`.
    private val FOUNDATION_DATA = DeclaredTypeName.typeName("Foundation.Data", true)

    fun builtInType(protoType: ProtoType): Boolean = protoType in BUILT_IN_TYPES.keys

    // TODO use a NameAllocator
    val ProtoType.safeName: String
      get() {
        return when (this.simpleName) {
          "Type" -> "Type_"
          "Error" -> "Error_"
          else -> this.simpleName
        }
      }

    // TODO use a NameAllocator
    val Field.safeName: String
      get() = when (name) {
        "description" -> "description_"
        else -> name
      }

    val MessageType.isExtensible: Boolean
      get() = extensionsList.isNotEmpty()

    val MessageType.declaredFieldsAndOneOfFields: List<Field>
      get() = declaredFields + oneOfs.flatMap { it.fields }

    val MessageType.isHeapAllocated get() = declaredFields.size + oneOfs.size >= 16

    private val SWIFT_COMMON_TYPES = setOf(
      "Any",
      "AnyClass",
      "AnyObject",
      "Array",
      "Bool",
      "Character",
      "ClosedRange",
      "Closure",
      "Collection",
      "Data",
      "DataProtocol",
      "Date",
      "Decimal",
      "Dictionary",
      "Double",
      "Error",
      "Float",
      "Int",
      "NumberFormatter",
      "Optional",
      "Protocol",
      "Range",
      "Result",
      "Sequence",
      "Set",
      "SortOrder",
      "String",
      "Tuple",
      "URL",
      "URLComponents",
      "UUID",
    )

    private val BUILT_IN_TYPES: Map<out ProtoType, DeclaredTypeName> = mapOf(
      ProtoType.BOOL to BOOL,
      ProtoType.BYTES to FOUNDATION_DATA,
      ProtoType.DOUBLE to DOUBLE,
      ProtoType.FLOAT to FLOAT,
      ProtoType.FIXED32 to UINT32,
      ProtoType.FIXED64 to UINT64,
      ProtoType.INT32 to INT32,
      ProtoType.INT64 to INT64,
      ProtoType.SFIXED32 to INT32,
      ProtoType.SFIXED64 to INT64,
      ProtoType.SINT32 to INT32,
      ProtoType.SINT64 to INT64,
      ProtoType.STRING to STRING,
      ProtoType.UINT32 to UINT32,
      ProtoType.UINT64 to UINT64,
      ProtoType.ANY to DeclaredTypeName.typeName("Wire.AnyMessage"),
//        Options.FIELD_OPTIONS to ClassName("com.google.protobuf", "FieldOptions"),
//        Options.MESSAGE_OPTIONS to ClassName("com.google.protobuf", "MessageOptions"),
//        Options.ENUM_OPTIONS to ClassName("com.google.protobuf", "EnumOptions")
    )

    private const val FLAG_REMOVE_CODABLE = "WIRE_REMOVE_CODABLE"
    private const val FLAG_REMOVE_EQUATABLE = "WIRE_REMOVE_EQUATABLE"
    private const val FLAG_REMOVE_HASHABLE = "WIRE_REMOVE_HASHABLE"
    private const val FLAG_REMOVE_REDACTABLE = "WIRE_REMOVE_REDACTABLE"

    private val NEEDS_CUSTOM_CODABLE = setOf("Duration", "Timestamp")

    @JvmStatic
    @JvmName("get")
    operator fun invoke(
      schema: Schema,
      existingTypeModuleName: Map<ProtoType, String> = emptyMap(),
    ): SwiftGenerator {
      val nameToTypeName = mutableMapOf<ProtoType, DeclaredTypeName>()

      fun putAll(enclosingClassName: DeclaredTypeName?, types: List<Type>) {
        for (type in types) {
          val protoType = type.type

          val className = if (enclosingClassName != null) {
            enclosingClassName.nestedType(protoType.safeName, alwaysQualify = true)
          } else {
            val safeName = protoType.safeName

            val moduleName = existingTypeModuleName[protoType] ?: ""
            // In some cases a proto declares a message that collides with built-in Foundation and Swift stdlib
            // types. For those we always qualify the type name to disambiguate.

            if (protoType.simpleName in SWIFT_COMMON_TYPES) {
              DeclaredTypeName.qualifiedTypeName("$moduleName.$safeName")
            } else {
              DeclaredTypeName(moduleName, safeName)
            }
          }

          nameToTypeName[protoType] = className
          putAll(className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        putAll(null, protoFile.types)

        for (service in protoFile.services) {
          val protoType = service.type
          val name = protoType.simpleName

          val moduleName = existingTypeModuleName[protoType] ?: ""
          val className = DeclaredTypeName(moduleName, name)

          nameToTypeName[protoType] = className
        }
      }

      nameToTypeName.putAll(BUILT_IN_TYPES)

      val referenceCycleIndirections =
        computeReferenceCycleIndirections(schema, existingTypeModuleName.keys)

      return SwiftGenerator(schema, nameToTypeName, referenceCycleIndirections)
    }

    private fun computeReferenceCycleIndirections(
      schema: Schema,
      existingTypes: Set<ProtoType>,
    ): Map<ProtoType, Set<ProtoType>> {
      val indirections = mutableMapOf<ProtoType, MutableSet<ProtoType>>()

      var nodes = schema.protoFiles
        .flatMap { it.typesAndNestedTypes() }
        .map { it.type }
        // Ignore types which were already generated. We cannot form a cycle with them.
        .filter { it !in existingTypes }
        .toSet()
      while (true) {
        val dagChecker = DagChecker(nodes) { protoType ->
          when (val type = schema.getType(protoType)!!) {
            is MessageType -> {
              type.declaredFieldsAndOneOfFields.map { it.type!! }
                // Remove edges known to need an indirection to break an already-seen cycle.
                .filter { it !in (indirections[protoType] ?: emptySet<ProtoType>()) }
            }
            is EnumType -> emptyList()
            is EnclosingType -> emptyList()
            else -> throw IllegalArgumentException("Unknown type: $protoType")
          }
        }

        val cycles = dagChecker.check()
        if (cycles.isEmpty()) {
          break
        }
        // Break the first edge in each cycle with an indirection.
        for (cycle in cycles) {
          val other = if (cycle.size > 1) cycle[1] else cycle[0]
          indirections.getOrPut(cycle[0], ::LinkedHashSet) += other
        }

        // We need to ensure that we've successfully broken all of the reported cycles. Feed the set
        // of nodes which were present in a cycle back into the check.
        nodes = cycles.flatten().toSet()
      }

      return indirections
    }
  }
}
