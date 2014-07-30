// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java.services;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An extremely simple example of a ServiceWriter.
 */
public class SimpleServiceWriter extends AbstractServiceWriter {

  public SimpleServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override List<String> getImports(Service service) {
    if (!service.getMethods().isEmpty()) {
      return Arrays.asList("java.io.IOException");
    }
    return Collections.emptyList();
  }

  @Override List<String> getThrows() {
    return Arrays.asList("java.io.IOException");
  }

  @Override String getRequestName(String requestType) {
    return lowerCaseInitialLetter(requestType);
  }
}
