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

/** A ServiceWriter that generates simple RxJava/Retrofit-compatible interfaces. */
public class RxJavaServiceWriter extends ServiceWriter {
  String requestType;
  String responseType;
  String func1Type;

  public RxJavaServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override
  public void emitService(Service service, Set<String> importedTypes) throws IOException {
    importedTypes.add("javax.inject.Inject");
    if (!service.getMethods().isEmpty()) {
      importedTypes.add("retrofit.http.Body");
      importedTypes.add("retrofit.http.POST");
      importedTypes.add("rx.functions.Func1");
    }

    writer.emitImports(importedTypes);
    writer.emitEmptyLine();

    if (!service.getDocumentation().isEmpty()) {
      writer.emitJavadoc(service.getDocumentation());
    }
    writer.beginType(service.getName(), "class", EnumSet.of(Modifier.PUBLIC, Modifier.FINAL));

    writer.emitEmptyLine();
    writer.beginType("Endpoint", "interface", EnumSet.of(Modifier.PUBLIC));
    for (Service.Method method : service.getMethods()) {
      writer.emitEmptyLine();
      writer.emitJavadoc(method.getDocumentation());

      setTypes(method);
      writer.emitAnnotation("POST",
          "\"/" + service.getFullyQualifiedName() + "/" + method.getName() + "\"");
      writer.beginMethod(responseType, getMethodName(method), EnumSet.noneOf(Modifier.class),
          Arrays.asList("@Body " + requestType, "request"), null);
      writer.endMethod();
    }
    writer.endType();

    for (Service.Method method : service.getMethods()) {
      writer.emitEmptyLine();

      setTypes(method);
      writer.emitField(func1Type, getMethodName(method),
          EnumSet.of(Modifier.PRIVATE, Modifier.FINAL),
          "\nnew " + func1Type + "() {\n"
              + "  @Override\n"
              + "  public " + responseType + " call(" + requestType + " request) {\n"
              + "    return endpoint." + getMethodName(method) + "(request);\n"
              + "  }\n"
              + "}");
    }

    writer.emitEmptyLine();
    writer.emitField("Endpoint", "endpoint", EnumSet.of(Modifier.PRIVATE, Modifier.FINAL));

    writer.emitEmptyLine();
    writer.emitAnnotation("Inject");
    writer.beginConstructor(EnumSet.of(Modifier.PUBLIC), "Endpoint", "endpoint");
    writer.emitStatement("this.endpoint = endpoint");
    writer.endConstructor();

    for (Service.Method method : service.getMethods()) {
      writer.emitEmptyLine();

      setTypes(method);
      writer.beginMethod(func1Type, "get" + method.getName(), EnumSet.of(Modifier.PUBLIC));
      writer.emitStatement("return " + getMethodName(method));
      writer.endMethod();
    }

    writer.endType();
  }

  private String getMethodName(Service.Method method) {
    return lowerCaseInitialLetter(method.getName());
  }

  private void setTypes(Service.Method method) {
    this.requestType = writer.compressType(method.getRequestType());
    this.responseType = writer.compressType(method.getResponseType());
    this.func1Type = "Func1<" + requestType + ", " + responseType + ">";
  }

  String lowerCaseInitialLetter(String name) {
    return name.substring(0, 1).toLowerCase(Locale.US) + name.substring(1);
  }
}
