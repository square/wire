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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.wire.internal.Util;
import com.squareup.wire.internal.protoparser.EnumConstantElement;
import com.squareup.wire.internal.protoparser.EnumElement;
import com.squareup.wire.internal.protoparser.ExtensionsElement;
import com.squareup.wire.internal.protoparser.FieldElement;
import com.squareup.wire.internal.protoparser.MessageElement;
import com.squareup.wire.internal.protoparser.OneOfElement;
import com.squareup.wire.internal.protoparser.TypeElement;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class Type {
  public abstract Location location();
  public abstract Name name();
  public abstract String documentation();
  public abstract Options options();
  public abstract ImmutableList<Type> nestedTypes();
  abstract void validate(Linker linker);
  abstract void link(Linker linker);
  abstract void linkOptions(Linker linker);
  abstract Type retainAll(Set<String> identifiers);

  static Type get(Name name, TypeElement type) {
    if (type instanceof EnumElement) {
      EnumElement enumElement = (EnumElement) type;

      ImmutableList.Builder<EnumConstant> constants = ImmutableList.builder();
      for (EnumConstantElement constant : enumElement.constants()) {
        constants.add(new EnumConstant(name.packageName(), constant));
      }

      Options options = new Options(
          Name.ENUM_OPTIONS, name.packageName(), enumElement.options());

      return new EnumType(name, enumElement, constants.build(), options);

    } else if (type instanceof MessageElement) {
      MessageElement messageElement = (MessageElement) type;
      String packageName = name.packageName();

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
        nestedTypes.add(Type.get(name.nestedType(nestedType.name()), nestedType));
      }

      ImmutableList.Builder<Extensions> extensionsList = ImmutableList.builder();
      for (ExtensionsElement element : messageElement.extensions()) {
        extensionsList.add(new Extensions(element));
      }

      Options options = new Options(
          Name.MESSAGE_OPTIONS, name.packageName(), messageElement.options());

      return new MessageType(name, messageElement, fields.build(), oneOfs.build(),
          nestedTypes.build(), extensionsList.build(), options);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type.getClass());
    }
  }

  /**
   * Names a protocol buffer message, enumerated type, service or a scalar. This class models a
   * fully-qualified name using the protocol buffer package.
   */
  public static final class Name {
    public static final Name BOOL = new Name("bool");
    public static final Name BYTES = new Name("bytes");
    public static final Name DOUBLE = new Name("double");
    public static final Name FLOAT = new Name("float");
    public static final Name FIXED32 = new Name("fixed32");
    public static final Name FIXED64 = new Name("fixed64");
    public static final Name INT32 = new Name("int32");
    public static final Name INT64 = new Name("int64");
    public static final Name SFIXED32 = new Name("sfixed32");
    public static final Name SFIXED64 = new Name("sfixed64");
    public static final Name SINT32 = new Name("sint32");
    public static final Name SINT64 = new Name("sint64");
    public static final Name STRING = new Name("string");
    public static final Name UINT32 = new Name("uint32");
    public static final Name UINT64 = new Name("uint64");

    static final Name FILE_OPTIONS = get("google.protobuf", "FileOptions");
    static final Name MESSAGE_OPTIONS = get("google.protobuf", "MessageOptions");
    static final Name FIELD_OPTIONS = get("google.protobuf", "FieldOptions");
    static final Name ENUM_OPTIONS = get("google.protobuf", "EnumOptions");
    static final Name ENUM_VALUE_OPTIONS = get("google.protobuf", "EnumValueOptions");
    static final Name SERVICE_OPTIONS = get("google.protobuf", "ServiceOptions");
    static final Name METHOD_OPTIONS = get("google.protobuf", "MethodOptions");

    private static final ImmutableMap<String, Name> SCALAR_TYPES
        = ImmutableMap.<String, Name>builder()
        .put(BOOL.string, BOOL)
        .put(BYTES.string, BYTES)
        .put(DOUBLE.string, DOUBLE)
        .put(FLOAT.string, FLOAT)
        .put(FIXED32.string, FIXED32)
        .put(FIXED64.string, FIXED64)
        .put(INT32.string, INT32)
        .put(INT64.string, INT64)
        .put(SFIXED32.string, SFIXED32)
        .put(SFIXED64.string, SFIXED64)
        .put(SINT32.string, SINT32)
        .put(SINT64.string, SINT64)
        .put(STRING.string, STRING)
        .put(UINT32.string, UINT32)
        .put(UINT64.string, UINT64)
        .build();

    private final String protoPackage;

    /** A chain of enclosed message names, outermost is first. */
    private final ImmutableList<String> names;
    private final boolean isScalar;
    private final String string;

    private Name(String scalarName) {
      this(null, ImmutableList.of(scalarName), true);
    }

    private Name(String protoPackage, ImmutableList<String> names, boolean isScalar) {
      this.protoPackage = protoPackage;
      this.names = names;
      this.isScalar = isScalar;
      this.string = (protoPackage != null ? (protoPackage + '.') : "") + Joiner.on('.').join(names);
    }

    public String packageName() {
      return protoPackage;
    }

    public String simpleName() {
      return names.get(names.size() - 1);
    }

    /** Returns the enclosing type, or null if this type is not nested in another type. */
    public Name enclosingTypeName() {
      if (names.size() == 1) return null;
      return new Name(protoPackage, names.subList(0, names.size() - 1), isScalar);
    }

    public boolean isScalar() {
      return isScalar;
    }

    public boolean isPackableScalar() {
      return isScalar && !equals(STRING) && !equals(BYTES);
    }

    public boolean isFieldOptions() {
      return equals(FIELD_OPTIONS);
    }

    public boolean isMessageOptions() {
      return equals(MESSAGE_OPTIONS);
    }

    public static Name get(String protoPackage, String name) {
      checkNotNull(name, "name");
      return new Name(protoPackage, ImmutableList.of(name), false);
    }

    public static Name getScalar(String name) {
      return SCALAR_TYPES.get(name);
    }

    public Name nestedType(String name) {
      checkState(!isScalar);
      checkNotNull(name, "name");
      return new Name(protoPackage, Util.concatenate(names, name), false);
    }

    @Override public boolean equals(Object o) {
      return o instanceof Name
          && Objects.equals(((Name) o).string, string);
    }

    @Override public int hashCode() {
      return string.hashCode();
    }

    @Override public String toString() {
      return string;
    }
  }
}
