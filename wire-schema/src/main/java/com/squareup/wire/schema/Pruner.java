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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Creates a new schema that contains only the types selected by an identifier set, including their
 * transitive dependencies.
 */
final class Pruner {
  final Schema schema;
  final IdentifierSet identifierSet;
  final MarkSet marks;

  /** Identifiers whose immediate dependencies have not yet been visited. */
  final Deque<String> queue;

  public Pruner(Schema schema, IdentifierSet identifierSet) {
    this.schema = schema;
    this.identifierSet = identifierSet;
    this.marks = new MarkSet(identifierSet);
    this.queue = new ArrayDeque<>(identifierSet.includes);
  }

  public Schema prune() {
    if (!identifierSet.includes.isEmpty()) {
      mark();
    }

    ImmutableList.Builder<ProtoFile> retained = ImmutableList.builder();
    for (ProtoFile protoFile : schema.protoFiles()) {
      retained.add(protoFile.retainAll(marks));
    }

    return new Schema(retained.build());
  }

  private void mark() {
    // File options are also roots.
    for (ProtoFile protoFile : schema.protoFiles()) {
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
          if (field == null) {
            field = ((MessageType) type).extensionField(member);
          }
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
        if (ProtoType.get(root).isScalar()) {
          continue; // Skip scalar types.
        }

        Type type = schema.getType(root);
        if (type != null) {
          markType(type);
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
  }

  private void mark(ProtoType type) {
    if (marks.mark(type)) {
      queue.add(type.toString()); // The transitive dependencies of this type must be visited.
    }
  }

  private void mark(ProtoType type, String member) {
    mark(type);
    if (marks.mark(type, member)) {
      queue.add(type + "#" + member); // The transitive dependencies of this member must be visited.
    }
  }

  private void markType(Type type) {
    markOptions(type.options());

    String enclosingTypeOrPackage = type.name().enclosingTypeOrPackage();
    Type enclosingType = schema.getType(enclosingTypeOrPackage);
    if (enclosingType != null) {
      mark(enclosingType.name());
    }

    if (marks.containsAllMembers(type.name())) {
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
    if (marks.containsAllMembers(wireEnum.name())) {
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
    for (Map.Entry<ProtoType, Field> entry : options.fields().entries()) {
      mark(entry.getKey(), entry.getValue().name());
    }
  }

  private void markService(Service service) {
    markOptions(service.options());
    if (marks.containsAllMembers(service.type())) {
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
