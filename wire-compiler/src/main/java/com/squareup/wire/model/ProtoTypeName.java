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
package com.squareup.wire.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.wire.internal.Util;
import java.lang.reflect.Field;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Names a protocol buffer message, enumerated type or a scalar. This class models a fully-qualified
 * name using the protocol buffer package.
 */
public final class ProtoTypeName {
  public static final ProtoTypeName BOOL = new ProtoTypeName("bool");
  public static final ProtoTypeName BYTES = new ProtoTypeName("bytes");
  public static final ProtoTypeName DOUBLE = new ProtoTypeName("double");
  public static final ProtoTypeName FLOAT = new ProtoTypeName("float");
  public static final ProtoTypeName FIXED32 = new ProtoTypeName("fixed32");
  public static final ProtoTypeName FIXED64 = new ProtoTypeName("fixed64");
  public static final ProtoTypeName INT32 = new ProtoTypeName("int32");
  public static final ProtoTypeName INT64 = new ProtoTypeName("int64");
  public static final ProtoTypeName SFIXED32 = new ProtoTypeName("sfixed32");
  public static final ProtoTypeName SFIXED64 = new ProtoTypeName("sfixed64");
  public static final ProtoTypeName SINT32 = new ProtoTypeName("sint32");
  public static final ProtoTypeName SINT64 = new ProtoTypeName("sint64");
  public static final ProtoTypeName STRING = new ProtoTypeName("string");
  public static final ProtoTypeName UINT32 = new ProtoTypeName("uint32");
  public static final ProtoTypeName UINT64 = new ProtoTypeName("uint64");

  static final ProtoTypeName FILE_OPTIONS = get("google.protobuf", "FileOptions");
  static final ProtoTypeName MESSAGE_OPTIONS = get("google.protobuf", "MessageOptions");
  static final ProtoTypeName FIELD_OPTIONS = get("google.protobuf", "FieldOptions");
  static final ProtoTypeName ENUM_OPTIONS = get("google.protobuf", "EnumOptions");
  static final ProtoTypeName ENUM_VALUE_OPTIONS = get("google.protobuf", "EnumValueOptions");
  static final ProtoTypeName SERVICE_OPTIONS = get("google.protobuf", "ServiceOptions");
  static final ProtoTypeName METHOD_OPTIONS = get("google.protobuf", "MethodOptions");

  private static final ImmutableMap<String, ProtoTypeName> SCALAR_TYPES;
  static {
    ImmutableMap.Builder<String, ProtoTypeName> scalarTypes = ImmutableMap.builder();
    try {
      for (Field field : ProtoTypeName.class.getDeclaredFields()) {
        if (field.getType() == ProtoTypeName.class) {
          ProtoTypeName protoTypeName = (ProtoTypeName) field.get(null);
          if (protoTypeName.isScalar) {
            scalarTypes.put(protoTypeName.names.get(0), protoTypeName);
          }
        }
      }
      SCALAR_TYPES = scalarTypes.build();
    } catch (IllegalAccessException e) {
      throw new AssertionError();
    }
  }

  private final String protoPackage;

  /** A chain of enclosed message names, outermost is first. */
  private final ImmutableList<String> names;
  private final boolean isScalar;
  private final String string;

  private ProtoTypeName(String scalarName) {
    this(null, ImmutableList.of(scalarName), true);
  }

  private ProtoTypeName(String protoPackage, ImmutableList<String> names, boolean isScalar) {
    this.protoPackage = protoPackage;
    this.names = names;
    this.isScalar = isScalar;
    this.string = (protoPackage != null ? (protoPackage + '.') : "") + Joiner.on('.').join(names);
  }

  public String packageName() {
    return protoPackage;
  }

  public String simpleName() {
    return names.get(names.size() - 1);
  }

  /** Returns the enclosing type, or null if this type is not nested in another type. */
  public ProtoTypeName enclosingTypeName() {
    if (names.size() == 1) return null;
    return new ProtoTypeName(protoPackage, names.subList(0, names.size() - 1), isScalar);
  }

  public boolean isScalar() {
    return isScalar;
  }

  public boolean isPackableScalar() {
    return isScalar && !equals(STRING) && !equals(BYTES);
  }

  public boolean isFieldOptions() {
    return equals(FIELD_OPTIONS);
  }

  public boolean isMessageOptions() {
    return equals(MESSAGE_OPTIONS);
  }

  public static ProtoTypeName get(String protoPackage, String name) {
    checkNotNull(name, "name");
    return new ProtoTypeName(protoPackage, ImmutableList.of(name), false);
  }

  public static ProtoTypeName getScalar(String name) {
    return SCALAR_TYPES.get(name);
  }

  public ProtoTypeName nestedType(String name) {
    checkState(!isScalar);
    checkNotNull(name, "name");
    return new ProtoTypeName(protoPackage, Util.concatenate(names, name), false);
  }

  @Override public boolean equals(Object o) {
    return o instanceof ProtoTypeName
        && Objects.equals(((ProtoTypeName) o).string, string);
  }

  @Override public int hashCode() {
    return string.hashCode();
  }

  @Override public String toString() {
    return string;
  }
}
