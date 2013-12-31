// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import static com.squareup.protoparser.ProtoFile.isValidTag;
import static com.squareup.protoparser.Utils.appendDocumentation;

public final class Extensions {
  private final String documentation;
  private final int start;
  private final int end;

  public Extensions(String documentation, int start, int end) {
    if (documentation == null) throw new NullPointerException("documentation");
    if (!isValidTag(start)) throw new IllegalArgumentException("Invalid start value: " + start);
    if (!isValidTag(end)) throw new IllegalArgumentException("Invalid end value: " + end);

    this.documentation = documentation;
    this.start = start;
    this.end = end;
  }

  public String getDocumentation() {
    return documentation;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Extensions)) return false;

    Extensions that = (Extensions) o;
    return end == that.end
        && start == that.start
        && documentation.equals(that.documentation);
  }

  @Override public int hashCode() {
    int result = documentation.hashCode();
    result = 31 * result + start;
    result = 31 * result + end;
    return result;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, documentation);
    builder.append("extensions ")
        .append(start);
    if (start != end) {
      builder.append(" to ");
      if (end < ProtoFile.MAX_TAG_VALUE) {
        builder.append(end);
      } else {
        builder.append("max");
      }
    }
    return builder.append(";\n").toString();
  }
}
