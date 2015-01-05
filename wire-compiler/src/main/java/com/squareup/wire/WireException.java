package com.squareup.wire;

public class WireException extends Exception {
  public WireException(String message) {
    super(message);
  }

  public WireException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
