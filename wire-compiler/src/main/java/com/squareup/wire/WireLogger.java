package com.squareup.wire;

interface WireLogger {
  void error(String message);
  void artifact(OutputArtifact artifact);
  void info(String message);
}
