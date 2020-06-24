package com.squareup.wire.swift

import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Field.EncodeMode
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import io.outfoxx.swiftpoet.ARRAY
import io.outfoxx.swiftpoet.BOOL
import io.outfoxx.swiftpoet.CASE_ITERABLE
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DATA
import io.outfoxx.swiftpoet.DICTIONARY
import io.outfoxx.swiftpoet.DOUBLE
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.FLOAT
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.INT32
import io.outfoxx.swiftpoet.INT64
import io.outfoxx.swiftpoet.Modifier.FILEPRIVATE
import io.outfoxx.swiftpoet.Modifier.FINAL
import io.outfoxx.swiftpoet.Modifier.PRIVATE
import io.outfoxx.swiftpoet.Modifier.PUBLIC
import io.outfoxx.swiftpoet.OPTIONAL
import io.outfoxx.swiftpoet.ParameterSpec
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

  fun generateType(type: Type) = when (type) {
    is MessageType -> generateMessage(type)
    is EnumType -> generateEnum(type)
    is EnclosingType -> generateEnclosing(type)
    else -> error("Unknown type $type")
  }

  @OptIn(ExperimentalStdlibApi::class) // TODO move to build flag
  private fun generateMessage(type: MessageType): TypeSpec {
    val structName = type.typeName
    val oneOfEnumNames = type.oneOfs.associateWith { structName.nestedType(it.name.capitalize(US)) }

    return TypeSpec.structBuilder(structName)
        .addModifiers(PUBLIC)
        .addSuperType(equatable)
        .addSuperType(protoCodable)
        .addSuperType(codable)
        .apply {
          type.declaredFields.forEach { field ->
            addMutableProperty(field.name, field.typeName, PUBLIC)
          }
          type.oneOfs.forEach { oneOf ->
            val enumName = oneOfEnumNames.getValue(oneOf)
            addMutableProperty(oneOf.name, enumName.makeOptional(), PUBLIC)
            addType(TypeSpec.enumBuilder(enumName)
                .addModifiers(PUBLIC)
                .addSuperType(equatable)
                .apply {
                  oneOf.fields.forEach { oneOfField ->
                    addEnumCase(oneOfField.name, oneOfField.typeName.makeNonOptional())
                  }
                }
                .addFunction(FunctionSpec.builder("encode")
                    .addParameter("to", "writer", protoWriter)
                    .addModifiers(FILEPRIVATE)
                    .throws(true)
                    .beginControlFlow("switch self")
                    .apply {
                      oneOf.fields.forEach { field ->
                        addStatement(
                            "case .%1N(let %1N): try writer.encode(tag: %2L, value: %1N)",
                            field.name, field.tag
                        )
                      }
                    }
                    .endControlFlow()
                    .build())
                .build())
          }

          addProperty(PropertySpec.varBuilder("unknownFields", DATA, PUBLIC)
              .initializer(".init()")
              .build())

          val codingKeys = structName.nestedType("CodingKeys")
          if (type.fieldsAndOneOfFields.isNotEmpty()) {
            // Define the keys which are the set of all direct properties and the properties within
            // each oneof.
            addType(TypeSpec.enumBuilder(codingKeys)
                .addModifiers(PRIVATE)
                .addSuperType(STRING)
                .addSuperType(codingKey)
                .apply {
                  // TODO ideally we could use fieldsAndOneOfFields here but that includes
                  //  extension fields which don't have properties yet.
                  type.declaredFields.forEach { field ->
                    addEnumCase(field.name)
                  }
                  type.oneOfs.forEach { oneOf ->
                    oneOf.fields.forEach { field ->
                      addEnumCase(field.name)
                    }
                  }
                }
                .build())
          }

          // If there are any oneofs we cannot rely on the built-in Codable support since the
          // keys of the nested associated enum are flattened into the enclosing parent.
          // TODO are there other examples where we need to intercept encoding/decoding?
          //  Yes, 64-bit ints need to be strings!
          if (type.oneOfs.isNotEmpty()) {
            addFunction(FunctionSpec.constructorBuilder()
                .addParameter("from", "decoder", decoder)
                .addModifiers(PUBLIC)
                .throws(true)
                .addStatement("let container = try decoder.container(keyedBy: %T.self)", codingKeys)
                .apply {
                  type.declaredFields.forEach { field ->
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
                  type.declaredFields.forEach { field ->
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
        .addFunction(FunctionSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .apply {
              type.declaredFields.forEach { field ->
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
        .addFunction(FunctionSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter("from", "reader", protoReader)
            .throws(true)
            .apply {
              // Declare locals into which everything is writen before promoting to members.
              type.declaredFields.forEach { field ->
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

              addCode(CodeBlock.builder()
                  .add("let unknownFields = try reader.forEachTag { tag in\n%>")
                  .beginControlFlow("switch tag")
                  .apply {
                    type.declaredFields.forEach { field ->
                      if (field.isMap) {
                        add("case %L: try reader.decode(into: &%N", field.tag, field.name)
                        field.keyType.encoding?.let { keyEncoding ->
                          add(", keyEncoding: .%N", keyEncoding)
                        }
                        field.valueType.encoding?.let { valueEncoding ->
                          add(", valueEncoding: .%N", valueEncoding)
                        }
                        add(")\n")
                      } else if (field.isRepeated) {
                        add("case %L: try reader.decode(into: &%N)\n", field.tag, field.name)
                      } else {
                        add(
                            "case %L: %N = try reader.decode(%T.self)\n", field.tag, field.name,
                            field.typeName.makeNonOptional()
                        )
                      }
                    }
                    type.oneOfs.forEach { oneOf ->
                      oneOf.fields.forEach { field ->
                        add(
                            "case %L: %N = .%N(try reader.decode(%T.self))\n", field.tag,
                            oneOf.name, field.name, field.typeName.makeNonOptional()
                        )
                      }
                    }
                  }
                  .add("default: try reader.readUnknownField(tag: tag)\n")
                  .endControlFlow()
                  .add("%<}\n")
                  .build())

              // Check required and bind members.
              addStatement("")
              type.declaredFields.forEach { field ->
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
            .addParameter("to", "writer", protoWriter)
            .throws(true)
            .apply {
              type.declaredFields.forEach { field ->
                if (field.isMap) {
                  addCode("try writer.encode(tag: %L, value: %N", field.tag, field.name)
                  field.keyType.encoding?.let { keyEncoding ->
                    addCode(", keyEncoding: .%N", keyEncoding)
                  }
                  field.valueType.encoding?.let { valueEncoding ->
                    addCode(", valueEncoding: .%N", valueEncoding)
                  }
                  addCode(")\n")
                } else {
                  addCode("try writer.encode(tag: %L, value: %N", field.tag, field.name)
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
                addStatement("try %N.encode(to: writer)", oneOf.name)
                endControlFlow()
              }
            }
            .addStatement("try writer.writeUnknownFields(unknownFields)")
            .build())
        .apply {
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType))
          }
        }
        .build()
  }

  private val ProtoType.encoding: String?
    get() = when (this) {
      ProtoType.SINT32, ProtoType.SINT64 -> "signed"
      ProtoType.FIXED32, ProtoType.FIXED64 -> "fixed"
      else -> null
    }

  private fun generateEnum(type: EnumType): TypeSpec {
    val enumName = type.typeName
    return TypeSpec.enumBuilder(enumName)
        .addModifiers(PUBLIC)
        .addSuperType(UINT32)
        .addSuperType(CASE_ITERABLE)
        .addSuperType(codable)
        .apply {
          type.constants.forEach { constant ->
            addEnumCase(constant.name, constant.tag.toString())
          }
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType))
          }
        }
        .build()
  }

  private fun generateEnclosing(type: EnclosingType): TypeSpec {
    return TypeSpec.classBuilder(type.typeName)
        .addModifiers(PUBLIC, FINAL)
        .apply {
          type.nestedTypes.forEach { nestedType ->
            addType(generateType(nestedType))
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
        ProtoType.FIXED32 to INT32,
        ProtoType.FIXED64 to INT64,
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
    operator fun invoke(schema: Schema): SwiftGenerator {
      val map = mutableMapOf<ProtoType, DeclaredTypeName>()

      fun putAll(moduleName: String, enclosingClassName: DeclaredTypeName?, types: List<Type>) {
        for (type in types) {
          val className = enclosingClassName?.nestedType(type.type.simpleName)
              ?: DeclaredTypeName(moduleName, type.type.simpleName)
          map[type.type] = className
          putAll(moduleName, className, type.nestedTypes)
        }
      }

      for (protoFile in schema.protoFiles) {
        val moduleName = protoFile.swiftModule()
        putAll(moduleName, null, protoFile.types)

        for (service in protoFile.services) {
          val className = DeclaredTypeName(moduleName, service.type().simpleName)
          map[service.type()] = className
        }
      }

      map.putAll(BUILT_IN_TYPES)

      return SwiftGenerator(schema, map)
    }
  }
}

private fun ProtoFile.swiftModule() = javaPackage() ?: packageName ?: ""
