// Copyright 2013 Square, Inc.
package com.squareup.wire;

/**
 * An exception thrown by the {@link WireCompiler}.
 */
public class WireCompilerException extends RuntimeException {
  public WireCompilerException(String s) {
    super(s);
  }
}
