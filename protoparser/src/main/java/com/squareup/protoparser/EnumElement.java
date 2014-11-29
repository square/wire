// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.protoparser.OptionElement.formatOptionList;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** An enumerated type declaration. */
@AutoValue
public abstract class EnumElement implements TypeElement {
  private static void validateTagUniqueness(String qualifiedName, List<ValueElement> values) {
    checkNotNull(qualifiedName, "qualifiedName");

    Set<Integer> tags = new LinkedHashSet<>();
    for (ValueElement value : values) {
      int tag = value.tag();
      if (!tags.add(tag)) {
        throw new IllegalStateException("Duplicate tag " + tag + " in " + qualifiedName);
      }
    }
  }

  private static boolean parseAllowAlias(List<OptionElement> options) {
    OptionElement option = OptionElement.findByName(options, "allow_alias");
    if (option != null) {
      Object value = option.value();
      return value instanceof Boolean && (Boolean) value;
    }
    return false;
  }

  /**
   * Though not mentioned in the spec, enum names use C++ scoping rules, meaning that enum values
   * are siblings of their declaring element, not children of it.
   */
  static void validateValueUniquenessInScope(String qualifiedName,
      List<TypeElement> nestedElements) {
    checkNotNull(qualifiedName, "qualifiedName");

    Set<String> names = new LinkedHashSet<>();
    for (TypeElement nestedElement : nestedElements) {
      if (nestedElement instanceof EnumElement) {
        EnumElement enumElement = (EnumElement) nestedElement;
        for (ValueElement value : enumElement.values()) {
          String name = value.name();
          if (!names.add(name)) {
            throw new IllegalStateException(
                "Duplicate enum name " + name + " in scope " + qualifiedName);
          }
        }
      }
    }
  }

  public static EnumElement create(String name, String qualifiedName, String documentation,
      List<OptionElement> options, List<ValueElement> values) {
    if (!parseAllowAlias(options)) {
      validateTagUniqueness(qualifiedName, values);
    }

    return new AutoValue_EnumElement(name, qualifiedName, documentation,
        immutableCopyOf(values, "values"), immutableCopyOf(options, "options"));
  }

  EnumElement() {
  }

  @Override public abstract String name();
  @Override public abstract String qualifiedName();
  @Override public abstract String documentation();
  public abstract List<ValueElement> values();
  @Override public abstract List<OptionElement> options();

  @Override public final List<TypeElement> nestedElements() {
    return Collections.emptyList(); // Enums do not allow nested type declarations.
  }

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("enum ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toDeclaration());
      }
    }
    if (!values().isEmpty()) {
      builder.append('\n');
      for (ValueElement value : values()) {
        appendIndented(builder, value.toString());
      }
    }
    return builder.append("}\n").toString();
  }

  /** An enum constant. */
  @AutoValue
  public abstract static class ValueElement {
    public static final int UNKNOWN_TAG = -1;

    /** Used to represent enums values where we just know the name. */
    static ValueElement anonymous(String name) {
      return ValueElement.create(name, UNKNOWN_TAG, "", Collections.<OptionElement>emptyList());
    }

    public static ValueElement create(String name, int tag, String documentation,
        List<OptionElement> options) {
      return new AutoValue_EnumElement_ValueElement(name, tag, documentation,
          immutableCopyOf(options, "options"));
    }

    ValueElement() {
    }

    public abstract String name();
    public abstract int tag();
    public abstract String documentation();
    public abstract List<OptionElement> options();

    @Override public final String toString() {
      StringBuilder builder = new StringBuilder();
      appendDocumentation(builder, documentation());
      builder.append(name())
          .append(" = ")
          .append(tag());
      if (!options().isEmpty()) {
        builder.append(" [\n");
        formatOptionList(builder, options());
        builder.append(']');
      }
      return builder.append(";\n").toString();
    }
  }
}
