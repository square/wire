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
import java.util.ArrayList;
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
  private final Map<ProtoTypeName, Map<String, WireField>> extensionsMap;

  // Context when linking.
  private final String enclosingProtoPackage;
  private final List<WireType> enclosingTypes;

  public Linker(String repoPath, IO io) {
    this.repoPath = repoPath;
    this.io = io;
    this.loadedDependencies = new LinkedHashSet<String>();
    this.protoTypeNames = new LinkedHashMap<String, WireType>();
    this.protoFilesMap = new LinkedHashMap<ProtoFile, WireProtoFile>();
    this.extensionsMap = new LinkedHashMap<ProtoTypeName, Map<String, WireField>>();
    this.enclosingProtoPackage = null;
    this.enclosingTypes = Collections.emptyList();
  }

  private Linker(Linker enclosing, String protoPackage, WireType type) {
    this.repoPath = enclosing.repoPath;
    this.io = enclosing.io;
    this.loadedDependencies = enclosing.loadedDependencies;
    this.protoTypeNames = enclosing.protoTypeNames;
    this.protoFilesMap = enclosing.protoFilesMap;
    this.extensionsMap = enclosing.extensionsMap;
    this.enclosingProtoPackage = protoPackage;
    this.enclosingTypes = type != null
        ? Util.concatenate(enclosing.enclosingTypes, type)
        : enclosing.enclosingTypes;
  }

  /** Recursively add {@code protoFile} and its dependencies. */
  public void add(ProtoFile protoFile) throws IOException {
    WireProtoFile wireProtoFile = new WireProtoFile(protoFile);
    protoFilesMap.put(protoFile, wireProtoFile);

    // Register the types.
    for (WireType type : wireProtoFile.types()) {
      register(type);
    }

    // Register the extensions.
    for (WireExtend extend : wireProtoFile.wireExtends()) {
      Map<String, WireField> map = extensionsMap.get(extend.protoTypeName());
      if (map == null) {
        map = new LinkedHashMap<String, WireField>();
        extensionsMap.put(extend.protoTypeName(), map);
      }
      for (WireField field : extend.fields()) {
        map.put(wireProtoFile.packageName() + "." + field.name(), field);
      }
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

  /**
   * Given a path of field references, this returns the corresponding fields. The path may be either
   * relative (to the current proto package) or absolute. Each path segment is separated by a dot.
   * This is particularly awkward because of the collision between the separators for package names
   * and fields.
   */
  List<WireField> fieldPath(ProtoTypeName extensionType, String fieldPath) {
    Map<String, WireField> extensionsForType = extensionsMap.get(extensionType);
    if (extensionsForType == null) {
      return null; // No known extensions for the given extension type.
    }

    String[] path = resolveFieldPath(fieldPath, extensionsForType.keySet());
    if (path == null && enclosingProtoPackage != null) {
      // If the path couldn't be resolved, attempt again by prefixing it with the package name.
      path = resolveFieldPath(enclosingProtoPackage + "." + fieldPath, extensionsForType.keySet());
    }
    if (path == null) {
      return null; // Unable to find the root of this field path.
    }

    List<WireField> fields = new ArrayList<WireField>();
    WireField field = extensionsForType.get(path[0]);
    fields.add(field);

    for (int i = 1; i < path.length; i++) {
      String pathSegment = path[i];
      ProtoTypeName valueType = field.type();
      WireType wireType = protoTypeNames.get(valueType.toString());
      if (wireType instanceof WireMessage) {
        field = ((WireMessage) wireType).getField(pathSegment);
        fields.add(field);
      } else {
        return null; // Unable to traverse this field path.
      }
    }

    return fields;
  }

  /**
   * Given a path like {@code a.b.c.d} and a set of paths like {@code {a.b.c, a.f.g, h.j}}, this
   * returns the original path split on dots such that the first element is in the set. For the
   * above example it would return the array {@code [a.b.c, d]}.
   *
   * <p>Typically the input path is a package name like {@code a.b}, followed by a dot and a
   * sequence of field names. The first field name is an extension field; subsequent field names
   * make a path within that extension.
   *
   * <p>Note that a single input may yield multiple possible answers, such as when package names
   * and field names collide. This method prefers shorter package names though that is an
   * implementation detail.
   */
  static String[] resolveFieldPath(String name, Set<String> fullyQualifiedNames) {
    // Try to resolve a local name.
    for (int i = 0; i < name.length(); i++) {
      i = name.indexOf('.', i);
      if (i == -1) i = name.length();

      String candidate = name.substring(0, i);
      if (fullyQualifiedNames.contains(candidate)) {
        String[] path = name.substring(i).split("\\.", -1);
        path[0] = name.substring(0, i);
        return path;
      }
    }

    return null;
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
