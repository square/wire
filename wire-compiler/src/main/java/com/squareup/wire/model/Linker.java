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

import com.squareup.protoparser.DataType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.IO;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Linker {
  private final String repoPath;
  private final IO io;
  private final Set<String> loadedDependencies;
  private final Map<String, WireType> protoTypeNames;
  private final Map<ProtoFile, WireProtoFile> protoFilesMap;

  // Context when linking.
  private final String enclosingProtoPackage;
  private final List<WireType> enclosingTypes;

  public Linker(String repoPath, IO io) {
    this.repoPath = repoPath;
    this.io = io;
    this.loadedDependencies = new LinkedHashSet<String>();
    this.protoTypeNames = new LinkedHashMap<String, WireType>();
    this.protoFilesMap = new LinkedHashMap<ProtoFile, WireProtoFile>();
    this.enclosingProtoPackage = null;
    this.enclosingTypes = Collections.emptyList();
  }

  private Linker(Linker enclosing, String protoPackage, WireType type) {
    this.repoPath = enclosing.repoPath;
    this.io = enclosing.io;
    this.loadedDependencies = enclosing.loadedDependencies;
    this.protoTypeNames = enclosing.protoTypeNames;
    this.protoFilesMap = enclosing.protoFilesMap;
    this.enclosingProtoPackage = protoPackage;
    this.enclosingTypes = type != null
        ? Util.concatenate(enclosing.enclosingTypes, type)
        : enclosing.enclosingTypes;
  }

  /** Recursively add {@code protoFile} and its dependencies. */
  public void add(ProtoFile protoFile) throws IOException {
    WireProtoFile wireProtoFile = new WireProtoFile(protoFile);
    protoFilesMap.put(protoFile, wireProtoFile);

    // Register the enclosed types.
    for (WireType type : wireProtoFile.types()) {
      register(type);
    }

    // Recursively load dependencies.
    for (String dependency : protoFile.dependencies()) {
      if (!loadedDependencies.contains(dependency)) {
        add(io.parse(repoPath + File.separator + dependency));
        loadedDependencies.add(dependency);
      }
    }
  }

  private void register(WireType type) {
    protoTypeNames.put(type.protoTypeName().toString(), type);
    for (WireType nestedType : type.nestedTypes()) {
      register(nestedType);
    }
  }

  public void link() {
    for (WireProtoFile wireProtoFile : protoFilesMap.values()) {
      wireProtoFile.link(this);
    }
  }

  /** Returns the proto type for {@code type} according to this linker. */
  ProtoTypeName protoTypeName(DataType type) {
    switch (type.kind()) {
      case SCALAR:
        return ProtoTypeName.getScalar(type.toString());

      case NAMED:
        return wireType(type.toString()).protoTypeName();

      default:
        // TODO(jwilson): report an error and return a sentinel instead of crashing here.
        throw new UnsupportedOperationException("unexpected type: " + type);
    }
  }

  /** Returns the wire type for the relative or fully-qualified name {@code name}. */
  WireType wireType(String name) {
    WireType fullyQualified = protoTypeNames.get(name);
    if (fullyQualified != null) return fullyQualified;

    if (enclosingProtoPackage != null) {
      WireType samePackage = protoTypeNames.get(enclosingProtoPackage + "." + name);
      if (samePackage != null) return samePackage;
    }

    // Look at the enclosing type, and its children, all the way up the nesting hierarchy.
    for (int i = enclosingTypes.size() - 1; i >= 0; i--) {
      WireType enclosingType = enclosingTypes.get(i);

      if (name.equals(enclosingType.protoTypeName().simpleName())) {
        return enclosingType;
      }

      for (WireType peerType : enclosingType.nestedTypes()) {
        if (name.equals(peerType.protoTypeName().simpleName())) {
          return peerType;
        }
      }
    }

    // TODO(jwilson): report an error and return a sentinel instead of crashing here.
    throw new IllegalArgumentException("unrecognized type name: " + name);
  }

  /** Returns a new linker that uses {@code protoPackage} to resolve local type names. */
  Linker withProtoPackage(String protoPackage) {
    return new Linker(this, protoPackage, null);
  }

  /** Returns a new linker that uses {@code message} to resolve local type names. */
  Linker withMessage(WireMessage message) {
    return new Linker(this, enclosingProtoPackage, message);
  }
}
