package com.squareup.wire;

final class Preconditions {
  private Preconditions() {
  }

  /** Throw {@link NullPointerException} for variable {@code name} if {@code o} is null. */
  static void checkNotNull(Object o, String name) {
    if (o == null) {
      throw new NullPointerException(name + " == null");
    }
  }

  /** Throw {@link IllegalArgumentException} with {@code message} if {@code assertion} is false. */
  static void checkArgument(boolean assertion, String message) {
    if (!assertion) {
      throw new IllegalArgumentException(message);
    }
  }
}
