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
import com.squareup.wire.schema.internal.Util;
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
  private final Multimap<String, String> imports;
  private final List<String> errors;
  private final List<Object> contextStack;

  public Linker(Iterable<ProtoFile> protoFiles) {
    this.protoFiles = ImmutableList.copyOf(protoFiles);
    this.protoTypeNames = new LinkedHashMap<>();
    this.imports = LinkedHashMultimap.create();
    this.contextStack = Collections.emptyList();
    this.errors = new ArrayList<>();
  }

  private Linker(Linker enclosing, Object additionalContext) {
    this.protoFiles = enclosing.protoFiles;
    this.protoTypeNames = enclosing.protoTypeNames;
    this.imports = enclosing.imports;
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
      Linker linker = withContext(protoFile);
      for (Extend extend : protoFile.extendList()) {
        extend.link(linker);
      }
    }

    // Link proto files and services.
    for (ProtoFile protoFile : protoFiles) {
      Linker linker = withContext(protoFile);
      for (Type type : protoFile.types()) {
        type.link(linker);
      }
      for (Service service : protoFile.services()) {
        service.link(linker);
      }
    }

    // Link options. We can't link any options until we've linked all fields!
    for (ProtoFile protoFile : protoFiles) {
      Linker linker = withContext(protoFile);
      protoFile.linkOptions(linker);
      for (Type type : protoFile.types()) {
        type.linkOptions(linker);
      }
      for (Service service : protoFile.services()) {
        service.linkOptions(linker);
      }
    }

    // Compute public imports so we know that importing a.proto also imports b.proto and c.proto.
    Multimap<String, String> publicImports = LinkedHashMultimap.create();
    for (ProtoFile protoFile : protoFiles) {
      publicImports.putAll(protoFile.location().path(), protoFile.publicImports());
    }
    // For each proto, gather its imports and its transitive imports.
    for (ProtoFile protoFile : protoFiles) {
      Collection<String> sink = imports.get(protoFile.location().path());
      addImports(sink, protoFile.imports(), publicImports);
      addImports(sink, protoFile.publicImports(), publicImports);
    }

    // Validate the linked schema.
    for (ProtoFile protoFile : protoFiles) {
      Linker linker = withContext(protoFile);
      for (Type type : protoFile.types()) {
        type.validate(linker);
      }
      for (Service service : protoFile.services()) {
        service.validate(linker);
      }
      for (Extend extend : protoFile.extendList()) {
        extend.validate(linker);
      }
    }

    if (!errors.isEmpty()) {
      throw new SchemaException(errors);
    }

    return new Schema(protoFiles);
  }

  /** Add all paths in {@code paths} to {@code sink}, plus their public imports, recursively. */
  private void addImports(Collection<String> sink,
      Collection<String> paths, Multimap<String, String> publicImports) {
    for (String path : paths) {
      if (sink.add(path)) {
        addImports(sink, publicImports.get(path), publicImports);
      }
    }
  }

  private void register(Type type) {
    protoTypeNames.put(type.type().toString(), type);
    for (Type nestedType : type.nestedTypes()) {
      register(nestedType);
    }
  }

  /** Returns the type name for the scalar, relative or fully-qualified name {@code name}. */
  ProtoType resolveType(String name) {
    return resolveType(name, false);
  }

  /** Returns the type name for the relative or fully-qualified name {@code name}. */
  ProtoType resolveMessageType(String name) {
    return resolveType(name, true);
  }

  private ProtoType resolveType(String name, boolean messageOnly) {
    ProtoType type = ProtoType.get(name);
    if (type.isScalar()) {
      if (messageOnly) {
        addError("expected a message but was %s", name);
      }
      return type;
    }

    if (type.isMap()) {
      if (messageOnly) {
        addError("expected a message but was %s", name);
      }
      ProtoType keyType = resolveType(type.keyType().toString(), false);
      ProtoType valueType = resolveType(type.valueType().toString(), false);
      return new ProtoType(keyType, valueType, name);
    }

    Type resolved = resolve(name, protoTypeNames);
    if (resolved == null) {
      addError("unable to resolve %s", name);
      return ProtoType.BYTES; // Just return any placeholder.
    }

    if (messageOnly && !(resolved instanceof MessageType)) {
      addError("expected a message but was %s", name);
      return ProtoType.BYTES; // Just return any placeholder.
    }

    return resolved.type();
  }

  <T> T resolve(String name, Map<String, T> map) {
    if (name.startsWith(".")) {
      // If name starts with a '.', the rest of it is fully qualified.
      T result = map.get(name.substring(1));
      if (result != null) return result;
    } else {
      // We've got a name suffix, like 'Person' or 'protos.Person'. Start the search from with the
      // longest prefix like foo.bar.Baz.Quux, shortening the prefix until we find a match.
      String prefix = resolveContext();
      while (!prefix.isEmpty()) {
        T result = map.get(prefix + '.' + name);
        if (result != null) return result;

        // Strip the last nested class name or package name from the end and try again.
        int dot = prefix.lastIndexOf('.');
        prefix = dot != -1 ? prefix.substring(0, dot) : "";
      }
      T result = map.get(name);
      if (result != null) return result;
    }
    return null;
  }

  private String resolveContext() {
    for (int i = contextStack.size() - 1; i >= 0; i--) {
      Object context = contextStack.get(i);
      if (context instanceof Type) {
        return ((Type) context).type().toString();
      } else if (context instanceof ProtoFile) {
        String packageName = ((ProtoFile) context).packageName();
        return packageName != null ? packageName : "";
      } else if (context instanceof Field && ((Field) context).isExtension()) {
        String packageName = ((Field) context).packageName();
        return packageName != null ? packageName : "";
      }
    }
    throw new IllegalStateException();
  }

  /** Returns the current package name from the context stack. */
  String packageName() {
    for (Object context : contextStack) {
      if (context instanceof ProtoFile) return ((ProtoFile) context).packageName();
    }
    return null;
  }

  /** Returns the type or null if it doesn't exist. */
  public Type get(ProtoType protoType) {
    return protoTypeNames.get(protoType.toString());
  }

  /** Returns the field named {@code field} on the message type of {@code self}. */
  Field dereference(Field self, String field) {
    if (field.startsWith("[") && field.endsWith("]")) {
      field = field.substring(1, field.length() - 1);
    }

    Type type = protoTypeNames.get(self.type().toString());
    if (type instanceof MessageType) {
      MessageType messageType = (MessageType) type;
      Field messageField = messageType.field(field);
      if (messageField != null) return messageField;

      Map<String, Field> typeExtensions = messageType.extensionFieldsMap();
      Field extensionField = resolve(field, typeExtensions);
      if (extensionField != null) return extensionField;
    }

    return null; // Unable to traverse this field path.
  }

  /** Validate that the tags of {@code fields} are unique and in range. */
  void validateFields(Iterable<Field> fields, ImmutableList<Reserved> reserveds) {
    Multimap<Integer, Field> tagToField = LinkedHashMultimap.create();
    Multimap<String, Field> nameToField = LinkedHashMultimap.create();
    for (Field field : fields) {
      int tag = field.tag();
      if (!Util.isValidTag(tag)) {
        withContext(field).addError("tag is out of range: %s", tag);
      }

      for (Reserved reserved : reserveds) {
        if (reserved.matchesTag(tag)) {
          withContext(field).addError("tag %s is reserved (%s)", tag, reserved.location());
        }
        if (reserved.matchesName(field.name())) {
          withContext(field).addError("name '%s' is reserved (%s)", field.name(),
              reserved.location());
        }
      }

      tagToField.put(tag, field);
      nameToField.put(field.qualifiedName(), field);
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

    for (Collection<Field> collidingFields : nameToField.asMap().values()) {
      if (collidingFields.size() > 1) {
        Field first = collidingFields.iterator().next();
        StringBuilder error = new StringBuilder();
        error.append(String.format("multiple fields share name %s:", first.name()));
        int index = 1;
        for (Field field : collidingFields) {
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
              index++, enumType.type(), constant, enumType.constant(constant).location()));
        }
        addError("%s", error);
      }
    }
  }

  void validateImport(Location location, ProtoType type) {
    // Map key type is always scalar. No need to validate it.
    if (type.isMap()) type = type.valueType();

    if (type.isScalar()) return;

    String path = location.path();
    String requiredImport = get(type).location().path();
    if (!path.equals(requiredImport) && !imports.containsEntry(path, requiredImport)) {
      addError("%s needs to import %s", path, requiredImport);
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
        ProtoType type = extend.type();
        error.append(type != null
            ? String.format("%s extend %s (%s)", prefix, type, extend.location())
            : String.format("%s extend (%s)", prefix, extend.location()));

      } else if (context instanceof Field) {
        Field field = (Field) context;
        error.append(String.format("%s field %s (%s)", prefix, field.name(), field.location()));

      } else if (context instanceof MessageType) {
        MessageType message = (MessageType) context;
        error.append(String.format("%s message %s (%s)",
            prefix, message.type(), message.location()));

      } else if (context instanceof EnumType) {
        EnumType enumType = (EnumType) context;
        error.append(String.format("%s enum %s (%s)",
            prefix, enumType.type(), enumType.location()));

      } else if (context instanceof Service) {
        Service service = (Service) context;
        error.append(String.format("%s service %s (%s)",
            prefix, service.type(), service.location()));

      } else if (context instanceof Extensions) {
        Extensions extensions = (Extensions) context;
        error.append(String.format("%s extensions (%s)",
            prefix, extensions.location()));
      }
    }

    errors.add(error.toString());
  }
}
