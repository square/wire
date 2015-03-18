// Copyright 2013 Square, Inc.
package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.element.Modifier;

abstract class AbstractServiceWriter extends ServiceWriter {

  public AbstractServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override
  public void emitService(ServiceElement service, Set<String> importedTypes) throws IOException {
    importedTypes.addAll(getImports(service));
    writer.emitImports(importedTypes);
    writer.emitEmptyLine();

    if (!service.documentation().isEmpty()) {
      writer.emitJavadoc(service.documentation());
    }
    writer.beginType(service.name(), "interface", EnumSet.of(Modifier.PUBLIC));
    for (RpcElement rpc : service.rpcs()) {
      String requestType = rpc.requestType().toString();
      int index = requestType.lastIndexOf('.');
      if (index != -1) {
        requestType = requestType.substring(index + 1);
      }

      if (!rpc.documentation().isEmpty()) {
        writer.emitJavadoc(rpc.documentation());
      }
      emitAnnotation(service, rpc);
      writer.beginMethod(rpc.responseType().toString(), getRpcName(rpc),
          EnumSet.noneOf(Modifier.class),
          Arrays.asList(getRequestType(requestType), getRequestName(requestType)), getThrows());
      writer.endMethod();
    }
    writer.endType();
  }

  List<String> getImports(ServiceElement service) {
    return null;
  }

  void emitAnnotation(ServiceElement service, RpcElement rpc) throws IOException {
    // no-op
  }

  String getRpcName(RpcElement rpc) {
    return lowerCaseInitialLetter(rpc.name());
  }

  String getRequestName(String requestType) {
    return lowerCaseInitialLetter(requestType);
  }

  String lowerCaseInitialLetter(String name) {
    return name.substring(0, 1).toLowerCase(Locale.US) + name.substring(1);
  }

  String getRequestType(String baseType) {
    return baseType;
  }

  List<String> getThrows() {
    return null;
  }
}
