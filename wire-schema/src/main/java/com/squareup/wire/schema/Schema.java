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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of .proto files that describe a set of messages. A schema is <i>linked</i>: each
 * field's type name is resolved to the corresponding type definition.
 *
 * <p>Use {@link Loader} to load a schema from source files.
 */
public final class Schema {
  private final ImmutableList<ProtoFile> protoFiles;
  private final ImmutableMap<String, Type> typesIndex;
  private final ImmutableMap<String, Service> servicesIndex;

  Schema(Iterable<ProtoFile> protoFiles) {
    this.protoFiles = ImmutableList.copyOf(protoFiles);
    this.typesIndex = buildTypesIndex(protoFiles);
    this.servicesIndex = buildServicesIndex(protoFiles);
  }

  public ImmutableList<ProtoFile> protoFiles() {
    return protoFiles;
  }

  /**
   * Returns a copy of this schema that retains only the types and services in {@code roots}, plus
   * their transitive dependencies. Names in {@code roots} should be of the following forms:
   *
   * <ul>
   *   <li>Fully qualified message names, like {@code squareup.protos.person.Person}.
   *   <li>Fully qualified service names, like {@code com.squareup.services.ExampleService}.
   *   <li>Fully qualified service names, followed by a '#', followed by an RPC name, like
   *       {@code com.squareup.services.ExampleService#SendSomeData}.
   * </ul>
   */
  public Schema retainRoots(Collection<String> roots) {
    return new Pruner().retainRoots(this, roots);
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
  public Service getService(Type.Name name) {
    return getService(name.toString());
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
  public Type getType(Type.Name name) {
    return getType(name.toString());
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
    typesByName.put(type.name().toString(), type);
    for (Type nested : type.nestedTypes()) {
      index(typesByName, nested);
    }
  }

  private static ImmutableMap<String, Service> buildServicesIndex(Iterable<ProtoFile> protoFiles) {
    ImmutableMap.Builder<String, Service> result = ImmutableMap.builder();
    for (ProtoFile protoFile : protoFiles) {
      for (Service service : protoFile.services()) {
        result.put(service.name().toString(), service);
      }
    }
    return result.build();
  }
}
