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

import java.util.Collection;

final class FileLinker {
  private final ProtoFile protoFile;
  private final Linker linker;

  FileLinker(ProtoFile protoFile, Linker linker) {
    this.protoFile = protoFile;
    this.linker = linker;
  }

  ProtoFile protoFile() {
    return protoFile;
  }

  /** Register public imports so we know that importing a.proto also imports b.proto and c.proto. */
  void registerDirectPublicImports() {
    for (String path : protoFile.publicImports()) {
      linker.addPublicImport(protoFile.location(), path);
    }
  }

  /**
   * Register the effective set of imports for this file, which includes the direct imports plus
   * the recursive traversal of public imports.
   */
  void registerTransitiveImports() {
    addImports(protoFile.location(), protoFile.imports());
    addImports(protoFile.location(), protoFile.publicImports());
  }

  /** Configures {@code location} to import {@code paths}, plus their public imports recursively. */
  private void addImports(Location location, Collection<String> paths) {
    for (String path : paths) {
      if (linker.addImport(location, path)) {
        addImports(location, linker.getPublicImports(path));
      }
    }
  }

  void registerTypes() {
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

  void linkExtensions() {
    for (Extend extend : protoFile.extendList()) {
      extend.link(linker);
    }
  }

  void linkMembers() {
    for (Type type : protoFile.types()) {
      type.link(linker);
    }
    for (Service service : protoFile.services()) {
      service.link(linker);
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
