// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
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
    checkNotNull(qualifiedName, "qualifiedName");

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
      if (field.label() == Label.ONE_OF) {
        throw new IllegalStateException("Field '"
            + field.name()
            + "' in "
            + qualifiedName
            + " improperly declares itself a member of a 'oneof' group but is not.");
      }
    }
  }

  public static MessageElement create(String name, String qualifiedName, String documentation,
      List<FieldElement> fields, List<OneOfElement> oneOfs, List<TypeElement> nestedElements,
      List<ExtensionsElement> extensions, List<OptionElement> options) {
    validateFieldTagUniqueness(qualifiedName, fields, oneOfs);
    validateFieldLabel(qualifiedName, fields);
    EnumElement.validateValueUniquenessInScope(qualifiedName, nestedElements);

    return new AutoValue_MessageElement(name, qualifiedName, documentation,
        immutableCopyOf(fields, "fields"), immutableCopyOf(oneOfs, "oneOfs"),
        immutableCopyOf(nestedElements, "nestedElements"),
        immutableCopyOf(extensions, "extensions"), immutableCopyOf(options, "options"));
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

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("message ")
        .append(name())
        .append(" {");
    if (!options().isEmpty()) {
      builder.append('\n');
      for (OptionElement option : options()) {
        appendIndented(builder, option.toDeclaration());
      }
    }
    if (!fields().isEmpty()) {
      builder.append('\n');
      for (FieldElement field : fields()) {
        appendIndented(builder, field.toString());
      }
    }
    if (!oneOfs().isEmpty()) {
      builder.append('\n');
      for (OneOfElement oneOf : oneOfs()) {
        appendIndented(builder, oneOf.toString());
      }
    }
    if (!extensions().isEmpty()) {
      builder.append('\n');
      for (ExtensionsElement extension : extensions()) {
        appendIndented(builder, extension.toString());
      }
    }
    if (!nestedElements().isEmpty()) {
      builder.append('\n');
      for (TypeElement type : nestedElements()) {
        appendIndented(builder, type.toString());
      }
    }
    return builder.append("}\n").toString();
  }

  public enum Label {
    OPTIONAL, REQUIRED, REPEATED,
    /** Indicates the field is a member of a {@code oneof} block. */
    ONE_OF
  }
}
