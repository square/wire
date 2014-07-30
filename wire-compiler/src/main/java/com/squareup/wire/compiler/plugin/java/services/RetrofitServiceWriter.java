// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java.services;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A ServiceWriter that generates simple Retrofit-compatible interfaces.
 */
public class RetrofitServiceWriter extends AbstractServiceWriter {

  public RetrofitServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override List<String> getImports(Service service) {
    if (!service.getMethods().isEmpty()) {
      return Arrays.asList("retrofit.http.Body", "retrofit.http.POST");
    }
    return Collections.emptyList();
  }

  @Override void emitAnnotation(Service service, Service.Method method) throws IOException {
    writer.emitAnnotation("POST",
        "\"/" + service.getFullyQualifiedName() + "/" + method.getName() + "\"");
  }

  @Override String getRequestType(String requestType) {
    return "@Body " + requestType;
  }

  @Override String getRequestName(String requestType) {
    return "request";
  }
}
