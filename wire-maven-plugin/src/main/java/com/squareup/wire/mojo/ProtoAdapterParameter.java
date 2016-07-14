/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.mojo;

import com.squareup.javapoet.ClassName;
import com.squareup.wire.java.AdapterConstant;
import com.squareup.wire.schema.ProtoType;

/**
 * A custom type adapter as configured in a pom.xml file.
 *
 * <pre>   {@code
 *
 *   <protoAdapter>
 *     <protoType>squareup.wire.exemplar.Locale</protoType>
 *     <javaName>java.util.Locale</javaName>
 *     <adapter>com.squareup.wire.exemplar.Exemplar#LOCALE_ADAPTER</adapter>
 *   </protoAdapter>
 * }</pre>
 */
public final class ProtoAdapterParameter {
  private String protoType;
  private String javaName;
  private String adapter;

  public ProtoType protoType() {
    return ProtoType.get(protoType);
  }

  public ClassName javaName() {
    return ClassName.bestGuess(javaName);
  }

  public AdapterConstant adapter() {
    return new AdapterConstant(adapter);
  }
}
