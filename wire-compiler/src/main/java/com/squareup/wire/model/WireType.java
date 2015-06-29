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

import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.TypeElement;
import java.util.List;

public abstract class WireType {
  public abstract ProtoTypeName protoTypeName();
  public abstract String documentation();
  public abstract List<WireOption> options();
  public abstract List<WireType> nestedTypes();
  abstract void link(Linker linker);

  static WireType get(ProtoTypeName protoTypeName, TypeElement type) {
    if (type instanceof EnumElement) {
      return new WireEnum(protoTypeName, (EnumElement) type);

    } else if (type instanceof MessageElement) {
      return new WireMessage(protoTypeName, (MessageElement) type);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }
}
