// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.ProtoFile;
import java.io.IOException;

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
}
