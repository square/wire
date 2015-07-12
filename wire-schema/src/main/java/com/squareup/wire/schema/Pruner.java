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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Removes unused types and services. */
public final class Pruner {
  /** Homogeneous identifiers including type names, service names, and RPC names. */
  final Set<String> marks = new LinkedHashSet<String>();

  /** Identifiers whose immediate dependencies have not yet been marked. */
  final Deque<String> queue = new ArrayDeque<String>();

  /**
   * Returns a new root set that contains only the types in {@code roots} and their transitive
   * dependencies.
   *
   * @param roots a set of identifiers to retain, which may be fully qualified type names, fully
   *     qualified service names, or service RPCs like {@code package.ServiceName#MethodName}.
   */
  public ImmutableList<WireProtoFile> retainRoots(
      List<WireProtoFile> protoFiles, Collection<String> roots) {
    if (roots.isEmpty()) throw new IllegalArgumentException();
    if (!marks.isEmpty()) throw new IllegalStateException();

    Map<String, Type> typesIndex = buildTypesIndex(protoFiles);
    Map<String, Service> servicesIndex = buildServicesIndex(protoFiles);

    // Mark and enqueue the roots.
    for (String s : roots) {
      mark(s);
    }

    // Extensions and options are also roots.
    for (WireProtoFile protoFile : protoFiles) {
      for (Extend extend : protoFile.extendList()) {
        markExtend(extend);
      }
      markOptions(protoFile.options());
    }

    // Mark everything reachable by what's enqueued, queueing new things as we go.
    for (String name; (name = queue.poll()) != null;) {
      if (Type.Name.getScalar(name) != null) {
        continue; // Skip scalar types.
      }

      Type type = typesIndex.get(name);
      if (type != null) {
        markType(type);
        continue;
      }

      Service service = servicesIndex.get(name);
      if (service != null) {
        markService(service);
        continue;
      }

      // If the root set contains a method name like 'Service#Method', only that RPC is marked.
      int hash = name.indexOf('#');
      if (hash != -1) {
        String serviceName = name.substring(0, hash);
        String rpcName = name.substring(hash + 1);
        Service partialService = servicesIndex.get(serviceName);
        if (partialService != null) {
          Rpc rpc = partialService.rpc(rpcName);
          if (rpc != null) {
            markOptions(partialService.options());
            markRpc(rpc);
            continue;
          }
        }
      }

      throw new IllegalArgumentException("Unexpected type: " + name);
    }

    ImmutableList.Builder<WireProtoFile> retained = ImmutableList.builder();
    for (WireProtoFile protoFile : protoFiles) {
      retained.add(protoFile.retainAll(marks));
    }

    return retained.build();
  }

  private static Map<String, Type> buildTypesIndex(Collection<WireProtoFile> protoFiles) {
    Map<String, Type> result = new LinkedHashMap<String, Type>();
    for (WireProtoFile protoFile : protoFiles) {
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

  private static ImmutableMap<String, Service> buildServicesIndex(
      Collection<WireProtoFile> protoFiles) {
    ImmutableMap.Builder<String, Service> result = ImmutableMap.builder();
    for (WireProtoFile protoFile : protoFiles) {
      for (Service service : protoFile.services()) {
        result.put(service.name().toString(), service);
      }
    }
    return result.build();
  }

  private void mark(Type.Name typeName) {
    mark(typeName.toString());
  }

  private void mark(String identifier) {
    if (marks.add(identifier)) {
      queue.add(identifier); // The transitive dependencies of this identifier must be visited.
    }
  }

  private void markExtend(Extend extend) {
    mark(extend.type());
    markFields(extend.fields());
  }

  private void markType(Type type) {
    markOptions(type.options());

    Type.Name enclosingTypeName = type.name().enclosingTypeName();
    if (enclosingTypeName != null) {
      mark(enclosingTypeName);
    }

    for (Type nestedType : type.nestedTypes()) {
      mark(nestedType.name());
    }

    if (type instanceof MessageType) {
      markMessage((MessageType) type);
    } else if (type instanceof EnumType) {
      markEnum((EnumType) type);
    }
  }

  private void markMessage(MessageType message) {
    markFields(message.fields());
    for (OneOf oneOf : message.oneOfs()) {
      markFields(oneOf.fields());
    }
  }

  private void markEnum(EnumType wireEnum) {
    markOptions(wireEnum.options());
    for (EnumConstant constant : wireEnum.constants()) {
      markOptions(constant.options());
    }
  }

  private void markFields(ImmutableList<Field> fields) {
    for (Field field : fields) {
      markField(field);
    }
  }

  private void markField(Field field) {
    markOptions(field.options());
    mark(field.type());
  }

  private void markOptions(Options options) {
    for (Field field : options.fields()) {
      markField(field);
    }
  }

  private void markService(Service service) {
    markOptions(service.options());
    for (Rpc rpc : service.rpcs()) {
      markRpc(rpc);
    }
  }

  private void markRpc(Rpc rpc) {
    markOptions(rpc.options());
    mark(rpc.requestType());
    mark(rpc.responseType());
  }
}
