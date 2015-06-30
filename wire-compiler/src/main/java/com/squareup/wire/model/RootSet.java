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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A collection of messages, enums and services. */
public final class RootSet {
  private final List<WireProtoFile> protoFiles;
  private final Map<String, WireType> typesIndex;
  private final Map<String, WireService> servicesIndex;

  RootSet(Collection<WireProtoFile> protoFiles) {
    this.protoFiles = Util.immutableList(protoFiles);
    this.typesIndex = buildTypesIndex(protoFiles);
    this.servicesIndex = buildServicesIndex(protoFiles);
  }

  private static Map<String, WireType> buildTypesIndex(Collection<WireProtoFile> protoFiles) {
    Map<String, WireType> result = new LinkedHashMap<String, WireType>();
    for (WireProtoFile protoFile : protoFiles) {
      for (WireType type : protoFile.types()) {
        index(result, type);
      }
    }
    return Util.immutableMap(result);
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
    return Util.immutableMap(result);
  }

  /**
   * Returns a new root set that contains only the types in {@code roots} and their transitive
   * dependencies.
   */
  public RootSet retainRoots(Set<String> roots) {
    // Mark and enqueue the roots.
    MarkSet markSet = new MarkSet();
    for (String s : roots) {
      markSet.mark(s);
    }

    // Extensions and options are also roots.
    for (WireProtoFile protoFile : protoFiles) {
      for (WireExtend extend : protoFile.wireExtends()) {
        markSet.markExtend(extend);
      }
      markSet.markOptions(protoFile.options());
    }

    // Mark everything reachable by what's enqueued, queueing new things as we go.
    for (String name; (name = markSet.queue.poll()) != null;) {
      if (ProtoTypeName.getScalar(name) != null) {
        continue; // Skip scalar types.
      }

      WireType type = typesIndex.get(name);
      if (type != null) {
        markSet.markType(type);
        continue;
      }

      WireService service = servicesIndex.get(name);
      if (service != null) {
        markSet.markService(service);
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
            markSet.markOptions(partialService.options());
            markSet.markRpc(rpc);
            continue;
          }
        }
      }

      throw new IllegalArgumentException("Unexpected type: " + name);
    }

    List<WireProtoFile> retained = new ArrayList<WireProtoFile>();
    for (WireProtoFile protoFile : protoFiles) {
      retained.add(protoFile.retainAll(markSet.marks));
    }

    return new RootSet(retained);
  }

  /** Visits all types and services and mark them. */
  private static class MarkSet {
    /** Homogeneous identifiers including type names, service names, and RPC names. */
    final Set<String> marks = new LinkedHashSet<String>();

    /** Identifiers whose immediate dependencies have not yet been marked. */
    final Deque<String> queue = new ArrayDeque<String>();

    public void mark(ProtoTypeName typeName) {
      mark(typeName.toString());
    }

    private void mark(String identifier) {
      if (marks.add(identifier)) {
        queue.add(identifier); // The transitive dependencies of this identifier must be visited.
      }
    }

    void markExtend(WireExtend extend) {
      mark(extend.protoTypeName());
      markFields(extend.fields());
    }

    void markType(WireType type) {
      markOptions(type.options());
      for (WireType nestedType : type.nestedTypes()) {
        mark(nestedType.protoTypeName());
      }
      if (type instanceof WireMessage) {
        markMessage((WireMessage) type);
      } else if (type instanceof WireEnum) {
        markEnum((WireEnum) type);
      }
    }

    void markMessage(WireMessage message) {
      markFields(message.fields());
      for (WireOneOf oneOf : message.oneOfs()) {
        markFields(oneOf.fields());
      }
    }

    void markEnum(WireEnum wireEnum) {
      markOptions(wireEnum.options());
      for (WireEnumConstant constant : wireEnum.constants()) {
        markOptions(constant.options());
      }
    }

    void markFields(List<WireField> fields) {
      for (WireField field : fields) {
        markField(field);
      }
    }

    void markField(WireField field) {
      markOptions(field.options());
      mark(field.type());
    }

    void markOptions(List<WireOption> options) {
      for (WireOption option : options) {
        if (option.fieldPath() != null) {
          markFields(option.fieldPath());
        }
      }
    }

    void markService(WireService service) {
      markOptions(service.options());
      for (WireRpc rpc : service.rpcs()) {
        markRpc(rpc);
      }
    }

    void markRpc(WireRpc rpc) {
      markOptions(rpc.options());
      mark(rpc.requestType());
      mark(rpc.responseType());
    }
  }
}
