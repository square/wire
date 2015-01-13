package com.squareup.wire;

final class StringWireLogger implements WireLogger {
  private final boolean isQuiet;
  private StringBuilder buffer = new StringBuilder();

  public StringWireLogger(boolean quiet) {
    this.isQuiet = quiet;
  }

  @Override public void error(String message) {
    buffer.append(message);
    buffer.append('\n');
  }

  @Override public void artifact(OutputArtifact artifact) {
    buffer.append(artifact.file().toString());
    buffer.append('\n');
  }

  @Override public void info(String message) {
    if (!isQuiet) {
      buffer.append(message);
      buffer.append('\n');
    }
  }

  public String getLog() {
    return buffer.toString();
  }
}
