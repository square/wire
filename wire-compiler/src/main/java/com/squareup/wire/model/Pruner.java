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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
  public List<WireProtoFile> retainRoots(List<WireProtoFile> protoFiles, Set<String> roots) {
    if (roots.isEmpty()) throw new IllegalArgumentException();
    if (!marks.isEmpty()) throw new IllegalStateException();

    Map<String, WireType> typesIndex = buildTypesIndex(protoFiles);
    Map<String, WireService> servicesIndex = buildServicesIndex(protoFiles);

    // Mark and enqueue the roots.
    for (String s : roots) {
      mark(s);
    }

    // Extensions and options are also roots.
    for (WireProtoFile protoFile : protoFiles) {
      for (WireExtend extend : protoFile.wireExtends()) {
        markExtend(extend);
      }
      markOptions(protoFile.options());
    }

    // Mark everything reachable by what's enqueued, queueing new things as we go.
    for (String name; (name = queue.poll()) != null;) {
      if (ProtoTypeName.getScalar(name) != null) {
        continue; // Skip scalar types.
      }

      WireType type = typesIndex.get(name);
      if (type != null) {
        markType(type);
        continue;
      }

      WireService service = servicesIndex.get(name);
      if (service != null) {
        markService(service);
        continue;
      }

      // If the root set contains a method name like 'Service#Method', only that RPC is marked.
      int hash = name.indexOf('#');
      if (hash != -1) {
        String serviceName = name.substring(0, hash);
        String rpcName = name.substring(hash + 1);
        WireService partialService = servicesIndex.get(serviceName);
        if (partialService != null) {
          WireRpc rpc = partialService.rpc(rpcName);
          if (rpc != null) {
            markOptions(partialService.options());
            markRpc(rpc);
            continue;
          }
        }
      }

      throw new IllegalArgumentException("Unexpected type: " + name);
    }

    List<WireProtoFile> retained = new ArrayList<WireProtoFile>();
    for (WireProtoFile protoFile : protoFiles) {
      retained.add(protoFile.retainAll(marks));
    }

    return ImmutableList.copyOf(retained);
  }

  private static Map<String, WireType> buildTypesIndex(Collection<WireProtoFile> protoFiles) {
    Map<String, WireType> result = new LinkedHashMap<String, WireType>();
    for (WireProtoFile protoFile : protoFiles) {
      for (WireType type : protoFile.types()) {
        index(result, type);
      }
    }
    return ImmutableMap.copyOf(result);
  }

  private static void index(Map<String, WireType> typesByName, WireType type) {
    typesByName.put(type.protoTypeName().toString(), type);
    for (WireType nested : type.nestedTypes()) {
      index(typesByName, nested);
    }
  }

  private static Map<String, WireService> buildServicesIndex(Collection<WireProtoFile> protoFiles) {
    Map<String, WireService> result = new LinkedHashMap<String, WireService>();
    for (WireProtoFile protoFile : protoFiles) {
      for (WireService service : protoFile.services()) {
        result.put(service.protoTypeName().toString(), service);
      }
    }
    return ImmutableMap.copyOf(result);
  }

  private void mark(ProtoTypeName typeName) {
    mark(typeName.toString());
  }

  private void mark(String identifier) {
    if (marks.add(identifier)) {
      queue.add(identifier); // The transitive dependencies of this identifier must be visited.
    }
  }

  private void markExtend(WireExtend extend) {
    mark(extend.protoTypeName());
    markFields(extend.fields());
  }

  private void markType(WireType type) {
    markOptions(type.options());

    ProtoTypeName enclosingTypeName = type.protoTypeName().enclosingTypeName();
    if (enclosingTypeName != null) {
      mark(enclosingTypeName);
    }

    for (WireType nestedType : type.nestedTypes()) {
      mark(nestedType.protoTypeName());
    }

    if (type instanceof WireMessage) {
      markMessage((WireMessage) type);
    } else if (type instanceof WireEnum) {
      markEnum((WireEnum) type);
    }
  }

  private void markMessage(WireMessage message) {
    markFields(message.fields());
    for (WireOneOf oneOf : message.oneOfs()) {
      markFields(oneOf.fields());
    }
  }

  private void markEnum(WireEnum wireEnum) {
    markOptions(wireEnum.options());
    for (WireEnumConstant constant : wireEnum.constants()) {
      markOptions(constant.options());
    }
  }

  private void markFields(List<WireField> fields) {
    for (WireField field : fields) {
      markField(field);
    }
  }

  private void markField(WireField field) {
    markOptions(field.options());
    mark(field.type());
  }

  private void markOptions(Options options) {
    for (WireField field : options.fields()) {
      markField(field);
    }
  }

  private void markService(WireService service) {
    markOptions(service.options());
    for (WireRpc rpc : service.rpcs()) {
      markRpc(rpc);
    }
  }

  private void markRpc(WireRpc rpc) {
    markOptions(rpc.options());
    mark(rpc.requestType());
    mark(rpc.responseType());
  }
}
