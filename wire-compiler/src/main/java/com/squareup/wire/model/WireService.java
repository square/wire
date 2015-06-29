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

public final class WireService {
  private final ProtoTypeName protoTypeName;
  private final ServiceElement element;
  private final List<WireRpc> rpcs;
  private final List<WireOption> options;

  WireService(ProtoTypeName protoTypeName, ServiceElement element) {
    this.protoTypeName = protoTypeName;
    this.element = element;

    List<WireRpc> rpcs = new ArrayList<WireRpc>();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new WireRpc(rpc));
    }
    this.rpcs = Collections.unmodifiableList(rpcs);

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : element.options()) {
      options.add(new WireOption(option));
    }
    this.options = Collections.unmodifiableList(options);
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

  public List<WireOption> options() {
    return options;
  }

  void link(Linker linker) {
    for (WireRpc rpc : rpcs) {
      rpc.link(linker);
    }
  }
}
