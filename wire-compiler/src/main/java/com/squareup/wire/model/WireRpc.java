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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WireRpc {
  private final String packageName;
  private final RpcElement element;
  private final List<WireOption> options;
  private ProtoTypeName requestType;
  private ProtoTypeName responseType;

  WireRpc(String packageName, RpcElement element) {
    this.packageName = packageName;
    this.element = element;

    List<WireOption> options = new ArrayList<WireOption>();
    for (OptionElement option : element.options()) {
      options.add(new WireOption(packageName, option));
    }
    this.options = Collections.unmodifiableList(options);
  }

  public String packageName() {
    return packageName;
  }

  public String name() {
    return element.name();
  }

  public String documentation() {
    return element.documentation();
  }

  public ProtoTypeName requestType() {
    return requestType;
  }

  public ProtoTypeName responseType() {
    return responseType;
  }

  public List<WireOption> options() {
    return options;
  }

  void link(Linker linker) {
    requestType = linker.resolveNamedType(packageName, element.requestType().name());
    responseType = linker.resolveNamedType(packageName, element.responseType().name());
    for (WireOption option : options) {
      option.link(ProtoTypeName.METHOD_OPTIONS, linker);
    }
  }
}
