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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoAdapter.EnumConstantNotFoundException;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireEnum;
import com.squareup.wire.WireField;
import com.squareup.wire.WireInternal;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoMember;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okio.ByteString;

import static com.google.common.base.Preconditions.checkArgument;
import static com.squareup.wire.schema.Options.ENUM_OPTIONS;
import static com.squareup.wire.schema.Options.ENUM_VALUE_OPTIONS;
import static com.squareup.wire.schema.Options.FIELD_OPTIONS;
import static com.squareup.wire.schema.Options.MESSAGE_OPTIONS;
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
  static final ProtoMember FIELD_DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated");
  static final ProtoMember ENUM_DEPRECATED = ProtoMember.get(ENUM_VALUE_OPTIONS, "deprecated");
  static final ProtoMember PACKED = ProtoMember.get(FIELD_OPTIONS, "packed");

  static final ClassName BYTE_STRING = ClassName.get(ByteString.class);
  static final ClassName STRING = ClassName.get(String.class);
  static final ClassName LIST = ClassName.get(List.class);
  static final ClassName MESSAGE = ClassName.get(Message.class);
  static final ClassName ADAPTER = ClassName.get(ProtoAdapter.class);
  static final ClassName BUILDER = ClassName.get(Message.Builder.class);
  static final ClassName PARCEL = ClassName.get("android.os", "Parcel");
  static final ClassName PARCELABLE = ClassName.get("android.os", "Parcelable");
  static final ClassName NULLABLE = ClassName.get("android.support.annotation", "Nullable");
  static final ClassName CREATOR = PARCELABLE.nestedClass("Creator");

  private static final Map<ProtoType, TypeName> BUILT_IN_TYPES_MAP =
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
          .put(FIELD_OPTIONS, ClassName.get("com.google.protobuf", "MessageOptions"))
          .put(ENUM_OPTIONS, ClassName.get("com.google.protobuf", "FieldOptions"))
          .put(MESSAGE_OPTIONS, ClassName.get("com.google.protobuf", "EnumOptions"))
          .build();

  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";

  /**
   * Preallocate all of the names we'll need for {@code type}. Names are allocated in precedence
   * order, so names we're stuck with (serialVersionUID etc.) occur before proto field names are
   * assigned. Names we aren't stuck with (typically for locals) yield to message fields.
   *
   * <p>Name allocations are computed once and reused because some types may be needed when
   * generating other types.
   */
  private final LoadingCache<MessageType, NameAllocator> typeToNameAllocator
      = CacheBuilder.newBuilder().build(new CacheLoader<MessageType, NameAllocator>() {
    @Override public NameAllocator load(MessageType type) throws Exception {
      NameAllocator nameAllocator = new NameAllocator();
      nameAllocator.newName("serialVersionUID", "serialVersionUID");
      nameAllocator.newName("ADAPTER", "ADAPTER");
      nameAllocator.newName("MESSAGE_OPTIONS", "MESSAGE_OPTIONS");
      if (emitAndroid) {
        nameAllocator.newName("CREATOR", "CREATOR");
      }
      Set<String> collidingNames = collidingFieldNames(type.fieldsAndOneOfFields());
      for (Field field : type.fieldsAndOneOfFields()) {
        String suggestion = collidingNames.contains(field.name())
            ? field.qualifiedName()
            : field.name();
        nameAllocator.newName(suggestion, field);
      }
      nameAllocator.newName("unknownFields", "unknownFields");
      nameAllocator.newName("result", "result");
      nameAllocator.newName("message", "message");
      nameAllocator.newName("other", "other");
      nameAllocator.newName("o", "o");
      nameAllocator.newName("builder", "builder");
      return nameAllocator;
    }
  });

  private final Schema schema;
  private final ImmutableMap<ProtoType, TypeName> nameToJavaName;
  private final boolean emitAndroid;
  private final boolean emitCompact;

  private JavaGenerator(Schema schema, ImmutableMap<ProtoType, TypeName> nameToJavaName,
      boolean emitAndroid, boolean emitCompact) {
    this.schema = schema;
    this.nameToJavaName = nameToJavaName;
    this.emitAndroid = emitAndroid;
    this.emitCompact = emitCompact;
  }

  public JavaGenerator withAndroid(boolean emitAndroid) {
    return new JavaGenerator(schema, nameToJavaName, emitAndroid, emitCompact);
  }

  public JavaGenerator withCompact(boolean compactGeneration) {
    return new JavaGenerator(schema, nameToJavaName, emitAndroid, compactGeneration);
  }

  public static JavaGenerator get(Schema schema) {
    Map<ProtoType, TypeName> nameToJavaName = new LinkedHashMap<>();
    nameToJavaName.putAll(BUILT_IN_TYPES_MAP);

    for (ProtoFile protoFile : schema.protoFiles()) {
      String javaPackage = javaPackage(protoFile);
      putAll(nameToJavaName, javaPackage, null, protoFile.types());

      for (Service service : protoFile.services()) {
        ClassName className = ClassName.get(javaPackage, service.type().simpleName());
        nameToJavaName.put(service.type(), className);
      }
    }

    return new JavaGenerator(schema, ImmutableMap.copyOf(nameToJavaName), false, false);
  }

  private static void putAll(Map<ProtoType, TypeName> wireToJava, String javaPackage,
      ClassName enclosingClassName, List<Type> types) {
    for (Type type : types) {
      ClassName className = enclosingClassName != null
          ? enclosingClassName.nestedClass(type.type().simpleName())
          : ClassName.get(javaPackage, type.type().simpleName());
      wireToJava.put(type.type(), className);
      putAll(wireToJava, javaPackage, className, type.nestedTypes());
    }
  }

  public Schema schema() {
    return schema;
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

  private CodeBlock singleAdapterFor(Field field) {
    ProtoType type = field.type();
    CodeBlock.Builder result = CodeBlock.builder();
    if (type.isScalar()) {
      result.add("$T.$L", ADAPTER, type.simpleName().toUpperCase(Locale.US));
    } else {
      result.add("$T.ADAPTER", typeName(type));
    }
    return result.build();
  }

  private CodeBlock adapterFor(Field field) {
    CodeBlock.Builder result = singleAdapterFor(field).toBuilder();
    if (field.isPacked()) {
      result.add(".asPacked()");
    } else if (field.isRepeated()) {
      result.add(".asRepeated()");
    }
    return result.build();
  }

  private static String javaPackage(ProtoFile protoFile) {
    String javaPackage = protoFile.javaPackage();
    if (javaPackage != null) {
      return javaPackage;
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

  static TypeName messageOf(TypeName type, ClassName builderType) {
    return ParameterizedTypeName.get(MESSAGE, type, builderType);
  }

  static TypeName adapterOf(TypeName messageType) {
    return ParameterizedTypeName.get(ADAPTER, messageType);
  }

  static TypeName builderOf(TypeName messageType, ClassName builderType) {
    return ParameterizedTypeName.get(BUILDER, messageType, builderType);
  }

  static TypeName creatorOf(TypeName messageType) {
    return ParameterizedTypeName.get(CREATOR, messageType);
  }

  private static boolean isRedacted(Field field) {
    return field.options().optionMatches(".*\\.redacted", "true");
  }

  /** A grab-bag of fixes for things that can go wrong when converting to javadoc. */
  static String sanitizeJavadoc(String documentation) {
    // Remove trailing whitespace on each line.
    documentation = documentation.replaceAll("[^\\S\n]+\n", "\n");
    documentation = documentation.replaceAll("\\s+$", "");
    documentation = documentation.replaceAll("\\*/", "&#42;/");
    // Rewrite '@see <url>' to use an html anchor tag
    documentation = documentation.replaceAll(
        "@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
  }

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec generateEnum(EnumType type) {
    ClassName javaType = (ClassName) typeName(type.type());

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
    Set<ProtoMember> allOptionFieldsBuilder = new LinkedHashSet<>();
    for (EnumConstant constant : type.constants()) {
      for (ProtoMember protoMember : constant.options().map().keySet()) {
        Field optionField = schema.getField(protoMember);
        if (allOptionFieldsBuilder.add(protoMember)) {
          TypeName optionJavaType = typeName(optionField.type());
          builder.addField(optionJavaType, optionField.name(), PUBLIC, FINAL);
          constructorBuilder.addParameter(optionJavaType, optionField.name());
          constructorBuilder.addStatement("this.$L = $L", optionField.name(), optionField.name());
        }
      }
    }
    ImmutableList<ProtoMember> allOptionMembers = ImmutableList.copyOf(allOptionFieldsBuilder);
    String enumArgsFormat = "$L" + Strings.repeat(", $L", allOptionMembers.size());
    builder.addMethod(constructorBuilder.build());

    MethodSpec.Builder fromValueBuilder = MethodSpec.methodBuilder("fromValue")
        .addJavadoc("Return the constant for {@code value} or null.\n")
        .addModifiers(PUBLIC, STATIC)
        .returns(javaType)
        .addParameter(int.class, "value")
        .beginControlFlow("switch (value)");

    Set<Integer> seenTags = new LinkedHashSet<>();
    for (EnumConstant constant : type.constants()) {
      Object[] enumArgs = new Object[allOptionMembers.size() + 1];
      enumArgs[0] = constant.tag();
      for (int i = 0; i < allOptionMembers.size(); i++) {
        ProtoMember protoMember = allOptionMembers.get(i);
        Field field = schema.getField(protoMember);
        Object value = constant.options().map().get(protoMember);
        enumArgs[i + 1] = value != null
            ? fieldInitializer(field.type(), value)
            : null;
      }

      TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder(enumArgsFormat, enumArgs);
      if (!constant.documentation().isEmpty()) {
        constantBuilder.addJavadoc("$L\n", sanitizeJavadoc(constant.documentation()));
      }

      if ("true".equals(constant.options().get(ENUM_DEPRECATED))) {
        constantBuilder.addAnnotation(Deprecated.class);
      }

      builder.addEnumConstant(constant.name(), constantBuilder.build());

      // Ensure constant case tags are unique, which might not be the case if allow_alias is true.
      if (seenTags.add(constant.tag())) {
        fromValueBuilder.addStatement("case $L: return $L", constant.tag(), constant.name());
      }
    }

    builder.addMethod(fromValueBuilder.addStatement("default: return null")
        .endControlFlow()
        .build());

    builder.addField(FieldSpec.builder(adapterOf(javaType), "ADAPTER")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer("$T.newEnumAdapter($T.class)", ProtoAdapter.class, javaType)
        .build());

    // Enum type options.
    FieldSpec options = optionsField(ENUM_OPTIONS, "ENUM_OPTIONS", type.options());
    if (options != null) {
      builder.addField(options);
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
    NameAllocator nameAllocator = typeToNameAllocator.getUnchecked(type);

    ClassName javaType = (ClassName) typeName(type.type());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName());
    builder.addModifiers(PUBLIC, FINAL);

    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    if (!type.documentation().isEmpty()) {
      builder.addJavadoc("$L\n", sanitizeJavadoc(type.documentation()));
    }

    builder.superclass(messageOf(javaType, builderJavaType));

    String protoAdapterName = "ProtoAdapter_" + javaType.simpleName();
    // TODO(jw): Update this to use the single-argument newName when JavaPoet is updated.
    String protoAdapterClassName = nameAllocator.newName(protoAdapterName, protoAdapterName);
    ClassName adapterJavaType = javaType.nestedClass(protoAdapterClassName);
    builder.addField(messageAdapterField(nameAllocator, javaType, adapterJavaType));
    // Note: The non-compact implementation is added at the very bottom of the surrounding type.

    builder.addField(FieldSpec.builder(TypeName.LONG, nameAllocator.get("serialVersionUID"))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$LL", 0L)
        .build());

    FieldSpec messageOptions = optionsField(
        MESSAGE_OPTIONS, nameAllocator.get("MESSAGE_OPTIONS"), type.options());
    if (messageOptions != null) {
      builder.addField(messageOptions);
    }

    for (Field field : type.fieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      String optionsFieldName = "FIELD_OPTIONS_" + fieldName.toUpperCase(Locale.US);
      FieldSpec fieldOptions = optionsField(FIELD_OPTIONS, optionsFieldName, field.options());
      if (fieldOptions != null) {
        builder.addField(fieldOptions);
      }
    }

    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName fieldJavaType = fieldType(field);

      if ((field.type().isScalar() || isEnum(field.type()))
          && !field.isRepeated()
          && !field.isPacked()) {
        builder.addField(defaultField(nameAllocator, field, fieldJavaType));
      }

      String fieldName = nameAllocator.get(field);
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldJavaType, fieldName, PUBLIC, FINAL);
      fieldBuilder.addAnnotation(wireFieldAnnotation(field));
      if (!field.documentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", sanitizeJavadoc(field.documentation()));
      }
      if (field.isExtension()) {
        fieldBuilder.addJavadoc("Extension source: $L\n", field.location().withoutBase());
      }
      if (field.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      if (emitAndroid && field.isOptional()) {
        fieldBuilder.addAnnotation(NULLABLE);
      }
      builder.addField(fieldBuilder.build());
    }

    builder.addMethod(messageFieldsConstructor(nameAllocator, type));
    builder.addMethod(messageFieldsAndUnknownFieldsConstructor(nameAllocator, type));
    builder.addMethod(newBuilder(nameAllocator, type));
    builder.addMethod(messageEquals(nameAllocator, type));
    builder.addMethod(messageHashCode(nameAllocator, type));

    if (emitAndroid) {
      String adapterName = nameAllocator.get("ADAPTER");

      builder.addSuperinterface(PARCELABLE);

      builder.addMethod(MethodSpec.methodBuilder("writeToParcel")
          .addAnnotation(Override.class)
          .addModifiers(PUBLIC)
          .addParameter(PARCEL, "out")
          .addParameter(int.class, "flags")
          .addStatement("out.writeByteArray($N.encode(this))", adapterName)
          .build());

      builder.addMethod(MethodSpec.methodBuilder("describeContents")
          .addAnnotation(Override.class)
          .addModifiers(PUBLIC)
          .returns(int.class)
          .addStatement("return 0")
          .build());

      TypeName creatorType = creatorOf(javaType);
      builder.addField(
          FieldSpec.builder(creatorType, nameAllocator.get("CREATOR"), PUBLIC, STATIC, FINAL)
              .initializer("$L", TypeSpec.anonymousClassBuilder("")
                  .superclass(creatorType)
                  .addMethod(MethodSpec.methodBuilder("createFromParcel")
                      .addAnnotation(Override.class)
                      .addModifiers(PUBLIC)
                      .returns(javaType)
                      .addParameter(PARCEL, "in")
                      .beginControlFlow("try")
                      .addStatement("return $N.decode(in.createByteArray())", adapterName)
                      .nextControlFlow("catch ($T e)", IOException.class)
                      .addStatement("throw new $T(e)", RuntimeException.class)
                      .endControlFlow()
                      .build())
                  .addMethod(MethodSpec.methodBuilder("newArray")
                      .addAnnotation(Override.class)
                      .addModifiers(PUBLIC)
                      .returns(ArrayTypeName.of(javaType))
                      .addParameter(int.class, "size")
                      .addStatement("return new $T[size]", javaType)
                      .build())
                  .build())
              .build());
    }

    if (!emitCompact) {
      builder.addMethod(messageToString(nameAllocator, type));
    }

    builder.addType(builder(nameAllocator, type, javaType, builderJavaType));

    for (Type nestedType : type.nestedTypes()) {
      TypeSpec typeSpec = nestedType instanceof MessageType
          ? generateMessage((MessageType) nestedType)
          : generateEnum((EnumType) nestedType);
      builder.addType(typeSpec);
    }

    if (!emitCompact) {
      // Add the ProtoAdapter implementation at the very bottom since it's ugly serialization code.
      builder.addType(
           messageAdapter(nameAllocator, type, javaType, adapterJavaType, builderJavaType));
    }

    return builder.build();
  }

  /** Returns the set of names that are not unique within {@code fields}. */
  private Set<String> collidingFieldNames(ImmutableList<Field> fields) {
    Set<String> fieldNames = new LinkedHashSet<>();
    Set<String> collidingNames = new LinkedHashSet<>();
    for (Field field : fields) {
      if (!fieldNames.add(field.name())) {
        collidingNames.add(field.name());
      }
    }
    return collidingNames;
  }

  private FieldSpec messageAdapterField(NameAllocator nameAllocator, ClassName javaType,
      ClassName adapterJavaType) {
    FieldSpec.Builder result = FieldSpec.builder(adapterOf(javaType), nameAllocator.get("ADAPTER"))
        .addModifiers(PUBLIC, STATIC, FINAL);
    if (emitCompact) {
      result.initializer("$T.newMessageAdapter($T.class)", ProtoAdapter.class, javaType);
    } else {
      result.initializer("new $T()", adapterJavaType);
    }
    return result.build();
  }

  private TypeSpec messageAdapter(NameAllocator nameAllocator, MessageType type, ClassName javaType,
      ClassName adapterJavaType, ClassName builderJavaType) {
    TypeSpec.Builder adapter = TypeSpec.classBuilder(adapterJavaType.simpleName())
            .addModifiers(PRIVATE, STATIC, FINAL)
            .superclass(adapterOf(javaType));

    adapter.addMethod(MethodSpec.constructorBuilder()
        .addStatement("super($T.LENGTH_DELIMITED, $T.class)", FieldEncoding.class, javaType)
        .build());

    adapter.addMethod(messageAdapterEncodedSize(nameAllocator, type, javaType));
    adapter.addMethod(messageAdapterEncode(nameAllocator, type, javaType));
    adapter.addMethod(messageAdapterDecode(nameAllocator, type, javaType, builderJavaType));
    adapter.addMethod(messageAdapterRedact(nameAllocator, type, javaType, builderJavaType));
    return adapter.build();
  }

  private MethodSpec messageAdapterEncodedSize(NameAllocator nameAllocator, MessageType type,
      TypeName javaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("encodedSize")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class)
        .addParameter(javaType, "value");

    result.addCode("$[");
    String leading = "return";
    for (Field field : type.fieldsAndOneOfFields()) {
      int fieldTag = field.tag();
      String fieldName = nameAllocator.get(field);
      CodeBlock adapter = adapterFor(field);
      if (field.isRequired() || field.isRepeated()) {
        result.addCode("$L $L.encodedSizeWithTag($L, value.$L)",
            leading, adapter, fieldTag, fieldName);
      } else {
        result.addCode("$1L (value.$4L != null ? $2L.encodedSizeWithTag($3L, value.$4L) : 0)",
            leading, adapter, fieldTag, fieldName);
      }
      leading = "\n+";
    }
    result.addCode("$L value.unknownFields().size();$]\n", leading);

    return result.build();
  }

  private MethodSpec messageAdapterEncode(NameAllocator nameAllocator, MessageType type,
      TypeName javaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("encode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(ProtoWriter.class, "writer")
        .addParameter(javaType, "value")
        .addException(IOException.class);

    for (Field field : type.fieldsAndOneOfFields()) {
      int fieldTag = field.tag();
      String fieldName = nameAllocator.get(field);
      CodeBlock adapter = adapterFor(field);
      if (field.isRequired()) {
        result.addStatement("$L.encodeWithTag(writer, $L, value.$L)", adapter, fieldTag, fieldName);
      } else {
        result.addStatement("if (value.$3L != null) $1L.encodeWithTag(writer, $2L, value.$3L)",
            adapter, fieldTag, fieldName);
      }
    }
    result.addStatement("writer.writeBytes(value.unknownFields())");

    return result.build();
  }

  private MethodSpec messageAdapterDecode(NameAllocator nameAllocator, MessageType type,
      TypeName javaType, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("decode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType)
        .addParameter(ProtoReader.class, "reader")
        .addException(IOException.class);

    result.addStatement("$1T builder = new $1T()", builderJavaType);
    result.addStatement("long token = reader.beginMessage()");
    result.beginControlFlow("for (int tag; (tag = reader.nextTag()) != -1;)");
    result.beginControlFlow("switch (tag)");

    for (Field field : type.fieldsAndOneOfFields()) {
      int fieldTag = field.tag();
      String fieldName = nameAllocator.get(field);
      CodeBlock adapter = singleAdapterFor(field);

      if (isEnum(field.type())) {
        result.beginControlFlow("case $L:", fieldTag);
        result.beginControlFlow("try");
        if (field.isRepeated()) {
          result.addStatement("builder.$L.add($L.decode(reader))", fieldName, adapter);
        } else {
          result.addStatement("builder.$L($L.decode(reader))", fieldName, adapter);
        }
        result.nextControlFlow("catch ($T e)", EnumConstantNotFoundException.class);
        result.addStatement("builder.addUnknownField(tag, $T.VARINT, (long) e.value)",
            FieldEncoding.class);
        result.endControlFlow(); // try/catch
        result.addStatement("break");
        result.endControlFlow(); // case
      } else {
        if (field.isRepeated()) {
          result.addStatement("case $L: builder.$L.add($L.decode(reader)); break", fieldTag,
              fieldName, adapter);
        } else {
          result.addStatement("case $L: builder.$L($L.decode(reader)); break", fieldTag, fieldName,
              adapter);
        }
      }
    }

    result.beginControlFlow("default:");
    result.addStatement("$T fieldEncoding = reader.peekFieldEncoding()", FieldEncoding.class);
    result.addStatement("$T value = fieldEncoding.rawProtoAdapter().decode(reader)", Object.class);
    result.addStatement("builder.addUnknownField(tag, fieldEncoding, value)");
    result.endControlFlow(); // default

    result.endControlFlow(); // switch
    result.endControlFlow(); // for
    result.addStatement("reader.endMessage(token)");
    result.addStatement("return builder.build()");
    return result.build();
  }

  private MethodSpec messageAdapterRedact(NameAllocator nameAllocator, MessageType type,
      ClassName javaType, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("redact")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType)
        .addParameter(javaType, "value");

    List<String> requiredRedacted = new ArrayList<>();
    for (Field field : type.fieldsAndOneOfFields()) {
      if (field.isRequired() && isRedacted(field)) {
        requiredRedacted.add(nameAllocator.get(field));
      }
    }
    if (!requiredRedacted.isEmpty()) {
      boolean isPlural = requiredRedacted.size() != 1;
      result.addStatement("throw new $T($S)", UnsupportedOperationException.class,
          (isPlural ? "Fields" : "Field") + " '" + Joiner.on("', '").join(requiredRedacted) + "' "
              + (isPlural ? "are" : "is") + " required and cannot be redacted.");
      return result.build();
    }

    result.addStatement("$1T builder = value.newBuilder()", builderJavaType);

    for (Field field : type.fieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      if (isRedacted(field)) {
        if (field.isRepeated()) {
          result.addStatement("builder.$N = $T.emptyList()", fieldName, Collections.class);
        } else {
          result.addStatement("builder.$N = null", fieldName);
        }
      } else if (!field.type().isScalar() && !isEnum(field.type())) {
        if (field.isRepeated()) {
          CodeBlock adapter = singleAdapterFor(field);
          result.addStatement("$T.redactElements(builder.$N, $L)", WireInternal.class, fieldName,
              adapter);
        } else {
          CodeBlock adapter = adapterFor(field);
          result.addStatement("if (builder.$1N != null) builder.$1N = $2L.redact(builder.$1N)",
              fieldName, adapter);
        }
      }
    }

    result.addStatement("builder.clearUnknownFields()");

    result.addStatement("return builder.build()");
    return result.build();
  }

  // Example:
  //
  // public static final FieldOptions FIELD_OPTIONS_FOO = new FieldOptions.Builder()
  //     .setExtension(Ext_custom_options.count, 42)
  //     .build();
  //
  private FieldSpec optionsField(ProtoType optionsType, String fieldName, Options options) {
    TypeName optionsJavaType = typeName(optionsType);

    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add("$[new $T.Builder()", optionsJavaType);

    boolean empty = true;
    for (Map.Entry<ProtoMember, ?> entry : options.map().entrySet()) {
      if (entry.getKey().equals(FIELD_DEPRECATED) || entry.getKey().equals(PACKED)) {
        continue;
      }

      Field optionField = schema.getField(entry.getKey());
      initializer.add("\n.$L($L)", fieldName(optionsType, optionField),
          fieldInitializer(optionField.type(), entry.getValue()));
      empty = false;
    }
    initializer.add("\n.build()$]");
    if (empty) return null;

    return FieldSpec.builder(optionsJavaType, fieldName)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer(initializer.build())
        .build();
  }

  private String fieldName(ProtoType type, Field field) {
    MessageType messageType = (MessageType) schema.getType(type);
    NameAllocator names = typeToNameAllocator.getUnchecked(messageType);
    return names.get(field);
  }

  private TypeName fieldType(Field field) {
    TypeName messageType = typeName(field.type());
    return field.isRepeated() ? listOf(messageType) : messageType;
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private FieldSpec defaultField(NameAllocator nameAllocator, Field field, TypeName fieldType) {
    String defaultFieldName = "DEFAULT_" + nameAllocator.get(field).toUpperCase(Locale.US);
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

    // We allow any package name to be used as long as it ends with '.redacted'.
    if (isRedacted(field)) {
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
  private MethodSpec messageFieldsConstructor(NameAllocator nameAllocator, MessageType type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder();
    result.addModifiers(PUBLIC);
    result.addCode("this(");
    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = nameAllocator.get(field);
      ParameterSpec.Builder param = ParameterSpec.builder(javaType, fieldName);
      if (emitAndroid && field.isOptional()) {
        param.addAnnotation(NULLABLE);
      }
      result.addParameter(param.build());
      result.addCode("$L, ", fieldName);
    }
    result.addCode("$T.EMPTY);\n", BYTE_STRING);
    return result.build();
  }

  // Example:
  //
  // public SimpleMessage(int optional_int32, long optional_int64, ByteString unknownFields) {
  //   super(unknownFields);
  //   this.optional_int32 = optional_int32;
  //   this.optional_int64 = optional_int64;
  // }
  //
  private MethodSpec messageFieldsAndUnknownFieldsConstructor(
      NameAllocator nameAllocator, MessageType type) {
    String unknownFieldsName = nameAllocator.get("unknownFields");
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addStatement("super($N)", unknownFieldsName);

    for (OneOf oneOf : type.oneOfs()) {
      if (oneOf.fields().size() < 2) continue;
      CodeBlock.Builder fieldNamesBuilder = CodeBlock.builder();
      boolean first = true;
      for (Field field : oneOf.fields()) {
        if (!first) fieldNamesBuilder.add(", ");
        fieldNamesBuilder.add("$N", nameAllocator.get(field));
        first = false;
      }
      CodeBlock fieldNames = fieldNamesBuilder.build();
      result.beginControlFlow("if ($T.countNonNull($L) > 1)", WireInternal.class, fieldNames);
      result.addStatement("throw new IllegalArgumentException($S)",
          "at most one of " + fieldNames + " may be non-null");
      result.endControlFlow();
    }
    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = nameAllocator.get(field);
      ParameterSpec.Builder param = ParameterSpec.builder(javaType, fieldName);
      if (emitAndroid && field.isOptional()) {
        param.addAnnotation(NULLABLE);
      }
      result.addParameter(param.build());
      if (field.isRepeated()) {
        result.addStatement("this.$1L = $2T.immutableCopyOf($1S, $1L)", fieldName,
            WireInternal.class);
      } else {
        result.addStatement("this.$1L = $1L", fieldName);
      }
    }

    result.addParameter(BYTE_STRING, unknownFieldsName);

    return result.build();
  }

  // Example:
  //
  // @Override
  // public boolean equals(Object other) {
  //   if (other == this) return true;
  //   if (!(other instanceof SimpleMessage)) return false;
  //   SimpleMessage o = (SimpleMessage) other;
  //   return equals(unknownFields(), o.unknownFields())
  //       && equals(optional_int32, o.optional_int32);
  //
  private MethodSpec messageEquals(NameAllocator nameAllocator, MessageType type) {
    String otherName = nameAllocator.get("other");
    String oName = nameAllocator.get("o");

    TypeName javaType = typeName(type.type());
    MethodSpec.Builder result = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(boolean.class)
        .addParameter(Object.class, otherName);

    List<Field> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty()) {
      result.addStatement("return $N instanceof $T", otherName, javaType);
      return result.build();
    }

    result.addStatement("if ($N == this) return true", otherName);
    result.addStatement("if (!($N instanceof $T)) return false", otherName, javaType);

    result.addStatement("$T $N = ($T) $N", javaType, oName, javaType, otherName);
    result.addCode("$[return $T.equals(unknownFields(), $N.unknownFields())", WireInternal.class,
        oName);
    for (Field field : fields) {
      String fieldName = nameAllocator.get(field);
      result.addCode("\n&& $T.equals($L, $N.$L)", WireInternal.class, fieldName, oName, fieldName);
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
  //     result = unknownFields().hashCode();
  //     result = result * 37 + (f != null ? f.hashCode() : 0);
  //     hashCode = result;
  //   }
  //   return result;
  // }
  //
  // For repeated fields, the final "0" in the example above changes to a "1"
  // in order to be the same as the system hash code for an empty list.
  //
  private MethodSpec messageHashCode(NameAllocator nameAllocator, MessageType type) {
    String resultName = nameAllocator.get("result");
    MethodSpec.Builder result = MethodSpec.methodBuilder("hashCode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class);

    List<Field> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty()) {
      result.addStatement("return unknownFields().hashCode()");
      return result.build();
    }

    result.addStatement("int $N = super.hashCode", resultName);
    result.beginControlFlow("if ($N == 0)", resultName);
    result.addStatement("$N = unknownFields().hashCode()", resultName);
    for (Field field : fields) {
      String fieldName = nameAllocator.get(field);
      result.addStatement("$N = $N * 37 + ($L != null ? $L.hashCode() : $L)",
          resultName, resultName, fieldName, fieldName, nullHashValue(field));
    }
    result.addStatement("super.hashCode = $N", resultName);
    result.endControlFlow();
    result.addStatement("return $N", resultName);
    return result.build();
  }

  private MethodSpec messageToString(NameAllocator nameAllocator, MessageType type) {
    String builderName = nameAllocator.get("builder");
    MethodSpec.Builder result = MethodSpec.methodBuilder("toString")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class);

    result.addStatement("$1T $2N = new $1T()", StringBuilder.class, builderName);

    for (Field field : type.fieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      if (isRedacted(field)) {
        result.addStatement("if ($L != null) $N.append(\", $N=██\")", fieldName, builderName,
            field.name());
      } else {
        result.addStatement("if ($1L != null) $2N.append(\", $3N=\").append($1L)", fieldName,
            builderName, field.name());
      }
    }

    result.addStatement("return builder.replace(0, 2, \"$L{\").append('}').toString()",
        type.type().simpleName());

    return result.build();
  }

  private TypeSpec builder(NameAllocator nameAllocator, MessageType type, ClassName javaType,
      ClassName builderType) {
    TypeSpec.Builder result = TypeSpec.classBuilder("Builder")
        .addModifiers(PUBLIC, STATIC, FINAL);

    result.superclass(builderOf(javaType, builderType));

    for (Field field : type.fieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      result.addField(fieldType(field), fieldName, PUBLIC);
    }

    result.addMethod(builderNoArgsConstructor(nameAllocator, type));

    for (Field field : type.fields()) {
      result.addMethod(setter(nameAllocator, builderType, null, field));
    }

    for (OneOf oneOf : type.oneOfs()) {
      for (Field field : oneOf.fields()) {
        result.addMethod(setter(nameAllocator, builderType, oneOf, field));
      }
    }

    result.addMethod(builderBuild(nameAllocator, type, javaType));
    return result.build();
  }

  // Example:
  //
  // public Builder() {
  //   names = newMutableList();
  // }
  //
  private MethodSpec builderNoArgsConstructor(NameAllocator nameAllocator, MessageType type) {
    MethodSpec.Builder result = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
    for (Field field : type.fieldsAndOneOfFields()) {
      if (field.isPacked() || field.isRepeated()) {
        String fieldName = nameAllocator.get(field);
        result.addStatement("$L = $T.newMutableList()", fieldName, WireInternal.class);
      }
    }
    return result.build();
  }

  // Example:
  //
  // @Override
  // public Message.Builder newBuilder() {
  //   Builder builder = new Builder();
  //   builder.optional_int32 = optional_int32;
  //   ...
  //   builder.addUnknownFields(unknownFields());
  //   return builder;
  // }
  private MethodSpec newBuilder(NameAllocator nameAllocator, MessageType message) {
    String builderName = nameAllocator.get("builder");
    ClassName javaType = (ClassName) typeName(message.type());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    MethodSpec.Builder result = MethodSpec.methodBuilder("newBuilder")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(builderJavaType)
        .addStatement("$1T $2L = new $1T()", builderJavaType, builderName);

    List<Field> fields = message.fieldsAndOneOfFields();
    for (Field field : fields) {
      String fieldName = nameAllocator.get(field);
      if (field.isRepeated()) {
        result.addStatement("$1L.$2L = $3T.copyOf($2S, $2L)", builderName, fieldName,
            WireInternal.class);
      } else {
        result.addStatement("$1L.$2L = $2L", builderName, fieldName);
      }
    }

    result.addStatement("$L.addUnknownFields(unknownFields())", builderName);
    result.addStatement("return $L", builderName);
    return result.build();
  }

  private MethodSpec setter(
      NameAllocator nameAllocator, TypeName builderType, OneOf oneOf, Field field) {
    TypeName javaType = fieldType(field);
    String fieldName = nameAllocator.get(field);

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
      result.addStatement("$T.checkElementsNotNull($L)", WireInternal.class, fieldName);
    }
    result.addStatement("this.$L = $L", fieldName, fieldName);

    if (oneOf != null) {
      for (Field other : oneOf.fields()) {
        if (field != other) {
          result.addStatement("this.$L = null", nameAllocator.get(other));
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
  private MethodSpec builderBuild(
      NameAllocator nameAllocator, MessageType message, ClassName javaType) {
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
          .addStatement("throw $T.missingRequiredFields($L)", WireInternal.class,
              missingArgs.build())
          .endControlFlow();
    }

    result.addCode("return new $T(", javaType);
    for (Field field : message.fieldsAndOneOfFields()) {
      result.addCode("$L, ", nameAllocator.get(field));
    }
    result.addCode("buildUnknownFields());\n");
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
        ProtoMember protoMember = (ProtoMember) entry.getKey();
        Field field = schema.getField(protoMember);
        CodeBlock valueInitializer = fieldInitializer(field.type(), entry.getValue());
        builder.add("\n$>$>.$L($L)$<$<", fieldName(type, field), valueInitializer);
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

  private static CodeBlock codeBlock(String format, Object... args) {
    return CodeBlock.builder().add(format, args).build();
  }

  private int nullHashValue(Field field) {
    return field.isRepeated() ? 1 : 0;
  }
}
