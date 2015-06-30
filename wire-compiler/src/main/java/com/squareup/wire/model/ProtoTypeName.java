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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.wire.model.Util.checkNotNull;
import static com.squareup.wire.model.Util.checkState;

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

  public static final ProtoTypeName FILE_OPTIONS = get("google.protobuf", "FileOptions");
  public static final ProtoTypeName MESSAGE_OPTIONS = get("google.protobuf", "MessageOptions");
  public static final ProtoTypeName FIELD_OPTIONS = get("google.protobuf", "FieldOptions");
  public static final ProtoTypeName ENUM_OPTIONS = get("google.protobuf", "EnumOptions");
  public static final ProtoTypeName ENUM_VALUE_OPTIONS = get("google.protobuf", "EnumValueOptions");
  public static final ProtoTypeName SERVICE_OPTIONS = get("google.protobuf", "ServiceOptions");
  public static final ProtoTypeName METHOD_OPTIONS = get("google.protobuf", "MethodOptions");

  private static final Map<String, ProtoTypeName> SCALAR_TYPES;
  static {
    Map<String, ProtoTypeName> scalarTypes = new LinkedHashMap<String, ProtoTypeName>();
    try {
      for (Field field : ProtoTypeName.class.getDeclaredFields()) {
        if (field.getType() == ProtoTypeName.class) {
          ProtoTypeName protoTypeName = (ProtoTypeName) field.get(null);
          if (protoTypeName.isScalar) {
            scalarTypes.put(protoTypeName.names.get(0), protoTypeName);
          }
        }
      }
      SCALAR_TYPES = Collections.unmodifiableMap(scalarTypes);
    } catch (IllegalAccessException e) {
      throw new AssertionError();
    }
  }

  private final String protoPackage;

  /** A chain of enclosed message names, outermost is first. */
  private final List<String> names;
  private final boolean isScalar;

  private ProtoTypeName(String scalarName) {
    this(null, Collections.singletonList(scalarName), true);
  }

  private ProtoTypeName(String protoPackage, List<String> names, boolean isScalar) {
    this.protoPackage = protoPackage;
    this.names = names;
    this.isScalar = isScalar;
  }

  public String packageName() {
    return protoPackage;
  }

  public String simpleName() {
    return names.get(names.size() - 1);
  }

  public static ProtoTypeName get(String protoPackage, String name) {
    checkNotNull(name, "name");
    return new ProtoTypeName(protoPackage, Collections.singletonList(name), false);
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
        && Util.equal(((ProtoTypeName) o).protoPackage, protoPackage)
        && ((ProtoTypeName) o).names.equals(names)
        && ((ProtoTypeName) o).isScalar == isScalar;
  }

  @Override public int hashCode() {
    int result = (protoPackage != null ? protoPackage.hashCode() : 0);
    result = result * 37 + names.hashCode();
    return result;
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    if (protoPackage != null) {
      result.append(protoPackage);
      result.append('.');
    }
    for (int i = 0, size = names.size(); i < size; i++) {
      if (i > 0) result.append('.');
      result.append(names.get(i));
    }
    return result.toString();
  }
}
