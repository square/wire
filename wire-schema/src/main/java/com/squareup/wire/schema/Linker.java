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

import com.squareup.wire.internal.protoparser.DataType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Links local field types and option types to the corresponding declarations. */
public final class Linker {
  private final Map<String, Type> protoTypeNames;
  private final Map<Type.Name, Map<String, Field>> extensionsMap;

  // Context when linking.
  private final List<Type> enclosingTypes;

  public Linker() {
    this.protoTypeNames = new LinkedHashMap<String, Type>();
    this.extensionsMap = new LinkedHashMap<Type.Name, Map<String, Field>>();
    this.enclosingTypes = Collections.emptyList();
  }

  private Linker(Linker enclosing, Type type) {
    this.protoTypeNames = enclosing.protoTypeNames;
    this.extensionsMap = enclosing.extensionsMap;
    this.enclosingTypes = type != null
        ? Util.concatenate(enclosing.enclosingTypes, type)
        : enclosing.enclosingTypes;
  }

  public void link(Collection<WireProtoFile> wireProtoFiles) {
    // Register the types.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (Type type : wireProtoFile.types()) {
        register(type);
      }
    }

    // Link extensions. This depends on type registration.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (Extend extend : wireProtoFile.extendList()) {
        extend.link(this);
      }
    }

    // Register extensions. This needs the extensions to be linked.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (Extend extend : wireProtoFile.extendList()) {
        Map<String, Field> map = extensionsMap.get(extend.type());
        if (map == null) {
          map = new LinkedHashMap<String, Field>();
          extensionsMap.put(extend.type(), map);
        }
        for (Field field : extend.fields()) {
          map.put(extend.packageName() + "." + field.name(), field);
        }
      }
    }

    // Link proto files and services.
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      for (Type type : wireProtoFile.types()) {
        type.link(this);
      }
      for (Service service : wireProtoFile.services()) {
        service.link(this);
      }
    }

    // Finally link options. We can't link any options until we've linked all fields!
    for (WireProtoFile wireProtoFile : wireProtoFiles) {
      wireProtoFile.options().link(this);
      for (Type type : wireProtoFile.types()) {
        type.linkOptions(this);
      }
      for (Service service : wireProtoFile.services()) {
        service.linkOptions(this);
      }
    }
  }

  private void register(Type type) {
    protoTypeNames.put(type.name().toString(), type);
    for (Type nestedType : type.nestedTypes()) {
      register(nestedType);
    }
  }

  /** Returns the type name for the scalar, relative or fully-qualified name {@code name}. */
  Type.Name resolveType(String packageName, DataType type) {
    switch (type.kind()) {
      case SCALAR:
        return Type.Name.getScalar(type.toString());

      case NAMED:
        return resolveNamedType(packageName, type.toString());

      default:
        // TODO(jwilson): report an error and return a sentinel instead of crashing here.
        throw new UnsupportedOperationException("unexpected type: " + type);
    }
  }

  /** Returns the type name for the relative or fully-qualified name {@code name}. */
  Type.Name resolveNamedType(String packageName, String name) {
    Type fullyQualified = protoTypeNames.get(name);
    if (fullyQualified != null) return fullyQualified.name();

    if (packageName != null) {
      Type samePackage = protoTypeNames.get(packageName + "." + name);
      if (samePackage != null) return samePackage.name();
    }

    // Look at the enclosing type, and its children, all the way up the nesting hierarchy.
    for (int i = enclosingTypes.size() - 1; i >= 0; i--) {
      Type enclosingType = enclosingTypes.get(i);

      if (name.equals(enclosingType.name().simpleName())) {
        return enclosingType.name();
      }

      for (Type peerType : enclosingType.nestedTypes()) {
        if (name.equals(peerType.name().simpleName())) {
          return peerType.name();
        }
      }
    }

    // TODO(jwilson): report an error and return a sentinel instead of crashing here.
    throw new IllegalArgumentException("unrecognized type name: " + name);
  }

  /** Returns the map of known extensions for {@code extensionType}. */
  public Map<String, Field> extensions(Type.Name extensionType) {
    return extensionsMap.get(extensionType);
  }

  /** Returns the field named {@code field} on the message type of {@code self}. */
  Field dereference(String packageName, Field self, String field) {
    if (field.startsWith("[") && field.endsWith("]")) {
      field = field.substring(1, field.length() - 1);
    }

    Type type = protoTypeNames.get(self.type().toString());
    if (type instanceof MessageType) {
      Field messageField = ((MessageType) type).field(field);
      if (messageField != null) {
        return messageField;
      }

      Map<String, Field> typeExtensions = extensionsMap.get(self.type());
      Field extensionField = typeExtensions.get(field);
      if (extensionField != null) {
        return extensionField;
      }

      Field fullyQualifiedExtensionField = typeExtensions.get(packageName + "." + field);
      if (fullyQualifiedExtensionField != null) {
        return fullyQualifiedExtensionField;
      }
    }

    return null; // Unable to traverse this field path.
  }

  /** Returns a new linker that uses {@code message} to resolve local type names. */
  Linker withMessage(MessageType message) {
    return new Linker(this, message);
  }
}
