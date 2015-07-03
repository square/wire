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
import com.squareup.wire.internal.Util;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Links local field types and option types to the corresponding declarations. */
public final class Linker {
  private final Map<String, WireType> protoTypeNames;
  private final Map<ProtoTypeName, Map<String, WireField>> extensionsMap;

  // Context when linking.
  private final List<WireType> enclosingTypes;

  public Linker() {
    this.protoTypeNames = new LinkedHashMap<String, WireType>();
    this.extensionsMap = new LinkedHashMap<ProtoTypeName, Map<String, WireField>>();
    this.enclosingTypes = Collections.emptyList();
  }

  private Linker(Linker enclosing, WireType type) {
    this.protoTypeNames = enclosing.protoTypeNames;
    this.extensionsMap = enclosing.extensionsMap;
    this.enclosingTypes = type != null
        ? Util.concatenate(enclosing.enclosingTypes, type)
        : enclosing.enclosingTypes;
  }

  public void link(Collection<WireProtoFile> wireProtoFiles) {
    // Register the types.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (WireType type : wireProtoFile.types()) {
        register(type);
      }
    }

    // Link extensions. This depends on type registration.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (WireExtend extend : wireProtoFile.wireExtends()) {
        extend.link(this);
      }
    }

    // Register extensions. This needs the extensions to be linked.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (WireExtend extend : wireProtoFile.wireExtends()) {
        Map<String, WireField> map = extensionsMap.get(extend.protoTypeName());
        if (map == null) {
          map = new LinkedHashMap<String, WireField>();
          extensionsMap.put(extend.protoTypeName(), map);
        }
        for (WireField field : extend.fields()) {
          map.put(extend.packageName() + "." + field.name(), field);
        }
      }
    }

    // Link proto files and services.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (WireType type : wireProtoFile.types()) {
        type.link(this);
      }
      for (WireService service : wireProtoFile.services()) {
        service.link(this);
      }
    }

    // Finally link options. We can't link any options until we've linked all fields!
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      wireProtoFile.options().link(this);
      for (WireType type : wireProtoFile.types()) {
        type.linkOptions(this);
      }
      for (WireService service : wireProtoFile.services()) {
        service.linkOptions(this);
      }
    }
  }

  private void register(WireType type) {
    protoTypeNames.put(type.protoTypeName().toString(), type);
    for (WireType nestedType : type.nestedTypes()) {
      register(nestedType);
    }
  }

  /** Returns the type name for the scalar, relative or fully-qualified name {@code name}. */
  ProtoTypeName resolveType(String packageName, DataType type) {
    switch (type.kind()) {
      case SCALAR:
        return ProtoTypeName.getScalar(type.toString());

      case NAMED:
        return resolveNamedType(packageName, type.toString());

      default:
        // TODO(jwilson): report an error and return a sentinel instead of crashing here.
        throw new UnsupportedOperationException("unexpected type: " + type);
    }
  }

  /** Returns the type name for the relative or fully-qualified name {@code name}. */
  ProtoTypeName resolveNamedType(String packageName, String name) {
    WireType fullyQualified = protoTypeNames.get(name);
    if (fullyQualified != null) return fullyQualified.protoTypeName();

    if (packageName != null) {
      WireType samePackage = protoTypeNames.get(packageName + "." + name);
      if (samePackage != null) return samePackage.protoTypeName();
    }

    // Look at the enclosing type, and its children, all the way up the nesting hierarchy.
    for (int i = enclosingTypes.size() - 1; i >= 0; i--) {
      WireType enclosingType = enclosingTypes.get(i);

      if (name.equals(enclosingType.protoTypeName().simpleName())) {
        return enclosingType.protoTypeName();
      }

      for (WireType peerType : enclosingType.nestedTypes()) {
        if (name.equals(peerType.protoTypeName().simpleName())) {
          return peerType.protoTypeName();
        }
      }
    }

    // TODO(jwilson): report an error and return a sentinel instead of crashing here.
    throw new IllegalArgumentException("unrecognized type name: " + name);
  }

  /** Returns the map of known extensions for {@code extensionType}. */
  public Map<String, WireField> extensions(ProtoTypeName extensionType) {
    return extensionsMap.get(extensionType);
  }

  /** Returns the field named {@code field} on the message type of {@code self}. */
  WireField dereference(String packageName, WireField self, String field) {
    if (field.startsWith("[") && field.endsWith("]")) {
      field = field.substring(1, field.length() - 1);
    }

    WireType wireType = protoTypeNames.get(self.type().toString());
    if (wireType instanceof WireMessage) {
      WireField messageField = ((WireMessage) wireType).field(field);
      if (messageField != null) {
        return messageField;
      }

      Map<String, WireField> typeExtensions = extensionsMap.get(self.type());
      WireField extensionField = typeExtensions.get(field);
      if (extensionField != null) {
        return extensionField;
      }

      WireField fullyQualifiedExtensionField = typeExtensions.get(packageName + "." + field);
      if (fullyQualifiedExtensionField != null) {
        return fullyQualifiedExtensionField;
      }
    }

    return null; // Unable to traverse this field path.
  }

  /** Returns a new linker that uses {@code message} to resolve local type names. */
  Linker withMessage(WireMessage message) {
    return new Linker(this, message);
  }
}
