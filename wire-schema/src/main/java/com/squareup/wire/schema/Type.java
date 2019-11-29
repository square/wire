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
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import java.util.List;
import java.util.Set;

public abstract class Type {
  public abstract Location location();
  public abstract ProtoType type();
  public abstract String documentation();
  public abstract Options options();
  public abstract List<Type> nestedTypes();
  abstract void linkMembers(Linker linker);
  abstract void linkOptions(Linker linker);
  abstract void validate(Linker linker);
  abstract Type retainAll(Schema schema, MarkSet markSet);

  /**
   * Returns a copy of this containing only the types in {@code linkedTypes}, or null if that set
   * is empty. This will return an {@code EnclosingType} if it is itself not linked, but its
   * nested types are linked.
   *
   * The returned type is a shadow of its former self. It it useful for linking against, but lacks
   * most of the members of the original type.
   */
  abstract Type retainLinked(Set<ProtoType> linkedTypes);

  public static Type get(String packageName, ProtoType protoType, TypeElement type) {
    if (type instanceof EnumElement) {
      return EnumType.fromElement(protoType, (EnumElement) type);
    } else if (type instanceof MessageElement) {
      return MessageType.fromElement(packageName, protoType, (MessageElement) type);
    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }

  static ImmutableList<Type> fromElements(String packageName, List<TypeElement> elements) {
    ImmutableList.Builder<Type> types = new ImmutableList.Builder<>();
    for (TypeElement element : elements) {
      ProtoType protoType = ProtoType.get(packageName, element.getName());
      types.add(Type.get(packageName, protoType, element));
    }
    return types.build();
  }

  static TypeElement toElement(Type type) {
    if (type instanceof EnumType) {
      return ((EnumType) type).toElement();
    } else if (type instanceof MessageType) {
      return ((MessageType) type).toElement();
    } else if (type instanceof EnclosingType) {
      return ((EnclosingType) type).toElement();
    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }

  static ImmutableList<TypeElement> toElements(List<Type> types) {
    ImmutableList.Builder<TypeElement> elements = new ImmutableList.Builder<>();
    for (Type type : types) {
      elements.add(Type.toElement(type));
    }
    return elements.build();
  }
}
