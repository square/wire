/*
 * Copyright (C) 2016 Square, Inc.
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

/** An empty type which only holds nested types. */
public final class EnclosingType extends Type {
  private final Location location;
  private final ProtoType type;
  private final String documentation;
  private final ImmutableList<Type> nestedTypes;

  EnclosingType(Location location, ProtoType type, String documentation,
      ImmutableList<Type> nestedTypes) {
    this.location = location;
    this.type = type;
    this.documentation = documentation;
    this.nestedTypes = nestedTypes;
  }

  @Override public Location location() {
    return location;
  }

  @Override public ProtoType type() {
    return type;
  }

  @Override public String documentation() {
    return documentation;
  }

  @Override public Options options() {
    throw new UnsupportedOperationException();
  }

  @Override public ImmutableList<Type> nestedTypes() {
    return nestedTypes;
  }

  @Override void link(Linker linker) {
    for (Type nestedType : nestedTypes) {
      nestedType.link(linker);
    }
  }

  @Override void linkOptions(Linker linker) {
    for (Type nestedType : nestedTypes) {
      nestedType.linkOptions(linker);
    }
  }

  @Override void validate(Linker linker) {
    for (Type nestedType : nestedTypes) {
      nestedType.validate(linker);
    }
  }

  @Override Type retainAll(Schema schema, MarkSet markSet) {
    ImmutableList.Builder<Type> retainedNestedTypesBuilder = ImmutableList.builder();
    for (Type nestedType : nestedTypes) {
      Type retainedNestedType = nestedType.retainAll(schema, markSet);
      if (retainedNestedType != null) {
        retainedNestedTypesBuilder.add(retainedNestedType);
      }
    }

    ImmutableList<Type> retainedNestedTypes = retainedNestedTypesBuilder.build();
    if (retainedNestedTypes.isEmpty()) {
      return null;
    }
    return new EnclosingType(location, type, documentation, retainedNestedTypes);
  }
}
