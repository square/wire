// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Locale;

import static com.squareup.protoparser.ProtoFile.isValidTag;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.appendIndented;
import static com.squareup.protoparser.Utils.immutableCopyOf;

@AutoValue
public abstract class FieldElement {
  public static FieldElement create(MessageElement.Label label, String type, String name, int tag,
      String documentation, List<OptionElement> options) {
    if (!isValidTag(tag)) throw new IllegalArgumentException("Illegal tag value: " + tag);
    return new AutoValue_FieldElement(label, type, name, tag, documentation,
        immutableCopyOf(options, "options"));
  }

  FieldElement() {
  }

  public abstract MessageElement.Label label();
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
    if (label() != MessageElement.Label.ONE_OF) {
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
