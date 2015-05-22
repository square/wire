// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
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

  @Override List<String> getImports(ServiceElement service) {
    if (!service.rpcs().isEmpty()) {
      return Arrays.asList("retrofit.http.Body", "retrofit.http.POST");
    }
    return Collections.emptyList();
  }

  @Override void emitAnnotation(ServiceElement service, RpcElement rpc) throws IOException {
    writer.emitAnnotation("POST",
        "\"/" + service.qualifiedName() + "/" + rpc.name() + "\"");
  }

  @Override String getRequestType(String requestType) {
    return "@Body " + requestType;
  }

  @Override String getRequestName(String requestType) {
    return "request";
  }
}
