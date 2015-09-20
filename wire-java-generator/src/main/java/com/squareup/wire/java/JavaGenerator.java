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
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.wire.Extension;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoAdapter.EnumConstantNotFoundException;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
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
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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
  static final ClassName TAG_MAP = ClassName.get(TagMap.class);
  static final TypeName MESSAGE_OPTIONS = ClassName.get("com.google.protobuf", "MessageOptions");
  static final TypeName FIELD_OPTIONS = ClassName.get("com.google.protobuf", "FieldOptions");
  static final TypeName ENUM_OPTIONS = ClassName.get("com.google.protobuf", "EnumOptions");
  static final ClassName PARCEL = ClassName.get("android.os", "Parcel");
  static final ClassName PARCELABLE = ClassName.get("android.os", "Parcelable");
  static final ClassName CREATOR = PARCELABLE.nestedClass("Creator");

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
  private final boolean emitAndroid;
  private final boolean emitFull;

  private JavaGenerator(Schema schema, ImmutableMap<ProtoType, TypeName> nameToJavaName,
      ImmutableMap<Field, ProtoFile> extensionFieldToFile, boolean emitOptions,
      ImmutableSet<String> enumOptions, boolean emitAndroid, boolean emitFull) {
    this.schema = schema;
    this.nameToJavaName = nameToJavaName;
    this.extensionFieldToFile = extensionFieldToFile;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
    this.emitAndroid = emitAndroid;
    this.emitFull = emitFull;
  }

  public JavaGenerator withOptions(boolean emitOptions, Collection<String> enumOptions) {
    return new JavaGenerator(schema, nameToJavaName, extensionFieldToFile, emitOptions,
        ImmutableSet.copyOf(enumOptions), emitAndroid, emitFull);
  }

  public JavaGenerator withAndroid(boolean emitAndroid) {
    return new JavaGenerator(schema, nameToJavaName, extensionFieldToFile, emitOptions, enumOptions,
        emitAndroid, emitFull);
  }

  public JavaGenerator withFull(boolean fullGeneration) {
    return new JavaGenerator(schema, nameToJavaName, extensionFieldToFile, emitOptions, enumOptions,
        emitAndroid, fullGeneration);
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
        ImmutableSet.<String>of(), false, false);
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
    return ParameterizedTypeName.get(MESSAGE, type);
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

    MethodSpec.Builder fromValueBuilder = MethodSpec.methodBuilder("fromValue")
        .addJavadoc("Return the constant for {@code value} or null.\n")
        .addModifiers(PUBLIC, STATIC)
        .returns(javaType)
        .addParameter(int.class, "value")
        .beginControlFlow("switch (value)");

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

      fromValueBuilder.addStatement("case $L: return $L", constant.tag(), constant.name());
    }

    builder.addMethod(fromValueBuilder.addStatement("default: return null")
        .endControlFlow()
        .build());

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
    // Preallocate all of the names we'll need for this type. Names are allocated in precedence
    // order, so names we're stuck with (serialVersionUID etc.) occur before proto field names are
    // assigned. Names we aren't stuck with (typically for locals) yield to message fields.
    NameAllocator nameAllocator = new NameAllocator();
    nameAllocator.newName("serialVersionUID", "serialVersionUID");
    nameAllocator.newName("ADAPTER", "ADAPTER");
    nameAllocator.newName("MESSAGE_OPTIONS", "MESSAGE_OPTIONS");
    if (emitAndroid) {
      nameAllocator.newName("CREATOR", "CREATOR");
    }
    for (Field field : type.fieldsAndOneOfFields()) {
      nameAllocator.newName(field.name(), field);
    }
    nameAllocator.newName("tagMap", "tagMap");
    nameAllocator.newName("result", "result");
    nameAllocator.newName("message", "message");
    nameAllocator.newName("other", "other");
    nameAllocator.newName("o", "o");
    nameAllocator.newName("builder", "builder");

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

    builder.addField(messageAdapter(nameAllocator, type, javaType, builderJavaType));

    builder.addField(FieldSpec.builder(TypeName.LONG, nameAllocator.get("serialVersionUID"))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$LL", 0L)
        .build());

    if (emitOptions) {
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
    }

    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName fieldJavaType = fieldType(field);

      if ((field.type().isScalar() || isEnum(field.type()))
          && !field.isRepeated()
          && !field.isPacked()) {
        builder.addField(defaultField(field, fieldJavaType));
      }

      String fieldName = nameAllocator.get(field);
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldJavaType, fieldName, PUBLIC, FINAL);
      if (!emitFull) {
        fieldBuilder.addAnnotation(wireFieldAnnotation(field));
      }
      if (!field.documentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", sanitizeJavadoc(field.documentation()));
      }
      if (field.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      builder.addField(fieldBuilder.build());
    }

    builder.addMethod(messageFieldsConstructor(nameAllocator, type));
    builder.addMethod(messageFieldsAndTagMapConstructor(nameAllocator, type));
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
                      .addStatement("return $N.decode(in.createByteArray())", adapterName)
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

    if (emitFull) {
      builder.addMethod(messageToString(nameAllocator, type));
    }

    builder.addType(builder(nameAllocator, type, javaType, builderJavaType));

    for (Type nestedType : type.nestedTypes()) {
      TypeSpec typeSpec = nestedType instanceof MessageType
          ? generateMessage((MessageType) nestedType)
          : generateEnum((EnumType) nestedType);
      builder.addType(typeSpec);
    }

    return builder.build();
  }

  private FieldSpec messageAdapter(NameAllocator nameAllocator, MessageType type,
      ClassName javaType, ClassName builderJavaType) {
    FieldSpec.Builder result = FieldSpec.builder(adapterOf(javaType), nameAllocator.get("ADAPTER"))
        .addModifiers(PUBLIC, STATIC, FINAL);
    if (!emitFull) {
      result.initializer("$T.newMessageAdapter($T.class)", ProtoAdapter.class, javaType);
    } else {
      TypeSpec.Builder adapter =
          TypeSpec.anonymousClassBuilder("$T.LENGTH_DELIMITED, $T.class", FieldEncoding.class,
              javaType).superclass(adapterOf(javaType));

      adapter.addMethod(messageAdapterEncodedSize(nameAllocator, type, javaType));
      adapter.addMethod(messageAdapterEncode(nameAllocator, type, javaType));
      adapter.addMethod(messageAdapterDecode(nameAllocator, type, javaType, builderJavaType));
      adapter.addMethod(messageAdapterRedact(nameAllocator, type, javaType, builderJavaType));

      result.initializer("$L", adapter.build());
    }
    return result.build();
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
        result.addCode("$L $L.encodedSize($L, value.$L)", leading, adapter, fieldTag, fieldName);
      } else {
        result.addCode("$1L (value.$4L != null ? $2L.encodedSize($3L, value.$4L) : 0)", leading,
            adapter, fieldTag, fieldName);
      }
      leading = "\n+";
    }
    result.addCode("$L value.tagMap().encodedSize();$]\n", leading);

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
        result.addStatement("$L.encodeTagged(writer, $L, value.$L)", adapter, fieldTag, fieldName);
      } else {
        result.addStatement("if (value.$3L != null) $1L.encodeTagged(writer, $2L, value.$3L)",
            adapter, fieldTag, fieldName);
      }
    }
    result.addStatement("value.tagMap().encode(writer)");

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
        result.addStatement("$T unknown = $T.unknown($T.class, tag, $T.VARINT)",
            extensionOf(javaType, TypeName.LONG.box()), EXTENSION, javaType, FieldEncoding.class);
        result.addStatement("builder.tagMap().add(unknown, (long) e.value)");
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
    result.beginControlFlow("try");
    WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
    result.addStatement("$T extension = reader.getExtension($T.class, tag)",
        extensionOf(javaType, wildcard), javaType);
    result.addStatement("builder.tagMap().add(extension, extension.getAdapter().decode(reader))");
    result.nextControlFlow("catch ($T e)", EnumConstantNotFoundException.class);
    result.addStatement("$T unknown = $T.unknown($T.class, tag, $T.VARINT)",
        extensionOf(javaType, TypeName.LONG.box()), EXTENSION, javaType, FieldEncoding.class);
    result.addStatement("builder.tagMap().add(unknown, (long) e.value)");
    result.endControlFlow(); // try/catch
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

    result.addStatement("$1T builder = new $1T(value)", builderJavaType);

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
          result.addStatement("redactElements(builder.$N, $L)", fieldName, adapter);
        } else {
          CodeBlock adapter = adapterFor(field);
          result.addStatement("if (builder.$1N != null) builder.$1N = $2L.redact(builder.$1N)",
              fieldName, adapter);
        }
      }
    }

    result.addStatement("builder.tagMap().redact()");

    result.addStatement("return builder.build()");
    return result.build();
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
      result.addParameter(javaType, fieldName);
      result.addCode("$L, ", fieldName);
    }
    result.addCode("$T.EMPTY);\n", TAG_MAP);
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
  private MethodSpec messageFieldsAndTagMapConstructor(
      NameAllocator nameAllocator, MessageType type) {
    String tagMapName = nameAllocator.get("tagMap");
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addStatement("super($N)", tagMapName);

    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = nameAllocator.get(field);
      result.addParameter(javaType, fieldName);
      if (field.isRepeated()) {
        result.addStatement("this.$L = immutableCopyOf($L)", fieldName, fieldName);
      } else {
        result.addStatement("this.$L = $L", fieldName, fieldName);
      }
    }

    result.addParameter(TAG_MAP, tagMapName);

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
  private MethodSpec messageEquals(NameAllocator nameAllocator, MessageType type) {
    String otherName = nameAllocator.get("other");
    String oName = nameAllocator.get("o");

    TypeName javaType = typeName(type.name());
    MethodSpec.Builder result = MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(boolean.class)
        .addParameter(Object.class, otherName);

    List<Field> fields = type.fieldsAndOneOfFields();
    if (fields.isEmpty() && type.extensions().isEmpty()) {
      result.addStatement("return $N instanceof $T", otherName, javaType);
      return result.build();
    }

    result.addStatement("if ($N == this) return true", otherName);
    result.addStatement("if (!($N instanceof $T)) return false", otherName, javaType);

    result.addStatement("$T $N = ($T) $N", javaType, oName, javaType, otherName);
    result.addCode("$[return equals(tagMap(), $N.tagMap())", oName);
    for (Field field : fields) {
      String fieldName = nameAllocator.get(field);
      result.addCode("\n&& equals($L, $N.$L)", fieldName, oName, fieldName);
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
  //     result = tagMap().hashCode();
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
      result.addStatement("return tagMap().hashCode()");
      return result.build();
    }

    result.addStatement("int $N = super.hashCode", resultName);
    result.beginControlFlow("if ($N == 0)", resultName);
    result.addStatement("$N = tagMap().hashCode()", resultName);
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

    result.addStatement("tagMap().appendToString($N)", builderName);
    result.addStatement("return builder.replace(0, 2, \"$L{\").append('}').toString()",
        type.name().simpleName());

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
    result.addMethod(builderCopyConstructor(nameAllocator, type));

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
        result.addStatement("$L = newMutableList()", fieldName);
      }
    }
    return result.build();
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
  private MethodSpec builderCopyConstructor(NameAllocator nameAllocator, MessageType message) {
    String messageName = nameAllocator.get("message");
    TypeName javaType = typeName(message.name());

    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(javaType, messageName);
    result.addStatement("super($N)", messageName);

    List<Field> fields = message.fieldsAndOneOfFields();
    if (!fields.isEmpty()) {
      result.addStatement("if ($N == null) return", messageName);
    }

    for (Field field : fields) {
      String fieldName = nameAllocator.get(field);
      if (field.isRepeated()) {
        result.addStatement("this.$L = copyOf($N.$L)", fieldName, messageName, fieldName);
      } else {
        result.addStatement("this.$L = $N.$L", fieldName, messageName, fieldName);
      }
    }

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
      result.addStatement("checkElementsNotNull($L)", fieldName);
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
          .addStatement("throw missingRequiredFields($L)", missingArgs.build())
          .endControlFlow();
    }

    result.addCode("return new $T(", javaType);
    for (Field field : message.fieldsAndOneOfFields()) {
      result.addCode("$L, ", nameAllocator.get(field));
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

    List<String> names = new ArrayList<>();
    for (Extend extend : protoFile.extendList()) {
      ProtoType extendType = extend.type();
      TypeName javaType = typeName(extendType);

      if (!emitOptions && (extendType.equals(Options.FIELD_OPTIONS)
          || extendType.equals(Options.MESSAGE_OPTIONS))) {
        continue;
      }

      for (Field field : extend.fields()) {
        FieldSpec fieldSpec = extensionField(protoFile, javaType, field);
        names.add(fieldSpec.name);
        builder.addField(fieldSpec);
      }
    }

    WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
    TypeName extensionType = ParameterizedTypeName.get(EXTENSION, wildcard, wildcard);
    TypeName listOfExtensionsType = ParameterizedTypeName.get(LIST, extensionType);
    builder.addField(FieldSpec.builder(listOfExtensionsType, "EXTENSIONS", PUBLIC, STATIC, FINAL)
        .initializer("$[$T.<$T>asList(\n$L)$]", Arrays.class, extensionType,
            Joiner.on(",\n").join(names))
        .build());

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
