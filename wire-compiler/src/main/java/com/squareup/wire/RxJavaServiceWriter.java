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

/** A ServiceWriter that generates simple RxJava/Retrofit-compatible interfaces. */
public class RxJavaServiceWriter extends ServiceWriter {
  String requestType;
  String responseType;
  String func1Type;

  public RxJavaServiceWriter(JavaWriter writer, List<String> options) {
    super(writer, options);
  }

  @Override
  public void emitService(ServiceElement service, Set<String> importedTypes) throws IOException {
    importedTypes.add("javax.inject.Inject");
    if (!service.rpcs().isEmpty()) {
      importedTypes.add("retrofit.http.Body");
      importedTypes.add("retrofit.http.POST");
      importedTypes.add("rx.functions.Func1");
    }

    writer.emitImports(importedTypes);
    writer.emitEmptyLine();

    if (!service.documentation().isEmpty()) {
      writer.emitJavadoc(service.documentation());
    }
    writer.beginType(service.name(), "class", EnumSet.of(Modifier.PUBLIC, Modifier.FINAL));

    writer.emitEmptyLine();
    writer.beginType("Endpoint", "interface", EnumSet.of(Modifier.PUBLIC));
    for (RpcElement rpc : service.rpcs()) {
      writer.emitEmptyLine();
      writer.emitJavadoc(rpc.documentation());

      setTypes(rpc);
      writer.emitAnnotation("POST",
          "\"/" + service.qualifiedName() + "/" + rpc.name() + "\"");
      writer.beginMethod(responseType, getMethodName(rpc), EnumSet.noneOf(Modifier.class),
          Arrays.asList("@Body " + requestType, "request"), null);
      writer.endMethod();
    }
    writer.endType();

    for (RpcElement rpc : service.rpcs()) {
      writer.emitEmptyLine();

      setTypes(rpc);
      writer.emitField(func1Type, getMethodName(rpc),
          EnumSet.of(Modifier.PRIVATE, Modifier.FINAL),
          "\nnew " + func1Type + "() {\n"
              + "  @Override\n"
              + "  public " + responseType + " call(" + requestType + " request) {\n"
              + "    return endpoint." + getMethodName(rpc) + "(request);\n"
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

    for (RpcElement rpc : service.rpcs()) {
      writer.emitEmptyLine();

      setTypes(rpc);
      writer.beginMethod(func1Type, "get" + rpc.name(), EnumSet.of(Modifier.PUBLIC));
      writer.emitStatement("return " + getMethodName(rpc));
      writer.endMethod();
    }

    writer.endType();
  }

  private String getMethodName(RpcElement rpc) {
    return lowerCaseInitialLetter(rpc.name());
  }

  private void setTypes(RpcElement rpc) {
    this.requestType = writer.compressType(rpc.requestType().toString());
    this.responseType = writer.compressType(rpc.responseType().toString());
    this.func1Type = "Func1<" + requestType + ", " + responseType + ">";
  }

  String lowerCaseInitialLetter(String name) {
    return name.substring(0, 1).toLowerCase(Locale.US) + name.substring(1);
  }
}
