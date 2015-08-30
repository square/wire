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

import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.schema.Service;
import java.util.List;

/**
 * Generates a Java service class for a .proto RPC service. Implementing classes must have a no
 * arguments constructor.
 */
public interface ServiceFactory {
  TypeSpec create(JavaGenerator javaGenerator, List<String> options, Service service);
}
