package com.squareup.wire;

import com.squareup.javapoet.JavaFile;
import java.io.File;

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

  public void artifact(File outputDirectory, JavaFile javaFile) {
    if (isQuiet) {
      System.out.printf("%s.%s%n",
          javaFile.packageName, javaFile.typeSpec.name);
    } else {
      System.out.printf("Writing %s.%s to %s%n",
          javaFile.packageName, javaFile.typeSpec.name, outputDirectory);
    }
  }

  public void error(String message) {
    System.err.println(message);
  }
}
