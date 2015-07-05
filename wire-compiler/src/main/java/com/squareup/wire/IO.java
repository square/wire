// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javapoet.JavaFile;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Interface to abstract file reads and writes, may be mocked for testing.
 */
public interface IO {
  /**
   * Parses the given file.
   */
  ProtoFile parse(String filename) throws IOException;

  void write(File outputDirectory, JavaFile javaFile) throws IOException;

  /**
   * Concrete implementation of the IO interface that proxies to the file system.
   */
  class FileIO implements IO {
    private static final Charset UTF_8 = Charset.forName("UTF8");

    @Override public ProtoFile parse(String filename) throws IOException {
      return ProtoParser.parse(filename,
          new InputStreamReader(new FileInputStream(filename), UTF_8));
    }

    @Override public void write(File outputDirectory, JavaFile javaFile) throws IOException {
      javaFile.writeTo(outputDirectory);
    }
  }
}
