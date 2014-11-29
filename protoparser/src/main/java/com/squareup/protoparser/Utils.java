package com.squareup.protoparser;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

final class Utils {
  static void appendDocumentation(StringBuilder builder, String documentation) {
    if (documentation.isEmpty()) {
      return;
    }
    for (String line : documentation.split("\n")) {
      builder.append("// ").append(line).append('\n');
    }
  }

  static void appendIndented(StringBuilder builder, String value) {
    for (String line : value.split("\n")) {
      builder.append("  ").append(line).append('\n');
    }
  }

  static <T> List<T> immutableCopyOf(List<T> list, String name) {
    return unmodifiableList(new ArrayList<>(checkNotNull(list, name)));
  }

  static <T> T checkNotNull(T value, String name) {
    if (value == null) {
      throw new NullPointerException(name);
    }
    return value;
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
