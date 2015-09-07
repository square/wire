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
import com.squareup.wire.internal.protoparser.EnumConstantElement;
import com.squareup.wire.internal.protoparser.EnumElement;
import com.squareup.wire.internal.protoparser.ExtensionsElement;
import com.squareup.wire.internal.protoparser.FieldElement;
import com.squareup.wire.internal.protoparser.MessageElement;
import com.squareup.wire.internal.protoparser.OneOfElement;
import com.squareup.wire.internal.protoparser.TypeElement;
import java.util.NavigableSet;

public abstract class Type {
  public abstract Location location();
  public abstract WireType name();
  public abstract String documentation();
  public abstract Options options();
  public abstract ImmutableList<Type> nestedTypes();
  abstract void validate(Linker linker);
  abstract void link(Linker linker);
  abstract void linkOptions(Linker linker);
  abstract Type retainAll(NavigableSet<String> identifiers);

  static Type get(String packageName, WireType wireType, TypeElement type) {
    if (type instanceof EnumElement) {
      EnumElement enumElement = (EnumElement) type;

      ImmutableList.Builder<EnumConstant> constants = ImmutableList.builder();
      for (EnumConstantElement constant : enumElement.constants()) {
        constants.add(new EnumConstant(constant));
      }

      Options options = new Options(WireType.ENUM_OPTIONS, enumElement.options());

      return new EnumType(wireType, enumElement, constants.build(), options);

    } else if (type instanceof MessageElement) {
      MessageElement messageElement = (MessageElement) type;

      ImmutableList.Builder<Field> fields = ImmutableList.builder();
      for (FieldElement field : messageElement.fields()) {
        fields.add(new Field(packageName, field));
      }

      ImmutableList.Builder<OneOf> oneOfs = ImmutableList.builder();
      for (OneOfElement oneOf : messageElement.oneOfs()) {
        oneOfs.add(new OneOf(packageName, oneOf));
      }

      ImmutableList.Builder<Type> nestedTypes = ImmutableList.builder();
      for (TypeElement nestedType : messageElement.nestedTypes()) {
        nestedTypes.add(Type.get(packageName, wireType.nestedType(nestedType.name()), nestedType));
      }

      ImmutableList.Builder<Extensions> extensionsList = ImmutableList.builder();
      for (ExtensionsElement element : messageElement.extensions()) {
        extensionsList.add(new Extensions(element));
      }

      Options options = new Options(WireType.MESSAGE_OPTIONS, messageElement.options());

      return new MessageType(wireType, messageElement, fields.build(), oneOfs.build(),
          nestedTypes.build(), extensionsList.build(), options);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }
}
