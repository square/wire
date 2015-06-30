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

import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class WireService {
  private final ProtoTypeName protoTypeName;
  private final ServiceElement element;
  private final List<WireRpc> rpcs;
  private final List<WireOption> options;

  private WireService(ProtoTypeName protoTypeName, ServiceElement element, List<WireRpc> rpcs,
      List<WireOption> options) {
    this.protoTypeName = protoTypeName;
    this.element = element;
    this.rpcs = Collections.unmodifiableList(rpcs);
    this.options = Collections.unmodifiableList(options);
  }

  public static WireService get(ProtoTypeName protoTypeName, ServiceElement element) {
    List<WireRpc> rpcs = new ArrayList<WireRpc>();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new WireRpc(protoTypeName.packageName(), rpc));
    }

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : element.options()) {
      options.add(new WireOption(protoTypeName.packageName(), option));
    }

    return new WireService(protoTypeName, element, rpcs, options);
  }

  public ProtoTypeName protoTypeName() {
    return protoTypeName;
  }

  public String documentation() {
    return element.documentation();
  }

  public List<WireRpc> rpcs() {
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

  public List<WireOption> options() {
    return options;
  }

  void link(Linker linker) {
    for (WireRpc rpc : rpcs) {
      rpc.link(linker);
    }
    for (WireOption option : options) {
      option.link(ProtoTypeName.SERVICE_OPTIONS, linker);
    }
  }

  WireService retainAll(Set<String> identifiers) {
    String serviceName = protoTypeName.toString();
    if (identifiers.contains(serviceName)) {
      return this; // Fully retained.
    }

    List<WireRpc> retainedRpcs = new ArrayList<WireRpc>();
    for (WireRpc rpc : rpcs) {
      if (identifiers.contains(serviceName + '#' + rpc.name())) {
        retainedRpcs.add(rpc);
      }
    }

    // If child RPCs are retained, return a subset of this service.
    if (!retainedRpcs.isEmpty()) {
      return new WireService(protoTypeName, element, retainedRpcs, options);
    }

    // Neither this service, nor any of its RPCs are retained.
    return null;
  }
}
