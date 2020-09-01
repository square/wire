package com.squareup.wire.swift

import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.Syntax.PROTO_3
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
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.INT32
import io.outfoxx.swiftpoet.INT64
import io.outfoxx.swiftpoet.Modifier.FILEPRIVATE
import io.outfoxx.swiftpoet.Modifier.MUTATING
import io.outfoxx.swiftpoet.Modifier.PRIVATE
import io.outfoxx.swiftpoet.Modifier.PUBLIC
import io.outfoxx.swiftpoet.Modifier.STATIC
import io.outfoxx.swiftpoet.OPTIONAL
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.ParameterizedTypeName
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.TypeAliasSpec
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.TypeSpec
import io.outfoxx.swiftpoet.UINT32
import io.outfoxx.swiftpoet.UINT64
import io.outfoxx.swiftpoet.joinToCode
import io.outfoxx.swiftpoet.parameterizedBy
import java.util.Locale.US

class SwiftGenerator private constructor(
  val schema: Schema,
  private val nameToTypeName: Map<ProtoType, DeclaredTypeName>,
  private val referenceCycleIndirections: Map<ProtoType, Set<ProtoType>>
) {
  private val proto2Codable = DeclaredTypeName.typeName("Wire.Proto2Codable")
  private val proto3Codable = DeclaredTypeName.typeName("Wire.Proto3Codable")
  private val protoReader = DeclaredTypeName.typeName("Wire.ProtoReader")
  private val protoWriter = DeclaredTypeName.typeName("Wire.ProtoWriter")
  private val heap = DeclaredTypeName.typeName("Wire.Heap")
  private val indirect = DeclaredTypeName.typeName("Wire.Indirect")
  private val redactable = DeclaredTypeName.typeName("Wire.Redactable")
  private val redactedKey = DeclaredTypeName.typeName("Wire.RedactedKey")
  private val equatable = DeclaredTypeName.typeName("Swift.Equatable")
  private val hashable = DeclaredTypeName.typeName("Swift.Hashable")
  private val codable = DeclaredTypeName.typeName("Swift.Codable")
  private val codingKey = DeclaredTypeName.typeName("Swift.CodingKey")
  private val encoder = DeclaredTypeName.typeName("Swift.Encoder")
  private val decoder = DeclaredTypeName.typeName("Swift.Decoder")

  private val deprecated = AttributeSpec.builder("available").addArguments("*", "deprecated").build()

  private val ProtoType.typeName
    get() = nameToTypeName.getValue(this)
  private val ProtoType.isMessage
    get() = schema.getType(this) is MessageType
  private val Type.typeName
    get() = type.typeName

  fun generatedTypeName(type: Type) = type.typeName

  internal val Field.isMap: Boolean
    get() = type!!.isMap
  internal val Field.keyType: ProtoType
    get() = type!!.keyType!!
  internal val Field.valueType: ProtoType
    get() = type!!.valueType!!

  private val Field.typeName: TypeName
    get() {
      return when (encodeMode!!) {
        EncodeMode.MAP -> DICTIONARY.parameterizedBy(keyType.typeName, valueType.typeName)
        EncodeMode.REPEATED,
        EncodeMode.PACKED -> ARRAY.parameterizedBy(type!!.typeName)
        EncodeMode.NULL_IF_ABSENT -> OPTIONAL.parameterizedBy(type!!.typeName)
        EncodeMode.REQUIRED -> type!!.typeName
        EncodeMode.OMIT_IDENTITY -> {
          when {
            isOneOf || type!!.isMessage -> OPTIONAL.parameterizedBy(type!!.typeName)
            else -> type!!.typeName
          }
        }
      }
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

  private fun generateType(type: Type, fileMembers: MutableList<FileMemberSpec>) = when (type) {
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

  private val MessageType.isHeapAllocated get() = fields.size + oneOfs.size >= 16

  @OptIn(ExperimentalStdlibApi::class) // TODO move to build flag
  private fun generateMessage(
    type: MessageType,
    fileMembers: MutableList<FileMemberSpec>
  ): List<TypeSpec> {
    val structType = type.typeName
    val oneOfEnumNames = type.oneOfs.associateWith { structType.nestedType(it.name.capitalize(US)) }

    // TODO use a NameAllocator
    val propertyNames = type.fields.map { it.name } + type.oneOfs.map { it.name }

    val storageType = structType.peerType("_${structType.simpleName}")
    val storageName = if ("storage" in propertyNames) "_storage" else "storage"

    val typeSpecs = mutableListOf<TypeSpec>()

    typeSpecs +=  TypeSpec.structBuilder(structType)
        .addModifiers(PUBLIC)
        .apply {
          if (type.documentation.isNotBlank()) {
            addDoc("%L\n", type.documentation.sanitizeDoc())
          }

          if (type.isHeapAllocated) {
            addProperty(PropertySpec.varBuilder(storageName, storageType, PRIVATE)
                .addAttribute(AttributeSpec.builder(heap).build())
                .build())
            generateMessageStoragePropertyDelegates(type, storageName, oneOfEnumNames)
            addFunction(FunctionSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameters(type, oneOfEnumNames, includeDefaults = true)
                .apply {
                  val storageParams = mutableListOf<CodeBlock>()
                  type.fields.forEach { field ->
                    storageParams += CodeBlock.of("%1N: %1N", field.name)
                  }
                  type.oneOfs.forEach { oneOf ->
                    storageParams += CodeBlock.of("%1N: %1N", oneOf.name)
                  }
                  addStatement("_storage = %T(value: %T(%L))", heap, storageType,
                      storageParams.joinToCode(separator = ",%W"))
                }
                .build())
            addFunction(FunctionSpec.builder("copyStorage")
                .addModifiers(PRIVATE, MUTATING)
                .beginControlFlow("if", "!isKnownUniquelyReferenced(&_%N)", storageName)
                .addStatement("_%1N = %2T(value: %1N)", storageName, heap)
                .endControlFlow("if")
                .build())
          } else {
            generateMessageProperties(type, oneOfEnumNames)
            generateMessageConstructor(type, oneOfEnumNames)
          }

          generateMessageOneOfs(type, oneOfEnumNames, fileMembers)

          type.nestedTypes.forEach { nestedType ->
            generateType(nestedType, fileMembers).forEach {
              addType(it)
            }
          }
        }
        .build()

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

    val redactionExtension = if (type.fields.any { it.isRedacted }) {
      ExtensionSpec.builder(structType)
          .addSuperType(redactable)
          .addType(TypeSpec.enumBuilder("RedactedKeys")
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
              .build())
    } else {
      null
    }

    if (type.isHeapAllocated) {
      typeSpecs += TypeSpec.structBuilder(storageType)
          .addModifiers(FILEPRIVATE)
          .apply {
            generateMessageProperties(type, oneOfEnumNames, forStorageType = true)
            generateMessageConstructor(type, oneOfEnumNames, includeDefaults = false)
          }
          .build()

      val structProtoCodableExtension = ExtensionSpec.builder(structType)
          .addSuperType(type.protoCodableType)
          .addFunction(FunctionSpec.constructorBuilder()
              .addModifiers(PUBLIC)
              .addParameter("from", "reader", protoReader)
              .throws(true)
              .addStatement("_%N = %T(value: try %T(from: reader))", storageName, heap, storageType)
              .build())
          .addFunction(FunctionSpec.builder("encode")
              .addModifiers(PUBLIC)
              .addParameter("to", "writer", protoWriter)
              .throws(true)
              .addStatement("try %N.encode(to: writer)", storageName)
              .build())
          .build()
      fileMembers += FileMemberSpec.builder(structProtoCodableExtension).build()

      val storageProtoCodableExtension = ExtensionSpec.builder(storageType)
          .messageProtoCodableExtension(type, structType, oneOfEnumNames, propertyNames)
          .build()
      fileMembers += FileMemberSpec.builder(storageProtoCodableExtension).build()

      val structCodableExtension = ExtensionSpec.builder(structType)
          .addSuperType(codable)
          .build()
      fileMembers += FileMemberSpec.builder(structCodableExtension)
          .addGuard("!$FLAG_REMOVE_CODABLE")
          .build()

      val storageCodableExtension = messageCodableExtension(type, storageType)
      fileMembers += FileMemberSpec.builder(storageCodableExtension)
          .addGuard("!$FLAG_REMOVE_CODABLE")
          .build()

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

      if (redactionExtension != null) {
        redactionExtension.addProperty(PropertySpec.varBuilder("description", STRING)
            .addModifiers(PUBLIC)
            .getter(FunctionSpec.getterBuilder()
                .addStatement("return %N.description", storageName)
                .build())
            .build())

        val storageRedactableExtension = ExtensionSpec.builder(storageType)
            .addSuperType(redactable)
            .addType(
                TypeAliasSpec.builder("RedactedKeys", structType.nestedType("RedactedKeys"))
                    .build())
            .build()
        fileMembers += FileMemberSpec.builder(storageRedactableExtension)
            .addGuard("!$FLAG_REMOVE_REDACTABLE")
            .build()
      }
    } else {
      val protoCodableExtension = ExtensionSpec.builder(structType)
          .messageProtoCodableExtension(type, structType, oneOfEnumNames, propertyNames)
          .build()
      fileMembers += FileMemberSpec.builder(protoCodableExtension).build()

      fileMembers += FileMemberSpec.builder(messageCodableExtension(type, structType))
          .addGuard("!$FLAG_REMOVE_CODABLE")
          .build()
    }

    if (redactionExtension != null) {
      fileMembers += FileMemberSpec.builder(redactionExtension.build())
          .addGuard("!$FLAG_REMOVE_REDACTABLE")
          .build()
    }

    return typeSpecs
  }

  private fun ExtensionSpec.Builder.messageProtoCodableExtension(
    type: MessageType,
    structType: DeclaredTypeName,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    propertyNames: Collection<String>
  ): ExtensionSpec.Builder = apply {
    addSuperType(type.protoCodableType)

    val reader = if ("reader" in propertyNames) "_reader" else "reader"
    val token = if ("token" in propertyNames) "_token" else "token"
    val tag = if ("tag" in propertyNames) "_tag" else "tag"
    addFunction(FunctionSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter("from", reader, protoReader)
        .throws(true)
        .apply {
          // Declare locals into which everything is writen before promoting to members.
          type.fields.forEach { field ->
            val localType = if (field.isRepeated || field.isMap) {
              field.typeName
            } else {
              field.typeName.makeOptional()
            }
            val initializer = when {
              field.isMap -> "[:]"
              field.isRepeated -> "[]"
              else -> "nil"
            }
            addStatement("var %N: %T = %L", field.name, localType, initializer)
          }
          type.oneOfs.forEach { oneOf ->
            val enumName = oneOfEnumNames.getValue(oneOf)
            addStatement("var %N: %T = nil", oneOf.name, enumName.makeOptional())
          }
          if (type.fieldsAndOneOfFields.isNotEmpty()) {
            addStatement("")
          }

          addStatement("let $token = try $reader.beginMessage()")
          beginControlFlow("while", "let $tag = try $reader.nextTag(token: $token)")
          beginControlFlow("switch", tag)
          type.fields.forEach { field ->
            val decoder = CodeBlock.Builder()
            if (field.isMap) {
              decoder.add("try $reader.decode(into: &%N", field.name)
              field.keyType.encoding?.let { keyEncoding ->
                decoder.add(", keyEncoding: .%N", keyEncoding)
              }
              field.valueType.encoding?.let { valueEncoding ->
                decoder.add(", valueEncoding: .%N", valueEncoding)
              }
            } else {
              if (field.isRepeated) {
                decoder.add("try $reader.decode(into: &%N", field.name)
              } else {
                decoder.add(
                    "%N = try $reader.decode(%T.self", field.name,
                    field.typeName.makeNonOptional()
                )
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
              addStatement(
                  "case %L: %N = .%N(try $reader.decode(%T.self))", field.tag,
                  oneOf.name, field.name, field.typeName.makeNonOptional()
              )
            }
          }
          addStatement("default: try $reader.readUnknownField(tag: $tag)")
          endControlFlow("switch")
          endControlFlow("while")
          addStatement("self.unknownFields = try $reader.endMessage(token: $token)")

          // Check required and bind members.
          addStatement("")
          type.fields.forEach { field ->
            val initializer = if (field.typeName.optional || field.isRepeated || field.isMap) {
              CodeBlock.of("%N", field.name)
            } else {
              CodeBlock.of("try %1T.checkIfMissing(%2N, %2S)", structType, field.name)
            }
            if (isIndirect(type, field)) {
              addStatement("_%N = %T(value: %L)", field.name, indirect, initializer)
            } else {
              addStatement("self.%N = %L", field.name, initializer)
            }
          }
          type.oneOfs.forEach { oneOf ->
            addStatement("self.%1N = %1N", oneOf.name)
          }
        }
        .build())

    val writer = if ("writer" in propertyNames) "_writer" else "writer"
    addFunction(FunctionSpec.builder("encode")
        .addModifiers(PUBLIC)
        .addParameter("to", writer, protoWriter)
        .throws(true)
        .apply {
          type.fields.forEach { field ->
            if (field.isMap) {
              addCode("try $writer.encode(tag: %L, value: self.%N", field.tag, field.name)
              field.keyType.encoding?.let { keyEncoding ->
                addCode(", keyEncoding: .%N", keyEncoding)
              }
              field.valueType.encoding?.let { valueEncoding ->
                addCode(", valueEncoding: .%N", valueEncoding)
              }
              addCode(")\n")
            } else {
              addCode("try $writer.encode(tag: %L, value: self.%N", field.tag, field.name)
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
        .build())
  }

  private val MessageType.protoCodableType: DeclaredTypeName
    get() = when (syntax) {
      PROTO_2 -> proto2Codable
      PROTO_3 -> proto3Codable
    }

  private fun messageCodableExtension(
    type: MessageType,
    structType: DeclaredTypeName
  ): ExtensionSpec {
    return ExtensionSpec.builder(structType)
        .addSuperType(codable)
        .apply {
          val codingKeys = structType.nestedType("CodingKeys")

          if (type.fieldsAndOneOfFields.isNotEmpty()) {
            // Define the keys which are the set of all direct properties and the properties within
            // each oneof.
            addType(TypeSpec.enumBuilder(codingKeys)
                .addModifiers(PUBLIC)
                .addSuperType(STRING)
                .addSuperType(codingKey)
                .apply {
                  type.fieldsAndOneOfFields.forEach { field ->
                    addEnumCase(field.name)
                  }
                }
                .build())
          }

          // If there are any oneofs we cannot rely on the built-in Codable support since the
          // keys of the nested associated enum are flattened into the enclosing parent.
          if (type.oneOfs.isNotEmpty()) {
            addFunction(FunctionSpec.constructorBuilder()
                .addParameter("from", "decoder", decoder)
                .addModifiers(PUBLIC)
                .throws(true)
                .addStatement("let container = try decoder.container(keyedBy: %T.self)", codingKeys)
                .apply {
                  type.fields.forEach { field ->
                    addStatement(
                        "self.%1N = try container.decode(%2T.self, forKey: .%1N)", field.name,
                        field.typeName
                    )
                  }
                  type.oneOfs.forEach { oneOf ->
                    oneOf.fields.forEachIndexed { index, field ->
                      if (index == 0) {
                        beginControlFlow("if", "container.contains(.%N)", field.name)
                      } else {
                        nextControlFlow("else if", "container.contains(.%N)", field.name)
                      }
                      addStatement(
                          "let %1N = try container.decode(%2T.self, forKey: .%1N)", field.name,
                          field.typeName.makeNonOptional()
                      )
                      addStatement("self.%1N = .%2N(%2N)", oneOf.name, field.name)
                    }
                    nextControlFlow("else", "")
                    addStatement("self.%N = nil", oneOf.name)
                    endControlFlow("if")
                  }
                }
                .build())
            addFunction(FunctionSpec.builder("encode")
                .addParameter("to", "encoder", encoder)
                .addModifiers(PUBLIC)
                .throws(true)
                .addStatement("var container = encoder.container(keyedBy: %T.self)", codingKeys)
                .apply {
                  type.fields.forEach { field ->
                    addStatement("try container.encode(self.%1N, forKey: .%1N)", field.name)
                  }
                  type.oneOfs.forEach { oneOf ->
                    beginControlFlow("switch", "self.%N", oneOf.name)
                    oneOf.fields.forEach { field ->
                      addStatement(
                          "case .%1N(let %1N): try container.encode(%1N, forKey: .%1N)", field.name
                      )
                    }
                    addStatement("case %T.none: break", OPTIONAL)
                    endControlFlow("switch")
                  }
                }
                .build())
          }
        }
        .build()
  }

  private fun ParameterSpec.Builder.withFieldDefault(field: Field) = apply {
    when {
      field.isMap -> defaultValue("[:]")
      field.isRepeated -> defaultValue("[]")
      field.typeName.optional -> defaultValue("nil")
    }
  }

  private fun FunctionSpec.Builder.addParameters(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    includeDefaults: Boolean = true
  ) = apply {
    type.fields.forEach { field ->
      val fieldType = field.typeName
      addParameter(ParameterSpec.builder(field.name, fieldType)
          .apply {
            if (includeDefaults) {
              withFieldDefault(field)
            }
          }
          .build())
    }
    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf).makeOptional()
      addParameter(
          ParameterSpec.builder(oneOf.name, enumName)
              .defaultValue("nil")
              .build()
      )
    }
  }

  private fun TypeSpec.Builder.generateMessageConstructor(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    includeDefaults: Boolean = true
  ) {
    addFunction(FunctionSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameters(type, oneOfEnumNames, includeDefaults)
        .apply {
          type.fields.forEach { field ->
            if (isIndirect(type, field)) {
              addStatement("_%1N = %2T(value: %1N)", field.name, indirect)
            } else {
              addStatement("self.%1N = %1N", field.name)
            }
          }
          type.oneOfs.forEach { oneOf ->
            addStatement("self.%1N = %1N", oneOf.name)
          }
        }
        .build())
  }

  private fun TypeSpec.Builder.generateMessageProperties(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    forStorageType: Boolean = false
  ) {
    type.fields.forEach { field ->
      val property = PropertySpec.varBuilder(field.name, field.typeName, PUBLIC)
      if (!forStorageType && field.documentation.isNotBlank()) {
        property.addDoc("%L\n", field.documentation.sanitizeDoc())
      }
      if (!forStorageType && field.isDeprecated) {
        property.addAttribute(deprecated)
      }
      if (field.typeName.needsJsonString()) {
        property.addAttribute("JSONString")
      }
      if (isIndirect(type, field)) {
        property.addAttribute(AttributeSpec.builder(indirect).build())
      }
      addProperty(property.build())
    }

    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      addProperty(PropertySpec.varBuilder(oneOf.name, enumName.makeOptional(), PUBLIC)
          .apply {
            if (oneOf.documentation.isNotBlank()) {
              addDoc("%N\n", oneOf.documentation.sanitizeDoc())
            }
          }
          .build())
    }

    addProperty(PropertySpec.varBuilder("unknownFields", DATA, PUBLIC)
        .initializer(".init()")
        .build())
  }

  private fun TypeSpec.Builder.generateMessageStoragePropertyDelegates(
    type: MessageType,
    storageName: String,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>
  ) {
    type.fields.forEach { field ->
      val property = PropertySpec.varBuilder(field.name, field.typeName, PUBLIC)
          .getter(FunctionSpec.getterBuilder()
              .addStatement("%N.%N", storageName, field.name)
              .build())
          .setter(FunctionSpec.setterBuilder()
              .addStatement("copyStorage()")
              .addStatement("%N.%N = newValue", storageName, field.name)
              .build())
      if (field.documentation.isNotBlank()) {
        property.addDoc("%L\n", field.documentation.sanitizeDoc())
      }
      if (field.isDeprecated) {
        property.addAttribute(
            AttributeSpec.builder("available")
                .addArguments("*", "deprecated")
                .build()
        )
      }
      addProperty(property.build())
    }

    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      addProperty(PropertySpec.varBuilder(oneOf.name, enumName.makeOptional(), PUBLIC)
          .getter(FunctionSpec.getterBuilder()
              .addStatement("%N.%N", storageName, oneOf.name)
              .build())
          .setter(FunctionSpec.setterBuilder()
              .addStatement("copyStorage()")
              .addStatement("%N.%N = newValue", storageName, oneOf.name)
              .build())
          .apply {
            if (oneOf.documentation.isNotBlank()) {
              addDoc("%N\n", oneOf.documentation.sanitizeDoc())
            }
          }
          .build())
    }

    addProperty(PropertySpec.varBuilder("unknownFields", DATA, PUBLIC)
        .getter(FunctionSpec.getterBuilder()
            .addStatement("%N.unknownFields", storageName)
            .build())
        .setter(FunctionSpec.setterBuilder()
            .addStatement("copyStorage()")
            .addStatement("%N.unknownFields = newValue", storageName)
            .build())
        .build())
  }

  private fun TypeSpec.Builder.generateMessageOneOfs(
    type: MessageType,
    oneOfEnumNames: Map<OneOf, DeclaredTypeName>,
    fileMembers: MutableList<FileMemberSpec>
  ) {
    type.oneOfs.forEach { oneOf ->
      val enumName = oneOfEnumNames.getValue(oneOf)

      // TODO use a NameAllocator
      val writer = if (oneOf.fields.any { it.name == "writer" }) "_writer" else "writer"
      addType(TypeSpec.enumBuilder(enumName)
          .addModifiers(PUBLIC)
          .apply {
            oneOf.fields.forEach { oneOfField ->
              addEnumCase(EnumerationCaseSpec.builder(oneOfField.name, oneOfField.typeName.makeNonOptional())
                  .apply {
                    if (oneOfField.documentation.isNotBlank()) {
                      addDoc("%L\n", oneOfField.documentation.sanitizeDoc())
                    }
                    if (oneOfField.isDeprecated) {
                      addAttribute(deprecated)
                    }                  }
                  .build())
            }
          }
          .addFunction(FunctionSpec.builder("encode")
              .addParameter("to", writer, protoWriter)
              .addModifiers(FILEPRIVATE)
              .throws(true)
              .beginControlFlow("switch", "self")
              .apply {
                oneOf.fields.forEach { field ->
                  addStatement(
                      "case .%1N(let %1N): try $writer.encode(tag: %2L, value: %1N)",
                      field.name, field.tag
                  )
                }
              }
              .endControlFlow("switch")
              .build())
          .build())

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

      if (oneOf.fields.any { it.isRedacted }) {
        val redactableExtension = ExtensionSpec.builder(enumName)
            .addSuperType(redactable)
            .addType(TypeSpec.enumBuilder("RedactedKeys")
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
                .build())
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
      else -> null
    }

  private fun TypeName.needsJsonString(): Boolean {
    val self = makeNonOptional()
    if (self == INT64 || self == UINT64) {
      return true
    }
    if (self is ParameterizedTypeName) {
      when (self.rawType) {
        ARRAY -> return self.typeArguments[0].needsJsonString()
        DICTIONARY -> return self.typeArguments[0].needsJsonString() ||
            self.typeArguments[1].needsJsonString()
      }
    }
    return false
  }

  private fun generateEnum(
    type: EnumType,
    fileMembers: MutableList<FileMemberSpec>
  ): TypeSpec {
    val enumName = type.typeName
    return TypeSpec.enumBuilder(enumName)
        .addModifiers(PUBLIC)
        .addSuperType(UINT32)
        .addSuperType(CASE_ITERABLE)
        .addSuperType(codable)
        .apply {
          if (type.documentation.isNotBlank()) {
            addDoc("%L\n", type.documentation.sanitizeDoc())
          }
          type.constants.forEach { constant ->
            addEnumCase(EnumerationCaseSpec.builder(constant.name, constant.tag)
                .apply {
                  if (constant.documentation.isNotBlank()) {
                    addDoc("%L\n", constant.documentation.sanitizeDoc())
                  }
                  if (constant.isDeprecated) {
                    addAttribute(deprecated)
                  }
                }
                .build())
          }
          // Swift won't synthesize CaseIterable conformance if any constants contain an availability
          // attribute. https://bugs.swift.org/browse/SR-7151
          if (type.constants.any { it.isDeprecated }) {
            addProperty(PropertySpec.varBuilder("allCases", ARRAY.parameterizedBy(enumName))
                .addModifiers(PUBLIC, STATIC)
                .getter(FunctionSpec.getterBuilder()
                    .addStatement("return [%L]", type.constants.map { CodeBlock.of(".%N", it.name) }
                        .joinToCode(",%W"))
                    .build())
                .build())
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
    fileMembers: MutableList<FileMemberSpec>
  ): TypeSpec {
    return TypeSpec.enumBuilder(type.typeName)
        .addModifiers(PUBLIC)
        .addDoc("%N\n",
            "*Note:* This type only exists to maintain class structure for its nested types and " +
                "is not an actual message.")
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
    private val BUILT_IN_TYPES = mapOf(
        ProtoType.BOOL to BOOL,
        ProtoType.BYTES to DATA,
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
        ProtoType.UINT64 to UINT64
//        ProtoType.ANY to ClassName("com.squareup.wire", "AnyMessage"),
//        Options.FIELD_OPTIONS to ClassName("com.google.protobuf", "FieldOptions"),
//        Options.MESSAGE_OPTIONS to ClassName("com.google.protobuf", "MessageOptions"),
//        Options.ENUM_OPTIONS to ClassName("com.google.protobuf", "EnumOptions")
    )

    private const val FLAG_REMOVE_CODABLE = "WIRE_REMOVE_CODABLE"
    private const val FLAG_REMOVE_EQUATABLE = "WIRE_REMOVE_EQUATABLE"
    private const val FLAG_REMOVE_HASHABLE = "WIRE_REMOVE_HASHABLE"
    private const val FLAG_REMOVE_REDACTABLE = "WIRE_REMOVE_REDACTABLE"

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      existingTypeModuleName: Map<ProtoType, String> = emptyMap()
    ): SwiftGenerator {
      val nameToTypeName = mutableMapOf<ProtoType, DeclaredTypeName>()

      fun putAll(enclosingClassName: DeclaredTypeName?, types: List<Type>) {
        for (type in types) {
          val protoType = type.type
          val name = protoType.simpleName
          val className = if (enclosingClassName != null) {
            // Temporary work around for https://bugs.swift.org/browse/SR-13160.
            enclosingClassName.nestedType(if (name == "Type") "Type_" else name)
          } else {
            val moduleName = existingTypeModuleName[protoType] ?: ""
            DeclaredTypeName(moduleName, name)
          }
          nameToTypeName[protoType] = className
          putAll(className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        putAll(null, protoFile.types)

        for (service in protoFile.services) {
          val protoType = service.type
          val moduleName = existingTypeModuleName[protoType] ?: ""
          val className = DeclaredTypeName(moduleName, protoType.simpleName)
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
      existingTypes: Set<ProtoType>
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
              type.fieldsAndOneOfFields.map { it.type!! }
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
