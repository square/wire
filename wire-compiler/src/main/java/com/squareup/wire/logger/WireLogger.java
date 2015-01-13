package com.squareup.wire.logger;

import com.squareup.wire.OutputArtifact;

public interface WireLogger {
  void error(String message);
  void artifact(OutputArtifact artifact);
  void info(String message);
}
