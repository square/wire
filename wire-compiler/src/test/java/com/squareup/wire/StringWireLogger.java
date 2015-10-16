package com.squareup.wire;

import com.squareup.javapoet.JavaFile;
import java.nio.file.Path;

final class StringWireLogger implements WireLogger {
  private boolean quiet;
  private StringBuilder buffer = new StringBuilder();

  @Override public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  @Override public void artifact(Path outputPath, JavaFile javaFile) {
    buffer.append(outputPath);
    buffer.append(" ");
    buffer.append(javaFile.packageName);
    buffer.append(".");
    buffer.append(javaFile.typeSpec.name);
    buffer.append('\n');
  }

  @Override public void info(String message) {
    if (!quiet) {
      buffer.append(message);
      buffer.append('\n');
    }
  }

  public String getLog() {
    return buffer.toString();
  }
}
