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

import com.squareup.protoparser.RpcElement;

public final class WireRpc {
  private final String packageName;
  private final RpcElement element;
  private final Options options;
  private ProtoTypeName requestType;
  private ProtoTypeName responseType;

  WireRpc(String packageName, RpcElement element) {
    this.packageName = packageName;
    this.element = element;
    this.options = new Options(ProtoTypeName.METHOD_OPTIONS, packageName, element.options());
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

  public Options options() {
    return options;
  }

  void link(Linker linker) {
    requestType = linker.resolveNamedType(packageName, element.requestType().name());
    responseType = linker.resolveNamedType(packageName, element.responseType().name());
  }

  void linkOptions(Linker linker) {
    options.link(linker);
  }
}
