package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.ServiceElement;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class TestRxJavaServiceWriter extends RxJavaServiceWriter {
  public TestRxJavaServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override public void emitService(ServiceElement service, Set<String> importedTypes) throws IOException {
    String serviceNameSuffix = options.get(0);
    ServiceElement overriddenService = ServiceElement.builder()
        .name(service.name() + serviceNameSuffix)
        .qualifiedName(service.qualifiedName() + serviceNameSuffix)
        .documentation(service.documentation())
        .addOptions(service.options())
        .addRpcs(service.rpcs())
        .build();
    super.emitService(overriddenService, importedTypes);
  }
}
