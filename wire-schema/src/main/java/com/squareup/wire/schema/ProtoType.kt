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
package com.squareup.wire.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Names a protocol buffer message, enumerated type, service, map, or a scalar. This class models a
 * fully-qualified name using the protocol buffer package.
 */
public final class ProtoType {
  public static final ProtoType BOOL = new ProtoType(true, "bool");
  public static final ProtoType BYTES = new ProtoType(true, "bytes");
  public static final ProtoType DOUBLE = new ProtoType(true, "double");
  public static final ProtoType FLOAT = new ProtoType(true, "float");
  public static final ProtoType FIXED32 = new ProtoType(true, "fixed32");
  public static final ProtoType FIXED64 = new ProtoType(true, "fixed64");
  public static final ProtoType INT32 = new ProtoType(true, "int32");
  public static final ProtoType INT64 = new ProtoType(true, "int64");
  public static final ProtoType SFIXED32 = new ProtoType(true, "sfixed32");
  public static final ProtoType SFIXED64 = new ProtoType(true, "sfixed64");
  public static final ProtoType SINT32 = new ProtoType(true, "sint32");
  public static final ProtoType SINT64 = new ProtoType(true, "sint64");
  public static final ProtoType STRING = new ProtoType(true, "string");
  public static final ProtoType UINT32 = new ProtoType(true, "uint32");
  public static final ProtoType UINT64 = new ProtoType(true, "uint64");

  private static final Map<String, ProtoType> SCALAR_TYPES;
  static {
    Map<String, ProtoType> scalarTypes = new LinkedHashMap<>();
    scalarTypes.put(BOOL.string, BOOL);
    scalarTypes.put(BYTES.string, BYTES);
    scalarTypes.put(DOUBLE.string, DOUBLE);
    scalarTypes.put(FLOAT.string, FLOAT);
    scalarTypes.put(FIXED32.string, FIXED32);
    scalarTypes.put(FIXED64.string, FIXED64);
    scalarTypes.put(INT32.string, INT32);
    scalarTypes.put(INT64.string, INT64);
    scalarTypes.put(SFIXED32.string, SFIXED32);
    scalarTypes.put(SFIXED64.string, SFIXED64);
    scalarTypes.put(SINT32.string, SINT32);
    scalarTypes.put(SINT64.string, SINT64);
    scalarTypes.put(STRING.string, STRING);
    scalarTypes.put(UINT32.string, UINT32);
    scalarTypes.put(UINT64.string, UINT64);
    SCALAR_TYPES = Collections.unmodifiableMap(scalarTypes);
  }

  private final boolean isScalar;
  private final String string;
  private final boolean isMap;
  private final ProtoType keyType;
  private final ProtoType valueType;

  /** Creates a scalar or message type. */
  private ProtoType(boolean isScalar, String string) {
    checkNotNull(string, "string == null");
    this.isScalar = isScalar;
    this.string = string;
    this.isMap = false;
    this.keyType = null;
    this.valueType = null;
  }

  /** Creates a map type. */
  ProtoType(ProtoType keyType, ProtoType valueType, String string) {
    checkNotNull(keyType, "keyType == null");
    checkNotNull(valueType, "valueType == null");
    checkNotNull(string, "string == null");
    checkArgument(
        keyType.isScalar() && !keyType.equals(BYTES) && !keyType.equals(DOUBLE) && !keyType.equals(
            FLOAT), "map key must be non-byte, non-floating point scalar: %s", keyType);
    this.isScalar = false;
    this.string = string;
    this.isMap = true;
    this.keyType = keyType; // TODO restrict what's allowed here
    this.valueType = valueType;
  }

  public String simpleName() {
    int dot = string.lastIndexOf('.');
    return string.substring(dot + 1);
  }

  /** Returns the enclosing type, or null if this type is not nested in another type. */
  public String enclosingTypeOrPackage() {
    int dot = string.lastIndexOf('.');
    return dot == -1 ? null : string.substring(0, dot);
  }

  public boolean isScalar() {
    return isScalar;
  }

  public boolean isMap() {
    return isMap;
  }

  /** The type of the map's keys. Only present when {@link #isMap} is true. */
  public ProtoType keyType() {
    return keyType;
  }

  /** The type of the map's values. Only present when {@link #isMap} is true. */
  public ProtoType valueType() {
    return valueType;
  }

  public static ProtoType get(String enclosingTypeOrPackage, String typeName) {
    return enclosingTypeOrPackage != null
        ? get(enclosingTypeOrPackage + '.' + typeName)
        : get(typeName);
  }

  public static ProtoType get(String name) {
    ProtoType scalar = SCALAR_TYPES.get(name);
    if (scalar != null) return scalar;

    if (name == null || name.isEmpty() || name.contains("#")) {
      throw new IllegalArgumentException("unexpected name: " + name);
    }

    if (name.startsWith("map<") && name.endsWith(">")) {
      int comma = name.indexOf(',');
      if (comma == -1) throw new IllegalArgumentException("expected ',' in map type: " + name);
      ProtoType key = get(name.substring(4, comma).trim());
      ProtoType value = get(name.substring(comma + 1, name.length() - 1).trim());
      return new ProtoType(key, value, name);
    }

    return new ProtoType(false, name);
  }

  public ProtoType nestedType(String name) {
    if (isScalar) {
      throw new UnsupportedOperationException("scalar cannot have a nested type");
    }
    if (isMap) {
      throw new UnsupportedOperationException("map cannot have a nested type");
    }
    if (name == null || name.contains(".") || name.isEmpty()) {
      throw new IllegalArgumentException("unexpected name: " + name);
    }
    return new ProtoType(false, string + '.' + name);
  }

  @Override public boolean equals(Object o) {
    return o instanceof ProtoType
        && string.equals(((ProtoType) o).string);
  }

  @Override public int hashCode() {
    return string.hashCode();
  }

  @Override public String toString() {
    return string;
  }
}
