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
import com.squareup.wire.schema.internal.parser.RpcElement;

public final class Rpc {
  private final Location location;
  private final String name;
  private final String documentation;
  private final String requestTypeElement;
  private final String responseTypeElement;
  private final Options options;
  private ProtoType requestType;
  private ProtoType responseType;
  private final boolean requestStreaming;
  private final boolean responseStreaming;

  private Rpc(Location location, String name, String documentation, String requestType,
      String responseType, boolean requestStreaming, boolean responseStreaming, Options options) {
    this.location = location;
    this.name = name;
    this.documentation = documentation;
    this.requestTypeElement = requestType;
    this.responseTypeElement = responseType;
    this.requestStreaming = requestStreaming;
    this.responseStreaming = responseStreaming;
    this.options = options;
  }

  public Location location() {
    return location;
  }

  public String name() {
    return name;
  }

  public String documentation() {
    return documentation;
  }

  public ProtoType requestType() {
    return requestType;
  }

  public ProtoType responseType() {
    return responseType;
  }

  public boolean requestStreaming() {
    return requestStreaming;
  }

  public boolean responseStreaming() {
    return responseStreaming;
  }

  public Options options() {
    return options;
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    requestType = linker.resolveMessageType(requestTypeElement);
    responseType = linker.resolveMessageType(responseTypeElement);
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    options.link(linker);
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    linker.validateImport(location(), requestType);
    linker.validateImport(location(), responseType);
  }

  Rpc retainAll(Schema schema, MarkSet markSet) {
    if (!markSet.contains(requestType) || !markSet.contains(responseType)) return null;
    Rpc result = new Rpc(location, name, documentation, requestTypeElement, responseTypeElement,
            requestStreaming, responseStreaming, options.retainAll(schema, markSet));
    result.requestType = requestType;
    result.responseType = responseType;
    return result;
  }

  static ImmutableList<Rpc> fromElements(ImmutableList<RpcElement> elements) {
    ImmutableList.Builder<Rpc> rpcs = new ImmutableList.Builder<>();
    for (RpcElement rpcElement : elements) {
      rpcs.add(new Rpc(rpcElement.location(), rpcElement.name(), rpcElement.documentation(),
          rpcElement.requestType(), rpcElement.responseType(),
          rpcElement.requestStreaming(), rpcElement.responseStreaming(),
          new Options(Options.METHOD_OPTIONS, rpcElement.options())));
    }
    return rpcs.build();
  }

  static ImmutableList<RpcElement> toElements(ImmutableList<Rpc> rpcs) {
    ImmutableList.Builder<RpcElement> elements = new ImmutableList.Builder<>();
    for (Rpc rpc : rpcs) {
      elements.add(RpcElement.builder(rpc.location)
          .documentation(rpc.documentation)
          .name(rpc.name)
          .requestType(rpc.requestTypeElement)
          .responseType(rpc.responseTypeElement)
          .requestStreaming(rpc.requestStreaming)
          .responseStreaming(rpc.responseStreaming)
          .options(rpc.options.toElements())
          .build());
    }
    return elements.build();
  }
}
