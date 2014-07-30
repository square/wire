// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.JavaWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * Interface to abstract file reads and writes, may be mocked for testing.
 */
interface IO {

  /**
   * Returns a JavaWriter for a given class. The output will be written to:
   *
   * <pre>{@code
   *   <output directory>/<java package converted to slashed form>/<className>.java
   * }</pre>
   */
  JavaWriter getJavaWriter(Path outputDirectory, String javaPackage, String className)
      throws IOException;

  /**
   * Concrete implementation of the IO interface that proxies to the file system.
   */
  class FileIO implements IO {
    private static final Charset UTF_8 = Charset.forName("UTF8");

    @Override
    public JavaWriter getJavaWriter(Path outputDirectory, String javaPackage, String className)
        throws IOException {
      String directory = outputDirectory + File.separator
          + javaPackage.replace(".", File.separator);
      boolean created = new File(directory).mkdirs();
      if (created) {
        System.out.println("Created output directory " + directory);
      }

      String fileName = directory + File.separator + className + ".java";
      System.out.println("Writing generated code to " + fileName);
      return new JavaWriter(new OutputStreamWriter(new FileOutputStream(fileName), UTF_8));
    }
  }
}
