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
package com.squareup.wire.internal.protoparser;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a scalar, map, or named type. While this class is an interface, only the
 * included implementations are supported.
 */
public interface DataType {
  enum Kind {
    /** Type is a {@link ScalarType}. */
    SCALAR,
    /** Type is a {@link MapType}. */
    MAP,
    /** Type is a {@link NamedType}. */
    NAMED
  }

  /** The kind of this type (and therefore implementing class). */
  Kind kind();

  enum ScalarType implements DataType {
    ANY,
    BOOL,
    BYTES,
    DOUBLE,
    FLOAT,
    FIXED32,
    FIXED64,
    INT32,
    INT64,
    SFIXED32,
    SFIXED64,
    SINT32,
    SINT64,
    STRING,
    UINT32,
    UINT64;

    @Override public Kind kind() {
      return Kind.SCALAR;
    }

    @Override public String toString() {
      return name().toLowerCase(Locale.US);
    }
  }

  final class MapType implements DataType {
    public static MapType create(DataType keyType, DataType valueType) {
      return new MapType(checkNotNull(keyType, "keyType"), checkNotNull(valueType, "valueType"));
    }

    private final DataType keyType;
    private final DataType valueType;

    private MapType(DataType keyType, DataType valueType) {
      this.keyType = keyType;
      this.valueType = valueType;
    }

    @Override public Kind kind() {
      return Kind.MAP;
    }

    public DataType keyType() {
      return keyType;
    }

    public DataType valueType() {
      return valueType;
    }

    @Override public String toString() {
      return "map<" + keyType + ", " + valueType + ">";
    }

    @Override public boolean equals(Object obj) {
      if (obj == this) return true;
      if (!(obj instanceof MapType)) return false;
      MapType other = (MapType) obj;
      return keyType.equals(other.keyType) && valueType.equals(other.valueType);
    }

    @Override public int hashCode() {
      return keyType.hashCode() * 37 + valueType.hashCode();
    }
  }

  final class NamedType implements DataType {
    public static NamedType create(String name) {
      return new NamedType(checkNotNull(name, "name"));
    }

    private final String name;

    private NamedType(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    @Override public Kind kind() {
      return Kind.NAMED;
    }

    @Override public String toString() {
      return name;
    }

    @Override public boolean equals(Object obj) {
      if (obj == this) return true;
      if (!(obj instanceof NamedType)) return false;
      NamedType other = (NamedType) obj;
      return name.equals(other.name);
    }

    @Override public int hashCode() {
      return name.hashCode();
    }
  }
}
