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
  private final Loader loader;
  private final Map<String, FileLinker> fileLinkers;
  private final Map<String, Type> protoTypeNames;
  private final List<String> errors;
  private final List<Object> contextStack;

  Linker(Loader loader) {
    this.loader = loader;
    this.fileLinkers = new LinkedHashMap<>();
    this.protoTypeNames = new LinkedHashMap<>();
    this.contextStack = Collections.emptyList();
    this.errors = new ArrayList<>();
  }

  private Linker(Linker enclosing, Object additionalContext) {
    this.loader = enclosing.loader;
    this.fileLinkers = enclosing.fileLinkers;
    this.protoTypeNames = enclosing.protoTypeNames;
    this.contextStack = Util.concatenate(enclosing.contextStack, additionalContext);
    this.errors = enclosing.errors;
  }

  /** Returns a linker for {@code path}, loading the file if necessary. */
  FileLinker getFileLinker(String path) {
    FileLinker existing = fileLinkers.get(path);
    if (existing != null) return existing;

    ProtoFile protoFile = loader.load(path);
    FileLinker result = new FileLinker(protoFile, withContext(protoFile));
    fileLinkers.put(path, result);
    return result;
  }

  /**
   * Link all features of all files in {@code sourceProtoFiles} to create a schema. This will also
   * partially link any imported files necessary.
   */
  public Schema link(Iterable<ProtoFile> sourceProtoFiles) {
    List<FileLinker> sourceFiles = new ArrayList<>();
    for (ProtoFile sourceFile : sourceProtoFiles) {
      FileLinker fileLinker = new FileLinker(sourceFile, withContext(sourceFile));
      fileLinkers.put(sourceFile.location().getPath(), fileLinker);
      sourceFiles.add(fileLinker);
    }

    for (FileLinker fileLinker : sourceFiles) {
      fileLinker.requireTypesRegistered();
    }

    // Also link descriptor.proto's types, which are necessary for options.
    FileLinker descriptorProto = getFileLinker("google/protobuf/descriptor.proto");
    descriptorProto.requireTypesRegistered();

    for (FileLinker fileLinker : sourceFiles) {
      fileLinker.linkExtensions();
    }

    for (FileLinker fileLinker : sourceFiles) {
      fileLinker.linkMembers();
    }

    for (FileLinker fileLinker : sourceFiles) {
      fileLinker.linkOptions();
    }

    for (FileLinker fileLinker : sourceFiles) {
      fileLinker.validate();
    }

    if (!errors.isEmpty()) {
      throw new SchemaException(errors);
    }

    ImmutableList.Builder<ProtoFile> result = ImmutableList.builder();
    for (FileLinker fileLinker : fileLinkers.values()) {
      result.add(fileLinker.protoFile());
    }
    return new Schema(result.build());
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

    // If no type could be resolved, load imported files and try again.
    if (resolved == null) {
      for (FileLinker fileLinker : contextImportedTypes()) {
        fileLinker.requireTypesRegistered();
      }
      resolved = resolve(name, protoTypeNames);
    }

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

  /**
   * Returns the files imported in the current context. These files declare the types that may be
   * resolved.
   */
  private List<FileLinker> contextImportedTypes() {
    ImmutableList.Builder<FileLinker> result = ImmutableList.builder();

    for (int i = contextStack.size() - 1; i >= 0; i--) {
      Object context = contextStack.get(i);
      if (context instanceof ProtoFile) {
        String path = ((ProtoFile) context).location().getPath();
        FileLinker fileLinker = getFileLinker(path);
        for (String effectiveImport : fileLinker.effectiveImports()) {
          result.add(getFileLinker(effectiveImport));
        }
      }
    }

    return result.build();
  }

  /** Adds {@code type}. */
  void addType(ProtoType protoType, Type type) {
    protoTypeNames.put(protoType.toString(), type);
  }

  /** Returns the type or null if it doesn't exist. */
  Type get(ProtoType protoType) {
    Type result = protoTypeNames.get(protoType.toString());
    if (result != null) return result;

    // If no type could be resolved, load imported files and try again.
    for (FileLinker fileLinker : contextImportedTypes()) {
      fileLinker.requireTypesRegistered();
    }
    return protoTypeNames.get(protoType.toString());
  }

  /**
   * Returns the type or null if it doesn't exist. Before this returns it ensures members are linked
   * so that options may dereference them.
   */
  Type getForOptions(ProtoType protoType) {
    Type result = get(protoType);
    if (result == null) return null;

    FileLinker fileLinker = getFileLinker(result.location().getPath());
    fileLinker.requireMembersLinked(result);
    return result;
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
  void validateFields(Iterable<Field> fields, List<Reserved> reserveds) {
    Multimap<Integer, Field> tagToField = LinkedHashMultimap.create();
    Multimap<String, Field> nameToField = LinkedHashMultimap.create();
    for (Field field : fields) {
      int tag = field.tag();
      if (!Util.isValidTag(tag)) {
        withContext(field).addError("tag is out of range: %s", tag);
      }

      for (Reserved reserved : reserveds) {
        if (reserved.matchesTag(tag)) {
          withContext(field).addError("tag %s is reserved (%s)", tag, reserved.getLocation());
        }
        if (reserved.matchesName(field.name())) {
          withContext(field).addError("name '%s' is reserved (%s)", field.name(),
              reserved.getLocation());
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
          nameToType.put(enumConstant.getName(), enumType);
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
              index++, enumType.type(), constant, enumType.constant(constant).getLocation()));
        }
        addError("%s", error);
      }
    }
  }

  void validateImport(Location location, ProtoType type) {
    // Map key type is always scalar. No need to validate it.
    if (type.isMap()) type = type.valueType();

    if (type.isScalar()) return;

    String path = location.getPath();
    String requiredImport = get(type).location().getPath();
    FileLinker fileLinker = getFileLinker(path);
    if (!path.equals(requiredImport) && !fileLinker.effectiveImports().contains(requiredImport)) {
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
            prefix, extensions.getLocation()));
      }
    }

    errors.add(error.toString());
  }
}
