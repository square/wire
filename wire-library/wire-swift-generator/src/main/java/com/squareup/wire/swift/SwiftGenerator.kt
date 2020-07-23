package com.squareup.wire.swift

import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Field.EncodeMode
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import io.outfoxx.swiftpoet.ARRAY
import io.outfoxx.swiftpoet.AttributeSpec
import io.outfoxx.swiftpoet.BOOL
import io.outfoxx.swiftpoet.CASE_ITERABLE
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DATA
import io.outfoxx.swiftpoet.DICTIONARY
import io.outfoxx.swiftpoet.DOUBLE
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.ExtensionSpec
import io.outfoxx.swiftpoet.FLOAT
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.INT32
import io.outfoxx.swiftpoet.INT64
import io.outfoxx.swiftpoet.Modifier.FILEPRIVATE
import io.outfoxx.swiftpoet.Modifier.FINAL
import io.outfoxx.swiftpoet.Modifier.PUBLIC
import io.outfoxx.swiftpoet.OPTIONAL
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.ParameterizedTypeName
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.TypeSpec
import io.outfoxx.swiftpoet.UINT32
import io.outfoxx.swiftpoet.UINT64
import io.outfoxx.swiftpoet.parameterizedBy
import java.util.Locale.US

class SwiftGenerator private constructor(
  val schema: Schema,
  private val nameToTypeName: Map<ProtoType, DeclaredTypeName>
) {
  private val protoCodable = DeclaredTypeName.typeName("Wire.Proto2Codable")
  private val protoReader = DeclaredTypeName.typeName("Wire.ProtoReader")
  private val protoWriter = DeclaredTypeName.typeName("Wire.ProtoWriter")
  private val equatable = DeclaredTypeName.typeName("Swift.Equatable")
  private val codable = DeclaredTypeName.typeName("Swift.Codable")
  private val codingKey = DeclaredTypeName.typeName("Swift.CodingKey")
  private val encoder = DeclaredTypeName.typeName("Swift.Encoder")
  private val decoder = DeclaredTypeName.typeName("Swift.Decoder")

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
    val extensions = mutableListOf<ExtensionSpec>()
    builder.addType(generateType(type, extensions))

    for (extension in extensions) {
      builder.addExtension(extension)
    }
  }

  private fun generateType(type: Type, extensions: MutableList<ExtensionSpec>) = when (type) {
    is MessageType -> generateMessage(type, extensions)
    is EnumType -> generateEnum(type, extensions)
    is EnclosingType -> generateEnclosing(type, extensions)
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

  @OptIn(ExperimentalStdlibApi::class) // TODO move to build flag
  private fun generateMessage(type: MessageType, extensions: MutableList<ExtensionSpec>): TypeSpec {
    val structName = type.typeName
    val oneOfEnumNames = type.oneOfs.associateWith { structName.nestedType(it.name.capitalize(US)) }

    val typeSpec =  TypeSpec.structBuilder(structName)
        .addModifiers(PUBLIC)
        .apply {
          if (type.documentation.isNotBlank()) {
            addKdoc("%L\n", type.documentation.sanitizeDoc())
          }
          type.fields.forEach { field ->
            val property = PropertySpec.varBuilder(field.name, field.typeName, PUBLIC)
            if (field.documentation.isNotBlank()) {
              property.addKdoc("%L\n", field.documentation.sanitizeDoc())
            }
            if (field.isDeprecated) {
              property.addAttribute(
                  AttributeSpec.builder("available")
                      .addArguments("*", "deprecated")
                      .build()
              )
            }
            if (field.typeName.needsJsonString()) {
              property.addAttribute(
                  AttributeSpec.builder("JSONString")
                      .build()
              )
            }
            addProperty(property.build())
          }
          type.oneOfs.forEach { oneOf ->
            val enumName = oneOfEnumNames.getValue(oneOf)

            addProperty(PropertySpec.varBuilder(oneOf.name, enumName.makeOptional(), PUBLIC)
                .apply {
                  if (oneOf.documentation.isNotBlank()) {
                    addKdoc("%N\n", oneOf.documentation.sanitizeDoc())
                  }
                }
                .build())

            // TODO use a NameAllocator
            val writer = if (oneOf.fields.any { it.name == "writer" }) "_writer" else "writer"
            addType(TypeSpec.enumBuilder(enumName)
                .addModifiers(PUBLIC)
                .apply {
                  oneOf.fields.forEach { oneOfField ->
                    // TODO SwiftPoet needs to support attributing an enum case.
                    // TODO SwiftPoet needs to support documenting an enum case.
                    addEnumCase(oneOfField.name, oneOfField.typeName.makeNonOptional())
                  }
                }
                .addFunction(FunctionSpec.builder("encode")
                    .addParameter("to", writer, protoWriter)
                    .addModifiers(FILEPRIVATE)
                    .throws(true)
                    .beginControlFlow("switch self")
                    .apply {
                      oneOf.fields.forEach { field ->
                        addStatement(
                            "case .%1N(let %1N): try $writer.encode(tag: %2L, value: %1N)",
                            field.name, field.tag
                        )
                      }
                    }
                    .endControlFlow()
                    .build())
                .build())

            extensions += ExtensionSpec.builder(enumName)
                .addSuperType(equatable)
                .build()
          }
        }
        .addProperty(PropertySpec.varBuilder("unknownFields", DATA, PUBLIC)
              .initializer(".init()")
              .build())
        .addFunction(FunctionSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .apply {
              type.fields.forEach { field ->
                val fieldType = field.typeName
                addParameter(ParameterSpec.builder(field.name, fieldType)
                    .apply {
                      when {
                        field.isMap -> defaultValue("[:]")
                        field.isRepeated -> defaultValue("[]")
                        fieldType.optional -> defaultValue("nil")
                      }
                    }
                    .build())
                addStatement("self.%1N = %1N", field.name)
              }
              type.oneOfs.forEach { oneOf ->
                val enumName = oneOfEnumNames.getValue(oneOf).makeOptional()
                addParameter(ParameterSpec.builder(oneOf.name, enumName)
                    .defaultValue("nil")
                    .build())
                addStatement("self.%1N = %1N", oneOf.name)
              }
            }
            .build())
        .apply {
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType, extensions))
          }
        }
        .build()

    extensions += ExtensionSpec.builder(structName)
        .addSuperType(equatable)
        .build()

    // TODO use a NameAllocator
    val propertyNames = type.fields.map { it.name } + type.oneOfs.map { it.name }
    val reader = if ("reader" in propertyNames) "_reader" else "reader"
    val writer = if ("writer" in propertyNames) "_writer" else "writer"
    val token = if ("token" in propertyNames) "_token" else "token"
    val tag = if ("tag" in propertyNames) "_tag" else "tag"
    extensions += ExtensionSpec.builder(structName)
        .addSuperType(protoCodable)
        .addFunction(FunctionSpec.constructorBuilder()
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
              beginControlFlow("while let $tag = try $reader.nextTag(token: $token)")
              addCode(CodeBlock.builder()
                  .add("switch $tag {\n")
                  .apply {
                    type.fields.forEach { field ->
                      add("case %L: ", field.tag)
                      if (field.isMap) {
                        add("try $reader.decode(into: &%N", field.name)
                        field.keyType.encoding?.let { keyEncoding ->
                          add(", keyEncoding: .%N", keyEncoding)
                        }
                        field.valueType.encoding?.let { valueEncoding ->
                          add(", valueEncoding: .%N", valueEncoding)
                        }
                      } else {
                        if (field.isRepeated) {
                          add("try $reader.decode(into: &%N", field.name)
                        } else {
                          add(
                              "%N = try $reader.decode(%T.self", field.name,
                              field.typeName.makeNonOptional()
                          )
                        }
                        field.type!!.encoding?.let { encoding ->
                          add(", encoding: .%N", encoding)
                        }
                      }
                      add(")\n")
                    }
                    type.oneOfs.forEach { oneOf ->
                      oneOf.fields.forEach { field ->
                        add(
                            "case %L: %N = .%N(try $reader.decode(%T.self))\n", field.tag,
                            oneOf.name, field.name, field.typeName.makeNonOptional()
                        )
                      }
                    }
                  }
                  .add("default: try $reader.readUnknownField(tag: $tag)\n")
                  .add("}\n")
                  .build())
              endControlFlow()
              addStatement("let unknownFields = try $reader.endMessage(token: $token)")

              // Check required and bind members.
              addStatement("")
              type.fields.forEach { field ->
                val initializer = if (field.typeName.optional) {
                  CodeBlock.of("%N", field.name)
                } else {
                  CodeBlock.of("try %1T.checkIfMissing(%2N, %2S)", structName, field.name)
                }
                addStatement("self.%N = %L", field.name, initializer)
              }
              type.oneOfs.forEach { oneOf ->
                addStatement("self.%1N = %1N", oneOf.name)
              }
              addStatement("self.unknownFields = unknownFields")
            }
            .build())
        .addFunction(FunctionSpec.builder("encode")
            .addModifiers(PUBLIC)
            .addParameter("to", writer, protoWriter)
            .throws(true)
            .apply {
              type.fields.forEach { field ->
                if (field.isMap) {
                  addCode("try $writer.encode(tag: %L, value: %N", field.tag, field.name)
                  field.keyType.encoding?.let { keyEncoding ->
                    addCode(", keyEncoding: .%N", keyEncoding)
                  }
                  field.valueType.encoding?.let { valueEncoding ->
                    addCode(", valueEncoding: .%N", valueEncoding)
                  }
                  addCode(")\n")
                } else {
                  addCode("try $writer.encode(tag: %L, value: %N", field.tag, field.name)
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
                beginControlFlow("if let %1N = %1N", oneOf.name)
                addStatement("try %N.encode(to: $writer)", oneOf.name)
                endControlFlow()
              }
            }
            .addStatement("try $writer.writeUnknownFields(unknownFields)")
            .build())
        .build()

    extensions += ExtensionSpec.builder(structName)
        .addSuperType(codable)
        .apply {
          val codingKeys = structName.nestedType("CodingKeys")

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
                        "%1N = try container.decode(%2T.self, forKey: .%1N)", field.name,
                        field.typeName
                    )
                  }
                  type.oneOfs.forEach { oneOf ->
                    oneOf.fields.forEachIndexed { index, field ->
                      if (index == 0) {
                        beginControlFlow("if (container.contains(.%N))", field.name)
                      } else {
                        nextControlFlow("else if (container.contains(.%N))", field.name)
                      }
                      addStatement(
                          "let %1N = try container.decode(%2T.self, forKey: .%1N)", field.name,
                          field.typeName.makeNonOptional()
                      )
                      addStatement("self.%1N = .%2N(%2N)", oneOf.name, field.name)
                    }
                    nextControlFlow("else")
                    addStatement("self.%N = nil", oneOf.name)
                    endControlFlow()
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
                    addStatement("try container.encode(%1N, forKey: .%1N)", field.name)
                  }
                  type.oneOfs.forEach { oneOf ->
                    beginControlFlow("switch self.%N", oneOf.name)
                    oneOf.fields.forEach { field ->
                      addStatement(
                          "case .%1N(let %1N): try container.encode(%1N, forKey: .%1N)", field.name
                      )
                    }
                    addStatement("case %T.none: break", OPTIONAL)
                    endControlFlow()
                  }
                }
                .build())
          }
        }
        .build()

    return typeSpec
  }

  private val ProtoType.encoding: String?
    get() = when (this) {
      ProtoType.SINT32, ProtoType.SINT64 -> "signed"
      ProtoType.FIXED32, ProtoType.FIXED64 -> "fixed"
      else -> null
    }

  private fun TypeName.needsJsonString(): Boolean {
    if (this == INT64 || this == UINT64) {
      return true
    }
    if (this is ParameterizedTypeName) {
      when (rawType) {
        ARRAY -> return typeArguments[0].needsJsonString()
        DICTIONARY -> return typeArguments[0].needsJsonString() || typeArguments[1].needsJsonString()
      }
    }
    return false
  }

  private fun generateEnum(type: EnumType, extensions: MutableList<ExtensionSpec>): TypeSpec {
    val enumName = type.typeName
    return TypeSpec.enumBuilder(enumName)
        .addModifiers(PUBLIC)
        .addSuperType(UINT32)
        .addSuperType(CASE_ITERABLE)
        .addSuperType(codable)
        .apply {
          if (type.documentation.isNotBlank()) {
            addKdoc("%L\n", type.documentation.sanitizeDoc())
          }
          type.constants.forEach { constant ->
            // TODO SwiftPoet needs to support attributing an enum case.
            // TODO SwiftPoet needs to support documenting an enum case.
            addEnumCase(constant.name, constant.tag.toString())
          }
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType, extensions))
          }
        }
        .build()
  }

  private fun generateEnclosing(type: EnclosingType, extensions: MutableList<ExtensionSpec>): TypeSpec {
    return TypeSpec.classBuilder(type.typeName)
        .addModifiers(PUBLIC, FINAL)
        .addKdoc("%N\n",
            "*Note:* This type only exists to maintain class structure for its nested types and " +
                "is not an actual message.")
        .apply {
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType, extensions))
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

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      existingTypeModuleName: Map<ProtoType, String> = emptyMap()
    ): SwiftGenerator {
      val map = mutableMapOf<ProtoType, DeclaredTypeName>()

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
          map[protoType] = className
          putAll(className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        putAll(null, protoFile.types)

        for (service in protoFile.services) {
          val protoType = service.type()
          val moduleName = existingTypeModuleName[protoType] ?: ""
          val className = DeclaredTypeName(moduleName, protoType.simpleName)
          map[protoType] = className
        }
      }

      map.putAll(BUILT_IN_TYPES)

      return SwiftGenerator(schema, map)
    }
  }
}
