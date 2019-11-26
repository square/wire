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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Creates a new schema that contains only the types selected by an identifier set, including their
 * transitive dependencies.
 */
final class Pruner {
  final Schema schema;
  final IdentifierSet identifierSet;
  final MarkSet marks;

  /**
   * {@link ProtoType types} and {@link ProtoMember members} whose immediate dependencies have not
   * yet been visited.
   */
  final Deque<Object> queue;

  Pruner(Schema schema, IdentifierSet identifierSet) {
    this.schema = schema;
    this.identifierSet = identifierSet;
    this.marks = new MarkSet(identifierSet);
    this.queue = new ArrayDeque<>();
  }

  public Schema prune() {
    markRoots();
    markReachable();

    ImmutableList<ProtoFile> retained = retainImports(retainAll(schema, marks));

    return new Schema(retained);
  }

  private ImmutableList<ProtoFile> retainAll(Schema schema, MarkSet marks) {
    ImmutableList.Builder<ProtoFile> retained = ImmutableList.builder();
    for (ProtoFile protoFile : schema.protoFiles()) {
      retained.add(protoFile.retainAll(schema, marks));
    }
    return retained.build();
  }

  private ImmutableList<ProtoFile> retainImports(ImmutableList<ProtoFile> protoFiles) {
    ImmutableList.Builder<ProtoFile> retained = ImmutableList.builder();
    for (ProtoFile protoFile : protoFiles) {
      retained.add(protoFile.retainImports(protoFiles));
    }
    return retained.build();
  }

  private void markRoots() {
    for (ProtoFile protoFile : schema.protoFiles()) {
      markRoots(protoFile);
    }
  }

  private void markRoots(ProtoFile protoFile) {
    for (Type type : protoFile.types()) {
      markRootsIncludingNested(type);
    }
    for (Service service : protoFile.services()) {
      markRoots(service.type());
    }
  }

  private void markRootsIncludingNested(Type type) {
    markRoots(type.type());

    for (Type nested : type.nestedTypes()) {
      markRootsIncludingNested(nested);
    }
  }

  private void markRoots(ProtoType protoType) {
    if (identifierSet.includes(protoType)) {
      marks.root(protoType);
      queue.add(protoType);
      return;
    }

    // The top-level type isn't a root, search for root members inside.
    for (Object reachable : reachableObjects(protoType)) {
      if (reachable instanceof ProtoMember) {
        ProtoMember member = (ProtoMember) reachable;
        if (identifierSet.includes(member)) {
          marks.root(member);
          marks.mark(member.type()); // Consider this type as visited.
          queue.add(member);
        }
      }
    }
  }

  /**
   * Mark everything transitively reachable from the queue, adding to the queue whenever a reachable
   * object brings along more reachable objects.
   */
  private void markReachable() {
    for (Object root; (root = queue.poll()) != null;) {
      List<Object> reachableMembers = reachableObjects(root);

      for (Object reachable : reachableMembers) {
        if (reachable instanceof ProtoType) {
          if (marks.mark((ProtoType) reachable)) {
            queue.add(reachable);
          }
        } else if (reachable instanceof ProtoMember) {
          if (marks.mark((ProtoMember) reachable)) {
            queue.add(reachable);
          }
        } else if (reachable == null) {
          // Skip nulls.
          // TODO(jwilson): create a dedicated UNLINKED type as a placeholder.
        } else {
          throw new IllegalStateException("unexpected object: " + reachable);
        }
      }
    }
  }

  /**
   * Returns everything reachable from {@code root} when traversing the graph. The returned list
   * contains instances of type {@link ProtoMember} and {@link ProtoType}.
   *
   * @param root either a {@link ProtoMember} or {@link ProtoType}.
   */
  private List<Object> reachableObjects(Object root) {
    List<Object> result = new ArrayList<>();
    Options options;

    if (root instanceof ProtoMember) {
      ProtoMember protoMember = (ProtoMember) root;
      String member = ((ProtoMember) root).member();
      Type type = schema.getType(protoMember.type());
      Service service = schema.getService(protoMember.type());

      if (type instanceof MessageType) {
        Field field = ((MessageType) type).field(member);
        if (field == null) {
          field = ((MessageType) type).extensionField(member);
        }
        if (field == null) {
          throw new IllegalStateException("unexpected member: " + member);
        }
        result.add(field.type());
        options = field.options();
      } else if (type instanceof EnumType) {
        EnumConstant constant = ((EnumType) type).constant(member);
        if (constant == null) {
          throw new IllegalStateException("unexpected member: " + member);
        }
        options = constant.getOptions();
      } else if (service != null) {
        Rpc rpc = service.rpc(member);
        if (rpc == null) {
          throw new IllegalStateException("unexpected rpc: " + member);
        }
        result.add(rpc.requestType());
        result.add(rpc.responseType());
        options = rpc.options();
      } else {
        throw new IllegalStateException("unexpected member: " + member);
      }
    } else if (root instanceof ProtoType) {
      ProtoType protoType = (ProtoType) root;

      if (protoType.isMap()) {
        result.add(protoType.keyType());
        result.add(protoType.valueType());
        return result;
      }

      if (protoType.isScalar()) {
        return result; // Skip scalar types.
      }

      Type type = schema.getType(protoType);
      Service service = schema.getService(protoType);

      if (type instanceof MessageType) {
        options = type.options();
        MessageType messageType = (MessageType) type;
        for (Field field : messageType.declaredFields()) {
          result.add(ProtoMember.get(protoType, field.name()));
        }
        for (Field field : messageType.extensionFields()) {
          result.add(ProtoMember.get(protoType, field.qualifiedName()));
        }
        for (OneOf oneOf : messageType.oneOfs()) {
          for (Field field : oneOf.fields()) {
            result.add(ProtoMember.get(protoType, field.name()));
          }
        }
      } else if (type instanceof EnumType) {
        options = type.options();
        EnumType wireEnum = (EnumType) type;
        for (EnumConstant constant : wireEnum.constants()) {
          result.add(ProtoMember.get(wireEnum.type(), constant.getName()));
        }
      } else if (service != null) {
        options = service.options();
        for (Rpc rpc : service.rpcs()) {
          result.add(ProtoMember.get(service.type(), rpc.name()));
        }
      } else {
        throw new IllegalStateException("unexpected type: " + protoType);
      }
    } else {
      throw new IllegalStateException("unexpected root: " + root);
    }

    for (ProtoMember member : options.fields(identifierSet).values()) {
      // If it's an extension, don't consider the entire enclosing type to be reachable.
      if (isExtensionField(member)) {
        result.add(member.type());
      }
      result.add(member);
    }
    return result;
  }

  private boolean isExtensionField(ProtoMember protoMember) {
    Type type = schema.getType(protoMember.type());
    return type instanceof MessageType && ((MessageType) type).field(protoMember.member()) != null;
  }
}
