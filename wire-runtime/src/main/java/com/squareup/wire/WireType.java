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
package com.squareup.wire;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.squareup.wire.Preconditions.checkNotNull;

/**
 * Names a protocol buffer message, enumerated type, service, or a scalar. This class models a
 * fully-qualified name using the protocol buffer package.
 */
public final class WireType {
  public static final WireType BOOL = new WireType(true, "bool");
  public static final WireType BYTES = new WireType(true, "bytes");
  public static final WireType DOUBLE = new WireType(true, "double");
  public static final WireType FLOAT = new WireType(true, "float");
  public static final WireType FIXED32 = new WireType(true, "fixed32");
  public static final WireType FIXED64 = new WireType(true, "fixed64");
  public static final WireType INT32 = new WireType(true, "int32");
  public static final WireType INT64 = new WireType(true, "int64");
  public static final WireType SFIXED32 = new WireType(true, "sfixed32");
  public static final WireType SFIXED64 = new WireType(true, "sfixed64");
  public static final WireType SINT32 = new WireType(true, "sint32");
  public static final WireType SINT64 = new WireType(true, "sint64");
  public static final WireType STRING = new WireType(true, "string");
  public static final WireType UINT32 = new WireType(true, "uint32");
  public static final WireType UINT64 = new WireType(true, "uint64");

  private static final Map<String, WireType> SCALAR_TYPES;
  static {
    Map<String, WireType> scalarTypes = new LinkedHashMap<>();
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

  private WireType(boolean isScalar, String string) {
    checkNotNull(string, "string == null");
    this.isScalar = isScalar;
    this.string = string;
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

  public static WireType get(String enclosingTypeOrPackage, String typeName) {
    return enclosingTypeOrPackage != null
        ? get(enclosingTypeOrPackage + '.' + typeName)
        : get(typeName);
  }

  public static WireType get(String name) {
    WireType scalar = SCALAR_TYPES.get(name);
    if (scalar != null) return scalar;

    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("unexpected name: " + name);
    }
    return new WireType(false, name);
  }

  public WireType nestedType(String name) {
    if (isScalar) {
      throw new UnsupportedOperationException("scalar cannot have a nested type");
    }
    if (name == null || name.contains(".") || name.isEmpty()) {
      throw new IllegalArgumentException("unexpected name: " + name);
    }
    return new WireType(false, string + '.' + name);
  }

  @Override public boolean equals(Object o) {
    return o instanceof WireType
        && string.equals(((WireType) o).string);
  }

  @Override public int hashCode() {
    return string.hashCode();
  }

  @Override public String toString() {
    return string;
  }
}
