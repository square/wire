// Copyright 2013 Square, Inc.
package com.squareup.wire;

/**
 * An exception thrown from a builder's {@link com.squareup.wire.Message.Builder#build()} method
 * when not all required fields have been set.
 */
public class UninitializedMessageException extends RuntimeException {

  public UninitializedMessageException() {
  }
}
