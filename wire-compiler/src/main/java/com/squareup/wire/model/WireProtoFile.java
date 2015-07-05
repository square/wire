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
package com.squareup.wire.model;

import com.google.common.collect.ImmutableList;
import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.Set;

public final class WireProtoFile {
  private final String sourcePath;
  private final ProtoFile protoFile;
  private final ImmutableList<WireType> types;
  private final ImmutableList<WireService> services;
  private final ImmutableList<WireExtend> wireExtends;
  private final Options options;

  private WireProtoFile(String sourcePath, ProtoFile protoFile, ImmutableList<WireType> types,
      ImmutableList<WireService> services, ImmutableList<WireExtend> wireExtends, Options options) {
    this.sourcePath = sourcePath;
    this.protoFile = protoFile;
    this.types = types;
    this.services = services;
    this.wireExtends = wireExtends;
    this.options = options;
  }

  public static WireProtoFile get(String sourcePath, ProtoFile protoFile) {
    String packageName = protoFile.packageName();

    ImmutableList.Builder<WireType> types = ImmutableList.builder();
    for (TypeElement type : protoFile.typeElements()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(packageName, type.name());
      types.add(WireType.get(protoTypeName, type));
    }

    ImmutableList.Builder<WireService> services = ImmutableList.builder();
    for (ServiceElement service : protoFile.services()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(packageName, service.name());
      services.add(WireService.get(protoTypeName, service));
    }

    ImmutableList.Builder<WireExtend> wireExtends = ImmutableList.builder();
    for (ExtendElement extend : protoFile.extendDeclarations()) {
      wireExtends.add(new WireExtend(packageName, extend));
    }

    Options options = new Options(
        ProtoTypeName.FILE_OPTIONS, packageName, protoFile.options());

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

  public ImmutableList<WireType> types() {
    return types;
  }

  public ImmutableList<WireService> services() {
    return services;
  }

  public ImmutableList<WireExtend> wireExtends() {
    return wireExtends;
  }

  public Options options() {
    return options;
  }

  /** Returns a new proto file that omits types and services not in {@code identifiers}. */
  WireProtoFile retainAll(Set<String> identifiers) {
    ImmutableList.Builder<WireType> retainedTypes = ImmutableList.builder();
    for (WireType type : types) {
      WireType retainedType = type.retainAll(identifiers);
      if (retainedType != null) {
        retainedTypes.add(retainedType);
      }
    }

    ImmutableList.Builder<WireService> retainedServices = ImmutableList.builder();
    for (WireService service : services) {
      WireService retainedService = service.retainAll(identifiers);
      if (retainedService != null) {
        retainedServices.add(retainedService);
      }
    }

    return new WireProtoFile(sourcePath, protoFile, retainedTypes.build(), retainedServices.build(),
        wireExtends, options);
  }
}
