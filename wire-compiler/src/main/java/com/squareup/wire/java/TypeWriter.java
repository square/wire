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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import com.squareup.wire.WireCompilerException;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
  private final ImmutableSet<String> enumOptions;

  public TypeWriter(JavaGenerator javaGenerator, boolean emitOptions, Set<String> enumOptions) {
    this.javaGenerator = javaGenerator;
    this.emitOptions = emitOptions;
    this.enumOptions = ImmutableSet.copyOf(enumOptions);
  }

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec toTypeSpec(Type type) {
    if (type instanceof MessageType) {
      return toTypeSpec((MessageType) type);
    } else if (type instanceof EnumType) {
      return toTypeSpec((EnumType) type);
    } else {
      throw new IllegalArgumentException("unexpected type: " + type);
    }
  }

  private TypeSpec toTypeSpec(EnumType type) {
    ClassName typeName = (ClassName) javaGenerator.typeName(type.name());

    TypeSpec.Builder builder = TypeSpec.enumBuilder(typeName.simpleName())
        .addModifiers(PUBLIC)
        .addSuperinterface(ProtoEnum.class);

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(type.documentation()));
    }

    // Output Private tag field
    builder.addField(TypeName.INT, "value", PRIVATE, FINAL);

    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    constructorBuilder.addStatement("this.value = value");
    constructorBuilder.addParameter(TypeName.INT, "value");

    // Enum constant options, each of which requires a constructor parameter and a field.
    Set<Field> allOptionFieldsBuilder = new LinkedHashSet<>();
    for (EnumConstant constant : type.constants()) {
      for (Field optionField : constant.options().map().keySet()) {
        String fullyQualifiedName = optionField.packageName() + "." + optionField.name();
        if (!enumOptions.contains(fullyQualifiedName)) {
          continue;
        }

        if (allOptionFieldsBuilder.add(optionField)) {
          TypeName javaType = javaGenerator.typeName(optionField.type());
          builder.addField(javaType, optionField.name(), PUBLIC, FINAL);
          constructorBuilder.addParameter(javaType, optionField.name());
          constructorBuilder.addStatement("this.$L = $L", optionField.name(), optionField.name());
        }
      }
    }
    ImmutableList<Field> allOptionFields = ImmutableList.copyOf(allOptionFieldsBuilder);
    String enumArgsFormat = "$L" + Strings.repeat(", $L", allOptionFields.size());
    builder.addMethod(constructorBuilder.build());

    for (EnumConstant constant : type.constants()) {
      Object[] enumArgs = new Object[allOptionFields.size() + 1];
      enumArgs[0] = constant.tag();
      for (int i = 0; i < allOptionFields.size(); i++) {
        Field key = allOptionFields.get(i);
        Object value = constant.options().map().get(key);
        enumArgs[i + 1] = value != null
            ? fieldInitializer(key.type(), value)
            : null;
      }

      TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder(enumArgsFormat, enumArgs);
      if (!constant.documentation().isEmpty()) {
        constantBuilder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(constant.documentation()));
      }

      builder.addEnumConstant(constant.name(), constantBuilder.build());
    }

    // Enum type options.
    if (emitOptions) {
      FieldSpec options = optionsField(JavaGenerator.ENUM_OPTIONS, "ENUM_OPTIONS", type.options());
      if (options != null) {
        builder.addField(options);
      }
    }

    // Public Getter
    builder.addMethod(MethodSpec.methodBuilder("getValue")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(TypeName.INT)
        .addStatement("return value")
        .build());

    return builder.build();
  }

  private TypeSpec toTypeSpec(MessageType type) {
    ClassName javaType = (ClassName) javaGenerator.typeName(type.name());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName());
    builder.addModifiers(PUBLIC, FINAL);

    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(type.documentation()));
    }

    builder.superclass(type.extensions().isEmpty()
        ? JavaGenerator.MESSAGE
        : JavaGenerator.extendableMessageOf(javaType));

    builder.addField(FieldSpec.builder(TypeName.LONG, "serialVersionUID")
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$LL", 0L)
        .build());

    if (emitOptions) {
      FieldSpec messageOptions = optionsField(
          JavaGenerator.MESSAGE_OPTIONS, "MESSAGE_OPTIONS", type.options());
      if (messageOptions != null) {
        builder.addField(messageOptions);
      }

      for (Field field : type.fieldsAndOneOfFields()) {
        String fieldName = "FIELD_OPTIONS_" + field.name().toUpperCase(Locale.US);
        FieldSpec fieldOptions = optionsField(
            JavaGenerator.FIELD_OPTIONS, fieldName, field.options());
        if (fieldOptions != null) {
          builder.addField(fieldOptions);
        }
      }
    }

    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName fieldType = fieldType(field);

      if ((field.type().isScalar() || javaGenerator.isEnum(field.type()))
          && !field.isRepeated()
          && !field.isPacked()) {
        builder.addField(defaultField(field, fieldType));
      }

      String name = sanitize(field.name());
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, name, PUBLIC, FINAL);
      fieldBuilder.addAnnotation(protoFieldAnnotation(field, javaGenerator.typeName(field.type())));
      if (!field.documentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(field.documentation()));
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

    for (Type nestedType : type.nestedTypes()) {
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
  private FieldSpec optionsField(TypeName optionsType, String fieldName, Options options) {
    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add("$[new $T.Builder()", optionsType);

    boolean empty = true;
    for (Map.Entry<Field, ?> entry : options.map().entrySet()) {
      Field extensionRoot = entry.getKey();
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

    return FieldSpec.builder(optionsType, fieldName)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer(initializer.build())
        .build();
  }

  private TypeName fieldType(Field field) {
    TypeName messageType = javaGenerator.typeName(field.type());
    return field.isRepeated() ? JavaGenerator.listOf(messageType) : messageType;
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private FieldSpec defaultField(Field field, TypeName fieldType) {
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
  private AnnotationSpec protoFieldAnnotation(Field field, TypeName messageType) {
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
      if (field.isPacked() && (isEnum || field.type().isPackableScalar())) {
        result.addMember("label", "$T.PACKED", Message.Label.class);
      } else if (field.label() != null) {
        result.addMember("label", "$T.$L", Message.Label.class, field.label());
      }
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
  private MethodSpec messageFieldsConstructor(MessageType type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder();
    result.addModifiers(PUBLIC);
    for (Field field : type.fieldsAndOneOfFields()) {
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
  private MethodSpec messageBuilderConstructor(MessageType type, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addParameter(builderJavaType, "builder");

    List<Field> fields = type.fieldsAndOneOfFields();
    if (fields.size() > 0) {
      result.addCode("this(");
      for (int i = 0; i < fields.size(); i++) {
        if (i > 0) result.addCode(", ");
        Field field = fields.get(i);
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
  private MethodSpec messageEquals(MessageType type) {
    TypeName javaType = javaGenerator.typeName(type.name());
    MethodSpec.Builder result = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(boolean.class)
        .addParameter(Object.class, "other");

    List<Field> fields = type.fieldsAndOneOfFields();
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
      Field field = fields.get(i);
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
  private MethodSpec messageHashCode(MessageType type) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("hashCode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class);

    List<Field> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty() && type.extensions().isEmpty()) {
      result.addStatement("return 0");
      return result.build();
    }

    if (fields.size() == 1 && type.extensions().isEmpty()) {
      Field field = fields.get(0);
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
    for (Field field : fields) {
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

  private TypeSpec builder(MessageType type, ClassName javaType, ClassName builderType) {
    TypeSpec.Builder result = TypeSpec.classBuilder("Builder")
        .addModifiers(PUBLIC, STATIC, FINAL);

    boolean hasExtensions = !type.extensions().isEmpty();
    result.superclass(hasExtensions
        ? JavaGenerator.extendableBuilderOf(javaType, builderType)
        : JavaGenerator.builderOf(javaType));

    List<Field> fields = type.fieldsAndOneOfFields();
    for (Field field : fields) {
      TypeName fieldJavaType = fieldType(field);
      FieldSpec.Builder fieldSpec =
          FieldSpec.builder(fieldJavaType, sanitize(field.name()), PUBLIC);
      if (field.isPacked() || field.isRepeated()) {
        fieldSpec.initializer("$T.emptyList()", Collections.class);
      }
      result.addField(fieldSpec.build());
    }

    result.addMethod(builderNoArgsConstructor(hasExtensions, builderType));
    result.addMethod(builderCopyConstructor(hasExtensions, builderType, type));

    for (Field field : type.fields()) {
      result.addMethod(setter(builderType, null, field));
    }

    for (OneOf oneOf : type.oneOfs()) {
      for (Field field : oneOf.fields()) {
        result.addMethod(setter(builderType, oneOf, field));
      }
    }

    result.addMethod(builderBuild(type, javaType));
    return result.build();
  }

  // Example:
  //
  // public Builder() {
  // }
  //
  private MethodSpec builderNoArgsConstructor(boolean hasExtensions, ClassName builderType) {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
    if (hasExtensions) {
      builder.addStatement("super($T.class)", builderType);
    }
    return builder.build();
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
  private MethodSpec builderCopyConstructor(boolean hasExtensions, ClassName builderType,
      MessageType message) {
    TypeName javaType = javaGenerator.typeName(message.name());

    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(javaType, "message");
    if (hasExtensions) {
      result.addStatement("super($T.class, message)", builderType);
    } else {
      result.addStatement("super(message)");
    }

    List<Field> fields = message.fieldsAndOneOfFields();
    if (!fields.isEmpty()) {
      result.addStatement("if (message == null) return");
    }

    for (Field field : fields) {
      String fieldName = sanitize(field.name());
      if (field.isRepeated()) {
        result.addStatement("this.$L = copyOf(message.$L)", fieldName, fieldName);
      } else {
        result.addStatement("this.$L = message.$L", fieldName, fieldName);
      }
    }

    return result.build();
  }

  private MethodSpec setter(TypeName builderType, OneOf oneOf, Field field) {
    TypeName javaType = fieldType(field);
    String fieldName = sanitize(field.name());

    MethodSpec.Builder result = MethodSpec.methodBuilder(fieldName)
        .addModifiers(PUBLIC)
        .addParameter(javaType, fieldName)
        .returns(builderType);

    if (!field.documentation().isEmpty()) {
      result.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(field.documentation()));
    }

    if (field.isDeprecated()) {
      result.addAnnotation(Deprecated.class);
    }

    if (field.isRepeated()) {
      result.addStatement("this.$L = canonicalizeList($L)", fieldName, fieldName);
    } else {
      result.addStatement("this.$L = $L", fieldName, fieldName);

      if (oneOf != null) {
        for (Field other : oneOf.fields()) {
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
  // public SimpleMessage build() {
  //   if (field_one == null) {
  //     throw missingRequiredFields(field_one, "field_one");
  //   }
  //   return new SimpleMessage(this);
  // }
  //
  // The call to checkRequiredFields will be emitted only if the message has
  // required fields.
  //
  private MethodSpec builderBuild(MessageType message, ClassName javaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType);

    List<Field> requiredFields = message.getRequiredFields();
    if (!requiredFields.isEmpty()) {
      CodeBlock.Builder conditionals = CodeBlock.builder().add("$[");
      CodeBlock.Builder missingArgs = CodeBlock.builder();
      for (int i = 0; i < requiredFields.size(); i++) {
        Field requiredField = requiredFields.get(i);
        if (i > 0) conditionals.add("\n|| ");
        conditionals.add("$L == null", requiredField.name());
        if (i > 0) missingArgs.add(",\n");
        missingArgs.add("$1L, $1S", requiredField.name());
      }

      result.beginControlFlow("if ($L)", conditionals.add("$]").build())
          .addStatement("throw missingRequiredFields($L)", missingArgs.build())
          .endControlFlow();
    }

    result.addStatement("return new $T(this)", javaType);
    return result.build();
  }

  private CodeBlock defaultValue(Field field) {
    Object defaultValue = field.getDefault();

    if (defaultValue == null && javaGenerator.isEnum(field.type())) {
      defaultValue = javaGenerator.enumDefault(field.type()).name();
    }

    if (field.type().isScalar() || defaultValue != null) {
      return fieldInitializer(field.type(), defaultValue);
    }

    throw new WireCompilerException("Field " + field + " cannot have default value");
  }

  private CodeBlock fieldInitializer(Type.Name type, Object value) {
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
        Field field = (Field) entry.getKey();
        CodeBlock valueInitializer = fieldInitializer(field.type(), entry.getValue());
        ClassName extensionClass = javaGenerator.extensionsClass(field);
        if (extensionClass != null) {
          builder.add("\n$>$>.setExtension($T.$L, $L)$<$<",
              extensionClass, field.name(), valueInitializer);
        } else {
          builder.add("\n$>$>.$L($L)$<$<", field.name(), valueInitializer);
        }
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

  private int nullHashValue(Field field) {
    return field.isRepeated() ? 1 : 0;
  }

  public TypeSpec extensionsType(ClassName javaTypeName, ProtoFile protoFile) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(javaTypeName.simpleName())
        .addModifiers(PUBLIC, FINAL);

    // Private no-args constructor
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .build());

    for (Extend extend : protoFile.extendList()) {
      Type.Name extendType = extend.type();
      TypeName javaType = javaGenerator.typeName(extendType);

      if (!emitOptions && (extendType.isFieldOptions() || extendType.isMessageOptions())) {
        continue;
      }

      for (Field field : extend.fields()) {
        builder.addField(extensionField(protoFile, javaType, field));
      }
    }

    return builder.build();
  }

  private FieldSpec extensionField(
      ProtoFile protoFile, TypeName extendType, Field field) {
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

    initializer.add(".setName($S)\n", protoFile.packageName() + "." + field.name());
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

  private String extensionLabel(Field field) {
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

  public TypeSpec registryType(ClassName javaTypeName, Schema schema) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(javaTypeName.simpleName())
        .addModifiers(PUBLIC, FINAL);

    ImmutableSet.Builder<TypeName> extensionClassesBuilder = ImmutableSet.builder();
    for (ProtoFile protoFile : schema.protoFiles()) {
      if (!protoFile.extendList().isEmpty()) {
        extensionClassesBuilder.add(javaGenerator.extensionsClass(protoFile));
      }
    }
    ImmutableList<TypeName> extensionClasses = extensionClassesBuilder.build().asList();

    CodeBlock.Builder initializer = CodeBlock.builder();
    if (extensionClasses.isEmpty()) {
      initializer.add("$T.emptyList()", Collections.class);
    } else {
      initializer.add("$>$>$T.unmodifiableList($T.asList(", Collections.class, Arrays.class);
      for (int i = 0, size = extensionClasses.size(); i < size; i++) {
        TypeName typeName = extensionClasses.get(i);
        initializer.add("\n$T.class", typeName);
        if (i + 1 < extensionClasses.size()) initializer.add(",");
      }
      initializer.add("))$<$<");
    }

    TypeName wildcard = extensionClasses.size() == 1
        ? extensionClasses.get(0)
        : WildcardTypeName.subtypeOf(Object.class);

    TypeName listType = JavaGenerator.listOf(ParameterizedTypeName.get(
        ClassName.get(Class.class), wildcard));

    builder.addField(FieldSpec.builder(listType, "EXTENSIONS", PUBLIC, STATIC, FINAL)
        .initializer(initializer.build())
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "$S", "unchecked")
            .build())
        .build());

    // Private no-args constructor
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .build());

    return builder.build();
  }
}
