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
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
import java.util.Set;

public final class WireService {
  private final ProtoTypeName protoTypeName;
  private final ServiceElement element;
  private final ImmutableList<WireRpc> rpcs;
  private final Options options;

  private WireService(ProtoTypeName protoTypeName, ServiceElement element,
      ImmutableList<WireRpc> rpcs, Options options) {
    this.protoTypeName = protoTypeName;
    this.element = element;
    this.rpcs = rpcs;
    this.options = options;
  }

  public static WireService get(ProtoTypeName protoTypeName, ServiceElement element) {
    ImmutableList.Builder<WireRpc> rpcs = ImmutableList.builder();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new WireRpc(protoTypeName.packageName(), rpc));
    }

    Options options = new Options(
        ProtoTypeName.SERVICE_OPTIONS, protoTypeName.packageName(), element.options());

    return new WireService(protoTypeName, element, rpcs.build(), options);
  }

  public ProtoTypeName protoTypeName() {
    return protoTypeName;
  }

  public String documentation() {
    return element.documentation();
  }

  public ImmutableList<WireRpc> rpcs() {
    return rpcs;
  }

  /** Returns the RPC named {@code name}, or null if this service has no such method. */
  public WireRpc rpc(String name) {
    for (WireRpc rpc : rpcs) {
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
    for (WireRpc rpc : rpcs) {
      rpc.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    for (WireRpc rpc : rpcs) {
      rpc.linkOptions(linker);
    }
    options.link(linker);
  }

  WireService retainAll(Set<String> identifiers) {
    String serviceName = protoTypeName.toString();
    if (identifiers.contains(serviceName)) {
      return this; // Fully retained.
    }

    ImmutableList.Builder<WireRpc> retainedRpcsBuilde = ImmutableList.builder();
    for (WireRpc rpc : rpcs) {
      if (identifiers.contains(serviceName + '#' + rpc.name())) {
        retainedRpcsBuilde.add(rpc);
      }
    }

    // If child RPCs are retained, return a subset of this service.
    ImmutableList<WireRpc> retainedRpcs = retainedRpcsBuilde.build();
    if (!retainedRpcs.isEmpty()) {
      return new WireService(protoTypeName, element, retainedRpcs, options);
    }

    // Neither this service, nor any of its RPCs are retained.
    return null;
  }
}
