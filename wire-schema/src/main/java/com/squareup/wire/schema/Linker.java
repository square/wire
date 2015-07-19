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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.squareup.wire.internal.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Links local field types and option types to the corresponding declarations. */
final class Linker {
  private final ImmutableList<ProtoFile> protoFiles;
  private final Map<String, Type> protoTypeNames;
  private final Map<Type.Name, Map<String, Field>> extensionsMap;
  private final List<String> errors;
  private final List<Object> contextStack;

  public Linker(Iterable<ProtoFile> protoFiles) {
    this.protoFiles = ImmutableList.copyOf(protoFiles);
    this.protoTypeNames = new LinkedHashMap<>();
    this.extensionsMap = new LinkedHashMap<>();
    this.contextStack = Collections.emptyList();
    this.errors = new ArrayList<>();
  }

  private Linker(Linker enclosing, Object additionalContext) {
    this.protoFiles = enclosing.protoFiles;
    this.protoTypeNames = enclosing.protoTypeNames;
    this.extensionsMap = enclosing.extensionsMap;
    this.contextStack = Util.concatenate(enclosing.contextStack, additionalContext);
    this.errors = enclosing.errors;
  }

  public Schema link() {
    // Register the types.
    for (ProtoFile protoFile : protoFiles) {
      for (Type type : protoFile.types()) {
        register(type);
      }
    }

    // Link extensions. This depends on type registration.
    for (ProtoFile protoFile : protoFiles) {
      for (Extend extend : protoFile.extendList()) {
        extend.link(this);
      }
    }

    // Register extensions. This needs the extensions to be linked.
    for (ProtoFile protoFile : protoFiles) {
      for (Extend extend : protoFile.extendList()) {
        Map<String, Field> map = extensionsMap.get(extend.type());
        if (map == null) {
          map = new LinkedHashMap<>();
          extensionsMap.put(extend.type(), map);
        }
        for (Field field : extend.fields()) {
          map.put(extend.packageName() + "." + field.name(), field);
        }
      }
    }

    // Link proto files and services.
    for (ProtoFile protoFile : protoFiles) {
      for (Type type : protoFile.types()) {
        type.link(this);
      }
      for (Service service : protoFile.services()) {
        service.link(this);
      }
    }

    // Link options. We can't link any options until we've linked all fields!
    for (ProtoFile protoFile : protoFiles) {
      protoFile.options().link(this);
      for (Type type : protoFile.types()) {
        type.linkOptions(this);
      }
      for (Service service : protoFile.services()) {
        service.linkOptions(this);
      }
    }

    // Validate the linked schema.
    for (ProtoFile protoFile : protoFiles) {
      for (Type type : protoFile.types()) {
        type.validate(this);
      }
      for (Extend extend : protoFile.extendList()) {
        extend.validate(this);
      }
    }

    if (!errors.isEmpty()) {
      throw new SchemaException(errors);
    }

    return new Schema(protoFiles);
  }

  private void register(Type type) {
    protoTypeNames.put(type.name().toString(), type);
    for (Type nestedType : type.nestedTypes()) {
      register(nestedType);
    }
  }

  /** Returns the type name for the scalar, relative or fully-qualified name {@code name}. */
  Type.Name resolveType(String packageName, String name) {
    return resolveType(packageName, name, false);
  }

  /** Returns the type name for the relative or fully-qualified name {@code name}. */
  Type.Name resolveNamedType(String packageName, String name) {
    return resolveType(packageName, name, true);
  }

  private Type.Name resolveType(String packageName, String name, boolean namedTypesOnly) {
    Type.Name scalar = Type.Name.getScalar(name);
    if (scalar != null) {
      if (namedTypesOnly) {
        addError("expected a message but was %s", name);
      }
      return scalar;
    }

    Type fullyQualified = protoTypeNames.get(name);
    if (fullyQualified != null) return fullyQualified.name();

    if (packageName != null) {
      Type samePackage = protoTypeNames.get(packageName + "." + name);
      if (samePackage != null) return samePackage.name();
    }

    // Look at the enclosing type, and its children, all the way up the nesting hierarchy.
    for (int i = contextStack.size() - 1; i >= 0; i--) {
      Object context = contextStack.get(i);
      if (!(context instanceof Type)) continue;

      Type enclosingType = (Type) context;

      if (name.equals(enclosingType.name().simpleName())) {
        return enclosingType.name();
      }

      for (Type peerType : enclosingType.nestedTypes()) {
        if (name.equals(peerType.name().simpleName())) {
          return peerType.name();
        }
      }
    }

    addError("unable to resolve %s", name);
    return Type.Name.BYTES; // Just return any placeholder.
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

  /** Validate that the tags of {@code fields} are unique and in range. */
  void validateTags(Iterable<Field> fields) {
    Multimap<Integer, Field> tagToField = LinkedHashMultimap.create();
    for (Field field : fields) {
      int tag = field.tag();
      if (!Util.isValidTag(tag)) {
        withContext(field).addError("tag is out of range: %s", tag);
      } else {
        tagToField.put(tag, field);
      }
    }

    for (Map.Entry<Integer, Collection<Field>> entry : tagToField.asMap().entrySet()) {
      if (entry.getValue().size() > 1) {
        StringBuilder error = new StringBuilder();
        error.append(String.format("multiple fields share tag %s:", entry.getKey()));
        int index = 1;
        for (Field field : entry.getValue()) {
          error.append(String.format("\n  %s. %s (%s)",
              index++, field.name(), field.location()));
        }
        addError("%s", error);
      }
    }
  }

  void validateEnumConstantNameUniqueness(Iterable<Type> nestedTypes) {
    Multimap<String, EnumType> nameToType = LinkedHashMultimap.create();
    for (Type type : nestedTypes) {
      if (type instanceof EnumType) {
        EnumType enumType = (EnumType) type;
        for (EnumConstant enumConstant : enumType.constants()) {
          nameToType.put(enumConstant.name(), enumType);
        }
      }
    }

    for (Map.Entry<String, Collection<EnumType>> entry : nameToType.asMap().entrySet()) {
      if (entry.getValue().size() > 1) {
        StringBuilder error = new StringBuilder();
        String constant = entry.getKey();
        int index = 1;
        error.append(String.format("multiple enums share constant %s:", constant));
        for (EnumType enumType : entry.getValue()) {
          error.append(String.format("\n  %s. %s.%s (%s)",
              index++, enumType.name(), constant, enumType.constant(constant).location()));
        }
        addError("%s", error);
      }
    }
  }

  /** Returns a new linker that uses {@code context} to resolve type names and report errors. */
  Linker withContext(Object context) {
    return new Linker(this, context);
  }

  void addError(String format, Object... args) {
    StringBuilder error = new StringBuilder();
    error.append(String.format(format, args));

    for (int i = contextStack.size() - 1; i >= 0; i--) {
      Object context = contextStack.get(i);
      String prefix = (i == contextStack.size() - 1) ? "\n  for" : "\n  in";

      if (context instanceof Rpc) {
        Rpc rpc = (Rpc) context;
        error.append(String.format("%s rpc %s (%s)", prefix, rpc.name(), rpc.location()));

      } else if (context instanceof Extend) {
        Extend extend = (Extend) context;
        Type.Name type = extend.type();
        error.append(type != null
            ? String.format("%s extend %s (%s)", prefix, type, extend.location())
            : String.format("%s extend (%s)", prefix, extend.location()));

      } else if (context instanceof Field) {
        Field field = (Field) context;
        error.append(String.format("%s field %s (%s)", prefix, field.name(), field.location()));

      } else if (context instanceof MessageType) {
        MessageType message = (MessageType) context;
        error.append(String.format("%s message %s (%s)",
            prefix, message.name(), message.location()));

      } else if (context instanceof EnumType) {
        EnumType enumType = (EnumType) context;
        error.append(String.format("%s enum %s (%s)",
            prefix, enumType.name(), enumType.location()));

      } else if (context instanceof Service) {
        Service service = (Service) context;
        error.append(String.format("%s service %s (%s)",
            prefix, service.name(), service.location()));

      } else if (context instanceof Extensions) {
        Extensions extensions = (Extensions) context;
        error.append(String.format("%s extensions (%s)",
            prefix, extensions.location()));
      }
    }

    errors.add(error.toString());
  }
}
