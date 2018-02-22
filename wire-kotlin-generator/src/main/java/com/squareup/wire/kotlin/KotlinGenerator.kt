package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.Message
import com.squareup.wire.WireEnum
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Options.ENUM_VALUE_OPTIONS
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoMember
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import okio.ByteString

class KotlinGenerator private constructor(
    val schema: Schema,
    private val nameToKotlinName: Map<ProtoType, ClassName>,
    val emitAndroid: Boolean
) {
  /** Returns the Kotlin type for [protoType]. */
  fun typeName(protoType: ProtoType) = nameToKotlinName[protoType]

  /** Returns the full name of the class generated for `type`.  */
  fun generatedTypeName(type: Type) = typeName(type.type())

  fun generateType(type: Type): TypeSpec = when (type) {
    is MessageType -> generateMessage(type)
    is EnumType -> generateEnum(type)
    is EnclosingType -> generateEnclosingType(type)
    else -> error("Unknown type $type")
  }

  private fun generateMessage(type: MessageType): TypeSpec {
    val kotlinType = requireNotNull(typeName(type.type())) { "Unknown type $type" }
    val builderJavaType = kotlinType.nestedClass("Builder")

    val builder = TypeSpec.classBuilder(type.type().simpleName())

    if (!type.documentation().isEmpty()) {
      builder.addKdoc("%L\n", type.documentation())
    }

    val messageType = if (emitAndroid) ANDROID_MESSAGE else MESSAGE
    builder.superclass(ParameterizedTypeName.get(messageType, kotlinType, builderJavaType))

    builder.addType(generateMessageBuilder(type, kotlinType, builderJavaType))

    for (nestedType in type.nestedTypes()) {
      builder.addType(generateType(nestedType))
    }

    return builder.build()
  }

  private fun generateMessageBuilder(
      type: MessageType,
      kotlinType: ClassName,
      builderType: ClassName
  ): TypeSpec {
    val builder = TypeSpec.classBuilder("Builder")
        .superclass(ParameterizedTypeName.get(MESSAGE_BUILDER, kotlinType, builderType))

    return builder.build()
  }

  private fun generateEnum(type: EnumType): TypeSpec {
    val builder = TypeSpec.enumBuilder(type.type().simpleName())
        .addSuperinterface(WireEnum::class)

    if (!type.documentation().isEmpty()) {
      builder.addKdoc("%L\n", type.documentation())
    }

    builder.addProperty(PropertySpec.builder("value", INT, OVERRIDE)
        .initializer("%N", "value")
        .build())

    builder.primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("value", INT)
        .build())

    for (constant in type.constants()) {
      val constantBuilder = TypeSpec.anonymousClassBuilder()

      if (!constant.documentation().isEmpty()) {
        constantBuilder.addKdoc("%L\n", constant.documentation())
      }

      if ("true" == constant.options().get(ENUM_DEPRECATED)) {
        constantBuilder.addAnnotation(AnnotationSpec.builder(Deprecated::class)
            .addMember("%S", "")
            .build())
      }

      builder.addEnumConstant(constant.name(), constantBuilder.build())
    }

    return builder.build()
  }

  private fun generateEnclosingType(type: EnclosingType): TypeSpec {
    val kotlinType = requireNotNull(typeName(type.type())) { "Unknown type $type" }

    val builder = TypeSpec.classBuilder(kotlinType.simpleName())
        .addModifiers(FINAL)

    var documentation = type.documentation()
    if (!documentation.isEmpty()) {
      documentation += "\n\n<p>"
    }
    documentation += "<b>NOTE:</b> This type only exists to maintain class structure" + " for its nested types and is not an actual message."
    builder.addKdoc("%L\n", documentation)

    builder.primaryConstructor(FunSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addStatement("throw new \$T()", AssertionError::class)
        .build())

    for (nestedType in type.nestedTypes()) {
      builder.addType(generateType(nestedType))
    }

    return builder.build()
  }

  companion object {
    private val MESSAGE = Message::class.asClassName()
    private val ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage")
    private val MESSAGE_BUILDER = Message.Builder::class.asClassName()

    private val ENUM_DEPRECATED = ProtoMember.get(ENUM_VALUE_OPTIONS, "deprecated")

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
    operator fun invoke(schema: Schema, emitAndroid: Boolean): KotlinGenerator {
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
