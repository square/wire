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
package com.squareup.wire.java;

import com.squareup.javapoet.ClassName;

public final class Adapter {
  public final ClassName className;
  public final String adapterName;

  public Adapter(ClassName className, String adapterName) {
    this.className = className;
    this.adapterName = adapterName;
  }

  public Adapter(String adapter) {
    String[] names = adapter.split("#");
    if (names.length != 2) {
      throw new IllegalArgumentException("Illegally formatted adapter: " + adapter + ".");
    }
    this.className = ClassName.bestGuess(names[0]);
    this.adapterName = names[1];
  }
}
