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
package com.squareup.wire;

import com.squareup.javapoet.ClassName;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.java.RxJavaServiceFactory;
import com.squareup.wire.schema.Service;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class TestRxJavaServiceFactory extends RxJavaServiceFactory {
  @Override protected ClassName interfaceName(
      JavaGenerator javaGenerator, List<String> options, Service service) {
    String serviceNameSuffix = options.get(0);
    ClassName baseName = super.interfaceName(javaGenerator, options, service);
    return baseName.peerClass(baseName.simpleName() + serviceNameSuffix);
  }
}
