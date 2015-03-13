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

  static <T> List<T> immutableCopyOf(List<T> list) {
    return unmodifiableList(new ArrayList<>(list));
  }

  static <T> T checkNotNull(T value, String name) {
    if (value == null) {
      throw new NullPointerException(name + " == null");
    }
    return value;
  }

  static void checkArgument(boolean condition, String message, Object... messageArgs) {
    if (!condition) {
      if (messageArgs.length > 0) {
        message = String.format(message, messageArgs);
      }
      throw new IllegalArgumentException(message);
    }
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
