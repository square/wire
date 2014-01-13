// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Interface to abstract file reads and writes, may be mocked for testing.
 */
interface IO {
  /**
   * Parses the given file.
   */
  ProtoFile parse(String filename) throws IOException;

  /**
   * Returns a JavaWriter for a given class. The output will be written to:
   *
   * <pre>{@code
   *   <output directory>/<java package converted to slashed form>/<className>.java
   * }</pre>
   */
  JavaWriter getJavaWriter(String outputDirectory, String javaPackage, String className)
      throws IOException;

  /**
   * Concrete implementation of the IO interface that proxies to the file system.
   */
  class FileIO implements IO {
    private static final Charset UTF_8 = Charset.forName("UTF8");

    @Override
    public ProtoFile parse(String filename) throws IOException {
      return ProtoSchemaParser.parse(filename,
          new InputStreamReader(new FileInputStream(filename), UTF_8));
    }

    @Override
    public JavaWriter getJavaWriter(String outputDirectory, String javaPackage, String className)
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
