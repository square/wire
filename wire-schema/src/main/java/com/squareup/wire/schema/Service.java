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
  private final WireType wireType;
  private final ServiceElement element;
  private final ImmutableList<Rpc> rpcs;
  private final Options options;

  private Service(WireType wireType, ServiceElement element,
      ImmutableList<Rpc> rpcs, Options options) {
    this.wireType = wireType;
    this.element = element;
    this.rpcs = rpcs;
    this.options = options;
  }

  public static Service get(WireType wireType, ServiceElement element) {
    ImmutableList.Builder<Rpc> rpcs = ImmutableList.builder();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new Rpc(wireType.packageName(), rpc));
    }

    Options options = new Options(
        WireType.SERVICE_OPTIONS, wireType.packageName(), element.options());

    return new Service(wireType, element, rpcs.build(), options);
  }

  public Location location() {
    return element.location();
  }

  public WireType type() {
    return wireType;
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

  void validate(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.validate(linker);
    }
  }

  Service retainAll(Set<String> identifiers) {
    String serviceName = wireType.toString();
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
      return new Service(wireType, element, retainedRpcs, options);
    }

    // Neither this service, nor any of its RPCs are retained.
    return null;
  }
}
