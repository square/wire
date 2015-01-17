package com.squareup.wire;

import java.io.File;

/**
 * A container class that represents an artifact output from the compiler.
 */
final class OutputArtifact {
  private final String outputDirectory;
  private final String className;
  private final String javaPackage;

  public OutputArtifact(String outputDirectory, String javaPackage, String className) {
    this.outputDirectory = outputDirectory;
    this.className = className;
    this.javaPackage = javaPackage;
  }

  public String outputDirectory() {
    return outputDirectory;
  }

  public String className() {
    return className;
  }

  public String javaPackage() {
    return javaPackage;
  }

  public File file() {
    String dir = outputDirectory + File.separator
        + javaPackage.replace(".", File.separator);
    return new File(dir, className + ".java");
  }

  public File dir() {
    return file().getParentFile();
  }

  public String fullClassName() {
    return javaPackage + "." + className;
  }
}
