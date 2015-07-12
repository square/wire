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
import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.Set;

public final class WireProtoFile {
  private final String sourcePath;
  private final ProtoFile protoFile;
  private final ImmutableList<Type> types;
  private final ImmutableList<Service> services;
  private final ImmutableList<Extend> extendList;
  private final Options options;

  private WireProtoFile(String sourcePath, ProtoFile protoFile, ImmutableList<Type> types,
      ImmutableList<Service> services, ImmutableList<Extend> extendList, Options options) {
    this.sourcePath = sourcePath;
    this.protoFile = protoFile;
    this.types = types;
    this.services = services;
    this.extendList = extendList;
    this.options = options;
  }

  public static WireProtoFile get(String sourcePath, ProtoFile protoFile) {
    String packageName = protoFile.packageName();

    ImmutableList.Builder<Type> types = ImmutableList.builder();
    for (TypeElement type : protoFile.typeElements()) {
      Type.Name name = Type.Name.get(packageName, type.name());
      types.add(Type.get(name, type));
    }

    ImmutableList.Builder<Service> services = ImmutableList.builder();
    for (ServiceElement service : protoFile.services()) {
      Type.Name name = Type.Name.get(packageName, service.name());
      services.add(Service.get(name, service));
    }

    ImmutableList.Builder<Extend> wireExtends = ImmutableList.builder();
    for (ExtendElement extend : protoFile.extendDeclarations()) {
      wireExtends.add(new Extend(packageName, extend));
    }

    Options options = new Options(
        Type.Name.FILE_OPTIONS, packageName, protoFile.options());

    return new WireProtoFile(sourcePath, protoFile, types.build(), services.build(),
        wireExtends.build(), options);
  }

  public String sourcePath() {
    return sourcePath;
  }

  /**
   * Returns the name of this proto file, like {@code simple_message} for {@code
   * squareup/protos/person/simple_message.proto}.
   */
  public String name() {
    String result = sourcePath;

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
    return protoFile.packageName();
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
  WireProtoFile retainAll(Set<String> identifiers) {
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

    return new WireProtoFile(sourcePath, protoFile, retainedTypes.build(), retainedServices.build(),
        extendList, options);
  }
}
