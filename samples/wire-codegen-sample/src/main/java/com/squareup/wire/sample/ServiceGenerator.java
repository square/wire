/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.sample;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Service;

final class ServiceGenerator {
  final JavaGenerator javaGenerator;

  ServiceGenerator(JavaGenerator javaGenerator) {
    this.javaGenerator = javaGenerator;
  }

  public TypeSpec api(Service service) {
    ClassName apiName = (ClassName) javaGenerator.typeName(service.type());

    TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(apiName.simpleName());
    typeBuilder.addModifiers(PUBLIC);

    if (!service.documentation().isEmpty()) {
      typeBuilder.addJavadoc("$L\n", service.documentation());
    }

    for (Rpc rpc : service.rpcs()) {
      ProtoType requestType = rpc.getRequestType();
      TypeName requestJavaType = javaGenerator.typeName(requestType);

      ProtoType responseType = rpc.getResponseType();
      TypeName responseJavaType = javaGenerator.typeName(responseType);

      MethodSpec.Builder rpcBuilder = MethodSpec.methodBuilder(rpc.getName());
      rpcBuilder.addModifiers(PUBLIC, ABSTRACT);
      rpcBuilder.returns(responseJavaType);
      rpcBuilder.addParameter(requestJavaType, "request");

      if (!rpc.getDocumentation().isEmpty()) {
        rpcBuilder.addJavadoc("$L\n", rpc.getDocumentation());
      }

      typeBuilder.addMethod(rpcBuilder.build());
    }

    return typeBuilder.build();
  }
}
