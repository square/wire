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

import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WireProtoFile {
  private final ProtoFile protoFile;
  private final List<WireType> types;
  private final List<WireService> services;
  private final List<WireExtend> wireExtends;
  private final List<WireOption> options;

  private WireProtoFile(ProtoFile protoFile, List<WireType> types, List<WireService> services,
      List<WireExtend> wireExtends, List<WireOption> options) {
    this.protoFile = protoFile;
    this.types = Util.immutableList(types);
    this.services = Util.immutableList(services);
    this.wireExtends = Util.immutableList(wireExtends);
    this.options = Util.immutableList(options);
  }

  public static WireProtoFile get(ProtoFile protoFile) {
    String packageName = protoFile.packageName();

    List<WireType> types = new ArrayList<WireType>();
    for (TypeElement type : protoFile.typeElements()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(packageName, type.name());
      types.add(WireType.get(protoTypeName, type));
    }

    List<WireService> services = new ArrayList<WireService>();
    for (ServiceElement service : protoFile.services()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(packageName, service.name());
      services.add(WireService.get(protoTypeName, service));
    }

    List<WireExtend> wireExtends = new ArrayList<WireExtend>();
    for (ExtendElement extend : protoFile.extendDeclarations()) {
      wireExtends.add(new WireExtend(packageName, extend));
    }

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : protoFile.options()) {
      options.add(new WireOption(packageName, option));
    }

    return new WireProtoFile(protoFile, types, services, wireExtends, options);
  }

  public String packageName() {
    return protoFile.packageName();
  }

  public List<WireType> types() {
    return types;
  }

  public List<WireService> services() {
    return services;
  }

  public List<WireExtend> wireExtends() {
    return wireExtends;
  }

  public List<WireOption> options() {
    return options;
  }

  /** Returns a new proto file that omits types and services not in {@code identifiers}. */
  WireProtoFile retainAll(Set<String> identifiers) {
    List<WireType> retainedTypes = new ArrayList<WireType>();
    for (WireType type : types) {
      WireType retainedType = type.retainAll(identifiers);
      if (retainedType != null) {
        retainedTypes.add(retainedType);
      }
    }

    List<WireService> retainedServices = new ArrayList<WireService>();
    for (WireService service : services) {
      WireService retainedService = service.retainAll(identifiers);
      if (retainedService != null) {
        retainedServices.add(retainedService);
      }
    }

    return new WireProtoFile(protoFile, retainedTypes, retainedServices, wireExtends, options);
  }
}
