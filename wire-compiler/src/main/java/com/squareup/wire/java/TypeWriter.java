/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.java;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import com.squareup.wire.WireCompilerException;
import com.squareup.wire.model.ProtoTypeName;
import com.squareup.wire.model.WireEnum;
import com.squareup.wire.model.WireEnumConstant;
import com.squareup.wire.model.WireExtend;
import com.squareup.wire.model.WireField;
import com.squareup.wire.model.WireMessage;
import com.squareup.wire.model.WireOneOf;
import com.squareup.wire.model.WireProtoFile;
import com.squareup.wire.model.WireType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okio.ByteString;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public final class TypeWriter {
  private static final ImmutableSet<String> JAVA_KEYWORDS = ImmutableSet.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
      "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
      "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while");

  private final JavaGenerator javaGenerator;
  private final boolean emitOptions;

  public TypeWriter(JavaGenerator javaGenerator, boolean emitOptions) {
    this.javaGenerator = javaGenerator;
    this.emitOptions = emitOptions;
  }

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec toTypeSpec(WireType type) {
    if (type instanceof WireMessage) {
      return toTypeSpec((WireMessage) type);
    } else if (type instanceof WireEnum) {
      return toTypeSpec((WireEnum) type);
    } else {
      throw new IllegalArgumentException("unexpected type: " + type);
    }
  }

  private TypeSpec toTypeSpec(WireEnum type) {
    ClassName typeName = (ClassName) javaGenerator.typeName(type.protoTypeName());

    TypeSpec.Builder builder = TypeSpec.enumBuilder(typeName.simpleName())
        .addModifiers(PUBLIC)
        .addSuperinterface(ProtoEnum.class);

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", type.documentation());
    }

    // TODO(jwilson): options.

    for (WireEnumConstant constant : type.constants()) {
      Object[] enumArgs = new Object[1];
      String[] enumArgsFormat = new String[1];

      enumArgs[0] = constant.tag();
      enumArgsFormat[0] = "$L";

      TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder(
          Joiner.on(", ").join(enumArgsFormat), enumArgs);
      if (!constant.documentation().isEmpty()) {
        constantBuilder.addJavadoc("$L\n", constant.documentation());
      }

      builder.addEnumConstant(constant.name(), constantBuilder.build());

      // TODO(jwilson): initializers.
    }

    // Output Private tag field
    builder.addField(TypeName.INT, "value", PRIVATE, FINAL);

    // TODO(jwilson): options.

    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    constructorBuilder.addParameter(TypeName.INT, "value");
    constructorBuilder.addStatement("this.value = value");
    // TODO(jwilson): private constructor extensions.
    builder.addMethod(constructorBuilder.build());

    // Public Getter
    builder.addMethod(MethodSpec.methodBuilder("getValue")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(TypeName.INT)
        .addStatement("return value")
        .build());

    return builder.build();
  }

  private TypeSpec toTypeSpec(WireMessage type) {
    ClassName javaType = (ClassName) javaGenerator.typeName(type.protoTypeName());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName());
    builder.addModifiers(PUBLIC, FINAL);

    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", type.documentation());
    }

    builder.superclass(type.extensions().isEmpty()
        ? JavaGenerator.MESSAGE
        : JavaGenerator.extendableMessageOf(javaType));

    builder.addField(FieldSpec.builder(TypeName.LONG, "serialVersionUID")
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$LL", 0L)
        .build());

    if (emitOptions) {
      for (WireField field : type.fieldsAndOneOfFields()) {
        FieldSpec options = fieldOptionsField(field);
        if (options != null) {
          builder.addField(options);
        }
      }
    }

    for (WireField field : type.fieldsAndOneOfFields()) {
      TypeName fieldType = fieldType(field);

      if (field.type().isScalar()
          || javaGenerator.isEnum(field.type())
          || field.isRepeated()) {
        builder.addField(defaultField(field, fieldType));
      }

      String name = sanitize(field.name());
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, name, PUBLIC, FINAL);
      fieldBuilder.addAnnotation(protoFieldAnnotation(field, javaGenerator.typeName(field.type())));
      if (!field.documentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", field.documentation());
      }
      if (field.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      builder.addField(fieldBuilder.build());
    }

    builder.addMethod(messageFieldsConstructor(type));
    builder.addMethod(messageBuilderConstructor(type, builderJavaType));
    builder.addMethod(messageEquals(type));
    builder.addMethod(messageHashCode(type));
    builder.addType(builder(type, javaType, builderJavaType));

    for (WireType nestedType : type.nestedTypes()) {
      builder.addType(toTypeSpec(nestedType));
    }

    return builder.build();
  }


  // Example:
  //
  // public static final FieldOptions FIELD_OPTIONS_FOO = new FieldOptions.Builder()
  //     .setExtension(Ext_custom_options.count, 42)
  //     .build();
  //
  private FieldSpec fieldOptionsField(WireField field) {
    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add("$[new $T.Builder()", JavaGenerator.FIELD_OPTIONS);

    boolean empty = true;
    for (Map.Entry<WireField, ?> entry : field.options().map().entrySet()) {
      WireField extensionRoot = entry.getKey();
      if (extensionRoot.name().equals("default")
          || extensionRoot.name().equals("deprecated")
          || extensionRoot.name().equals("packed")) {
        continue; // TODO(jwilson): also check that the declaring types match.
      }

      ClassName extensionClass = javaGenerator.extensionsClass(extensionRoot);
      initializer.add("\n.setExtension($T.$L, $L)", extensionClass, extensionRoot.name(),
          fieldInitializer(extensionRoot.type(), entry.getValue()));
      empty = false;
    }
    initializer.add("\n.build()$]");
    if (empty) return null;

    String optionsFieldName = "FIELD_OPTIONS_" + field.name().toUpperCase(Locale.US);
    return FieldSpec.builder(JavaGenerator.FIELD_OPTIONS, optionsFieldName)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer(initializer.build())
        .build();
  }

  private TypeName fieldType(WireField field) {
    TypeName messageType = javaGenerator.typeName(field.type());
    return field.isRepeated() ? JavaGenerator.listOf(messageType) : messageType;
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private FieldSpec defaultField(WireField field, TypeName fieldType) {
    String defaultFieldName = "DEFAULT_" + field.name().toUpperCase(Locale.US);
    return FieldSpec.builder(fieldType, defaultFieldName, PUBLIC, STATIC, FINAL)
        .initializer(defaultValue(field))
        .build();
  }

  // Example:
  //
  // @ProtoField(
  //   tag = 1,
  //   type = INT32
  // )
  //
  private AnnotationSpec protoFieldAnnotation(WireField field, TypeName messageType) {
    AnnotationSpec.Builder result = AnnotationSpec.builder(ProtoField.class);

    int tag = field.tag();
    result.addMember("tag", String.valueOf(tag));

    boolean isScalar = field.type().isScalar();
    boolean isEnum = javaGenerator.isEnum(field.type());

    String fieldType;
    if (isScalar) {
      fieldType = field.type().toString().toUpperCase(Locale.US);
    } else if (isEnum) {
      fieldType = "ENUM";
    } else {
      fieldType = null;
    }

    if (fieldType != null) {
      result.addMember("type", "$T.$L", Message.Datatype.class, fieldType);
    }

    if (!field.isOptional()) {
      String label;
      if (field.isPacked() && (isEnum || field.type().isPackableScalar())) {
        label = "PACKED";
      } else {
        label = field.label().toString();
      }
      result.addMember("label", "$T.$L", Message.Label.class, label);
    }

    if (field.isRepeated() && !isScalar) {
      String key = isEnum ? "enumType" : "messageType";
      result.addMember(key, "$T.class", messageType);
    }

    if (field.isDeprecated()) {
      result.addMember("deprecated", "true");
    }

    // We allow any package name to be used as long as it ends with '.redacted'.
    if (field.options().optionMatches(".*\\.redacted", "true")) {
      result.addMember("redacted", "true");
    }

    return result.build();
  }

  // Example:
  //
  // public SimpleMessage(int optional_int32, long optional_int64) {
  //   this.optional_int32 = optional_int32;
  //   this.optional_int64 = optional_int64;
  // }
  //
  private MethodSpec messageFieldsConstructor(WireMessage type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder();
    result.addModifiers(PUBLIC);
    for (WireField field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String sanitizedName = sanitize(field.name());
      result.addParameter(javaType, sanitizedName);
      if (field.isRepeated()) {
        result.addStatement("this.$L = immutableCopyOf($L)", sanitizedName, sanitizedName);
      } else {
        result.addStatement("this.$L = $L", sanitizedName, sanitizedName);
      }
    }
    return result.build();
  }

  // Example:
  //
  // private SimpleMessage(Builder builder) {
  //   this(builder.optional_int32, builder.optional_int64);
  //   setBuilder(builder);
  // }
  //
  private MethodSpec messageBuilderConstructor(WireMessage type, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addParameter(builderJavaType, "builder");

    List<WireField> fields = type.fieldsAndOneOfFields();
    if (fields.size() > 0) {
      result.addCode("this(");
      for (int i = 0; i < fields.size(); i++) {
        if (i > 0) result.addCode(", ");
        WireField field = fields.get(i);
        result.addCode("builder.$L", sanitize(field.name()));
      }
      result.addCode(");\n");
    }
    result.addStatement("setBuilder(builder)");
    return result.build();
  }

  // Example:
  //
  // @Override
  // public boolean equals(Object other) {
  //   if (other == this) return true;
  //   if (!(other instanceof SimpleMessage)) return false;
  //   SimpleMessage o = (SimpleMessage) other;
  //   if (!Wire.equals(optional_int32, o.optional_int32)) return false;
  //   return true;
  //
  private MethodSpec messageEquals(WireMessage type) {
    TypeName javaType = javaGenerator.typeName(type.protoTypeName());
    MethodSpec.Builder result = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(boolean.class)
        .addParameter(Object.class, "other");

    List<WireField> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty() && type.extensions().isEmpty()) {
      result.addStatement("return other instanceof $T", javaType);
      return result.build();
    }

    result.addStatement("if (other == this) return true");
    result.addStatement("if (!(other instanceof $T)) return false", javaType);

    if (fields.size() == 1 && type.extensions().isEmpty()) {
      String name = sanitize(fields.get(0).name());
      result.addStatement("return equals($L, (($T) other).$L)",
          addThisIfOneOf(name, "other", "o"), javaType, name);
      return result.build();
    }

    result.addStatement("$T o = ($T) other", javaType, javaType);
    if (!type.extensions().isEmpty()) {
      result.addStatement("if (!extensionsEqual(o)) return false");
    }
    result.addCode("$[return ");
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) result.addCode("\n&& ");
      WireField field = fields.get(i);
      String name = sanitize(field.name());
      result.addCode("equals($L, o.$L)", addThisIfOneOf(name, "other", "o"), name);
    }
    result.addCode(";\n$]");

    return result.build();
  }

  // Example:
  //
  // @Override
  // public int hashCode() {
  //   if (hashCode == 0) {
  //     int result = super.extensionsHashCode();
  //     result = result * 37 + (f != null ? f.hashCode() : 0);
  //     hashCode = result;
  //   }
  //   return hashCode;
  // }
  //
  // For repeated fields, the final "0" in the example above changes to a "1"
  // in order to be the same as the system hash code for an empty list.
  //
  private MethodSpec messageHashCode(WireMessage type) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("hashCode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class);

    List<WireField> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty() && type.extensions().isEmpty()) {
      result.addStatement("return 0");
      return result.build();
    }

    if (fields.size() == 1 && type.extensions().isEmpty()) {
      WireField field = fields.get(0);
      String name = sanitize(field.name());
      result.addStatement("int result = hashCode");
      result.addStatement(
          "return result != 0 ? result : (hashCode = $L != null ? $L.hashCode() : $L)",
          addThisIfOneOf(name, "result"), addThisIfOneOf(name, "result"), nullHashValue(field));
      return result.build();
    }

    result.addStatement("int result = hashCode");
    result.beginControlFlow("if (result == 0)");
    boolean afterFirstAssignment = false;
    if (!type.extensions().isEmpty()) {
      result.addStatement("result = extensionsHashCode()");
      afterFirstAssignment = true;
    }
    for (WireField field : fields) {
      String name = sanitize(field.name());
      name = addThisIfOneOf(name, "result");
      if (afterFirstAssignment) {
        result.addStatement("result = result * 37 + ($L != null ? $L.hashCode() : $L)",
            name, name, nullHashValue(field));
      } else {
        result.addStatement("result = $L != null ? $L.hashCode() : $L",
            name, name, nullHashValue(field));
        afterFirstAssignment = true;
      }
    }
    result.addStatement("hashCode = result");
    result.endControlFlow();
    result.addStatement("return result");
    return result.build();
  }

  private TypeSpec builder(WireMessage type, ClassName javaType, ClassName builderType) {
    TypeSpec.Builder result = TypeSpec.classBuilder("Builder")
        .addModifiers(PUBLIC, STATIC, FINAL);

    result.superclass(type.extensions().isEmpty()
        ? JavaGenerator.builderOf(javaType)
        : JavaGenerator.extendableBuilderOf(javaType));

    List<WireField> fields = type.fieldsAndOneOfFields();
    for (WireField field : fields) {
      TypeName fieldJavaType = fieldType(field);
      result.addField(fieldJavaType, sanitize(field.name()), PUBLIC);
    }

    result.addMethod(builderNoArgsConstructor());
    result.addMethod(builderCopyConstructor(type));

    for (WireField field : type.fields()) {
      result.addMethod(setter(builderType, null, field));
    }

    for (WireOneOf oneOf : type.oneOfs()) {
      for (WireField field : oneOf.fields()) {
        result.addMethod(setter(builderType, oneOf, field));
      }
    }

    if (!type.extensions().isEmpty()) {
      result.addMethod(builderSetExtension(javaType, builderType));
    }

    result.addMethod(builderBuild(type, javaType));
    return result.build();
  }

  // Example:
  //
  // public Builder() {
  // }
  //
  private MethodSpec builderNoArgsConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .build();
  }

  // Example:
  //
  // public Builder(SimpleMessage message) {
  //   super(message);
  //   if (message == null) return;
  //   this.optional_int32 = message.optional_int32;
  //   ...
  // }
  //
  private MethodSpec builderCopyConstructor(WireMessage message) {
    TypeName javaType = javaGenerator.typeName(message.protoTypeName());

    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(javaType, "message");
    result.addStatement("super(message)");

    List<WireField> fields = message.fieldsAndOneOfFields();
    if (!fields.isEmpty()) {
      result.addStatement("if (message == null) return");
    }

    for (WireField field : fields) {
      String fieldName = sanitize(field.name());
      if (field.isRepeated()) {
        result.addStatement("this.$L = copyOf(message.$L)", fieldName, fieldName);
      } else {
        result.addStatement("this.$L = message.$L", fieldName, fieldName);
      }
    }

    return result.build();
  }

  private MethodSpec setter(TypeName builderType, WireOneOf oneOf, WireField field) {
    TypeName javaType = fieldType(field);
    String fieldName = sanitize(field.name());

    MethodSpec.Builder result = MethodSpec.methodBuilder(fieldName)
        .addModifiers(PUBLIC)
        .addParameter(javaType, fieldName)
        .returns(builderType);

    if (!field.documentation().isEmpty()) {
      result.addJavadoc("$L\n", field.documentation());
    }

    if (field.isDeprecated()) {
      result.addAnnotation(Deprecated.class);
    }

    if (field.isRepeated()) {
      result.addStatement("this.$L = checkForNulls($L)", fieldName, fieldName);
    } else {
      result.addStatement("this.$L = $L", fieldName, fieldName);

      if (oneOf != null) {
        for (WireField other : oneOf.fields()) {
          if (field != other) {
            result.addStatement("this.$L = null", sanitize(other.name()));
          }
        }
      }
    }

    result.addStatement("return this");
    return result.build();
  }

  // Example:
  //
  // @Override
  // public <E> Builder setExtension(Extension<ExternalMessage, E> extension, E value) {
  //   super.setExtension(extension, value);
  //   return this;
  // }
  //
  private MethodSpec builderSetExtension(ClassName javaType, ClassName builderType) {
    TypeVariableName e = TypeVariableName.get("E");
    return MethodSpec.methodBuilder("setExtension")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addTypeVariable(e)
        .returns(builderType)
        .addParameter(JavaGenerator.extensionOf(javaType, e), "extension")
        .addParameter(e, "value")
        .addStatement("super.setExtension(extension, value)")
        .addStatement("return this")
        .build();
  }

  // Example:
  //
  // @Override
  // public SimpleMessage build() {
  //   checkRequiredFields();
  //   return new SimpleMessage(this);
  // }
  //
  // The call to checkRequiredFields will be emitted only if the message has
  // required fields.
  //
  private MethodSpec builderBuild(WireMessage message, ClassName javaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType);
    if (message.hasRequiredFields()) {
      result.addStatement("checkRequiredFields()");
    }
    result.addStatement("return new $T(this)", javaType);
    return result.build();
  }

  private CodeBlock defaultValue(WireField field) {
    if (field.isRepeated()) {
      return codeBlock("$T.emptyList()", Collections.class);
    }

    Object defaultValue = field.getDefault();

    if (defaultValue == null && javaGenerator.isEnum(field.type())) {
      defaultValue = javaGenerator.enumDefault(field.type()).name();
    }

    if (field.type().isScalar() || defaultValue != null) {
      return fieldInitializer(field.type(), defaultValue);
    }

    throw new WireCompilerException("Field " + field + " cannot have default value");
  }

  private CodeBlock fieldInitializer(ProtoTypeName type, Object value) {
    TypeName javaType = javaGenerator.typeName(type);

    if (value instanceof List) {
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add("$T.asList(", Arrays.class);
      boolean first = true;
      for (Object o : (List<?>) value) {
        if (!first) builder.add(",");
        first = false;
        builder.add("\n$>$>$L$<$<", fieldInitializer(type, o));
      }
      builder.add(")");
      return builder.build();

    } else if (value instanceof Map) {
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add("new $T.Builder()", javaType);
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        WireField field = (WireField) entry.getKey();
        builder.add("\n$>$>.$L($L)$<$<", field.name(), fieldInitializer(
            field.type(), entry.getValue()));
      }
      builder.add("\n$>$>.build()$<$<");
      return builder.build();

    } else if (javaType.equals(TypeName.BOOLEAN.box())) {
      return codeBlock("$L", value != null ? value : false);

    } else if (javaType.equals(TypeName.INT.box())) {
      return codeBlock("$L", value != null
          ? new BigDecimal(String.valueOf(value)).intValue()
          : 0);

    } else if (javaType.equals(TypeName.LONG.box())) {
      return codeBlock("$LL", value != null
          ? Long.toString(new BigDecimal(String.valueOf(value)).longValue())
          : 0L);

    } else if (javaType.equals(TypeName.FLOAT.box())) {
      return codeBlock("$Lf", value != null ? String.valueOf(value) : 0f);

    } else if (javaType.equals(TypeName.DOUBLE.box())) {
      return codeBlock("$Ld", value != null ? String.valueOf(value) : 0d);

    } else if (javaType.equals(JavaGenerator.STRING)) {
      return codeBlock("$S", value != null ? (String) value : "");

    } else if (javaType.equals(JavaGenerator.BYTE_STRING)) {
      if (value == null) {
        return codeBlock("$T.EMPTY", ByteString.class);
      } else {
        return codeBlock("$T.decodeBase64($S)", ByteString.class,
            ByteString.of(String.valueOf(value).getBytes(Charsets.ISO_8859_1)).base64());
      }

    } else if (javaGenerator.isEnum(type) && value != null) {
      return codeBlock("$T.$L", javaType, value);

    } else {
      throw new WireCompilerException(type + " is not an allowed scalar type");
    }
  }

  private String addThisIfOneOf(String name, String... matches) {
    for (String match : matches) {
      if (match.equals(name)) {
        return "this." + name;
      }
    }
    return name;
  }

  private static String sanitize(String name) {
    return JAVA_KEYWORDS.contains(name) ? "_" + name : name;
  }

  private static CodeBlock codeBlock(String format, Object... args) {
    return CodeBlock.builder().add(format, args).build();
  }

  private int nullHashValue(WireField field) {
    return field.isRepeated() ? 1 : 0;
  }

  public TypeSpec extensionsType(ClassName javaTypeName, WireProtoFile wireProtoFile) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(javaTypeName.simpleName())
        .addModifiers(PUBLIC, FINAL);

    // Private no-args constructor
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .build());

    for (WireExtend extend : wireProtoFile.wireExtends()) {
      ProtoTypeName extendType = extend.protoTypeName();
      TypeName javaType = javaGenerator.typeName(extendType);

      if (!emitOptions && (extendType.isFieldOptions() || extendType.isMessageOptions())) {
        continue;
      }

      for (WireField field : extend.fields()) {
        builder.addField(extensionField(wireProtoFile, javaType, field));
      }
    }

    return builder.build();
  }

  private FieldSpec extensionField(
      WireProtoFile wireProtoFile, TypeName extendType, WireField field) {
    TypeName fieldType = javaGenerator.typeName(field.type());

    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add("$[Extension\n");

    if (field.type().isScalar()) {
      initializer.add(".$LExtending($T.class)\n", field.type(), extendType);
    } else if (javaGenerator.isEnum(field.type())) {
      initializer.add(".enumExtending($T.class, $T.class)\n", fieldType, extendType);
    } else {
      initializer.add(".messageExtending($T.class, $T.class)\n", fieldType, extendType);
    }

    initializer.add(".setName($S)\n", wireProtoFile.packageName() + "." + field.name());
    initializer.add(".setTag($L)\n", field.tag());
    initializer.add(".build$L()$]", extensionLabel(field));

    if (field.isRepeated()) {
      fieldType = JavaGenerator.listOf(fieldType);
    }

    return FieldSpec.builder(JavaGenerator.extensionOf(extendType, fieldType), field.name())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer(initializer.build())
        .build();
  }

  private String extensionLabel(WireField field) {
    switch (field.label()) {
      case OPTIONAL:
        return "Optional";

      case REQUIRED:
        return "Required";

      case REPEATED:
        boolean packed = field.isPacked()
            && (javaGenerator.isEnum(field.type()) || field.type().isPackableScalar());
        return packed ? "Packed" : "Repeated";

      default:
        throw new WireCompilerException("Unknown extension label \"" + field.label() + "\"");
    }
  }
}
