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
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import java.util.List;

import static com.squareup.wire.schema.Options.FILE_OPTIONS;

public final class ProtoFile {
  static final ProtoMember JAVA_PACKAGE = ProtoMember.get(FILE_OPTIONS, "java_package");

  private final Location location;
  private final List<String> imports;
  private final List<String> publicImports;
  private final String packageName;
  private final List<Type> types;
  private final List<Service> services;
  private final List<Extend> extendList;
  private final Options options;
  private final Syntax syntax;
  private Object javaPackage;

  private ProtoFile(Location location, List<String> imports,
      List<String> publicImports, String packageName, List<Type> types,
      List<Service> services, List<Extend> extendList, Options options,
      Syntax syntax) {
    this.location = location;
    this.imports = imports;
    this.publicImports = publicImports;
    this.packageName = packageName;
    this.types = types;
    this.services = services;
    this.extendList = extendList;
    this.options = options;
    this.syntax = syntax;
  }

  static ProtoFile get(ProtoFileElement protoFileElement) {
    String packageName = protoFileElement.getPackageName();

    ImmutableList<Type> types = Type.fromElements(packageName, protoFileElement.getTypes());

    ImmutableList<Service> services =
        Service.fromElements(packageName, protoFileElement.getServices());

    ImmutableList<Extend> wireExtends =
        Extend.fromElements(packageName, protoFileElement.getExtendDeclarations());

    Options options = new Options(Options.FILE_OPTIONS, protoFileElement.getOptions());

    return new ProtoFile(protoFileElement.getLocation(), protoFileElement.getImports(),
        protoFileElement.getPublicImports(), packageName, types, services, wireExtends, options,
        protoFileElement.getSyntax());
  }

  ProtoFileElement toElement() {
    return new ProtoFileElement(
        location,
        packageName,
        syntax,
        imports,
        publicImports,
        Type.toElements(types),
        Service.toElements(services),
        Extend.toElements(extendList),
        options.toElements()
    );
  }

  public Location location() {
    return location;
  }

  List<String> imports() {
    return imports;
  }

  List<String> publicImports() {
    return publicImports;
  }

  /**
   * Returns the name of this proto file, like {@code simple_message} for {@code
   * squareup/protos/person/simple_message.proto}.
   */
  public String name() {
    String result = location().getPath();

    int slashIndex = result.lastIndexOf('/');
    if (slashIndex != -1) {
      result = result.substring(slashIndex + 1);
    }

    if (result.endsWith(".proto")) {
      result = result.substring(0, result.length() - ".proto".length());
    }

    return result;
  }

  public String packageName() {
    return packageName;
  }

  public Syntax syntax() {
    return syntax;
  }

  public String javaPackage() {
    return javaPackage != null ? String.valueOf(javaPackage) : null;
  }

  public List<Type> types() {
    return types;
  }

  public List<Service> services() {
    return services;
  }

  public List<Extend> extendList() {
    return extendList;
  }

  public Options options() {
    return options;
  }

  /** Returns a new proto file that omits types and services not in {@code identifiers}. */
  ProtoFile retainAll(Schema schema, MarkSet markSet) {
    ImmutableList.Builder<Type> retainedTypes = ImmutableList.builder();
    for (Type type : types) {
      Type retainedType = type.retainAll(schema, markSet);
      if (retainedType != null) {
        retainedTypes.add(retainedType);
      }
    }

    ImmutableList.Builder<Service> retainedServices = ImmutableList.builder();
    for (Service service : services) {
      Service retainedService = service.retainAll(schema, markSet);
      if (retainedService != null) {
        retainedServices.add(retainedService);
      }
    }

    ImmutableList.Builder<Extend> retainedExtends = ImmutableList.builder();
    for (Extend extend : extendList) {
      Extend retainedExtend = extend.retainAll(schema, markSet);
      if (retainedExtend != null) {
        retainedExtends.add(retainedExtend);
      }
    }

    ProtoFile result = new ProtoFile(location, imports, publicImports, packageName,
        retainedTypes.build(), retainedServices.build(), retainedExtends.build(),
        options.retainAll(schema, markSet), syntax);
    result.javaPackage = javaPackage;
    return result;
  }

  public ProtoFile retainImports(List<ProtoFile> retained) {
    ImmutableList.Builder<String> retainedImportsBuilder = ImmutableList.builder();
    for (String path : imports) {
      ProtoFile importedProtoFile = findProtoFile(retained, path);
      if (importedProtoFile == null) continue;

      if (importedProtoFile.types().isEmpty()
          && importedProtoFile.services().isEmpty()
          && importedProtoFile.extendList().isEmpty()) {

        // If we extend a google protobuf type, we should keep the import.
        if (path.startsWith("google/protobuf/")) {
          for (Extend extend : extendList) {
            if (extend.getName().startsWith("google.protobuf.")) {
              retainedImportsBuilder.add(path);
              break;
            }
          }
        }
      } else {
        retainedImportsBuilder.add(path);
      }
    }
    ImmutableList<String> retainedImports = retainedImportsBuilder.build();

    if (imports.size() != retainedImports.size()) {
      ProtoFile result = new ProtoFile(location, retainedImports, publicImports, packageName,
          types, services, extendList, options, syntax);
      result.javaPackage = javaPackage;
      return result;
    } else {
      return this;
    }
  }

  private static ProtoFile findProtoFile(List<ProtoFile> protoFiles, String path) {
    for (ProtoFile protoFile : protoFiles) {
      if (protoFile.location().getPath().equals(path)) {
        return protoFile;
      }
    }
    return null;
  }

  void linkOptions(Linker linker) {
    options.link(linker);
    javaPackage = options().get(JAVA_PACKAGE);
  }

  @Override public String toString() {
    return location().getPath();
  }

  public String toSchema() {
    return toElement().toSchema();
  }

  void validate(Linker linker) {
    linker.validateEnumConstantNameUniqueness(types);
  }

  /** Syntax version. */
  public enum Syntax {
    PROTO_2("proto2"),
    PROTO_3("proto3");

    private final String string;

    Syntax(String string) {
      this.string = string;
    }

    public static Syntax get(String string) {
      for (Syntax syntax : values()) {
        if (syntax.string.equals(string)) return syntax;
      }
      throw new IllegalArgumentException("unexpected syntax: " + string);
    }

    @Override public String toString() {
      return string;
    }
  }
}
