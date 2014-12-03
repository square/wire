// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.Collections;
import java.util.List;

import static com.squareup.protoparser.OptionElement.formatOptionList;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.immutableCopyOf;

/** An enum constant. */
@AutoValue
public abstract class EnumConstantElement {
  public static final int UNKNOWN_TAG = -1;

  /** Used to represent enums constants where we just know the name. */
  static EnumConstantElement anonymous(String name) {
    return EnumConstantElement.create(name, UNKNOWN_TAG, "",
        Collections.<OptionElement>emptyList());
  }

  public static EnumConstantElement create(String name, int tag, String documentation,
      List<OptionElement> options) {
    return new AutoValue_EnumConstantElement(name, tag, documentation,
        immutableCopyOf(options, "options"));
  }

  EnumConstantElement() {
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
