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

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.wire.ExtendableMessage;
import com.squareup.wire.Extension;
import com.squareup.wire.Message;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okio.ByteString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Converts proto definitions into Java source code.
 *
 * <p>This can map type names from protocol buffers (like {@code uint32}, {@code string}, or {@code
 * squareup.protos.person.Person} to the corresponding Java names (like {@code int}, {@code
 * java.lang.String}, or {@code com.squareup.protos.person.Person}).
 */
public final class JavaGenerator {
  public static final ClassName BYTE_STRING = ClassName.get(ByteString.class);
  public static final ClassName STRING = ClassName.get(String.class);
  public static final ClassName LIST = ClassName.get(List.class);
  public static final ClassName MESSAGE = ClassName.get(Message.class);
  public static final ClassName EXTENDABLE_MESSAGE = ClassName.get(ExtendableMessage.class);
  public static final ClassName BUILDER = ClassName.get(Message.Builder.class);
  public static final ClassName EXTENDABLE_BUILDER
      = ClassName.get(ExtendableMessage.ExtendableBuilder.class);
  public static final ClassName EXTENSION = ClassName.get(Extension.class);
  public static final TypeName MESSAGE_OPTIONS
      = ClassName.get("com.google.protobuf", "MessageOptions");
  public static final TypeName FIELD_OPTIONS = ClassName.get("com.google.protobuf", "FieldOptions");
  public static final TypeName ENUM_OPTIONS = ClassName.get("com.google.protobuf", "EnumOptions");

  private static final Map<Type.Name, TypeName> SCALAR_TYPES_MAP =
      ImmutableMap.<Type.Name, TypeName>builder()
          .put(Type.Name.BOOL, TypeName.BOOLEAN.box())
          .put(Type.Name.BYTES, ClassName.get(ByteString.class))
          .put(Type.Name.DOUBLE, TypeName.DOUBLE.box())
          .put(Type.Name.FLOAT, TypeName.FLOAT.box())
          .put(Type.Name.FIXED32, TypeName.INT.box())
          .put(Type.Name.FIXED64, TypeName.LONG.box())
          .put(Type.Name.INT32, TypeName.INT.box())
          .put(Type.Name.INT64, TypeName.LONG.box())
          .put(Type.Name.SFIXED32, TypeName.INT.box())
          .put(Type.Name.SFIXED64, TypeName.LONG.box())
          .put(Type.Name.SINT32, TypeName.INT.box())
          .put(Type.Name.SINT64, TypeName.LONG.box())
          .put(Type.Name.STRING, ClassName.get(String.class))
          .put(Type.Name.UINT32, TypeName.INT.box())
          .put(Type.Name.UINT64, TypeName.LONG.box())
          .build();

  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";

  private final Schema schema;
  private final ImmutableMap<Type.Name, TypeName> nameToJavaName;
  private final ImmutableMap<Field, ProtoFile> extensionFieldToFile;

  private JavaGenerator(
      Schema schema,
      ImmutableMap<Type.Name, TypeName> nameToJavaName,
      ImmutableMap<Field, ProtoFile> extensionFieldToFile) {
    this.schema = schema;
    this.nameToJavaName = nameToJavaName;
    this.extensionFieldToFile = extensionFieldToFile;
  }

  public static JavaGenerator get(Schema schema) {
    ImmutableMap.Builder<Type.Name, TypeName> nameToJavaName = ImmutableMap.builder();
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
        ClassName className = ClassName.get(javaPackage, service.name().simpleName());
        nameToJavaName.put(service.name(), className);
      }
    }

    return new JavaGenerator(schema, nameToJavaName.build(), extensionFieldToFile.build());
  }

  private static void putAll(ImmutableMap.Builder<Type.Name, TypeName> wireToJava,
      String javaPackage, ClassName enclosingClassName, List<Type> types) {
    for (Type type : types) {
      ClassName className = enclosingClassName != null
          ? enclosingClassName.nestedClass(type.name().simpleName())
          : ClassName.get(javaPackage, type.name().simpleName());
      wireToJava.put(type.name(), className);
      putAll(wireToJava, javaPackage, className, type.nestedTypes());
    }
  }

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

  public TypeName typeName(Type.Name name) {
    TypeName candidate = nameToJavaName.get(name);
    checkArgument(candidate != null, "unexpected type %s", name);
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

  public boolean isEnum(Type.Name type) {
    Type wireType = schema.getType(type);
    return wireType instanceof EnumType;
  }

  public EnumConstant enumDefault(Type.Name type) {
    EnumType wireEnum = (EnumType) schema.getType(type);
    return wireEnum.constants().get(0);
  }

  public static TypeName listOf(TypeName type) {
    return ParameterizedTypeName.get(LIST, type);
  }

  public static TypeName extendableMessageOf(TypeName type) {
    return ParameterizedTypeName.get(JavaGenerator.EXTENDABLE_MESSAGE, type);
  }

  public static TypeName builderOf(TypeName messageType) {
    return ParameterizedTypeName.get(BUILDER, messageType);
  }

  public static TypeName extendableBuilderOf(TypeName messageType, ClassName builderType) {
    return ParameterizedTypeName.get(EXTENDABLE_BUILDER, messageType, builderType);
  }

  public static TypeName extensionOf(TypeName messageType, TypeName fieldType) {
    return ParameterizedTypeName.get(EXTENSION, messageType, fieldType);
  }

  /** A grab-bag of fixes for things that can go wrong when converting to javadoc. */
  public static String sanitizeJavadoc(String documentation) {
    // Remove trailing whitespace on each line.
    documentation = documentation.replaceAll("[^\\S\n]+\n", "\n");
    documentation = documentation.replaceAll("\\s+$", "");
    // Rewrite '@see <url>' to use an html anchor tag
    documentation = documentation.replaceAll(
        "@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
  }

  public interface IO {
    IO DEFAULT = new IO() {
      @Override public void write(File outputDirectory, JavaFile javaFile) throws IOException {
        javaFile.writeTo(outputDirectory);
      }
    };

    void write(File outputDirectory, JavaFile javaFile) throws IOException;
  }
}
