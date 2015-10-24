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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.squareup.wire.ProtoAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of .proto files that describe a set of messages. A schema is <i>linked</i>: each
 * field's type name is resolved to the corresponding type definition.
 *
 * <p>Use {@link SchemaLoader} to load a schema from source files.
 */
public final class Schema {
  private static final Ordering<ProtoFile> PATH_ORDER = new Ordering<ProtoFile>() {
    @Override public int compare(ProtoFile left, ProtoFile right) {
      return left.location().path().compareTo(right.location().path());
    }
  };

  private final ImmutableList<ProtoFile> protoFiles;
  private final ImmutableMap<String, Type> typesIndex;
  private final ImmutableMap<String, Service> servicesIndex;

  Schema(Iterable<ProtoFile> protoFiles) {
    this.protoFiles = PATH_ORDER.immutableSortedCopy(protoFiles);
    this.typesIndex = buildTypesIndex(protoFiles);
    this.servicesIndex = buildServicesIndex(protoFiles);
  }

  public ImmutableList<ProtoFile> protoFiles() {
    return protoFiles;
  }

  /** Returns the proto file at {@code path}, or null if this schema has no such file. */
  public ProtoFile protoFile(String path) {
    for (ProtoFile protoFile : protoFiles) {
      if (protoFile.location().path().equals(path)) {
        return protoFile;
      }
    }
    return null;
  }

  /**
   * Returns a copy of this schema that retains only the types and services selected by {@code
   * identifierSet}, plus their transitive dependencies.
   */
  public Schema prune(IdentifierSet identifierSet) {
    return new Pruner(this, identifierSet).prune();
  }

  /**
   * Returns the service with the fully qualified name {@code name}, or null if this schema defines
   * no such service.
   */
  public Service getService(String name) {
    return servicesIndex.get(name);
  }

  /**
   * Returns the service with the fully qualified name {@code name}, or null if this schema defines
   * no such service.
   */
  public Service getService(ProtoType protoType) {
    return getService(protoType.toString());
  }

  /**
   * Returns the type with the fully qualified name {@code name}, or null if this schema defines no
   * such type.
   */
  public Type getType(String name) {
    return typesIndex.get(name);
  }

  /**
   * Returns the type with the fully qualified name {@code name}, or null if this schema defines no
   * such type.
   */
  public Type getType(ProtoType protoType) {
    return getType(protoType.toString());
  }

  public Field getField(ProtoMember protoMember) {
    Type type = getType(protoMember.type());
    if (!(type instanceof MessageType)) return null;
    Field field = ((MessageType) type).field(protoMember.member());
    if (field == null) {
      field = ((MessageType) type).extensionField(protoMember.member());
    }
    return field;
  }

  private static ImmutableMap<String, Type> buildTypesIndex(Iterable<ProtoFile> protoFiles) {
    Map<String, Type> result = new LinkedHashMap<>();
    for (ProtoFile protoFile : protoFiles) {
      for (Type type : protoFile.types()) {
        index(result, type);
      }
    }
    return ImmutableMap.copyOf(result);
  }

  private static void index(Map<String, Type> typesByName, Type type) {
    typesByName.put(type.type().toString(), type);
    for (Type nested : type.nestedTypes()) {
      index(typesByName, nested);
    }
  }

  private static ImmutableMap<String, Service> buildServicesIndex(Iterable<ProtoFile> protoFiles) {
    ImmutableMap.Builder<String, Service> result = ImmutableMap.builder();
    for (ProtoFile protoFile : protoFiles) {
      for (Service service : protoFile.services()) {
        result.put(service.type().toString(), service);
      }
    }
    return result.build();
  }

  /**
   * Returns a wire adapter for the message or enum type named {@code typeName}. The returned type
   * adapter doesn't have model classes to encode and decode from, so instead it uses scalar types
   * ({@linkplain String}, {@linkplain okio.ByteString ByteString}, {@linkplain Integer}, etc.),
   * {@linkplain Map maps}, and {@linkplain java.util.List lists}. It can both encode and decode
   * these objects. Map keys are field names.
   *
   * @param includeUnknown true to include values for unknown tags in the returned model. Map keys
   *     for such values is the unknown value's tag name as a string. Unknown values are decoded to
   *     {@linkplain Long}, {@linkplain Long}, {@linkplain Integer}, or {@linkplain okio.ByteString
   *     ByteString} for {@linkplain com.squareup.wire.FieldEncoding#VARINT VARINT}, {@linkplain
   *     com.squareup.wire.FieldEncoding#FIXED64 FIXED64}, {@linkplain
   *     com.squareup.wire.FieldEncoding#FIXED32 FIXED32}, or {@linkplain
   *     com.squareup.wire.FieldEncoding#LENGTH_DELIMITED LENGTH_DELIMITED} respectively.
   */
  public ProtoAdapter<Object> protoAdapter(String typeName, boolean includeUnknown) {
    Type type = getType(typeName);
    if (type == null) throw new IllegalArgumentException("unexpected type " + typeName);
    return new SchemaProtoAdapterFactory(this, includeUnknown).get(type.type());
  }
}
