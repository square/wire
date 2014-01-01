package com.squareup.wire.parser;

final class Preconditions {
  static void checkArgument(boolean value, String message) {
    if (!value) {
      throw new IllegalStateException(message);
    }
  }

  static void checkNotNull(Object o, String message) {
    if (o == null) {
      throw new NullPointerException(message);
    }
  }

  private Preconditions() {
    throw new AssertionError("No instances.");
  }
}
