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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.wire.ExtendableMessage;
import com.squareup.wire.Extension;
import com.squareup.wire.Message;
import com.squareup.wire.internal.Util;
import com.squareup.wire.model.ProtoTypeName;
import com.squareup.wire.model.WireEnum;
import com.squareup.wire.model.WireEnumConstant;
import com.squareup.wire.model.WireOption;
import com.squareup.wire.model.WireProtoFile;
import com.squareup.wire.model.WireType;
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

  private static final Map<ProtoTypeName, TypeName> SCALAR_TYPES_MAP =
      ImmutableMap.<ProtoTypeName, TypeName>builder()
          .put(ProtoTypeName.BOOL, TypeName.BOOLEAN.box())
          .put(ProtoTypeName.BYTES, ClassName.get(ByteString.class))
          .put(ProtoTypeName.DOUBLE, TypeName.DOUBLE.box())
          .put(ProtoTypeName.FLOAT, TypeName.FLOAT.box())
          .put(ProtoTypeName.FIXED32, TypeName.INT.box())
          .put(ProtoTypeName.FIXED64, TypeName.LONG.box())
          .put(ProtoTypeName.INT32, TypeName.INT.box())
          .put(ProtoTypeName.INT64, TypeName.LONG.box())
          .put(ProtoTypeName.SFIXED32, TypeName.INT.box())
          .put(ProtoTypeName.SFIXED64, TypeName.LONG.box())
          .put(ProtoTypeName.SINT32, TypeName.INT.box())
          .put(ProtoTypeName.SINT64, TypeName.LONG.box())
          .put(ProtoTypeName.STRING, ClassName.get(String.class))
          .put(ProtoTypeName.UINT32, TypeName.INT.box())
          .put(ProtoTypeName.UINT64, TypeName.LONG.box())
          .build();

  private final ImmutableMap<ProtoTypeName, TypeName> wireToJava;
  private final ImmutableMap<ProtoTypeName, WireType> wireToType;

  public JavaGenerator(
      ImmutableMap<ProtoTypeName, TypeName> wireToJava,
      ImmutableMap<ProtoTypeName, WireType> wireToType) {
    this.wireToJava = wireToJava;
    this.wireToType = wireToType;
  }

  public static JavaGenerator get(List<WireProtoFile> wireProtoFiles) {
    ImmutableMap.Builder<ProtoTypeName, TypeName> wireToJava = ImmutableMap.builder();
    ImmutableMap.Builder<ProtoTypeName, WireType> wireToType = ImmutableMap.builder();
    wireToJava.putAll(SCALAR_TYPES_MAP);

    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      String javaPackage = javaPackage(wireProtoFile);
      putAll(wireToJava, wireToType, javaPackage, null, wireProtoFile.types());
    }

    return new JavaGenerator(wireToJava.build(), wireToType.build());
  }

  private static void putAll(ImmutableMap.Builder<ProtoTypeName, TypeName> wireToJava,
      ImmutableMap.Builder<ProtoTypeName, WireType> wireToType, String javaPackage,
      ClassName enclosingClassName, List<WireType> types) {
    for (WireType type : types) {
      ClassName className = enclosingClassName != null
          ? enclosingClassName.nestedClass(type.protoTypeName().simpleName())
          : ClassName.get(javaPackage, type.protoTypeName().simpleName());
      wireToJava.put(type.protoTypeName(), className);
      wireToType.put(type.protoTypeName(), type);
      putAll(wireToJava, wireToType, javaPackage, className, type.nestedTypes());
    }
  }

  public ClassName extensionsClass(WireProtoFile protoFile) {
    return ClassName.get(javaPackage(protoFile), "Ext_" + protoFile.name());
  }

  public TypeName typeName(ProtoTypeName protoTypeName) {
    TypeName candidate = wireToJava.get(protoTypeName);
    checkArgument(candidate != null, "unexpected type %s", protoTypeName);
    return candidate;
  }

  private static String javaPackage(WireProtoFile wireProtoFile) {
    WireOption javaPackageOption = Util.findOption(wireProtoFile.options(), "java_package");
    if (javaPackageOption != null) {
      return String.valueOf(javaPackageOption.value());
    } else if (wireProtoFile.packageName() != null) {
      return wireProtoFile.packageName();
    } else {
      return "";
    }
  }

  public boolean isEnum(ProtoTypeName type) {
    WireType wireType = wireToType.get(type);
    return wireType instanceof WireEnum;
  }

  public WireEnumConstant enumDefault(ProtoTypeName type) {
    WireEnum wireEnum = (WireEnum) wireToType.get(type);
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

  public static TypeName extendableBuilderOf(TypeName messageType) {
    return ParameterizedTypeName.get(EXTENDABLE_BUILDER, messageType);
  }

  public static TypeName extensionOf(TypeName messageType, TypeName fieldType) {
    return ParameterizedTypeName.get(EXTENSION, messageType, fieldType);
  }
}
