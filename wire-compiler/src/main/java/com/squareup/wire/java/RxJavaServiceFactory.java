/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.java;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;
import java.util.List;
import javax.lang.model.element.Modifier;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

public class RxJavaServiceFactory implements ServiceFactory {
  public static final ClassName POST = ClassName.get("retrofit.http", "POST");
  public static final ClassName BODY = ClassName.get("retrofit.http", "Body");
  public static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");
  public static final ClassName INJECT = ClassName.get("javax.inject", "Inject");

  @Override public TypeSpec create(
      JavaGenerator javaGenerator, List<String> options, Service service) {
    ClassName interfaceName = interfaceName(javaGenerator, options, service);
    ClassName endpointName = interfaceName.nestedClass("Endpoint");

    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(interfaceName.simpleName());
    typeBuilder.addModifiers(Modifier.PUBLIC, FINAL);

    if (!service.documentation().isEmpty()) {
      typeBuilder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(service.documentation()));
    }

    TypeSpec.Builder endpointBuilder = TypeSpec.interfaceBuilder(endpointName.simpleName());
    endpointBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    typeBuilder.addMethod(MethodSpec.constructorBuilder()
        .addAnnotation(INJECT)
        .addModifiers(PUBLIC)
        .addParameter(endpointName, "endpoint")
        .addStatement("this.endpoint = endpoint")
        .build());

    for (Rpc rpc : service.rpcs()) {
      Type.Name requestType = rpc.requestType();
      TypeName requestJavaType = javaGenerator.typeName(requestType);
      Type.Name responseType = rpc.responseType();
      TypeName responseJavaType = javaGenerator.typeName(responseType);

      String methodName = upperToLowerCamel(rpc.name());
      MethodSpec.Builder rpcBuilder = MethodSpec.methodBuilder(methodName);
      rpcBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
      rpcBuilder.returns(responseJavaType);
      rpcBuilder.addAnnotation(AnnotationSpec.builder(POST)
          .addMember("value", "$S", "/" + service.name() + "/" + rpc.name())
          .build());

      rpcBuilder.addParameter(ParameterSpec.builder(requestJavaType, "request")
          .addAnnotation(BODY)
          .build());

      if (!rpc.documentation().isEmpty()) {
        rpcBuilder.addJavadoc("$L\n", JavaGenerator.sanitizeJavadoc(rpc.documentation()));
      }

      endpointBuilder.addMethod(rpcBuilder.build());

      TypeName functionType = ParameterizedTypeName.get(FUNC1, requestJavaType, responseJavaType);
      typeBuilder.addField(FieldSpec.builder(functionType, methodName)
          .addModifiers(PRIVATE, FINAL)
          .initializer("$L", TypeSpec.anonymousClassBuilder("")
              .addSuperinterface(functionType)
              .addMethod(MethodSpec.methodBuilder("call")
                  .addAnnotation(Override.class)
                  .addModifiers(PUBLIC)
                  .returns(responseJavaType)
                  .addParameter(requestJavaType, "request")
                  .addStatement("return endpoint.$L(request)", methodName)
                  .build())
              .build())
          .build());

      typeBuilder.addMethod(MethodSpec.methodBuilder("get" + rpc.name())
          .addModifiers(PUBLIC)
          .returns(functionType)
          .addStatement("return $L", methodName)
          .build());
    }

    typeBuilder.addField(endpointName, "endpoint", PRIVATE, FINAL);
    typeBuilder.addType(endpointBuilder.build());
    return typeBuilder.build();
  }

  private String upperToLowerCamel(String string) {
    return UPPER_CAMEL.to(LOWER_CAMEL, string);
  }

  protected ClassName interfaceName(
      JavaGenerator javaGenerator, List<String> options, Service service) {
    return (ClassName) javaGenerator.typeName(service.name());
  }
}
