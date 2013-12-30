package com.squareup.protoparser;

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

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
