package com.squareup.wire;

import com.squareup.javapoet.JavaFile;
import java.io.File;

interface WireLogger {
  void error(String message);
  void artifact(File outputDirectory, JavaFile javaFile);
  void info(String message);
}
