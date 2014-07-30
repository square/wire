package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
import com.squareup.wire.compiler.plugin.java.services.RxJavaServiceWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class TestRxJavaServiceWriter extends RxJavaServiceWriter {
  public TestRxJavaServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override public void emitService(Service service, Set<String> importedTypes) throws IOException {
    String serviceNameSuffix = options.get(0);
    Service overriddenService = new Service(service.getName() + serviceNameSuffix,
        service.getFullyQualifiedName() + serviceNameSuffix, service.getDocumentation(),
        service.getOptions(), service.getMethods());
    super.emitService(overriddenService, importedTypes);
  }
}
