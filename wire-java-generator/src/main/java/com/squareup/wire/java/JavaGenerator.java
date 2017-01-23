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
import com.google.common.collect.Ordering;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.EnumAdapter;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoAdapter.EnumConstantNotFoundException;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireEnum;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import com.squareup.wire.schema.EnclosingType;
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
import java.util.Comparator;
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
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
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
  static final ClassName ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage");
  static final ClassName ADAPTER = ClassName.get(ProtoAdapter.class);
  static final ClassName BUILDER = ClassName.get(Message.Builder.class);
  static final ClassName ENUM_ADAPTER = ClassName.get(EnumAdapter.class);
  static final ClassName NULLABLE = ClassName.get("android.support.annotation", "Nullable");
  static final ClassName CREATOR = ClassName.get("android.os", "Parcelable", "Creator");

  private static final Ordering<Field> TAG_ORDERING = Ordering.from(new Comparator<Field>() {
    @Override public int compare(Field o1, Field o2) {
      return Integer.compare(o1.tag(), o2.tag());
    }
  });

  private static final Map<ProtoType, ClassName> BUILT_IN_TYPES_MAP =
      ImmutableMap.<ProtoType, ClassName>builder()
          .put(ProtoType.BOOL, (ClassName) TypeName.BOOLEAN.box())
          .put(ProtoType.BYTES, ClassName.get(ByteString.class))
          .put(ProtoType.DOUBLE, (ClassName) TypeName.DOUBLE.box())
          .put(ProtoType.FLOAT, (ClassName) TypeName.FLOAT.box())
          .put(ProtoType.FIXED32, (ClassName) TypeName.INT.box())
          .put(ProtoType.FIXED64, (ClassName) TypeName.LONG.box())
          .put(ProtoType.INT32, (ClassName) TypeName.INT.box())
          .put(ProtoType.INT64, (ClassName) TypeName.LONG.box())
          .put(ProtoType.SFIXED32, (ClassName) TypeName.INT.box())
          .put(ProtoType.SFIXED64, (ClassName) TypeName.LONG.box())
          .put(ProtoType.SINT32, (ClassName) TypeName.INT.box())
          .put(ProtoType.SINT64, (ClassName) TypeName.LONG.box())
          .put(ProtoType.STRING, ClassName.get(String.class))
          .put(ProtoType.UINT32, (ClassName) TypeName.INT.box())
          .put(ProtoType.UINT64, (ClassName) TypeName.LONG.box())
          .put(FIELD_OPTIONS, ClassName.get("com.google.protobuf", "MessageOptions"))
          .put(ENUM_OPTIONS, ClassName.get("com.google.protobuf", "FieldOptions"))
          .put(MESSAGE_OPTIONS, ClassName.get("com.google.protobuf", "EnumOptions"))
          .build();

  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";

  /**
   * Preallocate all of the names we'll need for {@code type}. Names are allocated in precedence
   * order, so names we're stuck with (serialVersionUID etc.) occur before proto field names are
   * assigned.
   *
   * <p>Name allocations are computed once and reused because some types may be needed when
   * generating other types.
   */
  private final LoadingCache<MessageType, NameAllocator> nameAllocators
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
      return nameAllocator;
    }
  });

  private final Schema schema;
  private final ImmutableMap<ProtoType, ClassName> nameToJavaName;
  private final Profile profile;
  private final boolean emitAndroid;
  private final boolean emitCompact;

  private JavaGenerator(Schema schema, Map<ProtoType, ClassName> nameToJavaName, Profile profile,
      boolean emitAndroid, boolean emitCompact) {
    this.schema = schema;
    this.nameToJavaName = ImmutableMap.copyOf(nameToJavaName);
    this.profile = profile;
    this.emitAndroid = emitAndroid;
    this.emitCompact = emitCompact;
  }

  public JavaGenerator withAndroid(boolean emitAndroid) {
    return new JavaGenerator(schema, nameToJavaName, profile, emitAndroid, emitCompact);
  }

  public JavaGenerator withCompact(boolean emitCompact) {
    return new JavaGenerator(schema, nameToJavaName, profile, emitAndroid, emitCompact);
  }

  public JavaGenerator withProfile(Profile profile) {
    return new JavaGenerator(schema, nameToJavaName, profile, emitAndroid, emitCompact);
  }

  public static JavaGenerator get(Schema schema) {
    Map<ProtoType, ClassName> nameToJavaName = new LinkedHashMap<>();
    nameToJavaName.putAll(BUILT_IN_TYPES_MAP);

    for (ProtoFile protoFile : schema.protoFiles()) {
      String javaPackage = javaPackage(protoFile);
      putAll(nameToJavaName, javaPackage, null, protoFile.types());

      for (Service service : protoFile.services()) {
        ClassName className = ClassName.get(javaPackage, service.type().simpleName());
        nameToJavaName.put(service.type(), className);
      }
    }

    return new JavaGenerator(schema, nameToJavaName, new Profile(), false, false);
  }

  private static void putAll(Map<ProtoType, ClassName> wireToJava, String javaPackage,
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
    TypeName profileJavaName = profile.getTarget(protoType);
    if (profileJavaName != null) return profileJavaName;
    TypeName candidate = nameToJavaName.get(protoType);
    checkArgument(candidate != null, "unexpected type %s", protoType);
    return candidate;
  }

  /**
   * Returns the Java type of the abstract adapter class generated for a corresponding {@code
   * protoType}. Returns null if {@code protoType} is not using a custom proto adapter.
   */
  public ClassName abstractAdapterName(ProtoType protoType) {
    TypeName profileJavaName = profile.getTarget(protoType);
    if (profileJavaName == null) return null;

    ClassName javaName = nameToJavaName.get(protoType);
    return javaName.peerClass("Abstract" + javaName.simpleName() + "Adapter");
  }

  private CodeBlock singleAdapterFor(Field field) {
    return field.type().isMap()
        ? CodeBlock.of("$N", field.name())
        : singleAdapterFor(field.type());
  }

  private CodeBlock singleAdapterFor(ProtoType type) {
    CodeBlock.Builder result = CodeBlock.builder();
    if (type.isScalar()) {
      result.add("$T.$L", ADAPTER, type.simpleName().toUpperCase(Locale.US));
    } else if (type.isMap()) {
      throw new IllegalArgumentException("Cannot create single adapter for map type " + type);
    } else {
      AdapterConstant adapterConstant = profile.getAdapter(type);
      if (adapterConstant != null) {
        result.add("$T.$L", adapterConstant.className, adapterConstant.memberName);
      } else {
        result.add("$T.ADAPTER", typeName(type));
      }
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

  static TypeName messageOf(ClassName messageType, TypeName type, ClassName builderType) {
    return ParameterizedTypeName.get(messageType, type, builderType);
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

  static TypeName enumAdapterOf(TypeName enumType) {
    return ParameterizedTypeName.get(ENUM_ADAPTER, enumType);
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

  /** Returns the full name of the class generated for {@code type}. */
  public ClassName generatedTypeName(Type type) {
    ClassName abstractAdapterName = abstractAdapterName(type.type());
    return abstractAdapterName != null
        ? abstractAdapterName
        : (ClassName) typeName(type.type());
  }

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec generateType(Type type) {
    if (type instanceof MessageType) {
      AdapterConstant adapterConstant = profile.getAdapter(type.type());
      if (adapterConstant != null) {
        return generateAbstractAdapter((MessageType) type);
      }
      //noinspection deprecation: Only deprecated as a public API.
      return generateMessage((MessageType) type);
    }
    if (type instanceof EnumType) {
      //noinspection deprecation: Only deprecated as a public API.
      return generateEnum((EnumType) type);
    }
    if (type instanceof EnclosingType) {
      return generateEnclosingType((EnclosingType) type);
    }
    throw new IllegalStateException("Unknown type: " + type);
  }

  /** @deprecated Use {@link #generateType(Type)} */
  @Deprecated
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

    // ADAPTER
    FieldSpec.Builder adapterBuilder = FieldSpec.builder(adapterOf(javaType), "ADAPTER")
        .addModifiers(PUBLIC, STATIC, FINAL);
    ClassName adapterJavaType = javaType.nestedClass("ProtoAdapter_" + javaType.simpleName());
    if (!emitCompact) {
      adapterBuilder.initializer("new $T()", adapterJavaType);
    } else {
      adapterBuilder.initializer("$T.newEnumAdapter($T.class)", ProtoAdapter.class, javaType);
    }
    builder.addField(adapterBuilder.build());

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

    if (!emitCompact) {
      // Adds the ProtoAdapter implementation at the bottom.
      builder.addType(enumAdapter(javaType, adapterJavaType));
    }

    return builder.build();
  }

  /** @deprecated Use {@link #generateType(Type)} */
  @Deprecated
  public TypeSpec generateMessage(MessageType type) {
    NameAllocator nameAllocator = nameAllocators.getUnchecked(type);

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

    ClassName messageType = emitAndroid ? ANDROID_MESSAGE : MESSAGE;
    builder.superclass(messageOf(messageType, javaType, builderJavaType));

    String adapterName = nameAllocator.get("ADAPTER");
    String protoAdapterName = "ProtoAdapter_" + javaType.simpleName();
    String protoAdapterClassName = nameAllocator.newName(protoAdapterName);
    ClassName adapterJavaType = javaType.nestedClass(protoAdapterClassName);
    builder.addField(messageAdapterField(adapterName, javaType, adapterJavaType));
    // Note: The non-compact implementation is added at the very bottom of the surrounding type.

    if (emitAndroid) {
      TypeName creatorType = creatorOf(javaType);
      String creatorName = nameAllocator.get("CREATOR");
      builder.addField(FieldSpec.builder(creatorType, creatorName, PUBLIC, STATIC, FINAL)
          .initializer("$T.newCreator($L)", ANDROID_MESSAGE, adapterName)
          .build());
    }

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
        fieldBuilder.addJavadoc("Extension source: $L\n", field.location().withPathOnly());
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
    if (!emitCompact) {
      builder.addMethod(messageToString(nameAllocator, type));
    }

    builder.addType(builder(nameAllocator, type, javaType, builderJavaType));

    for (Type nestedType : type.nestedTypes()) {
      builder.addType(generateType(nestedType));
    }

    if (!emitCompact) {
      // Add the ProtoAdapter implementation at the very bottom since it's ugly serialization code.
      builder.addType(
          messageAdapter(nameAllocator, type, javaType, adapterJavaType, builderJavaType));
    }

    return builder.build();
  }

  private TypeSpec generateEnclosingType(EnclosingType type) {
    ClassName javaType = (ClassName) typeName(type.type());

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName())
        .addModifiers(PUBLIC, FINAL);
    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    String documentation = type.documentation();
    if (!documentation.isEmpty()) {
      documentation += "\n\n<p>";
    }
    documentation += "<b>NOTE:</b> This type only exists to maintain class structure"
        + " for its nested types and is not an actual message.\n";
    builder.addJavadoc(documentation);

    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addStatement("throw new $T()", AssertionError.class)
        .build());

    for (Type nestedType : type.nestedTypes()) {
      builder.addType(generateType(nestedType));
    }

    return builder.build();
  }

  /** Returns an abstract adapter for {@code type}. */
  public TypeSpec generateAbstractAdapter(MessageType type) {
    NameAllocator nameAllocator = nameAllocators.getUnchecked(type);
    ClassName adapterTypeName = abstractAdapterName(type.type());
    ClassName typeName = (ClassName) typeName(type.type());
    TypeSpec.Builder adapter = messageAdapter(
        nameAllocator, type, typeName, adapterTypeName, null).toBuilder();

    if (adapterTypeName.enclosingClassName() != null) {
      adapter.addModifiers(STATIC);
    }

    for (Type nestedType : type.nestedTypes()) {
      if (profile.getAdapter(nestedType.type()) == null) {
        throw new IllegalArgumentException("Missing custom proto adapter for "
            + nestedType.type().enclosingTypeOrPackage() + "." + nestedType.type().simpleName()
            + " when enclosing proto has custom proto adapter.");
      }
      if (nestedType instanceof MessageType) {
        adapter.addType(generateAbstractAdapter((MessageType) nestedType));
      }
    }

    return adapter.build();
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

  private FieldSpec messageAdapterField(String adapterName, ClassName javaType,
      ClassName adapterJavaType) {
    FieldSpec.Builder result = FieldSpec.builder(adapterOf(javaType), adapterName)
        .addModifiers(PUBLIC, STATIC, FINAL);
    if (emitCompact) {
      result.initializer("$T.newMessageAdapter($T.class)", ProtoAdapter.class, javaType);
    } else {
      result.initializer("new $T()", adapterJavaType);
    }
    return result.build();
  }

  private TypeSpec enumAdapter(ClassName javaType,  ClassName adapterJavaType) {
    return TypeSpec.classBuilder(adapterJavaType.simpleName())
        .superclass(enumAdapterOf(javaType))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addStatement("super($T.class)", javaType)
            .build())
        .addMethod(MethodSpec.methodBuilder("fromValue")
            .addAnnotation(Override.class)
            .addModifiers(PROTECTED)
            .returns(javaType)
            .addParameter(int.class, "value")
            .addStatement("return $T.fromValue(value)", javaType)
            .build())
        .build();
  }

  private TypeSpec messageAdapter(NameAllocator nameAllocator, MessageType type, ClassName javaType,
      ClassName adapterJavaType, ClassName builderType) {
    boolean useBuilder = builderType != null;
    TypeSpec.Builder adapter = TypeSpec.classBuilder(adapterJavaType.simpleName())
        .superclass(adapterOf(javaType));

    if (useBuilder) {
      adapter.addModifiers(PRIVATE, STATIC, FINAL);
    } else {
      adapter.addModifiers(PUBLIC, ABSTRACT);
    }

    adapter.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addStatement("super($T.LENGTH_DELIMITED, $T.class)", FieldEncoding.class, javaType)
        .build());

    if (!useBuilder) {
      MethodSpec.Builder fromProto = MethodSpec.methodBuilder("fromProto")
          .addModifiers(PUBLIC, ABSTRACT)
          .returns(javaType);

      for (Field field : type.fieldsAndOneOfFields()) {
        TypeName fieldType = fieldType(field);
        String fieldName = nameAllocator.get(field);
        fromProto.addParameter(fieldType, fieldName);
        adapter.addMethod(MethodSpec.methodBuilder(fieldName)
            .addModifiers(PUBLIC, ABSTRACT)
            .addParameter(javaType, "value")
            .returns(fieldType)
            .build());
      }

      adapter.addMethod(fromProto.build());
    }

    adapter.addMethod(messageAdapterEncodedSize(nameAllocator, type, javaType, useBuilder));
    adapter.addMethod(messageAdapterEncode(nameAllocator, type, javaType, useBuilder));
    adapter.addMethod(messageAdapterDecode(nameAllocator, type, javaType, useBuilder, builderType));
    adapter.addMethod(messageAdapterRedact(nameAllocator, type, javaType, useBuilder, builderType));

    for (Field field : type.fieldsAndOneOfFields()) {
      if (field.type().isMap()) {
        TypeName adapterType = adapterOf(fieldType(field));
        adapter.addField(FieldSpec.builder(adapterType, field.name(), PRIVATE, FINAL)
            .initializer("$T.newMapAdapter($L, $L)", ADAPTER,
                singleAdapterFor(field.type().keyType()),
                singleAdapterFor(field.type().valueType()))
            .build());
      }
    }

    return adapter.build();
  }

  private MethodSpec messageAdapterEncodedSize(NameAllocator nameAllocator, MessageType type,
      TypeName javaType, boolean useBuilder) {
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
      result.addCode("$L $L.encodedSizeWithTag($L, ", leading, adapter, fieldTag)
          .addCode((useBuilder ? "value.$L" : "$L(value)"), fieldName)
          .addCode(")");
      leading = "\n+";
    }
    if (useBuilder) {
      result.addCode("$L value.unknownFields().size()", leading);
    }
    result.addCode(";$]\n", leading);

    return result.build();
  }

  private MethodSpec messageAdapterEncode(NameAllocator nameAllocator, MessageType type,
      TypeName javaType, boolean useBuilder) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("encode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(ProtoWriter.class, "writer")
        .addParameter(javaType, "value")
        .addException(IOException.class);

    for (Field field : type.fieldsAndOneOfFields()) {
      int fieldTag = field.tag();
      CodeBlock adapter = adapterFor(field);
      result.addCode("$L.encodeWithTag(writer, $L, ", adapter, fieldTag)
          .addCode((useBuilder ? "value.$L" : "$L(value)"), nameAllocator.get(field))
          .addCode(");\n");
    }

    if (useBuilder) {
      result.addStatement("writer.writeBytes(value.unknownFields())");
    }

    return result.build();
  }

  private MethodSpec messageAdapterDecode(NameAllocator nameAllocator, MessageType type,
      TypeName javaType, boolean useBuilder, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("decode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType)
        .addParameter(ProtoReader.class, "reader")
        .addException(IOException.class);

    List<Field> fields = TAG_ORDERING.sortedCopy(type.fieldsAndOneOfFields());

    if (useBuilder) {
      result.addStatement("$1T builder = new $1T()", builderJavaType);
    } else {
      for (Field field : fields) {
        result.addStatement("$T $N = $L",
            fieldType(field), nameAllocator.get(field), initialValue(field));
      }
    }

    result.addStatement("long token = reader.beginMessage()");
    result.beginControlFlow("for (int tag; (tag = reader.nextTag()) != -1;)");
    result.beginControlFlow("switch (tag)");

    for (Field field : fields) {
      int fieldTag = field.tag();

      if (isEnum(field.type())) {
        result.beginControlFlow("case $L:", fieldTag);
        result.beginControlFlow("try");
        result.addCode(decodeAndAssign(field, nameAllocator, useBuilder));
        result.addCode(";\n");
        if (useBuilder) {
          result.nextControlFlow("catch ($T e)", EnumConstantNotFoundException.class);
          result.addStatement("builder.addUnknownField(tag, $T.VARINT, (long) e.value)",
              FieldEncoding.class);
          result.endControlFlow(); // try/catch
        } else {
          result.nextControlFlow("catch ($T ignored)", EnumConstantNotFoundException.class);
          result.endControlFlow(); // try/catch
        }
        result.addStatement("break");
        result.endControlFlow(); // case
      } else {
        result.addCode("case $L: $L; break;\n",
            fieldTag, decodeAndAssign(field, nameAllocator, useBuilder));
      }
    }

    result.beginControlFlow("default:");
    if (useBuilder) {
      result.addStatement("$T fieldEncoding = reader.peekFieldEncoding()", FieldEncoding.class)
          .addStatement("$T value = fieldEncoding.rawProtoAdapter().decode(reader)", Object.class)
          .addStatement("builder.addUnknownField(tag, fieldEncoding, value)");
    } else {
      result.addStatement("reader.skip()");
    }
    result.endControlFlow(); // default

    result.endControlFlow(); // switch
    result.endControlFlow(); // for
    result.addStatement("reader.endMessage(token)");

    if (useBuilder) {
      result.addStatement("return builder.build()");
    } else {
      result.addCode("return fromProto(");
      boolean first = true;
      for (Field field : type.fieldsAndOneOfFields()) {
        if (!first) result.addCode(", ");
        result.addCode("$N", nameAllocator.get(field));
        first = false;
      }
      result.addCode(");\n");
    }

    return result.build();
  }

  private CodeBlock decodeAndAssign(Field field, NameAllocator nameAllocator, boolean useBuilder) {
    String fieldName = nameAllocator.get(field);
    CodeBlock decode = CodeBlock.of("$L.decode(reader)", singleAdapterFor(field));
    if (field.isRepeated()) {
      return useBuilder
          ? CodeBlock.of("builder.$L.add($L)", fieldName, decode)
          : CodeBlock.of("$L.add($L)", fieldName, decode);
    } else if (field.type().isMap()) {
      return useBuilder
          ? CodeBlock.of("builder.$L.putAll($L)", fieldName, decode)
          : CodeBlock.of("$L.putAll($L)", fieldName, decode);
    } else {
      return useBuilder
          ? CodeBlock.of("builder.$L($L)", fieldName, decode)
          : CodeBlock.of("$L = $L", fieldName, decode);
    }
  }

  private MethodSpec messageAdapterRedact(NameAllocator nameAllocator, MessageType type,
      ClassName javaType, boolean useBuilder, ClassName builderJavaType) {
    MethodSpec.Builder result = MethodSpec.methodBuilder("redact")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(javaType)
        .addParameter(javaType, "value");

    int redactedFieldCount = 0;
    List<String> requiredRedacted = new ArrayList<>();
    for (Field field : type.fieldsAndOneOfFields()) {
      if (field.isRedacted()) {
        redactedFieldCount++;
        if (field.isRequired()) {
          requiredRedacted.add(nameAllocator.get(field));
        }
      }
    }

    if (!useBuilder) {
      result.addStatement((redactedFieldCount == 0) ? "return value" : "return null");
      return result.build();
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
      if (field.isRedacted()) {
        if (field.isRepeated()) {
          result.addStatement("builder.$N = $T.emptyList()", fieldName, Collections.class);
        } else if (field.type().isMap()) {
          result.addStatement("builder.$N = $T.emptyMap()", fieldName, Collections.class);
        } else {
          result.addStatement("builder.$N = null", fieldName);
        }
      } else if (!field.type().isScalar() && !isEnum(field.type())) {
        if (field.isRepeated()) {
          CodeBlock adapter = singleAdapterFor(field);
          result.addStatement("$T.redactElements(builder.$N, $L)", Internal.class, fieldName,
              adapter);
        } else if (field.type().isMap()) {
          // We only need to ask the values to redact themselves if the type is a message.
          if (!field.type().valueType().isScalar() && !isEnum(field.type().valueType())) {
            CodeBlock adapter = singleAdapterFor(field.type().valueType());
            result.addStatement("$T.redactElements(builder.$N, $L)", Internal.class, fieldName,
                adapter);
          }
        } else {
          CodeBlock adapter = adapterFor(field);
          if (!field.isRequired()) {
            result.addCode("if (builder.$N != null) ", fieldName);
          }
          result.addStatement("builder.$1N = $2L.redact(builder.$1N)", fieldName, adapter);
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
    NameAllocator names = nameAllocators.getUnchecked(messageType);
    return names.get(field);
  }

  private TypeName fieldType(Field field) {
    ProtoType type = field.type();
    if (type.isMap()) {
      return ParameterizedTypeName.get(ClassName.get(Map.class),
          typeName(type.keyType()),
          typeName(type.valueType()));
    }
    TypeName messageType = typeName(type);
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
    if (field.type().isMap()) {
      result.addMember("keyAdapter", "$S", adapterString(field.type().keyType()));
      result.addMember("adapter", "$S", adapterString(field.type().valueType()));
    } else {
      result.addMember("adapter", "$S", adapterString(field.type()));
    }

    if (!field.isOptional()) {
      if (field.isPacked()) {
        result.addMember("label", "$T.PACKED", WireField.Label.class);
      } else if (field.label() != null) {
        result.addMember("label", "$T.$L", WireField.Label.class, field.label());
      }
    }

    if (field.isRedacted()) {
      result.addMember("redacted", "true");
    }

    return result.build();
  }

  private String adapterString(ProtoType type) {
    if (type.isScalar()) {
      return ProtoAdapter.class.getName() + '#' + type.toString().toUpperCase(Locale.US);
    }

    AdapterConstant adapterConstant = profile.getAdapter(type);
    if (adapterConstant != null) {
      return reflectionName(adapterConstant.className) + "#" + adapterConstant.memberName;
    }

    return reflectionName((ClassName) typeName(type)) + "#ADAPTER";
  }

  private String reflectionName(ClassName className) {
    return className.packageName().isEmpty()
        ? Joiner.on('$').join(className.simpleNames())
        : className.packageName() + '.' + Joiner.on('$').join(className.simpleNames());
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
  //   super(ADAPTER, unknownFields);
  //   this.optional_int32 = optional_int32;
  //   this.optional_int64 = optional_int64;
  // }
  //
  private MethodSpec messageFieldsAndUnknownFieldsConstructor(
      NameAllocator nameAllocator, MessageType type) {
    NameAllocator localNameAllocator = nameAllocator.clone();

    String adapterName = localNameAllocator.get("ADAPTER");
    String unknownFieldsName = localNameAllocator.newName("unknownFields");
    MethodSpec.Builder result = MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addStatement("super($N, $N)", adapterName, unknownFieldsName);

    for (OneOf oneOf : type.oneOfs()) {
      if (oneOf.fields().size() < 2) continue;
      CodeBlock.Builder fieldNamesBuilder = CodeBlock.builder();
      boolean first = true;
      for (Field field : oneOf.fields()) {
        if (!first) fieldNamesBuilder.add(", ");
        fieldNamesBuilder.add("$N", localNameAllocator.get(field));
        first = false;
      }
      CodeBlock fieldNames = fieldNamesBuilder.build();
      result.beginControlFlow("if ($T.countNonNull($L) > 1)", Internal.class, fieldNames);
      result.addStatement("throw new IllegalArgumentException($S)",
          "at most one of " + fieldNames + " may be non-null");
      result.endControlFlow();
    }
    for (Field field : type.fieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = localNameAllocator.get(field);
      ParameterSpec.Builder param = ParameterSpec.builder(javaType, fieldName);
      if (emitAndroid && field.isOptional()) {
        param.addAnnotation(NULLABLE);
      }
      result.addParameter(param.build());
      if (field.isRepeated() || field.type().isMap()) {
        result.addStatement("this.$1L = $2T.immutableCopyOf($1S, $1L)", fieldName,
            Internal.class);
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
    NameAllocator localNameAllocator = nameAllocator.clone();
    String otherName = localNameAllocator.newName("other");
    String oName = localNameAllocator.newName("o");

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
    result.addCode("$[return unknownFields().equals($N.unknownFields())", oName);
    for (Field field : fields) {
      String fieldName = localNameAllocator.get(field);
      if (field.isRequired() || field.isRepeated() || field.type().isMap()) {
        result.addCode("\n&& $1L.equals($2N.$1L)", fieldName, oName);
      } else {
        result.addCode("\n&& $1T.equals($2L, $3N.$2L)", Internal.class, fieldName, oName);
      }
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
    NameAllocator localNameAllocator = nameAllocator.clone();

    String resultName = localNameAllocator.newName("result");
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
      String fieldName = localNameAllocator.get(field);
      result.addCode("$1N = $1N * 37 + ", resultName);
      if (field.isRepeated() || field.isRequired() || field.type().isMap()) {
        result.addStatement("$L.hashCode()", fieldName);
      } else {
        result.addStatement("($1L != null ? $1L.hashCode() : 0)", fieldName);
      }
    }
    result.addStatement("super.hashCode = $N", resultName);
    result.endControlFlow();
    result.addStatement("return $N", resultName);
    return result.build();
  }

  private MethodSpec messageToString(NameAllocator nameAllocator, MessageType type) {
    NameAllocator localNameAllocator = nameAllocator.clone();

    MethodSpec.Builder result = MethodSpec.methodBuilder("toString")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class);

    String builderName = localNameAllocator.newName("builder");
    result.addStatement("$1T $2N = new $1T()", StringBuilder.class, builderName);

    for (Field field : type.fieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      if (field.isRepeated() || field.type().isMap()) {
        result.addCode("if (!$N.isEmpty()) ", fieldName);
      } else if (!field.isRequired()) {
        result.addCode("if ($N != null) ", fieldName);
      }
      if (field.isRedacted()) {
        result.addStatement("$N.append(\", $N=██\")", builderName, field.name());
      } else {
        result.addStatement("$N.append(\", $N=\").append($L)", builderName, field.name(),
            fieldName);
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
      String fieldName = nameAllocator.get(field);
      CodeBlock initialValue = initialValue(field);
      if (initialValue != null) {
        result.addStatement("$L = $L", fieldName, initialValue);
      }
    }
    return result.build();
  }

  /** Returns the initial value of {@code field}, or null if it is doesn't have one. */
  private CodeBlock initialValue(Field field) {
    if (field.isPacked() || field.isRepeated()) {
      return CodeBlock.of("$T.newMutableList()", Internal.class);
    } else if (field.type().isMap()) {
      return CodeBlock.of("$T.newMutableMap()", Internal.class);
    } else {
      return null;
    }
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
    NameAllocator localNameAllocator = nameAllocator.clone();

    String builderName = localNameAllocator.newName("builder");
    ClassName javaType = (ClassName) typeName(message.type());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    MethodSpec.Builder result = MethodSpec.methodBuilder("newBuilder")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(builderJavaType)
        .addStatement("$1T $2L = new $1T()", builderJavaType, builderName);

    List<Field> fields = message.fieldsAndOneOfFields();
    for (Field field : fields) {
      String fieldName = localNameAllocator.get(field);
      if (field.isRepeated() || field.type().isMap()) {
        result.addStatement("$1L.$2L = $3T.copyOf($2S, $2L)", builderName, fieldName,
            Internal.class);
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

    if (field.isRepeated() || field.type().isMap()) {
      result.addStatement("$T.checkElementsNotNull($L)", Internal.class, fieldName);
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
        conditionals.add("$L == null", nameAllocator.get(requiredField));
        if (i > 0) missingArgs.add(",\n");
        missingArgs.add("$1L, $2S", nameAllocator.get(requiredField),
            requiredField.name());
      }

      result.beginControlFlow("if ($L)", conditionals.add("$]").build())
          .addStatement("throw $T.missingRequiredFields($L)", Internal.class,
              missingArgs.build())
          .endControlFlow();
    }

    result.addCode("return new $T(", javaType);
    for (Field field : message.fieldsAndOneOfFields()) {
      result.addCode("$L, ", nameAllocator.get(field));
    }
    result.addCode("super.buildUnknownFields());\n");
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
      return CodeBlock.of("$L", value != null ? value : false);

    } else if (javaType.equals(TypeName.INT.box())) {
      return CodeBlock.of("$L", value != null
          ? new BigDecimal(String.valueOf(value)).intValue()
          : 0);

    } else if (javaType.equals(TypeName.LONG.box())) {
      return CodeBlock.of("$LL", value != null
          ? Long.toString(new BigDecimal(String.valueOf(value)).longValue())
          : 0L);

    } else if (javaType.equals(TypeName.FLOAT.box())) {
      return CodeBlock.of("$Lf", value != null ? String.valueOf(value) : 0f);

    } else if (javaType.equals(TypeName.DOUBLE.box())) {
      return CodeBlock.of("$Ld", value != null ? String.valueOf(value) : 0d);

    } else if (javaType.equals(STRING)) {
      return CodeBlock.of("$S", value != null ? (String) value : "");

    } else if (javaType.equals(BYTE_STRING)) {
      if (value == null) {
        return CodeBlock.of("$T.EMPTY", ByteString.class);
      } else {
        return CodeBlock.of("$T.decodeBase64($S)", ByteString.class,
            ByteString.of(String.valueOf(value).getBytes(Charsets.ISO_8859_1)).base64());
      }

    } else if (isEnum(type) && value != null) {
      return CodeBlock.of("$T.$L", javaType, value);

    } else {
      throw new IllegalStateException(type + " is not an allowed scalar type");
    }
  }
}
