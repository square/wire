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

import static com.squareup.wire.schema.Options.FILE_OPTIONS;

public final class ProtoFile {
  static final ProtoMember JAVA_PACKAGE = ProtoMember.get(FILE_OPTIONS, "java_package");

  private final Location location;
  private final ImmutableList<String> imports;
  private final ImmutableList<String> publicImports;
  private final String packageName;
  private final ImmutableList<Type> types;
  private final ImmutableList<Service> services;
  private final ImmutableList<Extend> extendList;
  private final Options options;
  private final Syntax syntax;
  private Object javaPackage;

  private ProtoFile(Location location, ImmutableList<String> imports,
      ImmutableList<String> publicImports, String packageName, ImmutableList<Type> types,
      ImmutableList<Service> services, ImmutableList<Extend> extendList, Options options,
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
    String packageName = protoFileElement.packageName();

    ImmutableList<Type> types = Type.fromElements(packageName, protoFileElement.types());

    ImmutableList<Service> services =
        Service.fromElements(packageName, protoFileElement.services());

    ImmutableList<Extend> wireExtends =
        Extend.fromElements(packageName, protoFileElement.extendDeclarations());

    Options options = new Options(Options.FILE_OPTIONS, protoFileElement.options());

    return new ProtoFile(protoFileElement.location(), protoFileElement.imports(),
        protoFileElement.publicImports(), packageName, types, services, wireExtends, options,
        protoFileElement.syntax());
  }

  ProtoFileElement toElement() {
    return ProtoFileElement.builder(location)
        .imports(imports)
        .publicImports(publicImports)
        .packageName(packageName)
        .types(Type.toElements(types))
        .services(Service.toElements(services))
        .extendDeclarations(Extend.toElements(extendList))
        .options(options.toElements())
        .syntax(syntax)
        .build();
  }

  public Location location() {
    return location;
  }

  ImmutableList<String> imports() {
    return imports;
  }

  ImmutableList<String> publicImports() {
    return publicImports;
  }

  /**
   * Returns the name of this proto file, like {@code simple_message} for {@code
   * squareup/protos/person/simple_message.proto}.
   */
  public String name() {
    String result = location().path();

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

  public String javaPackage() {
    return javaPackage != null ? String.valueOf(javaPackage) : null;
  }

  public ImmutableList<Type> types() {
    return types;
  }

  public ImmutableList<Service> services() {
    return services;
  }

  ImmutableList<Extend> extendList() {
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

    ProtoFile result = new ProtoFile(location, imports, publicImports, packageName,
        retainedTypes.build(), retainedServices.build(), extendList,
        options.retainAll(schema, markSet), syntax);
    result.javaPackage = javaPackage;
    return result;
  }

  void linkOptions(Linker linker) {
    options.link(linker);
    javaPackage = options().get(JAVA_PACKAGE);
  }

  @Override public String toString() {
    return location().path();
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
