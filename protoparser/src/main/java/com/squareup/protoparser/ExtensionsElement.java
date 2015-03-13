// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;

import static com.squareup.protoparser.ProtoFile.isValidTag;
import static com.squareup.protoparser.Utils.appendDocumentation;
import static com.squareup.protoparser.Utils.checkArgument;

@AutoValue
public abstract class ExtensionsElement {
  public static ExtensionsElement create(int start, int end) {
    return create(start, end, "");
  }

  public static ExtensionsElement create(int start, int end, String documentation) {
    checkArgument(isValidTag(start), "Invalid start value: %s", start);
    checkArgument(isValidTag(end), "Invalid end value: %s", end);

    return new AutoValue_ExtensionsElement(documentation, start, end);
  }

  ExtensionsElement() {
  }

  public abstract String documentation();
  public abstract int start();
  public abstract int end();

  @Override public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation());
    builder.append("extensions ")
        .append(start());
    if (start() != end()) {
      builder.append(" to ");
      if (end() < ProtoFile.MAX_TAG_VALUE) {
        builder.append(end());
      } else {
        builder.append("max");
      }
    }
    return builder.append(";\n").toString();
  }
}
