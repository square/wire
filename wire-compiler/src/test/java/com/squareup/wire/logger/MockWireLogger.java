package com.squareup.wire.logger;

import com.squareup.wire.OutputArtifact;


public class MockWireLogger implements WireLogger {

  @Override public void error(String message) {
  }

  @Override public void artifact(OutputArtifact artifact) {
  }

  @Override public void info(String message) {
  }
}
