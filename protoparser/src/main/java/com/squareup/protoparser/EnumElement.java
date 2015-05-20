// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** An enumerated type declaration. */
@AutoValue
public abstract class EnumElement implements TypeElement {
  private static void validateTagUniqueness(String qualifiedName,
      List<EnumConstantElement> constants) {
    checkNotNull(qualifiedName, "qualifiedName");

    Set<Integer> tags = new LinkedHashSet<>();
    for (EnumConstantElement constant : constants) {
      int tag = constant.tag();
      if (!tags.add(tag)) {
        throw new IllegalStateException("Duplicate tag " + tag + " in " + qualifiedName);
      }
    }
  }

  private static boolean parseAllowAlias(List<OptionElement> options) {
    OptionElement option = OptionElement.findByName(options, "allow_alias");
    return option != null && "true".equals(option.value());
  }

  /**
   * Though not mentioned in the spec, enum names use C++ scoping rules, meaning that enum constants
   * are siblings of their declaring element, not children of it.
   */
  static void validateValueUniquenessInScope(String qualifiedName,
      List<TypeElement> nestedElements) {
    Set<String> names = new LinkedHashSet<>();
    for (TypeElement nestedElement : nestedElements) {
      if (nestedElement instanceof EnumElement) {
        EnumElement enumElement = (EnumElement) nestedElement;
        for (EnumConstantElement constant : enumElement.constants()) {
          String name = constant.name();
          if (!names.add(name)) {
            throw new IllegalStateException(
                "Duplicate enum constant " + name + " in scope " + qualifiedName);
          }
        }
      }
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  EnumElement() {
  }

  @Override public abstract String name();
  @Override public abstract String qualifiedName();
  @Override public abstract String documentation();
  public abstract List<EnumConstantElement> constants();
  @Override public abstract List<OptionElement> options();

  @Override public final List<TypeElement> nestedElements() {
    return Collections.emptyList(); // Enums do not allow nested type declarations.
  }

  @Override public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("enum ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchemaDeclaration());
      }
    }
    if (!constants().isEmpty()) {
      builder.append('\n');
      for (EnumConstantElement constant : constants()) {
        appendIndented(builder, constant.toSchema());
      }
    }
    return builder.append("}\n").toString();
  }

  public static final class Builder {
    private String name;
    private String qualifiedName;
    private String documentation = "";
    private final List<EnumConstantElement> constants = new ArrayList<>();
    private final List<OptionElement> options = new ArrayList<>();

    private Builder() {
    }

    public Builder name(String name) {
      this.name = checkNotNull(name, "name");
      if (qualifiedName == null) {
        qualifiedName = name;
      }
      return this;
    }

    public Builder qualifiedName(String qualifiedName) {
      this.qualifiedName = checkNotNull(qualifiedName, "qualifiedName");
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = checkNotNull(documentation, "documentation");
      return this;
    }

    public Builder addConstant(EnumConstantElement constant) {
      constants.add(checkNotNull(constant, "constant"));
      return this;
    }

    public Builder addConstants(Collection<EnumConstantElement> constants) {
      for (EnumConstantElement constant : checkNotNull(constants, "constants")) {
        addConstant(constant);
      }
      return this;
    }

    public Builder addOption(OptionElement option) {
      options.add(checkNotNull(option, "option"));
      return this;
    }

    public Builder addOptions(Collection<OptionElement> options) {
      for (OptionElement option : checkNotNull(options, "options")) {
        addOption(option);
      }
      return this;
    }

    public EnumElement build() {
      checkNotNull(name, "name");
      checkNotNull(qualifiedName, "qualifiedName");

      if (!parseAllowAlias(options)) {
        validateTagUniqueness(qualifiedName, constants);
      }
      return new AutoValue_EnumElement(name, qualifiedName, documentation,
          immutableCopyOf(constants), immutableCopyOf(options));
    }
  }
}
