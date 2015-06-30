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

import com.squareup.protoparser.EnumConstantElement;
import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.OneOfElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class WireType {
  public abstract ProtoTypeName protoTypeName();
  public abstract String documentation();
  public abstract List<WireOption> options();
  public abstract List<WireType> nestedTypes();
  abstract void link(Linker linker);
  abstract WireType retainAll(Set<String> identifiers);

  static WireType get(ProtoTypeName protoTypeName, TypeElement type) {
    if (type instanceof EnumElement) {
      EnumElement enumElement = (EnumElement) type;

      List<WireEnumConstant> constants = new ArrayList<WireEnumConstant>();
      for (EnumConstantElement constant : enumElement.constants()) {
        constants.add(new WireEnumConstant(protoTypeName.packageName(), constant));
      }

      List<WireOption> options = new ArrayList<WireOption>();
      for (OptionElement option : enumElement.options()) {
        options.add(new WireOption(protoTypeName.packageName(), option));
      }

      return new WireEnum(protoTypeName, enumElement, constants, options);

    } else if (type instanceof MessageElement) {
      MessageElement messageElement = (MessageElement) type;
      String packageName = protoTypeName.packageName();

      List<WireField> fields = new ArrayList<WireField>();
      for (FieldElement field : messageElement.fields()) {
        fields.add(new WireField(packageName, field));
      }

      List<WireOneOf> oneOfs = new ArrayList<WireOneOf>();
      for (OneOfElement oneOf : messageElement.oneOfs()) {
        oneOfs.add(new WireOneOf(packageName, oneOf));
      }

      List<WireType> nestedTypes = new ArrayList<WireType>();
      for (TypeElement nestedType : messageElement.nestedElements()) {
        nestedTypes.add(WireType.get(protoTypeName.nestedType(nestedType.name()), nestedType));
      }

      List<WireOption> options = new ArrayList<WireOption>();
      for (OptionElement option : messageElement.options()) {
        options.add(new WireOption(packageName, option));
      }

      return new WireMessage(protoTypeName, messageElement, fields, oneOfs, nestedTypes, options);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }
}
