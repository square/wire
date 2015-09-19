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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import com.squareup.wire.Extension;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoType;
import com.squareup.wire.TagMap;
import com.squareup.wire.WireEnum;
import com.squareup.wire.WireField;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okio.ByteString;

import static com.google.common.base.Preconditions.checkArgument;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Generates Java source code that matches proto definitions.
 *
 * <p>This can map type names from protocol buffers (like {@code uint32}, {@code string}, or {@code
 * squareup.protos.person.Person} to the corresponding Java names (like {@code int}, {@code
 * java.lang.String}, or {@code com.squareup.protos.person.Person}).
 */
public final class JavaGenerator {
  static final ClassName BYTE_STRING = ClassName.get(ByteString.class);
  static final ClassName STRING = ClassName.get(String.class);
  static final ClassName LIST = ClassName.get(List.class);
  static final ClassName MESSAGE = ClassName.get(Message.class);
  static final ClassName ADAPTER = ClassName.get(ProtoAdapter.class);
  static final ClassName BUILDER = ClassName.get(Message.Builder.class);
  static final ClassName EXTENSION = ClassName.get(Extension.class);
  static final ClassName TAGMAP = ClassName.get(TagMap.class);
  static final TypeName MESSAGE_OPTIONS = ClassName.get("com.google.protobuf", "MessageOptions");
  static final TypeName FIELD_OPTIONS = ClassName.get("com.google.protobuf", "FieldOptions");
  static final TypeName ENUM_OPTIONS = ClassName.get("com.google.protobuf", "EnumOptions");

  private static final ImmutableSet<String> JAVA_KEYWORDS = ImmutableSet.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
      "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
      "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while");

  private static final Map<ProtoType, TypeName> SCALAR_TYPES_MAP =
      ImmutableMap.<ProtoType, TypeName>builder()
          .put(ProtoType.BOOL, TypeName.BOOLEAN.box())
          .put(ProtoType.BYTES, ClassName.get(ByteString.class))
          .put(ProtoType.DOUBLE, TypeName.DOUBLE.box())
          .put(ProtoType.FLOAT, TypeName.FLOAT.box())
          .put(ProtoType.FIXED32, TypeName.INT.box())
          .put(ProtoType.FIXED64, TypeName.LONG.box())
          .put(ProtoType.INT32, TypeName.INT.box())
          .put(ProtoType.INT64, TypeName.LONG.box())
          .put(ProtoType.SFIXED32, TypeName.INT.box())
          .put(ProtoType.SFIXED64, TypeName.LONG.box())
          .put(ProtoType.SINT32, TypeName.INT.box())
          .put(ProtoType.SINT64, TypeName.LONG.box())
          .put(ProtoType.STRING, ClassName.get(String.class))
          .put(ProtoType.UINT32, TypeName.INT.box())
          .put(ProtoType.UINT64, TypeName.LONG.box())
          .build();

  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";

  private final Schema schema;
  private final ImmutableMap<ProtoType, TypeName> nameToJavaName;
  private final ImmutableMap<Field, ProtoFile> extensionFieldToFile;
  private final boolean emitOptions;
  private final ImmutableSet<String> enumOptions;

  private JavaGenerator(
      Schema schema,
      ImmutableMap<ProtoType, TypeName> nameToJavaName,
      ImmutableMap<Field, ProtoFile> extensionFieldToFile,
      boolean emitOptions,
      ImmutableSet<String> enumOptions) {
    this.schema = schema;
    this.nameToJavaName = nameToJavaName;
    this.extensionFieldToFile = extensionFieldToFile;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
  }

  public JavaGenerator withOptions(boolean emitOptions, Collection<String> enumOptions) {
    return new JavaGenerator(schema, nameToJavaName, extensionFieldToFile, emitOptions,
        ImmutableSet.copyOf(enumOptions));
  }

  public static JavaGenerator get(Schema schema) {
    ImmutableMap.Builder<ProtoType, TypeName> nameToJavaName = ImmutableMap.builder();
    ImmutableMap.Builder<Field, ProtoFile> extensionFieldToFile = ImmutableMap.builder();
    nameToJavaName.putAll(SCALAR_TYPES_MAP);

    for (ProtoFile protoFile : schema.protoFiles()) {
      String javaPackage = javaPackage(protoFile);
      putAll(nameToJavaName, javaPackage, null, protoFile.types());

      for (Extend extend : protoFile.extendList()) {
        for (Field field : extend.fields()) {
          extensionFieldToFile.put(field, protoFile);
        }
      }
      for (Service service : protoFile.services()) {
        ClassName className = ClassName.get(javaPackage, service.type().simpleName());
        nameToJavaName.put(service.type(), className);
      }
    }

    return new JavaGenerator(schema, nameToJavaName.build(), extensionFieldToFile.build(), false,
        ImmutableSet.<String>of());
  }

  private static void putAll(ImmutableMap.Builder<ProtoType, TypeName> wireToJava,
      String javaPackage, ClassName enclosingClassName, List<Type> types) {
    for (Type type : types) {
      ClassName className = enclosingClassName != null
          ? enclosingClassName.nestedClass(type.name().simpleName())
          : ClassName.get(javaPackage, type.name().simpleName());
      wireToJava.put(type.name(), className);
      putAll(wireToJava, javaPackage, className, type.nestedTypes());
    }
  }

  public Schema schema() {
    return schema;
  }

  /**
   * Returns the extensions class name. The returned name is derived from the proto file name, like
   * {@code Ext_person} for {@code person.proto}. Its package is the proto file’s Java package.
   */
  public ClassName extensionsClass(ProtoFile protoFile) {
    return ClassName.get(javaPackage(protoFile), "Ext_" + protoFile.name());
  }

  /**
   * Returns the extensions class like {@code Ext_person} for {@code field}, or null if the field
   * wasn't declared by an extension.
   */
  public ClassName extensionsClass(Field field) {
    ProtoFile protoFile = extensionFieldToFile.get(field);
    return protoFile != null ? extensionsClass(protoFile) : null;
  }

  /**
   * Returns the Java type for {@code protoType}.
   *
   * @throws IllegalArgumentException if there is no known Java type for {@code protoType}, such as
   *     if that type wasn't in this generator's schema.
   */
  public TypeName typeName(ProtoType protoType) {
    TypeName candidate = nameToJavaName.get(protoType);
    checkArgument(candidate != null, "unexpected type %s", protoType);
    return candidate;
  }

  private static String javaPackage(ProtoFile protoFile) {
    Object javaPackageOption = protoFile.options().get("java_package");
    if (javaPackageOption != null) {
      return String.valueOf(javaPackageOption);
    } else if (protoFile.packageName() != null) {
      return protoFile.packageName();
    } else {
      return "";
    }
  }

  public boolean isEnum(ProtoType type) {
    return schema.getType(type) instanceof EnumType;
  }

  EnumConstant enumDefault(ProtoType type) {
    EnumType wireEnum = (EnumType) schema.getType(type);
    return wireEnum.constants().get(0);
  }

  static TypeName listOf(TypeName type) {
    return ParameterizedTypeName.get(LIST, type);
  }

  static TypeName messageOf(TypeName type) {
    return ParameterizedTypeName.get(JavaGenerator.MESSAGE, type);
  }

  static TypeName adapterOf(TypeName messageType) {
    return ParameterizedTypeName.get(ADAPTER, messageType);
  }

  static TypeName builderOf(TypeName messageType, ClassName builderType) {
    return ParameterizedTypeName.get(BUILDER, messageType, builderType);
  }

  static TypeName extensionOf(TypeName messageType, TypeName fieldType) {
    return ParameterizedTypeName.get(EXTENSION, messageType, fieldType);
  }

  /** A grab-bag of fixes for things that can go wrong when converting to javadoc. */
  static String sanitizeJavadoc(String documentation) {
    // Remove trailing whitespace on each line.
    documentation = documentation.replaceAll("[^\\S\n]+\n", "\n");
    documentation = documentation.replaceAll("\\s+$", "");
    // Rewrite '@see <url>' to use an html anchor tag
    documentation = documentation.replaceAll(
        "@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
  }


  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec generateEnum(EnumType type) {
    ClassName javaType = (ClassName) typeName(type.name());

    TypeSpec.Builder builder = TypeSpec.enumBuilder(javaType.simpleName())
        .addModifiers(PUBLIC)
        .addSuperinterface(WireEnum.class);

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", sanitizeJavadoc(type.documentation()));
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
          TypeName optionJavaType = typeName(optionField.type());
          builder.addField(optionJavaType, optionField.name(), PUBLIC, FINAL);
          constructorBuilder.addParameter(optionJavaType, optionField.name());
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
        constantBuilder.addJavadoc("$L\n", sanitizeJavadoc(constant.documentation()));
      }

      builder.addEnumConstant(constant.name(), constantBuilder.build());
    }

    builder.addField(FieldSpec.builder(adapterOf(javaType), "ADAPTER")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer("$T.newEnumAdapter($T.class)", ProtoAdapter.class, javaType)
        .build());

    // Enum type options.
    if (emitOptions) {
      FieldSpec options = optionsField(ENUM_OPTIONS, "ENUM_OPTIONS", type.options());
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

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec generateMessage(MessageType type) {
    ClassName javaType = (ClassName) typeName(type.name());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName());
    builder.addModifiers(PUBLIC, FINAL);

    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", sanitizeJavadoc(type.documentation()));
    }

    builder.superclass(messageOf(javaType));

    builder.addField(FieldSpec.builder(adapterOf(javaType), "ADAPTER")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer("$T.newMessageAdapter($T.class)", ProtoAdapter.class, javaType)
        .build());

    builder.addField(FieldSpec.builder(TypeName.LONG, "serialVersionUID")
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$LL", 0L)
        .build());

    if (emitOptions) {
      FieldSpec messageOptions = optionsField(MESSAGE_OPTIONS, "MESSAGE_OPTIONS", type.options());
      if (messageOptions != null) {
        builder.addField(messageOptions);
      }

      for (Field field : type.fieldsAndOneOfFields()) {
        String fieldName = "FIELD_OPTIONS_" + field.name().toUpperCase(Locale.US);
        FieldSpec fieldOptions = optionsField(FIELD_OPTIONS, fieldName, field.options());
        if (fieldOptions != null) {
          builder.addField(fieldOptions);
        }
      }
    }

    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName fieldJavaType = fieldType(field);

      if ((field.type().isScalar() || isEnum(field.type()))
          && !field.isRepeated()
          && !field.isPacked()) {
        builder.addField(defaultField(field, fieldJavaType));
      }

      String name = sanitize(field.name());
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldJavaType, name, PUBLIC, FINAL);
      fieldBuilder.addAnnotation(wireFieldAnnotation(field));
      if (!field.documentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", sanitizeJavadoc(field.documentation()));
      }
      if (field.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      builder.addField(fieldBuilder.build());
    }

    builder.addMethod(messageFieldsConstructor(type));
    builder.addMethod(messageFieldsAndTagMapConstructor(type));
    builder.addMethod(messageEquals(type));
    builder.addMethod(messageHashCode(type));
    builder.addMethod(messageWriteReplace(type));
    builder.addType(builder(type, javaType, builderJavaType));

    for (Type nestedType : type.nestedTypes()) {
      TypeSpec typeSpec = nestedType instanceof MessageType
          ? generateMessage((MessageType) nestedType)
          : generateEnum((EnumType) nestedType);
      builder.addType(typeSpec);
    }

    return builder.build();
  }

  // Example:
  //
  // private Object writeReplace() throws ObjectStreamException {
  //   return super.createSerializedForm();
  // }
  //
  private MethodSpec messageWriteReplace(MessageType type) {
    return MethodSpec.methodBuilder("writeReplace")
        .addModifiers(PRIVATE)
        .returns(Object.class)
        .addException(ObjectStreamException.class)
        .addStatement("return super.createSerializedForm()")
        .build();
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

      ClassName extensionClass = extensionsClass(extensionRoot);
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
    TypeName messageType = typeName(field.type());
    return field.isRepeated() ? listOf(messageType) : messageType;
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
  // @WireField(
  //   tag = 1,
  //   type = INT32
  // )
  //
  private AnnotationSpec wireFieldAnnotation(Field field) {
    AnnotationSpec.Builder result = AnnotationSpec.builder(WireField.class);

    int tag = field.tag();
    result.addMember("tag", String.valueOf(tag));
    result.addMember("adapter", "$S", adapterString(field.type()));

    if (!field.isOptional()) {
      if (field.isPacked()) {
        result.addMember("label", "$T.PACKED", WireField.Label.class);
      } else if (field.label() != null) {
        result.addMember("label", "$T.$L", WireField.Label.class, field.label());
      }
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

  private String adapterString(ProtoType type) {
    return type.isScalar()
          ? ProtoAdapter.class.getName() + '#' + type.toString().toUpperCase(Locale.US)
          : reflectionName((ClassName) typeName(type)) + "#ADAPTER";
  }

  private String reflectionName(ClassName className) {
    return className.packageName() + '.' + Joiner.on('$').join(className.simpleNames());
  }

  // Example:
  //
  // public SimpleMessage(int optional_int32, long optional_int64) {
  //   this(builder.optional_int32, builder.optional_int64, null);
  // }
  //
  private MethodSpec messageFieldsConstructor(MessageType type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder();
    result.addModifiers(PUBLIC);
    result.addCode("this(");
    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String sanitizedName = sanitize(field.name());
      result.addParameter(javaType, sanitizedName);
      result.addCode("$L, ", sanitize(field.name()));
    }
    result.addCode("null);\n");
    return result.build();
  }

  // Example:
  //
  // public SimpleMessage(int optional_int32, long optional_int64, TagMap tagMap) {
  //   super(tagMap);
  //   this.optional_int32 = optional_int32;
  //   this.optional_int64 = optional_int64;
  // }
  //
  private MethodSpec messageFieldsAndTagMapConstructor(MessageType type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addStatement("super(tagMap)");

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

    result.addParameter(TAGMAP, "tagMap");

    return result.build();
  }

  // Example:
  //
  // @Override
  // public boolean equals(Object other) {
  //   if (other == this) return true;
  //   if (!(other instanceof SimpleMessage)) return false;
  //   SimpleMessage o = (SimpleMessage) other;
  //   return equals(tagMap(), o.tagMap())
  //       && equals(optional_int32, o.optional_int32);
  //
  private MethodSpec messageEquals(MessageType type) {
    TypeName javaType = typeName(type.name());
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

    result.addStatement("$T o = ($T) other", javaType, javaType);
    result.addCode("$[return equals(tagMap(), o.tagMap())");
    for (Field field : fields) {
      String name = sanitize(field.name());
      result.addCode("\n&& equals($L, o.$L)", addThisIfOneOf(name, "other", "o"), name);
    }
    result.addCode(";\n$]");

    return result.build();
  }

  // Example:
  //
  // @Override
  // public int hashCode() {
  //   int result = hashCode;
  //   if (result == 0) {
  //     result = tagMap() != null ? tagMap().hashCode() : 0
  //     result = result * 37 + (f != null ? f.hashCode() : 0);
  //     hashCode = result;
  //   }
  //   return result;
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
    if (fields.isEmpty()) {
      result.addStatement("return tagMap() != null ? tagMap().hashCode() : 0");
      return result.build();
    }

    result.addStatement("int result = hashCode");
    result.beginControlFlow("if (result == 0)");
    result.addStatement("result = tagMap() != null ? tagMap().hashCode() : 0");
    for (Field field : fields) {
      String name = sanitize(field.name());
      name = addThisIfOneOf(name, "result");
      result.addStatement("result = result * 37 + ($L != null ? $L.hashCode() : $L)",
          name, name, nullHashValue(field));
    }
    result.addStatement("hashCode = result");
    result.endControlFlow();
    result.addStatement("return result");
    return result.build();
  }

  private TypeSpec builder(MessageType type, ClassName javaType, ClassName builderType) {
    TypeSpec.Builder result = TypeSpec.classBuilder("Builder")
        .addModifiers(PUBLIC, STATIC, FINAL);

    result.superclass(builderOf(javaType, builderType));

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

    result.addMethod(builderNoArgsConstructor());
    result.addMethod(builderCopyConstructor(type));

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
  private MethodSpec builderNoArgsConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(PUBLIC).build();
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
  private MethodSpec builderCopyConstructor(MessageType message) {
    TypeName javaType = typeName(message.name());

    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(javaType, "message");
    result.addStatement("super(message)");

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
      result.addJavadoc("$L\n", sanitizeJavadoc(field.documentation()));
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

    result.addCode("return new $T(", javaType);
    for (Field field : message.fieldsAndOneOfFields()) {
      result.addCode("$L, ", sanitize(field.name()));
    }
    result.addCode("buildTagMap());\n");
    return result.build();
  }

  private CodeBlock defaultValue(Field field) {
    Object defaultValue = field.getDefault();

    if (defaultValue == null && isEnum(field.type())) {
      defaultValue = enumDefault(field.type()).name();
    }

    if (field.type().isScalar() || defaultValue != null) {
      return fieldInitializer(field.type(), defaultValue);
    }

    throw new IllegalStateException("Field " + field + " cannot have default value");
  }

  private CodeBlock fieldInitializer(ProtoType type, Object value) {
    TypeName javaType = typeName(type);

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
        ClassName extensionClass = extensionsClass(field);
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

    } else if (javaType.equals(STRING)) {
      return codeBlock("$S", value != null ? (String) value : "");

    } else if (javaType.equals(BYTE_STRING)) {
      if (value == null) {
        return codeBlock("$T.EMPTY", ByteString.class);
      } else {
        return codeBlock("$T.decodeBase64($S)", ByteString.class,
            ByteString.of(String.valueOf(value).getBytes(Charsets.ISO_8859_1)).base64());
      }

    } else if (isEnum(type) && value != null) {
      return codeBlock("$T.$L", javaType, value);

    } else {
      throw new IllegalStateException(type + " is not an allowed scalar type");
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

  public TypeSpec generateExtensionsClass(ClassName javaTypeName, ProtoFile protoFile) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(javaTypeName.simpleName())
        .addModifiers(PUBLIC, FINAL);

    // Private no-args constructor
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .build());

    for (Extend extend : protoFile.extendList()) {
      ProtoType extendType = extend.type();
      TypeName javaType = typeName(extendType);

      if (!emitOptions && (extendType.equals(Options.FIELD_OPTIONS)
          || extendType.equals(Options.MESSAGE_OPTIONS))) {
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
    TypeName fieldType = typeName(field.type());

    if (field.isRepeated()) {
      fieldType = listOf(fieldType);
    }

    return FieldSpec.builder(extensionOf(extendType, fieldType), field.name())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer("$[$T.get($T.class,\n$T.$L,\n$S,\n$L,\n$S)$]", Extension.class, extendType,
            WireField.Label.class, extensionLabel(field),
            protoFile.packageName() + "." + field.name(),
            field.tag(), adapterString(field.type()))
        .build();
  }

  private String extensionLabel(Field field) {
    switch (field.label()) {
      case OPTIONAL:
        return "OPTIONAL";

      case REQUIRED:
        return "REQUIRED";

      case REPEATED:
        return field.isPacked() ? "PACKED" : "REPEATED";

      default:
        throw new AssertionError("Unexpected extension label \"" + field.label() + "\"");
    }
  }

  public TypeSpec generateRegistry(ClassName javaTypeName) {
    TypeSpec.Builder builder = TypeSpec.classBuilder(javaTypeName.simpleName())
        .addModifiers(PUBLIC, FINAL);

    ImmutableSet.Builder<TypeName> extensionClassesBuilder = ImmutableSet.builder();
    for (ProtoFile protoFile : schema().protoFiles()) {
      if (!protoFile.extendList().isEmpty()) {
        extensionClassesBuilder.add(extensionsClass(protoFile));
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

    TypeName listType = listOf(ParameterizedTypeName.get(ClassName.get(Class.class), wildcard));

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
