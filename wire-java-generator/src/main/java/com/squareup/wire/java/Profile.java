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
package com.squareup.wire.java;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.wire.java.internal.ProfileFileElement;
import com.squareup.wire.java.internal.TypeConfigElement;
import com.squareup.wire.schema.ProtoType;

/**
 * Describes how to map {@code .proto} to {@code .java}. A single repository of {@code .proto} files
 * may have multiple profiles; for example a project may target both Android and Java.
 */
public final class Profile {
  private final ImmutableList<ProfileFileElement> profileFiles;

  Profile(ImmutableList<ProfileFileElement> profileFiles) {
    this.profileFiles = profileFiles;
  }

  public Profile() {
    this(ImmutableList.<ProfileFileElement>of());
  }

  public TypeName getTarget(ProtoType type) {
    TypeConfigElement typeConfig = typeConfig(type);
    return typeConfig != null ? ClassName.bestGuess(typeConfig.target()) : null;
  }

  public AdapterConstant getAdapter(ProtoType type) {
    TypeConfigElement typeConfig = typeConfig(type);
    return typeConfig != null ? new AdapterConstant(typeConfig.adapter()) : null;
  }

  /** Returns the config for {@code type}, or null if it is not configured. */
  private TypeConfigElement typeConfig(ProtoType type) {
    for (ProfileFileElement element : profileFiles) {
      for (TypeConfigElement typeConfig : element.typeConfigs()) {
        if (typeConfig.type().equals(type.toString())) return typeConfig;
      }
    }
    return null;
  }
}
