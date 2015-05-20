// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class MessageElement implements TypeElement {
  static void validateFieldTagUniqueness(String qualifiedName, List<FieldElement> fields,
      List<OneOfElement> oneOfs) {
    List<FieldElement> allFields = new ArrayList<>(fields);
    for (OneOfElement oneOf : oneOfs) {
      allFields.addAll(oneOf.fields());
    }

    Set<Integer> tags = new LinkedHashSet<>();
    for (FieldElement field : allFields) {
      int tag = field.tag();
      if (!tags.add(tag)) {
        throw new IllegalStateException("Duplicate tag " + tag + " in " + qualifiedName);
      }
    }
  }

  static void validateFieldLabel(String qualifiedName, List<FieldElement> fields) {
    for (FieldElement field : fields) {
      if (field.label() == FieldElement.Label.ONE_OF) {
        throw new IllegalStateException("Field '"
            + field.name()
            + "' in "
            + qualifiedName
            + " improperly declares itself a member of a 'oneof' group but is not.");
      }
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  MessageElement() {
  }

  @Override public abstract String name();
  @Override public abstract String qualifiedName();
  @Override public abstract String documentation();
  public abstract List<FieldElement> fields();
  public abstract List<OneOfElement> oneOfs();
  @Override public abstract List<TypeElement> nestedElements();
  public abstract List<ExtensionsElement> extensions();
  @Override public abstract List<OptionElement> options();

  @Override public final String toSchema() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("message ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toSchemaDeclaration());
      }
    }
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (FieldElement field : fields()) {
        appendIndented(builder, field.toSchema());
      }
    }
    if (!oneOfs().isEmpty()) {
      builder.append('\n');
      for (OneOfElement oneOf : oneOfs()) {
        appendIndented(builder, oneOf.toSchema());
      }
    }
    if (!extensions().isEmpty()) {
      builder.append('\n');
      for (ExtensionsElement extension : extensions()) {
        appendIndented(builder, extension.toSchema());
      }
    }
    if (!nestedElements().isEmpty()) {
      builder.append('\n');
      for (TypeElement type : nestedElements()) {
        appendIndented(builder, type.toSchema());
      }
    }
    return builder.append("}\n").toString();
  }

  public static final class Builder {
    private String name;
    private String qualifiedName;
    private String documentation = "";
    private final List<FieldElement> fields = new ArrayList<>();
    private final List<OneOfElement> oneOfs = new ArrayList<>();
    private final List<TypeElement> nestedElements = new ArrayList<>();
    private final List<ExtensionsElement> extensions = new ArrayList<>();
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

    public Builder addField(FieldElement field) {
      fields.add(checkNotNull(field, "field"));
      return this;
    }

    public Builder addFields(Collection<FieldElement> fields) {
      for (FieldElement field : checkNotNull(fields, "fields")) {
        addField(field);
      }
      return this;
    }

    public Builder addOneOf(OneOfElement oneOf) {
      oneOfs.add(checkNotNull(oneOf, "oneOf"));
      return this;
    }

    public Builder addOneOfs(Collection<OneOfElement> oneOfs) {
      for (OneOfElement oneOf : checkNotNull(oneOfs, "oneOfs")) {
        addOneOf(oneOf);
      }
      return this;
    }

    public Builder addType(TypeElement type) {
      nestedElements.add(checkNotNull(type, "type"));
      return this;
    }

    public Builder addTypes(Collection<TypeElement> types) {
      for (TypeElement type : checkNotNull(types, "types")) {
        addType(type);
      }
      return this;
    }

    public Builder addExtensions(ExtensionsElement extensions) {
      this.extensions.add(checkNotNull(extensions, "extensions"));
      return this;
    }

    public Builder addExtensions(Collection<ExtensionsElement> extensions) {
      for (ExtensionsElement extension : checkNotNull(extensions, "extensions")) {
        addExtensions(extension);
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

    public MessageElement build() {
      checkNotNull(name, "name");
      checkNotNull(qualifiedName, "qualifiedName");

      validateFieldTagUniqueness(qualifiedName, fields, oneOfs);
      validateFieldLabel(qualifiedName, fields);
      EnumElement.validateValueUniquenessInScope(qualifiedName, nestedElements);

      return new AutoValue_MessageElement(name, qualifiedName, documentation,
          immutableCopyOf(fields), immutableCopyOf(oneOfs), immutableCopyOf(nestedElements),
          immutableCopyOf(extensions), immutableCopyOf(options));
    }
  }
}
