package com.squareup.wire;

final class ConsoleWireLogger implements WireLogger {
  private final boolean isQuiet;

  public ConsoleWireLogger(boolean quiet) {
    this.isQuiet = quiet;
  }

  public void info(String message) {
    if (!isQuiet) {
      System.out.println(message);
    }
  }

  public void artifact(OutputArtifact artifact) {
    String msg;
    if (isQuiet) {
      msg = artifact.file().toString();
    } else {
      msg = "Writing generated code to " + artifact.file().toString();
    }
    System.out.println(msg);
  }

  public void error(String message) {
    System.err.println(message);
  }
}
