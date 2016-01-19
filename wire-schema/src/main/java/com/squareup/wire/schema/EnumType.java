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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.squareup.wire.schema.internal.parser.EnumElement;
import java.util.Collection;
import java.util.Map;

import static com.squareup.wire.schema.Options.ENUM_OPTIONS;

public final class EnumType extends Type {
  static final ProtoMember ALLOW_ALIAS = ProtoMember.get(ENUM_OPTIONS, "allow_alias");

  private final ProtoType protoType;
  private final Location location;
  private final String documentation;
  private final String name;
  private final ImmutableList<EnumConstant> constants;
  private final Options options;
  private Object allowAlias;

  private EnumType(ProtoType protoType, Location location, String documentation, String name,
      ImmutableList<EnumConstant> constants, Options options) {
    this.protoType = protoType;
    this.location = location;
    this.documentation = documentation;
    this.name = name;
    this.constants = constants;
    this.options = options;
  }

  @Override public Location location() {
    return location;
  }

  @Override public ProtoType type() {
    return protoType;
  }

  @Override public String documentation() {
    return documentation;
  }

  @Override public Options options() {
    return options;
  }

  @Override public ImmutableList<Type> nestedTypes() {
    return ImmutableList.of(); // Enums do not allow nested type declarations.
  }

  public boolean allowAlias() {
    return "true".equals(allowAlias);
  }

  /** Returns the constant named {@code name}, or null if this enum has no such constant. */
  public EnumConstant constant(String name) {
    for (EnumConstant constant : constants()) {
      if (constant.name().equals(name)) {
        return constant;
      }
    }
    return null;
  }

  /** Returns the constant tagged {@code tag}, or null if this enum has no such constant. */
  public EnumConstant constant(int tag) {
    for (EnumConstant constant : constants()) {
      if (constant.tag() == tag) {
        return constant;
      }
    }
    return null;
  }

  public ImmutableList<EnumConstant> constants() {
    return constants;
  }

  @Override void link(Linker linker) {
  }

  @Override void linkOptions(Linker linker) {
    options.link(linker);
    for (EnumConstant constant : constants) {
      constant.linkOptions(linker);
    }
    allowAlias = options.get(ALLOW_ALIAS);
  }

  @Override void validate(Linker linker) {
    linker = linker.withContext(this);

    if (!"true".equals(allowAlias)) {
      validateTagUniqueness(linker);
    }
  }

  private void validateTagUniqueness(Linker linker) {
    Multimap<Integer, EnumConstant> tagToConstant = LinkedHashMultimap.create();
    for (EnumConstant constant : constants) {
      tagToConstant.put(constant.tag(), constant);
    }

    for (Map.Entry<Integer, Collection<EnumConstant>> entry : tagToConstant.asMap().entrySet()) {
      if (entry.getValue().size() > 1) {
        StringBuilder error = new StringBuilder();
        error.append(String.format("multiple enum constants share tag %s:", entry.getKey()));
        int index = 1;
        for (EnumConstant constant : entry.getValue()) {
          error.append(String.format("\n  %s. %s (%s)",
              index++, constant.name(), constant.location()));
        }
        linker.addError("%s", error);
      }
    }
  }

  @Override Type retainAll(Schema schema, MarkSet markSet) {
    // If this type is not retained, prune it.
    if (!markSet.contains(protoType)) return null;

    ImmutableList.Builder<EnumConstant> retainedConstants = ImmutableList.builder();
    for (EnumConstant constant : constants) {
      if (markSet.contains(ProtoMember.get(protoType, constant.name()))) {
        retainedConstants.add(constant.retainAll(schema, markSet));
      }
    }

    EnumType result = new EnumType(protoType, location, documentation, name,
        retainedConstants.build(), options.retainAll(schema, markSet));
    result.allowAlias = allowAlias;
    return result;
  }

  static EnumType fromElement(ProtoType protoType, EnumElement enumElement) {
    ImmutableList<EnumConstant> constants = EnumConstant.fromElements(enumElement.constants());
    Options options = new Options(Options.ENUM_OPTIONS, enumElement.options());

    return new EnumType(protoType, enumElement.location(), enumElement.documentation(),
        enumElement.name(), constants, options);
  }

  EnumElement toElement() {
    return EnumElement.builder(location)
        .name(name)
        .documentation(documentation)
        .constants(EnumConstant.toElements(constants))
        .options(options.toElements())
        .build();
  }
}
