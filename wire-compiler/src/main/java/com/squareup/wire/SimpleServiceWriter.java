// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.ServiceElement;
import java.util.Collections;
import java.util.List;

/**
 * An extremely simple example of a ServiceWriter.
 */
public class SimpleServiceWriter extends AbstractServiceWriter {

  public SimpleServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override List<String> getImports(ServiceElement service) {
    if (!service.rpcs().isEmpty()) {
      return Collections.singletonList("java.io.IOException");
    }
    return Collections.emptyList();
  }

  @Override List<String> getThrows() {
    return Collections.singletonList("java.io.IOException");
  }

  @Override String getRequestName(String requestType) {
    return lowerCaseInitialLetter(requestType);
  }
}
