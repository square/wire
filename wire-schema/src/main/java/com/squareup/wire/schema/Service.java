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
import com.squareup.wire.internal.protoparser.RpcElement;
import com.squareup.wire.internal.protoparser.ServiceElement;
import java.util.Set;

public final class Service {
  private final Type.Name name;
  private final ServiceElement element;
  private final ImmutableList<Rpc> rpcs;
  private final Options options;

  private Service(Type.Name name, ServiceElement element,
      ImmutableList<Rpc> rpcs, Options options) {
    this.name = name;
    this.element = element;
    this.rpcs = rpcs;
    this.options = options;
  }

  public static Service get(Type.Name name, ServiceElement element) {
    ImmutableList.Builder<Rpc> rpcs = ImmutableList.builder();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new Rpc(name.packageName(), rpc));
    }

    Options options = new Options(
        Type.Name.SERVICE_OPTIONS, name.packageName(), element.options());

    return new Service(name, element, rpcs.build(), options);
  }

  public Location location() {
    return element.location();
  }

  public Type.Name name() {
    return name;
  }

  public String documentation() {
    return element.documentation();
  }

  public ImmutableList<Rpc> rpcs() {
    return rpcs;
  }

  /** Returns the RPC named {@code name}, or null if this service has no such method. */
  public Rpc rpc(String name) {
    for (Rpc rpc : rpcs) {
      if (rpc.name().equals(name)) {
        return rpc;
      }
    }
    return null;
  }

  public Options options() {
    return options;
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.linkOptions(linker);
    }
    options.link(linker);
  }

  Service retainAll(Set<String> identifiers) {
    String serviceName = name.toString();
    if (identifiers.contains(serviceName)) {
      return this; // Fully retained.
    }

    ImmutableList.Builder<Rpc> retainedRpcsBuilde = ImmutableList.builder();
    for (Rpc rpc : rpcs) {
      if (identifiers.contains(serviceName + '#' + rpc.name())) {
        retainedRpcsBuilde.add(rpc);
      }
    }

    // If child RPCs are retained, return a subset of this service.
    ImmutableList<Rpc> retainedRpcs = retainedRpcsBuilde.build();
    if (!retainedRpcs.isEmpty()) {
      return new Service(name, element, retainedRpcs, options);
    }

    // Neither this service, nor any of its RPCs are retained.
    return null;
  }
}
