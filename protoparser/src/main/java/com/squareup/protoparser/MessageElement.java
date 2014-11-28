// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.squareup.protoparser.ProtoFile.isValidTag;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.checkNotNull;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class MessageElement implements TypeElement {
  static void validateFieldTagUniqueness(String qualifiedName, List<FieldElement> fields,
      List<OneOfElement> oneOfs) {
    checkNotNull(qualifiedName, "qualifiedName");

    List<FieldElement> allFields = new ArrayList<FieldElement>(fields);
    for (OneOfElement oneOf : oneOfs) {
      allFields.addAll(oneOf.fields());
    }

    Set<Integer> tags = new LinkedHashSet<Integer>();
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

  @AutoValue
  public abstract static class FieldElement {
    public static FieldElement create(Label label, String type, String name, int tag,
        String documentation, List<OptionElement> options) {
      if (!isValidTag(tag)) throw new IllegalArgumentException("Illegal tag value: " + tag);
      return new AutoValue_MessageElement_FieldElement(label, type, name, tag, documentation,
          immutableCopyOf(options, "options"));
    }

    FieldElement() {
    }

    public abstract Label label();
    /**
     * Returns the type of this field. May be a message type name, an enum type
     * name, or a <a href="https://developers.google.com/protocol-buffers/docs/proto#scalar">
     * scalar value type</a> like {@code int64} or {@code bytes}.
     */
    public abstract String type();
    public abstract String name();
    public abstract int tag();
    public abstract String documentation();
    public abstract List<OptionElement> options();

    /** Returns true when the {@code deprecated} option is present and set to true. */
    public final boolean isDeprecated() {
      OptionElement deprecatedOption = OptionElement.findByName(options(), "deprecated");
      return deprecatedOption != null && "true".equals(deprecatedOption.value());
    }

    /** Returns true when the {@code packed} option is present and set to true. */
    public final boolean isPacked() {
      OptionElement packedOption = OptionElement.findByName(options(), "packed");
      return packedOption != null && "true".equals(packedOption.value());
    }

    /** Returns the {@code default} option value or {@code null}. */
    public final String getDefault() {
      OptionElement defaultOption = OptionElement.findByName(options(), "default");
      return defaultOption != null ? (String) defaultOption.value() : null;
    }

    @Override public final String toString() {
      StringBuilder builder = new StringBuilder();
      appendDocumentation(builder, documentation());
      if (label() != Label.ONE_OF) {
        builder.append(label().name().toLowerCase(Locale.US)).append(' ');
      }
      builder.append(type())
          .append(' ')
          .append(name())
          .append(" = ")
          .append(tag());
      if (!options().isEmpty()) {
        builder.append(" [\n");
        for (OptionElement option : options()) {
          appendIndented(builder, option.toString());
        }
        builder.append(']');
      }
      return builder.append(";\n").toString();
    }
  }

  @AutoValue
  public abstract static class OneOfElement {
    public static OneOfElement create(String name, String documentation,
        List<FieldElement> fields) {
      return new AutoValue_MessageElement_OneOfElement(name, documentation,
          immutableCopyOf(fields, "fields"));
    }

    OneOfElement() {
    }

    public abstract String name();
    public abstract String documentation();
    public abstract List<FieldElement> fields();

    @Override public final String toString() {
      StringBuilder builder = new StringBuilder();
      appendDocumentation(builder, documentation());
      builder.append("oneof ").append(name()).append(" {");
      if (!fields().isEmpty()) {
        builder.append('\n');
        for (FieldElement field : fields()) {
          appendIndented(builder, field.toString());
        }
      }
      return builder.append("}\n").toString();
    }
  }
}
