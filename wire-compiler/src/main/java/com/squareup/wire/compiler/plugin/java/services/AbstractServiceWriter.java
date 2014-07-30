// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler.plugin.java.services;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Service;
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
  public void emitService(Service service, Set<String> importedTypes) throws IOException {
    importedTypes.addAll(getImports(service));
    writer.emitImports(importedTypes);
    writer.emitEmptyLine();

    if (!service.getDocumentation().isEmpty()) {
      writer.emitJavadoc(service.getDocumentation());
    }
    writer.beginType(service.getName(), "interface", EnumSet.of(Modifier.PUBLIC));
    for (Service.Method method : service.getMethods()) {
      String requestType = shorten(method.getRequestType());

      if (!method.getDocumentation().isEmpty()) {
        writer.emitJavadoc(method.getDocumentation());
      }
      emitAnnotation(service, method);
      writer.beginMethod(shorten(method.getResponseType()), getMethodName(method),
          EnumSet.noneOf(Modifier.class),
          Arrays.asList(getRequestType(requestType), getRequestName(requestType)), getThrows());
      writer.endMethod();
    }
    writer.endType();
  }

  private String shorten(String typeName) {
    int index = typeName.lastIndexOf('.');
    if (index != -1) {
      return typeName.substring(index + 1);
    }
    return typeName;
  }

  List<String> getImports(Service service) {
    return null;
  }

  void emitAnnotation(Service service, Service.Method method) throws IOException {
    // no-op
  }

  String getMethodName(Service.Method method) {
    return lowerCaseInitialLetter(method.getName());
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
