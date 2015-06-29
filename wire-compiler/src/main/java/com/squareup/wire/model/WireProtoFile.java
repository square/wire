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
import java.util.Collections;
import java.util.List;

public final class WireProtoFile {
  private final ProtoFile protoFile;
  private final List<WireType> types;
  private final List<WireService> services;
  private final List<WireExtend> wireExtends;
  private final List<WireOption> options;

  WireProtoFile(ProtoFile protoFile) {
    this.protoFile = protoFile;

    String protoPackage = protoFile.packageName();

    List<WireType> types = new ArrayList<WireType>();
    for (TypeElement type : protoFile.typeElements()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(protoPackage, type.name());
      types.add(WireType.get(protoTypeName, type));
    }
    this.types = Collections.unmodifiableList(types);

    List<WireService> services = new ArrayList<WireService>();
    for (ServiceElement service : protoFile.services()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(protoPackage, service.name());
      services.add(new WireService(protoTypeName, service));
    }
    this.services = Collections.unmodifiableList(services);

    List<WireExtend> wireExtends = new ArrayList<WireExtend>();
    for (ExtendElement extend : protoFile.extendDeclarations()) {
      wireExtends.add(new WireExtend(extend));
    }
    this.wireExtends = Collections.unmodifiableList(wireExtends);

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : protoFile.options()) {
      options.add(new WireOption(option));
    }
    this.options = Collections.unmodifiableList(options);
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

  public void link(Linker linker) {
    linker = linker.withProtoPackage(packageName());
    for (WireType type : types) {
      type.link(linker);
    }
    for (WireService service : services) {
      service.link(linker);
    }
    for (WireExtend extend : wireExtends) {
      extend.link(linker);
    }
    for (WireOption option : options) {
      option.link(ProtoTypeName.FILE_OPTIONS, linker);
    }
  }
}
