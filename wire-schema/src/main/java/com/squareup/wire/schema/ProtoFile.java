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
import com.squareup.wire.internal.protoparser.ExtendElement;
import com.squareup.wire.internal.protoparser.ProtoFileElement;
import com.squareup.wire.internal.protoparser.ServiceElement;
import com.squareup.wire.internal.protoparser.TypeElement;
import java.util.Set;

public final class ProtoFile {
  private final ProtoFileElement element;
  private final ImmutableList<Type> types;
  private final ImmutableList<Service> services;
  private final ImmutableList<Extend> extendList;
  private final Options options;

  private ProtoFile(ProtoFileElement element, ImmutableList<Type> types,
      ImmutableList<Service> services, ImmutableList<Extend> extendList, Options options) {
    this.element = element;
    this.types = types;
    this.services = services;
    this.extendList = extendList;
    this.options = options;
  }

  public static ProtoFile get(ProtoFileElement protoFileElement) {
    String packageName = protoFileElement.packageName();

    ImmutableList.Builder<Type> types = ImmutableList.builder();
    for (TypeElement type : protoFileElement.types()) {
      Type.Name name = Type.Name.get(packageName, type.name());
      types.add(Type.get(name, type));
    }

    ImmutableList.Builder<Service> services = ImmutableList.builder();
    for (ServiceElement service : protoFileElement.services()) {
      Type.Name name = Type.Name.get(packageName, service.name());
      services.add(Service.get(name, service));
    }

    ImmutableList.Builder<Extend> wireExtends = ImmutableList.builder();
    for (ExtendElement extend : protoFileElement.extendDeclarations()) {
      wireExtends.add(new Extend(packageName, extend));
    }

    Options options = new Options(
        Type.Name.FILE_OPTIONS, packageName, protoFileElement.options());

    return new ProtoFile(protoFileElement, types.build(), services.build(),
        wireExtends.build(), options);
  }

  public Location location() {
    return element.location();
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
    return element.packageName();
  }

  public ImmutableList<Type> types() {
    return types;
  }

  public ImmutableList<Service> services() {
    return services;
  }

  public ImmutableList<Extend> extendList() {
    return extendList;
  }

  public Options options() {
    return options;
  }

  /** Returns a new proto file that omits types and services not in {@code identifiers}. */
  ProtoFile retainAll(Set<String> identifiers) {
    ImmutableList.Builder<Type> retainedTypes = ImmutableList.builder();
    for (Type type : types) {
      Type retainedType = type.retainAll(identifiers);
      if (retainedType != null) {
        retainedTypes.add(retainedType);
      }
    }

    ImmutableList.Builder<Service> retainedServices = ImmutableList.builder();
    for (Service service : services) {
      Service retainedService = service.retainAll(identifiers);
      if (retainedService != null) {
        retainedServices.add(retainedService);
      }
    }

    return new ProtoFile(
        element, retainedTypes.build(), retainedServices.build(), extendList, options);
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
