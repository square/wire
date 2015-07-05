package com.squareup.wire;

final class Preconditions {
  private Preconditions() {
  }

  /** Throw {@link NullPointerException} with {@code message} if {@code o} is null. */
  static void checkNotNull(Object o, String message) {
    if (o == null) {
      throw new NullPointerException(message);
    }
  }
}
