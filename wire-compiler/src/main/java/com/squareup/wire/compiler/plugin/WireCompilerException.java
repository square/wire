package com.squareup.wire.compiler.plugin;

/**
 * An exception thrown by the Wire compiler.
 */
public class WireCompilerException extends RuntimeException {
  public WireCompilerException(String message) {
    super(message);
  }
}
