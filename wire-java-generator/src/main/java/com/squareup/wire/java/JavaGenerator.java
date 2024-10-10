/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.java;

import static com.google.common.base.Preconditions.checkArgument;
import static com.squareup.wire.schema.internal.JvmLanguages.annotationName;
import static com.squareup.wire.schema.internal.JvmLanguages.annotationTargetType;
import static com.squareup.wire.schema.internal.JvmLanguages.builtInAdapterString;
import static com.squareup.wire.schema.internal.JvmLanguages.eligibleAsAnnotationMember;
import static com.squareup.wire.schema.internal.JvmLanguages.hasEponymousType;
import static com.squareup.wire.schema.internal.JvmLanguages.javaPackage;
import static com.squareup.wire.schema.internal.JvmLanguages.legacyQualifiedFieldName;
import static com.squareup.wire.schema.internal.JvmLanguages.optionValueToInt;
import static com.squareup.wire.schema.internal.JvmLanguages.optionValueToLong;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.NameAllocator;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;
import com.squareup.wire.EnumAdapter;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoAdapter.EnumConstantNotFoundException;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireEnclosingType;
import com.squareup.wire.WireEnum;
import com.squareup.wire.WireEnumConstant;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import com.squareup.wire.schema.AdapterConstant;
import com.squareup.wire.schema.EnclosingType;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.Profile;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoMember;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import com.squareup.wire.schema.internal.NameFactory;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import okio.ByteString;

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
  static final ClassName WIRE_ENCLOSING_TYPE = ClassName.get(WireEnclosingType.class);
  static final ClassName ANDROID_MESSAGE = MESSAGE.peerClass("AndroidMessage");
  static final ClassName ADAPTER = ClassName.get(ProtoAdapter.class);
  static final ClassName BUILDER = ClassName.get(Message.Builder.class);
  static final ClassName ENUM_ADAPTER = ClassName.get(EnumAdapter.class);
  static final ClassName NULLABLE = ClassName.get("androidx.annotation", "Nullable");
  static final ClassName CREATOR = ClassName.get("android.os", "Parcelable", "Creator");

  private static final Ordering<Field> TAG_ORDERING =
      Ordering.from(
          new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
              return Integer.compare(o1.getTag(), o2.getTag());
            }
          });

  public static boolean builtInType(ProtoType protoType) {
    return BUILT_IN_TYPES_MAP.containsKey(protoType);
  }

  private static final Map<ProtoType, TypeName> BUILT_IN_TYPES_MAP =
      ImmutableMap.<ProtoType, TypeName>builder()
          .put(ProtoType.BOOL, TypeName.BOOLEAN)
          .put(ProtoType.BYTES, ClassName.get(ByteString.class))
          .put(ProtoType.DOUBLE, TypeName.DOUBLE)
          .put(ProtoType.FLOAT, TypeName.FLOAT)
          .put(ProtoType.FIXED32, TypeName.INT)
          .put(ProtoType.FIXED64, TypeName.LONG)
          .put(ProtoType.INT32, TypeName.INT)
          .put(ProtoType.INT64, TypeName.LONG)
          .put(ProtoType.SFIXED32, TypeName.INT)
          .put(ProtoType.SFIXED64, TypeName.LONG)
          .put(ProtoType.SINT32, TypeName.INT)
          .put(ProtoType.SINT64, TypeName.LONG)
          .put(ProtoType.STRING, ClassName.get(String.class))
          .put(ProtoType.UINT32, TypeName.INT)
          .put(ProtoType.UINT64, TypeName.LONG)
          .put(ProtoType.ANY, ClassName.get("com.squareup.wire", "AnyMessage"))
          .put(ProtoType.DURATION, ClassName.get("java.time", "Duration"))
          .put(ProtoType.TIMESTAMP, ClassName.get("java.time", "Instant"))
          .put(ProtoType.EMPTY, ClassName.get("kotlin", "Unit"))
          .put(
              ProtoType.STRUCT_MAP,
              ParameterizedTypeName.get(
                  ClassName.get("java.util", "Map"),
                  ClassName.get("java.lang", "String"),
                  WildcardTypeName.subtypeOf(Object.class)))
          .put(ProtoType.STRUCT_VALUE, ClassName.get("java.lang", "Object"))
          .put(ProtoType.STRUCT_NULL, ClassName.get("java.lang", "Void"))
          .put(
              ProtoType.STRUCT_LIST,
              ParameterizedTypeName.get(
                  ClassName.get("java.util", "List"), WildcardTypeName.subtypeOf(Object.class)))
          .put(ProtoType.DOUBLE_VALUE, TypeName.DOUBLE)
          .put(ProtoType.FLOAT_VALUE, TypeName.FLOAT)
          .put(ProtoType.INT64_VALUE, TypeName.LONG)
          .put(ProtoType.UINT64_VALUE, TypeName.LONG)
          .put(ProtoType.INT32_VALUE, TypeName.INT)
          .put(ProtoType.UINT32_VALUE, TypeName.INT)
          .put(ProtoType.BOOL_VALUE, TypeName.BOOLEAN)
          .put(ProtoType.STRING_VALUE, ClassName.get(String.class))
          .put(ProtoType.BYTES_VALUE, ClassName.get(ByteString.class))
          .build();

  private static final Map<ProtoType, CodeBlock> PROTOTYPE_TO_IDENTITY_VALUES =
      ImmutableMap.<ProtoType, CodeBlock>builder()
          .put(ProtoType.BOOL, CodeBlock.of("false"))
          .put(ProtoType.STRING, CodeBlock.of("\"\""))
          .put(ProtoType.BYTES, CodeBlock.of("$T.$L", ByteString.class, "EMPTY"))
          .put(ProtoType.DOUBLE, CodeBlock.of("0.0"))
          .put(ProtoType.FLOAT, CodeBlock.of("0f"))
          .put(ProtoType.FIXED64, CodeBlock.of("0L"))
          .put(ProtoType.INT64, CodeBlock.of("0L"))
          .put(ProtoType.SFIXED64, CodeBlock.of("0L"))
          .put(ProtoType.SINT64, CodeBlock.of("0L"))
          .put(ProtoType.UINT64, CodeBlock.of("0L"))
          .put(ProtoType.FIXED32, CodeBlock.of("0"))
          .put(ProtoType.INT32, CodeBlock.of("0"))
          .put(ProtoType.SFIXED32, CodeBlock.of("0"))
          .put(ProtoType.SINT32, CodeBlock.of("0"))
          .put(ProtoType.UINT32, CodeBlock.of("0"))
          .build();

  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";
  private static final int MAX_PARAMS_IN_CONSTRUCTOR = 16;

  private static final String DOUBLE_FULL_BLOCK = "\u2588\u2588";

  /**
   * Preallocate all of the names we'll need for {@code type}. Names are allocated in precedence
   * order, so names we're stuck with (serialVersionUID etc.) occur before proto field names are
   * assigned.
   *
   * <p>Name allocations are computed once and reused because some types may be needed when
   * generating other types.
   */
  private final LoadingCache<Type, NameAllocator> nameAllocators =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<Type, NameAllocator>() {
                @Override
                public NameAllocator load(Type type) throws Exception {
                  NameAllocator nameAllocator = new NameAllocator();

                  if (type instanceof MessageType) {
                    nameAllocator.newName("serialVersionUID", "serialVersionUID");
                    nameAllocator.newName("ADAPTER", "ADAPTER");
                    nameAllocator.newName("MESSAGE_OPTIONS", "MESSAGE_OPTIONS");
                    if (emitAndroid) {
                      nameAllocator.newName("CREATOR", "CREATOR");
                    }

                    List<Field> fieldsAndOneOfFields =
                        ((MessageType) type).getFieldsAndOneOfFields();
                    Set<String> collidingNames = collidingFieldNames(fieldsAndOneOfFields);
                    for (Field field : fieldsAndOneOfFields) {
                      String suggestion =
                          collidingNames.contains(field.getName())
                                  || (field.getName().equals(field.getType().getSimpleName())
                                      && !field.getType().isScalar())
                                  || hasEponymousType(schema, field)
                              ? legacyQualifiedFieldName(field)
                              : field.getName();
                      nameAllocator.newName(suggestion, field);
                    }

                  } else if (type instanceof EnumType) {
                    nameAllocator.newName("value", "value");
                    nameAllocator.newName("i", "i");
                    nameAllocator.newName("reader", "reader");
                    nameAllocator.newName("writer", "writer");

                    for (EnumConstant constant : ((EnumType) type).getConstants()) {
                      nameAllocator.newName(constant.getName(), constant);
                    }
                  }

                  return nameAllocator;
                }
              });

  private final Schema schema;

  /** Proto type to the corresponding Java type. This honors the Java package extension. */
  private final ImmutableMap<ProtoType, TypeName> typeToJavaName;

  /** Proto member to the corresponding Java type. This is only used for extension fields. */
  private final ImmutableMap<ProtoMember, TypeName> memberToJavaName;

  private final Profile profile;
  private final boolean emitAndroid;
  private final boolean emitAndroidAnnotations;
  private final boolean emitCompact;
  private final boolean emitDeclaredOptions;
  private final boolean emitAppliedOptions;
  private final boolean buildersOnly;

  private JavaGenerator(
      Schema schema,
      Map<ProtoType, TypeName> typeToJavaName,
      Map<ProtoMember, TypeName> memberToJavaName,
      Profile profile,
      boolean emitAndroid,
      boolean emitAndroidAnnotations,
      boolean emitCompact,
      boolean emitDeclaredOptions,
      boolean emitAppliedOptions,
      boolean buildersOnly) {
    this.schema = schema;
    this.typeToJavaName = ImmutableMap.copyOf(typeToJavaName);
    this.memberToJavaName = ImmutableMap.copyOf(memberToJavaName);
    this.profile = profile;
    this.emitAndroid = emitAndroid;
    this.emitAndroidAnnotations = emitAndroidAnnotations;
    this.emitCompact = emitCompact;
    this.emitDeclaredOptions = emitDeclaredOptions;
    this.emitAppliedOptions = emitAppliedOptions;
    this.buildersOnly = buildersOnly;
  }

  public JavaGenerator withAndroid(boolean emitAndroid) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public JavaGenerator withAndroidAnnotations(boolean emitAndroidAnnotations) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public JavaGenerator withCompact(boolean emitCompact) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public JavaGenerator withProfile(Profile profile) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public JavaGenerator withOptions(boolean emitDeclaredOptions, boolean emitAppliedOptions) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public JavaGenerator withBuildersOnly(boolean buildersOnly) {
    return new JavaGenerator(
        schema,
        typeToJavaName,
        memberToJavaName,
        profile,
        emitAndroid,
        emitAndroidAnnotations,
        emitCompact,
        emitDeclaredOptions,
        emitAppliedOptions,
        buildersOnly);
  }

  public static JavaGenerator get(Schema schema) {
    Map<ProtoType, TypeName> nameToJavaName = new LinkedHashMap<>();
    Map<ProtoMember, TypeName> memberToJavaName = new LinkedHashMap<>();

    for (ProtoFile protoFile : schema.getProtoFiles()) {
      String javaPackage = javaPackage(protoFile);
      putAll(nameToJavaName, javaPackage, null, protoFile.getTypes());

      for (Service service : protoFile.getServices()) {
        ClassName className = ClassName.get(javaPackage, service.type().getSimpleName());
        nameToJavaName.put(service.type(), className);
      }

      putAllExtensions(
          schema, protoFile, protoFile.getTypes(), protoFile.getExtendList(), memberToJavaName);
    }

    nameToJavaName.putAll(BUILT_IN_TYPES_MAP);

    return new JavaGenerator(
        schema,
        nameToJavaName,
        memberToJavaName,
        new Profile(),
        false /* emitAndroid */,
        false /* emitAndroidAnnotations */,
        false /* emitCompact */,
        false /* emitDeclaredOptions */,
        false /* emitAppliedOptions */,
        false /* buildersOnly */);
  }

  private static void putAllExtensions(
      Schema schema,
      ProtoFile protoFile,
      List<Type> types,
      List<Extend> extendList,
      Map<ProtoMember, TypeName> memberToJavaName) {

    for (Extend extend : extendList) {
      if (annotationTargetType(extend) == null) continue;

      for (Field field : extend.getFields()) {
        if (!eligibleAsAnnotationMember(schema, field)) continue;

        ProtoMember protoMember = extend.member(field);
        ClassName annotationName =
            annotationName(protoFile, field, new ClassNameFactory(), "Option");
        if (memberToJavaName.containsValue(annotationName)) {
          // To avoid conflicts for same named options of different types, we generate a more
          // precise name. i.e. 'ObjectiveOption' will become 'ObjectiveFieldOption'.
          String extendSimpleName = extend.getType().getSimpleName();
          memberToJavaName.put(
              protoMember,
              annotationName(
                  protoFile,
                  field,
                  new ClassNameFactory(),
                  extendSimpleName.substring(0, extendSimpleName.length() - 1)));
        } else {
          memberToJavaName.put(protoMember, annotationName);
        }
      }
    }

    for (Type type : types) {
      putAllExtensions(
          schema, protoFile, type.getNestedTypes(), type.getNestedExtendList(), memberToJavaName);
    }
  }

  private static class ClassNameFactory implements NameFactory<ClassName> {
    @Override
    public ClassName newName(String packageName, String simpleName) {
      return ClassName.get(packageName, simpleName);
    }

    @Override
    public ClassName nestedName(ClassName enclosing, String simpleName) {
      return enclosing.nestedClass(simpleName);
    }
  }

  private static void putAll(
      Map<ProtoType, TypeName> wireToJava,
      String javaPackage,
      ClassName enclosingClassName,
      List<Type> types) {
    for (Type type : types) {
      ClassName className =
          enclosingClassName != null
              ? enclosingClassName.nestedClass(type.getType().getSimpleName())
              : ClassName.get(javaPackage, type.getType().getSimpleName());
      wireToJava.put(type.getType(), className);
      putAll(wireToJava, javaPackage, className, type.getNestedTypes());
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
    TypeName profileJavaName = profile.javaTarget(protoType);
    if (profileJavaName != null) return profileJavaName;
    TypeName candidate = typeToJavaName.get(protoType);
    checkArgument(candidate != null, "unexpected type %s", protoType);
    return candidate;
  }

  /**
   * Returns the Java type of the abstract adapter class generated for a corresponding {@code
   * protoType}. Returns null if {@code protoType} is not using a custom proto adapter.
   */
  public @Nullable ClassName abstractAdapterName(ProtoType protoType) {
    TypeName profileJavaName = profile.javaTarget(protoType);
    if (profileJavaName == null) return null;

    TypeName typeName = typeToJavaName.get(protoType);
    Type type = schema.getType(protoType);

    ClassName javaName;
    if (typeName instanceof ClassName) {
      javaName = (ClassName) typeName;
    } else if (typeName instanceof ParameterizedTypeName) {
      javaName = ((ParameterizedTypeName) typeName).rawType();
    } else {
      throw new IllegalArgumentException("Unexpected typeName :" + typeName);
    }
    return type instanceof EnumType
        ? javaName.peerClass(javaName.simpleName() + "Adapter")
        : javaName.peerClass("Abstract" + javaName.simpleName() + "Adapter");
  }

  private CodeBlock singleAdapterFor(Field field, NameAllocator nameAllocator) {
    return field.getType().isMap()
        ? CodeBlock.of("$NAdapter()", nameAllocator.get(field))
        : singleAdapterFor(field.getType());
  }

  private CodeBlock singleAdapterFor(ProtoType type) {
    CodeBlock.Builder result = CodeBlock.builder();
    if (type.isScalar()) {
      result.add("$T.$L", ADAPTER, type.getSimpleName().toUpperCase(Locale.US));
    } else if (type.equals(ProtoType.DURATION)) {
      result.add("$T.$L", ADAPTER, "DURATION");
    } else if (type.equals(ProtoType.TIMESTAMP)) {
      result.add("$T.$L", ADAPTER, "INSTANT");
    } else if (type.equals(ProtoType.EMPTY)) {
      result.add("$T.$L", ADAPTER, "EMPTY");
    } else if (type.equals(ProtoType.STRUCT_MAP)) {
      result.add("$T.$L", ADAPTER, "STRUCT_MAP");
    } else if (type.equals(ProtoType.STRUCT_VALUE)) {
      result.add("$T.$L", ADAPTER, "STRUCT_VALUE");
    } else if (type.equals(ProtoType.STRUCT_NULL)) {
      result.add("$T.$L", ADAPTER, "STRUCT_NULL");
    } else if (type.equals(ProtoType.STRUCT_LIST)) {
      result.add("$T.$L", ADAPTER, "STRUCT_LIST");
    } else if (type.equals(ProtoType.DOUBLE_VALUE)) {
      result.add("$T.$L", ADAPTER, "DOUBLE_VALUE");
    } else if (type.equals(ProtoType.FLOAT_VALUE)) {
      result.add("$T.$L", ADAPTER, "FLOAT_VALUE");
    } else if (type.equals(ProtoType.INT64_VALUE)) {
      result.add("$T.$L", ADAPTER, "INT64_VALUE");
    } else if (type.equals(ProtoType.UINT64_VALUE)) {
      result.add("$T.$L", ADAPTER, "UINT64_VALUE");
    } else if (type.equals(ProtoType.INT32_VALUE)) {
      result.add("$T.$L", ADAPTER, "INT32_VALUE");
    } else if (type.equals(ProtoType.UINT32_VALUE)) {
      result.add("$T.$L", ADAPTER, "UINT32_VALUE");
    } else if (type.equals(ProtoType.BOOL_VALUE)) {
      result.add("$T.$L", ADAPTER, "BOOL_VALUE");
    } else if (type.equals(ProtoType.STRING_VALUE)) {
      result.add("$T.$L", ADAPTER, "STRING_VALUE");
    } else if (type.equals(ProtoType.BYTES_VALUE)) {
      result.add("$T.$L", ADAPTER, "BYTES_VALUE");
    } else if (type.isMap()) {
      throw new IllegalArgumentException("Cannot create single adapter for map type " + type);
    } else {
      AdapterConstant adapterConstant = profile.getAdapter(type);
      if (adapterConstant != null) {
        result.add("$T.$L", adapterConstant.javaClassName, adapterConstant.memberName);
      } else {
        result.add("$T.ADAPTER", typeName(type));
      }
    }
    return result.build();
  }

  private CodeBlock adapterFor(Field field, NameAllocator nameAllocator) {
    CodeBlock.Builder result = singleAdapterFor(field, nameAllocator).toBuilder();
    if (field.isPacked()) {
      result.add(".asPacked()");
    } else if (field.isRepeated()) {
      result.add(".asRepeated()");
    }
    return result.build();
  }

  public boolean isEnum(ProtoType type) {
    return schema.getType(type) instanceof EnumType;
  }

  EnumConstant enumDefault(ProtoType type) {
    EnumType wireEnum = (EnumType) schema.getType(type);
    return wireEnum.getConstants().get(0);
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
    documentation =
        documentation.replaceAll("@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
  }

  /** Returns the full name of the class generated for {@code type}. */
  public ClassName generatedTypeName(Type type) {
    ClassName abstractAdapterName = abstractAdapterName(type.getType());
    return abstractAdapterName != null ? abstractAdapterName : (ClassName) typeName(type.getType());
  }

  /** Returns the generated code for {@code type}, which may be a top-level or a nested type. */
  public TypeSpec generateType(Type type) {
    AdapterConstant adapterConstant = profile.getAdapter(type.getType());
    if (adapterConstant != null) {
      return generateAdapterForCustomType(type);
    }
    if (type instanceof MessageType) {
      return generateMessage((MessageType) type);
    }
    if (type instanceof EnumType) {
      return generateEnum((EnumType) type);
    }
    if (type instanceof EnclosingType) {
      return generateEnclosingType((EnclosingType) type);
    }
    throw new IllegalStateException("Unknown type: " + type);
  }

  private TypeSpec generateEnum(EnumType type) {
    NameAllocator nameAllocator = nameAllocators.getUnchecked(type);
    String value = nameAllocator.get("value");
    ClassName javaType = (ClassName) typeName(type.getType());

    TypeSpec.Builder builder =
        TypeSpec.enumBuilder(javaType.simpleName())
            .addModifiers(PUBLIC)
            .addSuperinterface(WireEnum.class);

    if (!type.getDocumentation().isEmpty()) {
      builder.addJavadoc("$L\n", sanitizeJavadoc(type.getDocumentation()));
    }

    for (AnnotationSpec annotation : optionAnnotations(type.getOptions())) {
      builder.addAnnotation(annotation);
    }

    if (type.isDeprecated()) {
      builder.addAnnotation(Deprecated.class);
    }

    // Output Private tag field
    builder.addField(TypeName.INT, value, PRIVATE, FINAL);

    // Enum constructor takes the constant tag.
    builder.addMethod(
        MethodSpec.constructorBuilder()
            .addStatement("this.$1N = $1N", value)
            .addParameter(TypeName.INT, value)
            .build());

    MethodSpec.Builder fromValueBuilder =
        MethodSpec.methodBuilder("fromValue")
            .addJavadoc("Return the constant for {@code $N} or null.\n", value)
            .addModifiers(PUBLIC, STATIC)
            .returns(javaType)
            .addParameter(int.class, value)
            .beginControlFlow("switch ($N)", value);

    Set<Integer> seenTags = new LinkedHashSet<>();
    for (EnumConstant constant : type.getConstants()) {
      TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder("$L", constant.getTag());
      if (!constant.getDocumentation().isEmpty()) {
        constantBuilder.addJavadoc("$L\n", sanitizeJavadoc(constant.getDocumentation()));
      }

      for (AnnotationSpec annotation : optionAnnotations(constant.getOptions())) {
        constantBuilder.addAnnotation(annotation);
      }
      AnnotationSpec wireEnumConstantAnnotation =
          wireEnumConstantAnnotation(nameAllocator, constant);
      if (wireEnumConstantAnnotation != null) {
        constantBuilder.addAnnotation(wireEnumConstantAnnotation);
      }

      if (constant.isDeprecated()) {
        constantBuilder.addAnnotation(Deprecated.class);
      }

      builder.addEnumConstant(nameAllocator.get(constant), constantBuilder.build());

      // Ensure constant case tags are unique, which might not be the case if allow_alias is true.
      if (seenTags.add(constant.getTag())) {
        fromValueBuilder.addStatement(
            "case $L: return $L", constant.getTag(), nameAllocator.get(constant));
      }
    }

    builder.addMethod(
        fromValueBuilder.addStatement("default: return null").endControlFlow().build());

    // ADAPTER
    FieldSpec.Builder adapterBuilder =
        FieldSpec.builder(adapterOf(javaType), "ADAPTER").addModifiers(PUBLIC, STATIC, FINAL);
    ClassName adapterJavaType = javaType.nestedClass("ProtoAdapter_" + javaType.simpleName());
    if (!emitCompact) {
      adapterBuilder.initializer("new $T()", adapterJavaType);
    } else {
      adapterBuilder.initializer("$T.newEnumAdapter($T.class)", ProtoAdapter.class, javaType);
    }
    builder.addField(adapterBuilder.build());

    // Public Getter
    builder.addMethod(
        MethodSpec.methodBuilder("getValue")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(TypeName.INT)
            .addStatement("return $N", value)
            .build());

    if (!emitCompact) {
      // Adds the ProtoAdapter implementation at the bottom.
      builder.addType(enumAdapter(javaType, adapterJavaType, type));
    }

    return builder.build();
  }

  private TypeSpec generateMessage(MessageType type) {
    boolean constructorTakesAllFields = constructorTakesAllFields(type);

    NameAllocator nameAllocator = nameAllocators.getUnchecked(type);

    ClassName javaType = (ClassName) typeName(type.getType());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    TypeSpec.Builder builder = TypeSpec.classBuilder(javaType.simpleName());
    builder.addModifiers(PUBLIC, FINAL);

    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    if (!type.getDocumentation().isEmpty()) {
      builder.addJavadoc("$L\n", sanitizeJavadoc(type.getDocumentation()));
    }

    for (AnnotationSpec annotation : optionAnnotations(type.getOptions())) {
      builder.addAnnotation(annotation);
    }

    if (type.isDeprecated()) {
      builder.addAnnotation(Deprecated.class);
    }

    ClassName messageType = emitAndroid ? ANDROID_MESSAGE : MESSAGE;
    builder.superclass(messageOf(messageType, javaType, builderJavaType));

    String adapterName = nameAllocator.get("ADAPTER");
    String protoAdapterName = "ProtoAdapter_" + javaType.simpleName();
    String protoAdapterClassName = nameAllocator.newName(protoAdapterName);
    ClassName adapterJavaType = javaType.nestedClass(protoAdapterClassName);
    builder.addField(
        messageAdapterField(
            adapterName, javaType, adapterJavaType, type.getType(), type.getSyntax()));
    // Note: The non-compact implementation is added at the very bottom of the surrounding type.

    if (emitAndroid) {
      TypeName creatorType = creatorOf(javaType);
      String creatorName = nameAllocator.get("CREATOR");
      builder.addField(
          FieldSpec.builder(creatorType, creatorName, PUBLIC, STATIC, FINAL)
              .initializer("$T.newCreator($L)", ANDROID_MESSAGE, adapterName)
              .build());
    }

    builder.addField(
        FieldSpec.builder(TypeName.LONG, nameAllocator.get("serialVersionUID"))
            .addModifiers(PRIVATE, STATIC, FINAL)
            .initializer("$LL", 0L)
            .build());

    for (Field field : type.getFieldsAndOneOfFields()) {
      TypeName fieldJavaType = fieldType(field);
      Field.EncodeMode encodeMode = field.getEncodeMode();

      if ((field.getType().isScalar() || isEnum(field.getType()))
          && !field.getType().equals(ProtoType.STRUCT_NULL)
          && encodeMode != Field.EncodeMode.REPEATED
          && encodeMode != Field.EncodeMode.PACKED
          && encodeMode != Field.EncodeMode.OMIT_IDENTITY) {
        builder.addField(defaultField(nameAllocator, field, fieldJavaType));
      }

      String fieldName = nameAllocator.get(field);
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldJavaType, fieldName, PUBLIC, FINAL);
      if (!field.getDocumentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", sanitizeJavadoc(field.getDocumentation()));
      }
      for (AnnotationSpec annotation : optionAnnotations(field.getOptions())) {
        fieldBuilder.addAnnotation(annotation);
      }
      fieldBuilder.addAnnotation(wireFieldAnnotation(nameAllocator, field, type));
      if (field.isExtension()) {
        fieldBuilder.addJavadoc("Extension source: $L\n", field.getLocation().withPathOnly());
      }
      if (field.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      if (emitAndroidAnnotations && encodeMode == Field.EncodeMode.NULL_IF_ABSENT) {
        fieldBuilder.addAnnotation(NULLABLE);
      }
      builder.addField(fieldBuilder.build());
    }

    if (constructorTakesAllFields) {
      builder.addMethod(messageFieldsConstructor(nameAllocator, type));
    }
    builder.addMethod(messageConstructor(nameAllocator, type, builderJavaType));

    builder.addMethod(newBuilder(nameAllocator, type));

    builder.addMethod(messageEquals(nameAllocator, type));
    builder.addMethod(messageHashCode(nameAllocator, type));
    if (!emitCompact) {
      builder.addMethod(messageToString(nameAllocator, type));
    }

    builder.addType(builder(nameAllocator, type, javaType, builderJavaType));

    for (Type nestedType : type.getNestedTypes()) {
      builder.addType(generateType(nestedType));
    }

    for (Extend nestedExtend : type.getNestedExtendList()) {
      for (Field extension : nestedExtend.getFields()) {
        TypeSpec extensionOption = generateOptionType(nestedExtend, extension);
        if (extensionOption != null) {
          builder.addType(extensionOption);
        }
      }
    }

    if (!emitCompact) {
      // Add the ProtoAdapter implementation at the very bottom since it's ugly serialization code.
      builder.addType(
          messageAdapter(nameAllocator, type, javaType, adapterJavaType, builderJavaType));
    }

    return builder.build();
  }

  /** Decides if a constructor should take all fields or a builder as a parameter. */
  private boolean constructorTakesAllFields(MessageType type) {
    return type.fields().size() < MAX_PARAMS_IN_CONSTRUCTOR;
  }

  private TypeSpec generateEnclosingType(EnclosingType type) {
    ClassName javaType = (ClassName) typeName(type.getType());

    TypeSpec.Builder builder =
        TypeSpec.classBuilder(javaType.simpleName())
            .addModifiers(PUBLIC, FINAL)
            .addAnnotation(WIRE_ENCLOSING_TYPE);
    if (javaType.enclosingClassName() != null) {
      builder.addModifiers(STATIC);
    }

    String documentation = type.getDocumentation();
    if (!documentation.isEmpty()) {
      documentation += "\n\n<p>";
    }
    documentation +=
        "<b>NOTE:</b> This type only exists to maintain class structure"
            + " for its nested types and is not an actual message.";
    builder.addJavadoc("$L\n", documentation);

    builder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .addStatement("throw new $T()", AssertionError.class)
            .build());

    for (Type nestedType : type.getNestedTypes()) {
      builder.addType(generateType(nestedType));
    }

    return builder.build();
  }

  /** Returns a standalone adapter for {@code type}. */
  public TypeSpec generateAdapterForCustomType(Type type) {
    NameAllocator nameAllocator = nameAllocators.getUnchecked(type);
    ClassName adapterTypeName = abstractAdapterName(type.getType());
    ClassName typeName = (ClassName) typeName(type.getType());

    TypeSpec.Builder adapter;
    if (type instanceof MessageType) {
      adapter =
          messageAdapter(nameAllocator, (MessageType) type, typeName, adapterTypeName, null)
              .toBuilder();
    } else {
      adapter = enumAdapter(nameAllocator, (EnumType) type, typeName, adapterTypeName).toBuilder();
    }

    if (adapterTypeName.enclosingClassName() != null) adapter.addModifiers(STATIC);

    for (Type nestedType : type.getNestedTypes()) {
      if (profile.getAdapter(nestedType.getType()) == null) {
        throw new IllegalArgumentException(
            "Missing custom proto adapter for "
                + nestedType.getType().getEnclosingTypeOrPackage()
                + "."
                + nestedType.getType().getSimpleName()
                + " when enclosing proto has custom proto adapter.");
      }
      adapter.addType(generateAdapterForCustomType(nestedType));
    }

    return adapter.build();
  }

  /** Returns the set of names that are not unique within {@code fields}. */
  private Set<String> collidingFieldNames(List<Field> fields) {
    Set<String> fieldNames = new LinkedHashSet<>();
    Set<String> collidingNames = new LinkedHashSet<>();
    for (Field field : fields) {
      if (!fieldNames.add(field.getName())) {
        collidingNames.add(field.getName());
      }
    }
    return collidingNames;
  }

  private FieldSpec messageAdapterField(
      String adapterName,
      ClassName javaType,
      ClassName adapterJavaType,
      ProtoType protoType,
      Syntax syntax) {
    FieldSpec.Builder result =
        FieldSpec.builder(adapterOf(javaType), adapterName).addModifiers(PUBLIC, STATIC, FINAL);
    if (emitCompact) {
      result.initializer(
          "$T.newMessageAdapter($T.class, $S, $T.$L)",
          ProtoAdapter.class,
          javaType,
          protoType.getTypeUrl(),
          Syntax.class,
          syntax.name());
    } else {
      result.initializer("new $T()", adapterJavaType);
    }
    return result.build();
  }

  /**
   * Generates a custom enum adapter to decode a proto enum to a user-specified Java type. Users
   * need to instantiate a constant instance of this adapter that provides all enum constants in the
   * constructor in the proper order.
   *
   * <pre>{@code
   * public static final ProtoAdapter<Roshambo> ADAPTER
   *     = new RoshamboAdapter(Roshambo.ROCK, Roshambo.SCISSORS, Roshambo.PAPER);
   * }</pre>
   */
  private TypeSpec enumAdapter(
      NameAllocator nameAllocator, EnumType type, ClassName javaType, ClassName adapterJavaType) {
    String value = nameAllocator.get("value");
    String i = nameAllocator.get("i");
    String reader = nameAllocator.get("reader");
    String writer = nameAllocator.get("writer");

    TypeSpec.Builder builder = TypeSpec.classBuilder(adapterJavaType.simpleName());
    builder.superclass(adapterOf(javaType));
    builder.addModifiers(PUBLIC);

    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    constructorBuilder.addModifiers(PUBLIC);
    constructorBuilder.addStatement("super($T.VARINT, $T.class)", FieldEncoding.class, javaType);
    for (EnumConstant constant : type.getConstants()) {
      String name = nameAllocator.get(constant);
      FieldSpec.Builder fieldBuilder =
          FieldSpec.builder(javaType, name).addModifiers(PROTECTED, FINAL);
      if (!constant.getDocumentation().isEmpty()) {
        fieldBuilder.addJavadoc("$L\n", sanitizeJavadoc(constant.getDocumentation()));
      }
      if (constant.isDeprecated()) {
        fieldBuilder.addAnnotation(Deprecated.class);
      }
      builder.addField(fieldBuilder.build());

      constructorBuilder.addParameter(javaType, name);
      constructorBuilder.addStatement("this.$N = $N", name, name);
    }
    builder.addMethod(constructorBuilder.build());

    MethodSpec.Builder toValueBuilder =
        MethodSpec.methodBuilder("toValue")
            .addModifiers(PROTECTED)
            .returns(int.class)
            .addParameter(javaType, value);
    for (EnumConstant constant : type.getConstants()) {
      String name = nameAllocator.get(constant);
      toValueBuilder.addStatement("if ($N.equals($N)) return $L", value, name, constant.getTag());
    }
    toValueBuilder.addStatement("return $L", -1);
    builder.addMethod(toValueBuilder.build());

    MethodSpec.Builder fromValueBuilder =
        MethodSpec.methodBuilder("fromValue")
            .addModifiers(PROTECTED)
            .returns(javaType)
            .addParameter(int.class, value);
    fromValueBuilder.beginControlFlow("switch ($N)", value);
    for (EnumConstant constant : type.getConstants()) {
      String name = nameAllocator.get(constant);
      fromValueBuilder.addStatement("case $L: return $N", constant.getTag(), name);
    }
    fromValueBuilder.addStatement(
        "default: throw new $T($N, $T.class)",
        EnumConstantNotFoundException.class,
        value,
        javaType);
    fromValueBuilder.endControlFlow();
    builder.addMethod(fromValueBuilder.build());

    builder.addMethod(
        MethodSpec.methodBuilder("encodedSize")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(int.class)
            .addParameter(javaType, value)
            .addStatement("return $T.UINT32.encodedSize(toValue($N))", ProtoAdapter.class, value)
            .build());

    builder.addMethod(enumEncode(javaType, value, i, writer, false));
    builder.addMethod(enumEncode(javaType, value, i, writer, true));

    builder.addMethod(
        MethodSpec.methodBuilder("decode")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(javaType)
            .addParameter(ProtoReader.class, reader)
            .addException(IOException.class)
            .addStatement("int $N = $N.readVarint32()", value, reader)
            .addStatement("return fromValue($N)", value)
            .build());

    builder.addMethod(
        MethodSpec.methodBuilder("redact")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(javaType)
            .addParameter(javaType, "value")
            .addStatement("return value")
            .build());

    return builder.build();
  }

  private MethodSpec enumEncode(
      ClassName javaType, String value, String localInt, String localWriter, boolean reverse) {
    return MethodSpec.methodBuilder("encode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(reverse ? ReverseProtoWriter.class : ProtoWriter.class, localWriter)
        .addParameter(javaType, value)
        .addException(IOException.class)
        .addStatement("int $N = toValue($N)", localInt, value)
        .addStatement(
            "if ($N == $L) throw new $T($S + $N)",
            localInt,
            -1,
            ProtocolException.class,
            "Unexpected enum constant: ",
            value)
        .addStatement("$N.writeVarint32($N)", localWriter, localInt)
        .build();
  }

  private TypeSpec enumAdapter(ClassName javaType, ClassName adapterJavaType, EnumType enumType) {
    return TypeSpec.classBuilder(adapterJavaType.simpleName())
        .superclass(enumAdapterOf(javaType))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addStatement(
                    "super($T.class, $T.$L, $L)",
                    javaType,
                    Syntax.class,
                    enumType.getSyntax().name(),
                    identity(enumType))
                .build())
        .addMethod(
            MethodSpec.methodBuilder("fromValue")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(javaType)
                .addParameter(int.class, "value")
                .addStatement("return $T.fromValue(value)", javaType)
                .build())
        .build();
  }

  private TypeSpec messageAdapter(
      NameAllocator nameAllocator,
      MessageType type,
      ClassName javaType,
      ClassName adapterJavaType,
      ClassName builderType) {
    boolean useBuilder = builderType != null;
    TypeSpec.Builder adapter =
        TypeSpec.classBuilder(adapterJavaType.simpleName()).superclass(adapterOf(javaType));

    if (useBuilder) {
      adapter.addModifiers(PRIVATE, STATIC, FINAL);
    } else {
      adapter.addModifiers(PUBLIC, ABSTRACT);
    }

    adapter.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addStatement(
                "super($T.LENGTH_DELIMITED, $T.class, $S, $T.$L, null, $S)",
                FieldEncoding.class,
                javaType,
                type.getType().getTypeUrl(),
                Syntax.class,
                type.getSyntax().name(),
                type.getLocation().getPath())
            .build());

    if (!useBuilder) {
      MethodSpec.Builder fromProto =
          MethodSpec.methodBuilder("fromProto").addModifiers(PUBLIC, ABSTRACT).returns(javaType);

      for (Field field : type.getFieldsAndOneOfFields()) {
        TypeName fieldType = fieldType(field);
        String fieldName = nameAllocator.get(field);
        fromProto.addParameter(fieldType, fieldName);
        adapter.addMethod(
            MethodSpec.methodBuilder(fieldName)
                .addModifiers(PUBLIC, ABSTRACT)
                .addParameter(javaType, "value")
                .returns(fieldType)
                .build());
      }

      adapter.addMethod(fromProto.build());
    }

    adapter.addMethod(messageAdapterEncodedSize(nameAllocator, type, javaType, useBuilder));
    adapter.addMethod(messageAdapterEncode(nameAllocator, type, javaType, useBuilder, false));
    adapter.addMethod(messageAdapterEncode(nameAllocator, type, javaType, useBuilder, true));
    adapter.addMethod(messageAdapterDecode(nameAllocator, type, javaType, useBuilder, builderType));
    adapter.addMethod(messageAdapterRedact(nameAllocator, type, javaType, useBuilder, builderType));

    for (Field field : type.getFieldsAndOneOfFields()) {
      if (field.getType().isMap()) {
        TypeName adapterType = adapterOf(fieldType(field));
        String fieldName = nameAllocator.get(field);
        adapter.addField(FieldSpec.builder(adapterType, fieldName, PRIVATE).build());
        // Map adapters have to be lazy in order to avoid a circular reference when its value type
        // is the same as its enclosing type.
        adapter.addMethod(mapAdapter(nameAllocator, adapterType, fieldName, field.getType()));
      }
    }

    return adapter.build();
  }

  private MethodSpec messageAdapterEncodedSize(
      NameAllocator nameAllocator, MessageType type, TypeName javaType, boolean useBuilder) {
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("encodedSize")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(int.class)
            .addParameter(javaType, "value");

    String resultName = nameAllocator.clone().newName("result");
    result.addStatement("int $L = 0", resultName);
    for (Field field : type.getFieldsAndOneOfFields()) {
      int fieldTag = field.getTag();
      String fieldName = nameAllocator.get(field);
      CodeBlock adapter = adapterFor(field, nameAllocator);
      boolean omitIdentity = field.getEncodeMode().equals(Field.EncodeMode.OMIT_IDENTITY);
      if (omitIdentity) {
        result.beginControlFlow(
            "if (!$T.equals(value.$L, $L))",
            ClassName.get(Objects.class),
            fieldName,
            identityValue(field));
      }
      result
          .addCode("$L += ", resultName)
          .addCode("$L.encodedSizeWithTag($L, ", adapter, fieldTag)
          .addCode((useBuilder ? "value.$L" : "$L(value)"), fieldName)
          .addCode(");\n");
      if (omitIdentity) {
        result.endControlFlow();
      }
    }
    if (useBuilder) {
      result.addStatement("$L += value.unknownFields().size()", resultName);
    }
    result.addStatement("return $L", resultName);

    return result.build();
  }

  private MethodSpec messageAdapterEncode(
      NameAllocator nameAllocator,
      MessageType type,
      TypeName javaType,
      boolean useBuilder,
      boolean reverse) {
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("encode")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .addParameter(reverse ? ReverseProtoWriter.class : ProtoWriter.class, "writer")
            .addParameter(javaType, "value")
            .addException(IOException.class);

    List<CodeBlock> encodeCalls = new ArrayList<>();

    for (Field field : type.getFieldsAndOneOfFields()) {
      int fieldTag = field.getTag();
      CodeBlock adapter = adapterFor(field, nameAllocator);
      String fieldName = nameAllocator.get(field);
      CodeBlock.Builder encodeCall = CodeBlock.builder();
      if (field.getEncodeMode().equals(Field.EncodeMode.OMIT_IDENTITY)) {
        encodeCall.add(
            "if (!$T.equals(value.$L, $L)) ",
            ClassName.get(Objects.class),
            fieldName,
            identityValue(field));
      }
      encodeCall
          .add("$L.encodeWithTag(writer, $L, ", adapter, fieldTag)
          .add((useBuilder ? "value.$L" : "$L(value)"), fieldName)
          .add(");\n");
      encodeCalls.add(encodeCall.build());
    }

    if (useBuilder) {
      encodeCalls.add(
          CodeBlock.builder().addStatement("writer.writeBytes(value.unknownFields())").build());
    }

    if (reverse) {
      Collections.reverse(encodeCalls);
    }

    for (CodeBlock encodeCall : encodeCalls) {
      result.addCode(encodeCall);
    }

    return result.build();
  }

  private MethodSpec messageAdapterDecode(
      NameAllocator nameAllocator,
      MessageType type,
      TypeName javaType,
      boolean useBuilder,
      ClassName builderJavaType) {
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("decode")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(javaType)
            .addParameter(ProtoReader.class, "reader")
            .addException(IOException.class);

    List<Field> fields = TAG_ORDERING.sortedCopy(type.getFieldsAndOneOfFields());

    if (useBuilder) {
      result.addStatement("$1T builder = new $1T()", builderJavaType);
    } else {
      for (Field field : fields) {
        result.addStatement(
            "$T $N = $L", fieldType(field), nameAllocator.get(field), initialValue(field));
      }
    }

    result.addStatement("long token = reader.beginMessage()");
    result.beginControlFlow("for (int tag; (tag = reader.nextTag()) != -1;)");
    result.beginControlFlow("switch (tag)");

    for (Field field : fields) {
      int fieldTag = field.getTag();

      if (isEnum(field.getType()) && !field.getType().equals(ProtoType.STRUCT_NULL)) {
        result.beginControlFlow("case $L:", fieldTag);
        result.beginControlFlow("try");
        result.addCode(decodeAndAssign(field, nameAllocator, useBuilder));
        result.addCode(";\n");
        if (useBuilder) {
          result.nextControlFlow("catch ($T e)", EnumConstantNotFoundException.class);
          result.addStatement(
              "builder.addUnknownField(tag, $T.VARINT, (long) e.value)", FieldEncoding.class);
          result.endControlFlow(); // try/catch
        } else {
          result.nextControlFlow("catch ($T ignored)", EnumConstantNotFoundException.class);
          result.endControlFlow(); // try/catch
        }
        result.addStatement("break");
        result.endControlFlow(); // case
      } else {
        result.addCode(
            "case $L: $L; break;\n", fieldTag, decodeAndAssign(field, nameAllocator, useBuilder));
      }
    }

    result.beginControlFlow("default:");
    if (useBuilder) {
      result.addStatement("reader.readUnknownField(tag)");
    } else {
      result.addStatement("reader.skip()");
    }
    result.endControlFlow(); // default

    result.endControlFlow(); // switch
    result.endControlFlow(); // for
    if (useBuilder) {
      result.addStatement("builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token))");
    } else {
      result.addStatement("reader.endMessageAndGetUnknownFields(token)");
    }

    if (useBuilder) {
      result.addStatement("return builder.build()");
    } else {
      result.addCode("return fromProto(");
      boolean first = true;
      for (Field field : type.getFieldsAndOneOfFields()) {
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
    CodeBlock decode = CodeBlock.of("$L.decode(reader)", singleAdapterFor(field, nameAllocator));
    if (field.isRepeated()) {
      return useBuilder
          ? field.getType().equals(ProtoType.STRUCT_NULL)
              ? CodeBlock.of("builder.$L.add(($T) $L)", fieldName, Void.class, decode)
              : CodeBlock.of("builder.$L.add($L)", fieldName, decode)
          : CodeBlock.of("$L.add($L)", fieldName, decode);
    } else if (field.getType().isMap()) {
      return useBuilder
          ? CodeBlock.of("builder.$L.putAll($L)", fieldName, decode)
          : CodeBlock.of("$L.putAll($L)", fieldName, decode);
    } else {
      return useBuilder
          ? field.getType().equals(ProtoType.STRUCT_NULL)
              ? CodeBlock.of("builder.$L(($T) $L)", fieldName, Void.class, decode)
              : CodeBlock.of("builder.$L($L)", fieldName, decode)
          : CodeBlock.of("$L = $L", fieldName, decode);
    }
  }

  private MethodSpec messageAdapterRedact(
      NameAllocator nameAllocator,
      MessageType type,
      ClassName javaType,
      boolean useBuilder,
      ClassName builderJavaType) {
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("redact")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(javaType)
            .addParameter(javaType, "value");

    int redactedFieldCount = 0;
    List<String> requiredRedacted = new ArrayList<>();
    for (Field field : type.getFieldsAndOneOfFields()) {
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
      result.addStatement(
          "throw new $T($S)",
          UnsupportedOperationException.class,
          (isPlural ? "Fields" : "Field")
              + " '"
              + Joiner.on("', '").join(requiredRedacted)
              + "' "
              + (isPlural ? "are" : "is")
              + " required and cannot be redacted.");
      return result.build();
    }

    result.addStatement("$1T builder = value.newBuilder()", builderJavaType);

    for (Field field : type.getFieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      if (field.isRedacted()) {
        if (field.isRepeated()) {
          result.addStatement("builder.$N = $T.emptyList()", fieldName, Collections.class);
        } else if (field.getType().isMap()) {
          result.addStatement("builder.$N = $T.emptyMap()", fieldName, Collections.class);
        } else {
          result.addStatement("builder.$N = null", fieldName);
        }
      } else if (!field.getType().isScalar() && !isEnum(field.getType())) {
        if (field.isRepeated()) {
          CodeBlock adapter = singleAdapterFor(field, nameAllocator);
          result.addStatement(
              "$T.redactElements(builder.$N, $L)", Internal.class, fieldName, adapter);
        } else if (field.getType().isMap()) {
          // We only need to ask the values to redact themselves if the type is a message.
          if (!field.getType().getValueType().isScalar()
              && !isEnum(field.getType().getValueType())) {
            CodeBlock adapter = singleAdapterFor(field.getType().getValueType());
            result.addStatement(
                "$T.redactElements(builder.$N, $L)", Internal.class, fieldName, adapter);
          }
        } else {
          CodeBlock adapter = adapterFor(field, nameAllocator);
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

  private String fieldName(ProtoType type, Field field) {
    MessageType messageType = (MessageType) schema.getType(type);
    NameAllocator names = nameAllocators.getUnchecked(messageType);
    return names.get(field);
  }

  private TypeName fieldType(Field field) {
    ProtoType type = field.getType();
    if (type.isMap()) {
      return ParameterizedTypeName.get(
          ClassName.get(Map.class),
          typeName(type.getKeyType()).box(),
          typeName(type.getValueType()).box());
    }

    TypeName messageType = typeName(type);
    switch (field.getEncodeMode()) {
      case REPEATED:
      case PACKED:
        return listOf(messageType.box());
      case NULL_IF_ABSENT:
      case REQUIRED:
        return messageType.box();
      default:
        if (isWrapper(field.getType())) return messageType.box();
        return messageType;
    }
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
  private AnnotationSpec wireFieldAnnotation(
      NameAllocator nameAllocator, Field field, MessageType message) {
    AnnotationSpec.Builder result = AnnotationSpec.builder(WireField.class);

    NameAllocator localNameAllocator = nameAllocator.clone();

    int tag = field.getTag();
    result.addMember("tag", String.valueOf(tag));
    if (field.getType().isMap()) {
      result.addMember("keyAdapter", "$S", adapterString(field.getType().getKeyType()));
      result.addMember("adapter", "$S", adapterString(field.getType().getValueType()));
    } else {
      result.addMember("adapter", "$S", adapterString(field.getType()));
    }

    WireField.Label wireFieldLabel;
    //noinspection ConstantConditions
    switch (field.getEncodeMode()) {
      case REQUIRED:
        wireFieldLabel = WireField.Label.REQUIRED;
        break;
      case OMIT_IDENTITY:
        // Wrapper types don't omit identity values on JSON as other proto3 messages would.
        if (field.getType().isWrapper()) {
          wireFieldLabel = null;
        } else {
          wireFieldLabel = WireField.Label.OMIT_IDENTITY;
        }
        break;
      case REPEATED:
        wireFieldLabel = WireField.Label.REPEATED;
        break;
      case PACKED:
        wireFieldLabel = WireField.Label.PACKED;
        break;
      case MAP:
      case NULL_IF_ABSENT:
      default:
        wireFieldLabel = null;
    }
    if (wireFieldLabel != null) {
      result.addMember("label", "$T.$L", WireField.Label.class, wireFieldLabel);
    }

    if (field.isRedacted()) {
      result.addMember("redacted", "true");
    }

    String generatedName = localNameAllocator.get(field);
    if (!generatedName.equals(field.getName())) {
      result.addMember("declaredName", "$S", field.getName());
    }

    if (!field.getJsonName().equals(field.getName())) {
      result.addMember("jsonName", "$S", field.getJsonName());
    }

    if (field.isOneOf()) {
      String oneofName = null;
      for (OneOf oneOf : message.getOneOfs()) {
        if (oneOf.getFields().contains(field)) {
          oneofName = oneOf.getName();
          break;
        }
      }
      if (oneofName == null) {
        throw new IllegalArgumentException("No oneof found for field: " + field.getQualifiedName());
      }
      result.addMember("oneofName", "$S", oneofName);
    }

    return result.build();
  }

  // Example:
  //
  // @WireEnumConstant(
  //   declaredName = "final",
  // )
  //
  private @Nullable AnnotationSpec wireEnumConstantAnnotation(
      NameAllocator nameAllocator, EnumConstant constant) {
    AnnotationSpec.Builder result = AnnotationSpec.builder(WireEnumConstant.class);

    NameAllocator localNameAllocator = nameAllocator.clone();

    String generatedName = localNameAllocator.get(constant);
    if (generatedName.equals(constant.getName())) {
      return null;
    }
    result.addMember("declaredName", "$S", constant.getName());

    return result.build();
  }

  private String adapterString(ProtoType type) {
    String builtInAdapterString = builtInAdapterString(type, false);
    if (builtInAdapterString != null) {
      return builtInAdapterString;
    }

    AdapterConstant adapterConstant = profile.getAdapter(type);
    if (adapterConstant != null) {
      return reflectionName(adapterConstant.javaClassName) + "#" + adapterConstant.memberName;
    }

    return reflectionName(typeName(type)) + "#ADAPTER";
  }

  private String reflectionName(TypeName typeName) {
    ClassName className;
    if (typeName instanceof ParameterizedTypeName) {
      className = ((ParameterizedTypeName) typeName).rawType();
    } else {
      className = (ClassName) typeName;
    }
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
    if (!buildersOnly) result.addModifiers(PUBLIC);
    result.addCode("this(");
    for (Field field : type.getFieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = nameAllocator.get(field);
      ParameterSpec.Builder param = ParameterSpec.builder(javaType, fieldName);
      if (emitAndroidAnnotations && field.getEncodeMode() == Field.EncodeMode.NULL_IF_ABSENT) {
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
  // Alternate example, where the constructor takes in a builder, would be the case when there are
  // too many fields:
  //
  // public SimpleMessage(Builder builder, ByteString unknownFields) {
  //   super(ADAPTER, unknownFields);
  //   this.optional_int32 = builder.optional_int32;
  //   this.optional_int64 = builder.optional_int64;
  // }
  //
  private MethodSpec messageConstructor(
      NameAllocator nameAllocator, MessageType type, ClassName builderJavaType) {
    boolean constructorTakesAllFields = constructorTakesAllFields(type);

    NameAllocator localNameAllocator = nameAllocator.clone();

    String adapterName = localNameAllocator.get("ADAPTER");
    String unknownFieldsName = localNameAllocator.newName("unknownFields");
    String builderName = localNameAllocator.newName("builder");
    MethodSpec.Builder result =
        MethodSpec.constructorBuilder()
            .addStatement("super($N, $N)", adapterName, unknownFieldsName);
    if (!buildersOnly) result.addModifiers(PUBLIC);

    for (OneOf oneOf : type.getOneOfs()) {
      if (oneOf.getFields().size() < 2) continue;
      CodeBlock.Builder fieldNamesBuilder = CodeBlock.builder();
      boolean first = true;
      for (Field field : oneOf.getFields()) {
        if (!first) fieldNamesBuilder.add(", ");
        if (constructorTakesAllFields) {
          fieldNamesBuilder.add("$N", localNameAllocator.get(field));
        } else {
          fieldNamesBuilder.add("$N.$N", builderName, localNameAllocator.get(field));
        }
        first = false;
      }
      CodeBlock fieldNames = fieldNamesBuilder.build();
      result.beginControlFlow("if ($T.countNonNull($L) > 1)", Internal.class, fieldNames);
      result.addStatement(
          "throw new IllegalArgumentException($S)",
          "at most one of " + fieldNames + " may be non-null");
      result.endControlFlow();
    }
    for (Field field : type.getFieldsAndOneOfFields()) {
      TypeName javaType = fieldType(field);
      String fieldName = localNameAllocator.get(field);
      String fieldAccessName =
          constructorTakesAllFields ? fieldName : builderName + "." + fieldName;

      if (constructorTakesAllFields) {
        ParameterSpec.Builder param = ParameterSpec.builder(javaType, fieldName);
        if (emitAndroidAnnotations && field.getEncodeMode() == Field.EncodeMode.NULL_IF_ABSENT) {
          param.addAnnotation(NULLABLE);
        }
        result.addParameter(param.build());
      }

      if (field.getEncodeMode() == Field.EncodeMode.OMIT_IDENTITY) {
        // Other scalars use not-boxed types to guarantee a value.
        if (field.getType().isScalar()
                && (field.getType() == ProtoType.STRING || field.getType() == ProtoType.BYTES)
            || (isEnum(field.getType()) && !field.getType().equals(ProtoType.STRUCT_NULL))) {
          result.beginControlFlow("if ($L == null)", fieldAccessName);
          result.addStatement(
              "throw new IllegalArgumentException($S)", fieldAccessName + " == null");
          result.endControlFlow();
        }
      }

      if (field.getType().isMap() && isStruct(field.getType().getValueType())) {
        result.addStatement(
            "this.$1L = $2T.immutableCopyOfMapWithStructValues($1S, $3L)",
            fieldName,
            Internal.class,
            fieldAccessName);
      } else if (isStruct(field.getType())) {
        result.addStatement(
            "this.$1L = $2T.immutableCopyOfStruct($1S, $3L)",
            fieldName,
            Internal.class,
            fieldAccessName);
      } else if (field.isRepeated() || field.getType().isMap()) {
        result.addStatement(
            "this.$1L = $2T.immutableCopyOf($1S, $3L)", fieldName, Internal.class, fieldAccessName);
      } else {
        result.addStatement("this.$1L = $2L", fieldName, fieldAccessName);
      }
    }

    if (!constructorTakesAllFields) {
      result.addParameter(builderJavaType, builderName);
    }

    result.addParameter(BYTE_STRING, unknownFieldsName);

    return result.build();
  }

  private boolean isStruct(ProtoType protoType) {
    return protoType.equals(ProtoType.STRUCT_MAP)
        || protoType.equals(ProtoType.STRUCT_LIST)
        || protoType.equals(ProtoType.STRUCT_VALUE)
        || protoType.equals(ProtoType.STRUCT_NULL);
  }

  private boolean isWrapper(ProtoType protoType) {
    return protoType.equals(ProtoType.DOUBLE_VALUE)
        || protoType.equals(ProtoType.FLOAT_VALUE)
        || protoType.equals(ProtoType.INT64_VALUE)
        || protoType.equals(ProtoType.UINT64_VALUE)
        || protoType.equals(ProtoType.INT32_VALUE)
        || protoType.equals(ProtoType.UINT32_VALUE)
        || protoType.equals(ProtoType.BOOL_VALUE)
        || protoType.equals(ProtoType.STRING_VALUE)
        || protoType.equals(ProtoType.BYTES_VALUE);
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

    TypeName javaType = typeName(type.getType());
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("equals")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(boolean.class)
            .addParameter(Object.class, otherName);

    result.addStatement("if ($N == this) return true", otherName);
    result.addStatement("if (!($N instanceof $T)) return false", otherName, javaType);

    result.addStatement("$T $N = ($T) $N", javaType, oName, javaType, otherName);
    result.addCode("$[return unknownFields().equals($N.unknownFields())", oName);

    List<Field> fields = type.getFieldsAndOneOfFields();
    for (Field field : fields) {
      String fieldName = localNameAllocator.get(field);
      if (field.isRequired() || field.isRepeated() || field.getType().isMap()) {
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
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("hashCode")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(int.class);

    List<Field> fields = type.getFieldsAndOneOfFields();
    if (fields.isEmpty()) {
      result.addStatement("return unknownFields().hashCode()");
      return result.build();
    }

    result.addStatement("int $N = super.hashCode", resultName);
    result.beginControlFlow("if ($N == 0)", resultName);
    result.addStatement("$N = unknownFields().hashCode()", resultName);
    for (Field field : fields) {
      String fieldName = localNameAllocator.get(field);
      TypeName typeName = fieldType(field);
      result.addCode("$1N = $1N * 37 + ", resultName);
      if (typeName == TypeName.BOOLEAN) {
        result.addStatement("$T.hashCode($N)", Boolean.class, fieldName);
      } else if (typeName == TypeName.INT) {
        result.addStatement("$T.hashCode($N)", Integer.class, fieldName);
      } else if (typeName == TypeName.LONG) {
        result.addStatement("$T.hashCode($N)", Long.class, fieldName);
      } else if (typeName == TypeName.FLOAT) {
        result.addStatement("$T.hashCode($N)", Float.class, fieldName);
      } else if (typeName == TypeName.DOUBLE) {
        result.addStatement("$T.hashCode($N)", Double.class, fieldName);
      } else if (field.isRequired() || field.isRepeated() || field.getType().isMap()) {
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

  // Example:
  //
  // private ProtoAdapter<Map<String, ModelEvaluation>> modelsAdapter() {
  //   ProtoAdapter<Map<String, ModelEvaluation>> result = models;
  //   if (result == null) {
  //     result = ProtoAdapter.newMapAdapter(ProtoAdapter.STRING, ModelEvaluation.ADAPTER);
  //     models = result;
  //   }
  //   return result;
  // }
  //
  private MethodSpec mapAdapter(
      NameAllocator nameAllocator, TypeName adapterType, String fieldName, ProtoType mapType) {
    NameAllocator localNameAllocator = nameAllocator.clone();

    String resultName = localNameAllocator.newName("result");
    MethodSpec.Builder result =
        MethodSpec.methodBuilder(fieldName + "Adapter").addModifiers(PRIVATE).returns(adapterType);

    result.addStatement("$T $N = $N", adapterType, resultName, fieldName);
    result.beginControlFlow("if ($N == null)", resultName);
    result.addStatement(
        "$N = $T.newMapAdapter($L, $L)",
        resultName,
        ADAPTER,
        singleAdapterFor(mapType.getKeyType()),
        singleAdapterFor(mapType.getValueType()));
    result.addStatement("$N = $N", fieldName, resultName);
    result.endControlFlow();
    result.addStatement("return $N", resultName);
    return result.build();
  }

  private MethodSpec messageToString(NameAllocator nameAllocator, MessageType type) {
    NameAllocator localNameAllocator = nameAllocator.clone();

    MethodSpec.Builder result =
        MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(String.class);

    String builderName = localNameAllocator.newName("builder");
    result.addStatement("$1T $2N = new $1T()", StringBuilder.class, builderName);

    for (Field field : type.getFieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      TypeName fieldType = fieldType(field);
      if (field.isRepeated() || field.getType().isMap()) {
        result.addCode("if (!$N.isEmpty()) ", fieldName);
      } else if (!field.isRequired() && !fieldType.isPrimitive()) {
        result.addCode("if ($N != null) ", fieldName);
      }
      if (field.isRedacted()) {
        result.addStatement(
            "$N.append(\", $N=$L\")", builderName, field.getName(), DOUBLE_FULL_BLOCK);
      } else if (field.getType().equals(ProtoType.STRING)) {
        result.addStatement(
            "$N.append(\", $N=\").append($T.sanitize($L))",
            builderName,
            field.getName(),
            Internal.class,
            fieldName);
      } else {
        result.addStatement(
            "$N.append(\", $N=\").append($L)", builderName, field.getName(), fieldName);
      }
    }

    result.addStatement(
        "return builder.replace(0, 2, \"$L{\").append('}').toString()",
        type.getType().getSimpleName());

    return result.build();
  }

  private TypeSpec builder(
      NameAllocator nameAllocator, MessageType type, ClassName javaType, ClassName builderType) {
    TypeSpec.Builder result = TypeSpec.classBuilder("Builder").addModifiers(PUBLIC, STATIC, FINAL);

    result.superclass(builderOf(javaType, builderType));

    for (Field field : type.getFieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      result.addField(fieldType(field), fieldName, PUBLIC);
    }

    result.addMethod(builderNoArgsConstructor(nameAllocator, type));

    for (Field field : type.fields()) {
      result.addMethod(setter(nameAllocator, builderType, null, field));
    }

    for (OneOf oneOf : type.getOneOfs()) {
      for (Field field : oneOf.getFields()) {
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
    for (Field field : type.getFieldsAndOneOfFields()) {
      String fieldName = nameAllocator.get(field);
      CodeBlock initialValue = initialValue(field);
      if (initialValue != null) {
        result.addStatement("$L = $L", fieldName, initialValue);
      }
    }
    return result.build();
  }

  /** Returns the initial value of {@code field}, or null if it is doesn't have one. */
  private @Nullable CodeBlock initialValue(Field field) {
    if (field.isPacked() || field.isRepeated()) {
      return CodeBlock.of("$T.newMutableList()", Internal.class);
    } else if (field.getType().isMap()) {
      return CodeBlock.of("$T.newMutableMap()", Internal.class);
    } else if (field.getEncodeMode() == Field.EncodeMode.OMIT_IDENTITY) {
      CodeBlock identityValue = identityValue(field);
      if (identityValue.equals(CodeBlock.of("null"))) {
        return null;
      } else {
        return identityValue(field);
      }
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
    ClassName javaType = (ClassName) typeName(message.getType());
    ClassName builderJavaType = javaType.nestedClass("Builder");

    MethodSpec.Builder result =
        MethodSpec.methodBuilder("newBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(builderJavaType)
            .addStatement("$1T $2L = new $1T()", builderJavaType, builderName);

    List<Field> fields = message.getFieldsAndOneOfFields();
    for (Field field : fields) {
      String fieldName = localNameAllocator.get(field);
      if (field.isRepeated() || field.getType().isMap()) {
        result.addStatement("$1L.$2L = $3T.copyOf($2L)", builderName, fieldName, Internal.class);
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

    MethodSpec.Builder result =
        MethodSpec.methodBuilder(fieldName)
            .addModifiers(PUBLIC)
            .addParameter(javaType, fieldName)
            .returns(builderType);

    if (!field.getDocumentation().isEmpty()) {
      result.addJavadoc("$L\n", sanitizeJavadoc(field.getDocumentation()));
    }

    if (field.isDeprecated()) {
      result.addAnnotation(Deprecated.class);
    }

    if (field.isRepeated() || field.getType().isMap()) {
      result.addStatement("$T.checkElementsNotNull($L)", Internal.class, fieldName);
    }
    result.addStatement("this.$L = $L", fieldName, fieldName);

    if (oneOf != null) {
      for (Field other : oneOf.getFields()) {
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
  //   return new SimpleMessage(field_one, super.buildUnknownFields());
  // }
  //
  // The call to checkRequiredFields will be emitted only if the message has
  // required fields. The constructor can also take instance of the builder
  // rather than individual fields depending on how many fields there are.
  //
  private MethodSpec builderBuild(
      NameAllocator nameAllocator, MessageType message, ClassName javaType) {
    MethodSpec.Builder result =
        MethodSpec.methodBuilder("build")
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
        missingArgs.add("$1L, $2S", nameAllocator.get(requiredField), requiredField.getName());
      }

      result
          .beginControlFlow("if ($L)", conditionals.add("$]").build())
          .addStatement("throw $T.missingRequiredFields($L)", Internal.class, missingArgs.build())
          .endControlFlow();
    }

    boolean constructorTakesAllFields = constructorTakesAllFields(message);

    result.addCode("return new $T(", javaType);
    if (constructorTakesAllFields) {
      for (Field field : message.getFieldsAndOneOfFields()) {
        result.addCode("$L, ", nameAllocator.get(field));
      }
    } else {
      result.addCode("this, ");
    }

    result.addCode("super.buildUnknownFields());\n");
    return result.build();
  }

  private CodeBlock defaultValue(Field field) {
    Object defaultValue = field.getDefault();

    if (defaultValue == null && isEnum(field.getType())) {
      defaultValue = enumDefault(field.getType()).getName();
    }

    if (field.getType().isScalar() || defaultValue != null) {
      return fieldInitializer(field.getType(), defaultValue, false);
    }

    throw new IllegalStateException("Field " + field + " cannot have default value");
  }

  private CodeBlock fieldInitializer(ProtoType type, @Nullable Object value, boolean annotation) {
    TypeName javaType = typeName(type);

    if (value instanceof List) {
      CodeBlock.Builder builder = CodeBlock.builder();
      if (annotation) {
        builder.add("{");
      } else {
        builder.add("$T.asList(", Arrays.class);
      }
      boolean first = true;
      for (Object o : (List<?>) value) {
        if (!first) builder.add(",");
        first = false;
        builder.add("\n$>$>$L$<$<", fieldInitializer(type, o, annotation));
      }
      builder.add(annotation ? "}" : ")");
      return builder.build();

    } else if (value instanceof Map) {
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add("new $T.Builder()", javaType);
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        ProtoMember protoMember = (ProtoMember) entry.getKey();
        Field field = schema.getField(protoMember);
        CodeBlock valueInitializer =
            fieldInitializer(field.getType(), entry.getValue(), annotation);
        builder.add("\n$>$>.$L($L)$<$<", fieldName(type, field), valueInitializer);
      }
      builder.add("\n$>$>.build()$<$<");
      return builder.build();

    } else if (javaType.equals(TypeName.BOOLEAN)) {
      return CodeBlock.of("$L", value != null ? value : false);

    } else if (javaType.equals(TypeName.INT)) {
      return CodeBlock.of("$L", optionValueToInt(value));

    } else if (javaType.equals(TypeName.LONG)) {
      return CodeBlock.of("$LL", optionValueToLong(value));

    } else if (javaType.equals(TypeName.FLOAT)) {
      if (value == null) {
        return CodeBlock.of("0.0f");
      } else if ("inf".equals(value)) {
        return CodeBlock.of("Float.POSITIVE_INFINITY");
      } else if ("-inf".equals(value)) {
        return CodeBlock.of("Float.NEGATIVE_INFINITY");
      } else if ("nan".equals(value) || "-nan".equals(value)) {
        return CodeBlock.of("Float.NaN");
      } else {
        return CodeBlock.of("$Lf", String.valueOf(value));
      }

    } else if (javaType.equals(TypeName.DOUBLE)) {
      if (value == null) {
        return CodeBlock.of("0.0d");
      } else if ("inf".equals(value)) {
        return CodeBlock.of("Double.POSITIVE_INFINITY");
      } else if ("-inf".equals(value)) {
        return CodeBlock.of("Double.NEGATIVE_INFINITY");
      } else if ("nan".equals(value) || "-nan".equals(value)) {
        return CodeBlock.of("Double.NaN");
      } else {
        return CodeBlock.of("$Ld", String.valueOf(value));
      }

    } else if (javaType.equals(STRING)) {
      return CodeBlock.of("$S", value != null ? value : "");

    } else if (javaType.equals(BYTE_STRING)) {
      if (value == null) {
        return CodeBlock.of("$T.EMPTY", ByteString.class);
      } else {
        return CodeBlock.of(
            "$T.decodeBase64($S)",
            ByteString.class,
            ByteString.encodeString(String.valueOf(value), Charsets.ISO_8859_1).base64());
      }

    } else if (isEnum(type) && value != null) {
      return CodeBlock.of("$T.$L", javaType, value);

    } else {
      throw new IllegalStateException(type + " is not an allowed scalar type");
    }
  }

  private CodeBlock identityValue(Field field) {
    switch (field.getEncodeMode()) {
      case MAP:
        return CodeBlock.of("$T.emptyMap()", Collections.class);
      case REPEATED:
      case PACKED:
        return CodeBlock.of("$T.emptyList()", Collections.class);
      case NULL_IF_ABSENT:
        return CodeBlock.of("null");
      case OMIT_IDENTITY:
        ProtoType protoType = field.getType();
        Type type = schema.getType(protoType);
        if (protoType.equals(ProtoType.STRUCT_NULL)) {
          return CodeBlock.of("null");
        } else if (field.isOneOf()) {
          return CodeBlock.of("null");
        } else if (protoType.isScalar()) {
          CodeBlock value = PROTOTYPE_TO_IDENTITY_VALUES.get(protoType);
          if (value == null) {
            throw new IllegalArgumentException("Unexpected scalar proto type: " + protoType);
          }
          return value;
        } else if (type instanceof MessageType) {
          return CodeBlock.of("null");
        } else if (type instanceof EnumType) {
          return identity((EnumType) type);
        }
      case REQUIRED:
      default:
        throw new IllegalArgumentException(
            "No identity value for field: " + field + "(" + field.getEncodeMode() + ")");
    }
  }

  private CodeBlock identity(EnumType enumType) {
    EnumConstant constantZero = enumType.constant(0);
    if (constantZero == null) return CodeBlock.of("null");

    return CodeBlock.of(
        "$T.$L",
        typeName(enumType.getType()),
        nameAllocators.getUnchecked(enumType).get(constantZero));
  }

  /** Returns the full name of the class generated for {@code member}. */
  public ClassName generatedTypeName(ProtoMember member) {
    return (ClassName) memberToJavaName.get(member);
  }

  // Example:
  //
  // @Retention(RetentionPolicy.RUNTIME)
  // @Target(ElementType.FIELD)
  // public @interface MyFieldOption {
  //   String value();
  // }
  public @Nullable TypeSpec generateOptionType(Extend extend, Field field) {
    checkArgument(extend.getFields().contains(field));

    if (!emitDeclaredOptions) return null;

    ElementType elementType = annotationTargetType(extend);
    if (elementType == null) return null;

    if (!eligibleAsAnnotationMember(schema, field)) return null;
    TypeName returnType;
    if (field.getLabel().equals(Field.Label.REPEATED)) {
      TypeName typeName = typeName(field.getType());
      if (typeName.equals(TypeName.LONG)
          || typeName.equals(TypeName.INT)
          || typeName.equals(TypeName.FLOAT)
          || typeName.equals(TypeName.DOUBLE)
          || typeName.equals(TypeName.BOOLEAN)
          || typeName.equals(ClassName.get(String.class))
          || isEnum(field.getType())) {
        returnType = ArrayTypeName.of(typeName);
      } else {
        throw new IllegalStateException("Unsupported annotation for " + field.getType());
      }
    } else {
      returnType = typeName(field.getType());
    }

    ClassName javaType = generatedTypeName(extend.member(field));

    TypeSpec.Builder builder =
        TypeSpec.annotationBuilder(javaType.simpleName())
            .addModifiers(PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(Retention.class)
                    .addMember("value", "$T.$L", RetentionPolicy.class, RetentionPolicy.RUNTIME)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(Target.class)
                    .addMember("value", "$T.$L", ElementType.class, elementType)
                    .build());

    if (!field.getDocumentation().isEmpty()) {
      builder.addJavadoc("$L\n", field.getDocumentation());
    }

    builder.addMethod(
        MethodSpec.methodBuilder("value")
            .returns(returnType)
            .addModifiers(PUBLIC, ABSTRACT)
            .build());

    return builder.build();
  }

  private List<AnnotationSpec> optionAnnotations(Options options) {
    List<AnnotationSpec> result = new ArrayList<>();
    for (Map.Entry<ProtoMember, Object> entry : options.getMap().entrySet()) {
      AnnotationSpec annotationSpec = optionAnnotation(entry.getKey(), entry.getValue());
      if (annotationSpec != null) {
        result.add(annotationSpec);
      }
    }
    return result;
  }

  private @Nullable AnnotationSpec optionAnnotation(ProtoMember protoMember, Object value) {
    if (!emitAppliedOptions) return null;

    Field field = schema.getField(protoMember);
    if (field == null) return null;
    if (!eligibleAsAnnotationMember(schema, field)) return null;

    ClassName type = (ClassName) memberToJavaName.get(protoMember);
    CodeBlock fieldValue = fieldInitializer(field.getType(), value, true);

    return AnnotationSpec.builder(type).addMember("value", fieldValue).build();
  }
}
