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
import com.squareup.wire.WireType;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.NavigableSet;
import java.util.TreeSet;

/** Removes unused types and services. */
final class Pruner {
  /**
   * Homogeneous identifiers including type names, service names, field names, enum constants, and
   * RPC names. This uses a navigable set so that we can easily check for the presence of member
   * fields.
   *
   * <p>Member names are omitted unless a specific subset is being retained. That is, if a type name
   * {@code Foo} is present, all of Foo's members are retained unless some member name {@code
   * Foo#Bar} is in the set.
   */
  final NavigableSet<String> marks = new TreeSet<>();

  /** Identifiers whose immediate dependencies have not yet been marked. */
  final Deque<String> queue = new ArrayDeque<>();

  /**
   * Returns a new root set that contains only the types in {@code roots} and their transitive
   * dependencies.
   *
   * @param roots a set of identifiers to retain, which may be fully qualified type names, fully
   *     qualified service names, or service RPCs like {@code package.ServiceName#MethodName}.
   */
  public Schema retainRoots(Schema schema, Collection<String> roots) {
    if (roots.isEmpty()) throw new IllegalArgumentException();
    if (!marks.isEmpty()) throw new IllegalStateException();

    // Mark and enqueue the roots.
    for (String s : roots) {
      mark(s);
    }

    // Extensions and options are also roots.
    for (ProtoFile protoFile : schema.protoFiles()) {
      for (Extend extend : protoFile.extendList()) {
        markExtend(extend);
      }
      markOptions(protoFile.options());
    }

    // Mark everything reachable by what's enqueued, queueing new things as we go.
    for (String root; (root = queue.poll()) != null;) {
      int hash = root.indexOf('#');
      if (hash != -1) {
        // If the root set contains a field like 'Message#field' or an RPC like 'Service#Method',
        // only that member is marked.
        String typeOrServiceName = root.substring(0, hash);
        String member = root.substring(hash + 1);

        Type type = schema.getType(typeOrServiceName);
        if (type instanceof MessageType) {
          Field field = ((MessageType) type).field(member);
          if (field != null) {
            markOptions(type.options());
            markField(field);
            mark(type.name());
            continue;
          }
        } else if (type instanceof EnumType) {
          EnumConstant constant = ((EnumType) type).constant(member);
          if (constant != null) {
            markOptions(type.options());
            markOptions(constant.options());
            mark(type.name());
            continue;
          }
        }

        Service service = schema.getService(typeOrServiceName);
        if (service != null) {
          Rpc rpc = service.rpc(member);
          if (rpc != null) {
            markOptions(service.options());
            markRpc(rpc);
            mark(service.type());
            continue;
          }
        }

        throw new IllegalArgumentException("Unexpected member: " + root);

      } else {
        if (WireType.get(root).isScalar()) {
          continue; // Skip scalar types.
        }

        Type type = schema.getType(root);
        if (type != null) {
          markType(schema, type);
          continue;
        }

        Service service = schema.getService(root);
        if (service != null) {
          markService(service);
          continue;
        }

        throw new IllegalArgumentException("Unexpected type: " + root);
      }
    }

    ImmutableList.Builder<ProtoFile> retained = ImmutableList.builder();
    for (ProtoFile protoFile : schema.protoFiles()) {
      retained.add(protoFile.retainAll(marks));
    }

    return new Schema(retained.build());
  }

  /** Returns true if any RPC, enum constant or field of {@code identifier} is in marks. */
  static boolean hasMarkedMember(NavigableSet<String> marks, WireType typeName) {
    // If there's a member field, it will sort immediately after <Name># in the marks set.
    String prefix = typeName + "#";
    String ceiling = marks.ceiling(prefix);
    return ceiling != null && ceiling.startsWith(prefix);
  }

  private void mark(WireType wireType) {
    mark(wireType.toString());
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

  private void markType(Schema schema, Type type) {
    markOptions(type.options());

    String enclosingTypeOrPackage = type.name().enclosingTypeOrPackage();
    Type enclosingType = schema.getType(enclosingTypeOrPackage);
    if (enclosingType != null) {
      mark(enclosingTypeOrPackage);
    }

    if (!hasMarkedMember(marks, type.name())) {
      for (Type nestedType : type.nestedTypes()) {
        mark(nestedType.name());
      }

      if (type instanceof MessageType) {
        markMessage((MessageType) type);
      } else if (type instanceof EnumType) {
        markEnum((EnumType) type);
      }
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
    if (!hasMarkedMember(marks, wireEnum.name())) {
      for (EnumConstant constant : wireEnum.constants()) {
        markOptions(constant.options());
      }
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
    if (!hasMarkedMember(marks, service.type())) {
      for (Rpc rpc : service.rpcs()) {
        markRpc(rpc);
      }
    }
  }

  private void markRpc(Rpc rpc) {
    markOptions(rpc.options());
    mark(rpc.requestType());
    mark(rpc.responseType());
  }
}
