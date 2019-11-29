/*
 * Copyright (C) 2019 Square, Inc.
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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FileLinker {
  private final ProtoFile protoFile;
  private final Linker linker;

  /** Lazily computed set of files used to reference other types and options. */
  private ImmutableSet<String> effectiveImports;

  /** True once this linker has registered its types with the enclosing linker. */
  private boolean typesRegistered;

  private boolean extensionsLinked;
  private boolean importedExtensionsRegistered;

  /** The set of types defined in this file whose members have been linked. */
  private final Set<ProtoType> typesWithMembersLinked = new LinkedHashSet<>();

  FileLinker(ProtoFile protoFile, Linker linker) {
    this.protoFile = protoFile;
    this.linker = linker;
  }

  ProtoFile protoFile() {
    return protoFile;
  }

  /**
   * Returns all effective imports. This is computed on-demand by unioning all direct imports plus
   * the recursive set of all public imports.
   */
  ImmutableSet<String> effectiveImports() {
    if (effectiveImports == null) {
      Set<String> sink = new LinkedHashSet<>();
      addImportsRecursive(sink, protoFile.imports());
      addImportsRecursive(sink, protoFile.publicImports());
      effectiveImports = ImmutableSet.copyOf(sink);
    }
    return effectiveImports;

  }

  private void addImportsRecursive(Set<String> sink, Collection<String> paths) {
    for (String path : paths) {
      if (sink.add(path)) {
        FileLinker fileLinker = linker.getFileLinker(path);
        addImportsRecursive(sink, fileLinker.protoFile.publicImports());
      }
    }
  }

  void requireTypesRegistered() {
    if (typesRegistered) return;
    typesRegistered = true;

    for (Type type : protoFile.types()) {
      addTypes(type);
    }
  }

  private void addTypes(Type type) {
    linker.addType(type.type(), type);
    for (Type nestedType : type.nestedTypes()) {
      addTypes(nestedType);
    }
  }

  void requireExtensionsLinked() {
    if (extensionsLinked) return;
    extensionsLinked = true;

    requireTypesRegistered();
    for (Extend extend : protoFile.extendList()) {
      extend.link(linker);
    }
  }

  /**
   * This file might use extensions defined on one of the files we import. Make sure those
   * extensions are registered before we try to use our extensions.
   */
  void requireImportedExtensionsRegistered() {
    if (importedExtensionsRegistered) return;
    importedExtensionsRegistered = true;

    for (FileLinker importedFileLinker : linker.contextImportedTypes()) {
      importedFileLinker.requireExtensionsLinked();
    }
  }

  void linkMembers() {
    linkMembersRecursive(protoFile.types());
    for (Service service : protoFile.services()) {
      service.link(linker);
    }
  }

  /** Link the members of {@code types} and their nested types. */
  private void linkMembersRecursive(List<Type> types) {
    for (Type type : types) {
      requireMembersLinked(type);
      linkMembersRecursive(type.nestedTypes());
    }
  }

  /** Link the members of {@code type} that haven't been linked already. */
  void requireMembersLinked(Type type) {
    if (typesWithMembersLinked.add(type.type())) {
      type.linkMembers(linker);
    }
  }

  /**
   * This requires traversal of members of imported types! This may potentially include non-direct
   * dependencies!
   */
  void linkOptions() {
    protoFile.linkOptions(linker);
    for (Type type : protoFile.types()) {
      type.linkOptions(linker);
    }
    for (Service service : protoFile.services()) {
      service.linkOptions(linker);
    }
  }

  void validate() {
    protoFile.validate(linker);

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
}
