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
      return left.getLocation().getPath().compareTo(right.getLocation().getPath());
    }
  };

  private final ImmutableList<ProtoFile> protoFiles;
  private final ImmutableMap<String, Type> typesIndex;
  private final ImmutableMap<String, Service> servicesIndex;
  private final Map<ProtoType, ProtoFile> protoFilesIndex;

  Schema(Iterable<ProtoFile> protoFiles) {
    this.protoFiles = PATH_ORDER.immutableSortedCopy(protoFiles);
    this.protoFilesIndex = new LinkedHashMap<>();
    this.typesIndex = buildTypesIndex(protoFiles, this.protoFilesIndex);
    this.servicesIndex = buildServicesIndex(protoFiles, this.protoFilesIndex);
  }

  public ImmutableList<ProtoFile> getProtoFiles() {
    return protoFiles;
  }

  /** Returns the proto file at {@code path}, or null if this schema has no such file. */
  public ProtoFile protoFile(String path) {
    for (ProtoFile protoFile : protoFiles) {
      if (protoFile.getLocation().getPath().equals(path)) {
        return protoFile;
      }
    }
    return null;
  }

  /** Returns the proto file containing this {@code protoType}, or null if there isn't such file. */
  public ProtoFile protoFile(ProtoType protoType) {
    return protoFilesIndex.get(protoType);
  }

  /**
   * Returns a copy of this schema that retains only the types and services selected by {@code
   * pruningRules}, plus their transitive dependencies.
   */
  public Schema prune(PruningRules pruningRules) {
    return new Pruner(this, pruningRules).prune();
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
    Type type = getType(protoMember.getType());
    if (!(type instanceof MessageType)) return null;
    Field field = ((MessageType) type).field(protoMember.getMember());
    if (field == null) {
      field = ((MessageType) type).extensionField(protoMember.getMember());
    }
    return field;
  }

  public Field getField(String typeName, String memberName) {
    return getField(ProtoType.get(typeName), memberName);
  }

  public Field getField(ProtoType protoType, String memberName) {
    return getField(ProtoMember.get(protoType, memberName));
  }

  public static Schema fromFiles(Iterable<ProtoFile> sourceFiles) {
    return new Linker(CoreLoader.INSTANCE).link(sourceFiles);
  }

  static Schema fromFiles(Iterable<ProtoFile> sourceProtoFiles, Loader pathFilesLoader) {
    return new Linker(pathFilesLoader).link(sourceProtoFiles);
  }

  private static ImmutableMap<String, Type> buildTypesIndex(Iterable<ProtoFile> protoFiles,
      Map<ProtoType, ProtoFile> protoFilesIndex) {
    Map<String, Type> result = new LinkedHashMap<>();
    for (ProtoFile protoFile : protoFiles) {
      for (Type type : protoFile.getTypes()) {
        index(result, type, protoFile, protoFilesIndex);
      }
    }
    return ImmutableMap.copyOf(result);
  }

  private static void index(Map<String, Type> typesByName, Type type, ProtoFile protoFile,
      Map<ProtoType, ProtoFile> protoFilesIndex) {
    ProtoType protoType = type.getType();
    if (!protoFilesIndex.containsKey(protoType)) {
      protoFilesIndex.put(protoType, protoFile);
    }

    typesByName.put(type.getType().toString(), type);
    for (Type nested : type.getNestedTypes()) {
      index(typesByName, nested, protoFile, protoFilesIndex);
    }
  }

  private static ImmutableMap<String, Service> buildServicesIndex(Iterable<ProtoFile> protoFiles,
      Map<ProtoType, ProtoFile> protoFilesIndex) {
    ImmutableMap.Builder<String, Service> result = ImmutableMap.builder();
    for (ProtoFile protoFile : protoFiles) {
      for (Service service : protoFile.getServices()) {
        result.put(service.type().toString(), service);
        protoFilesIndex.put(service.type(), protoFile);
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
    return new SchemaProtoAdapterFactory(this, includeUnknown).get(type.getType());
  }

  boolean isExtensionField(ProtoMember protoMember) {
    Type type = getType(protoMember.getType());
    return type instanceof MessageType
        && ((MessageType) type).extensionField(protoMember.getMember()) != null;
  }
}
