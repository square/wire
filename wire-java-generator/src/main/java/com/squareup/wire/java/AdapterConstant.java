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
import com.squareup.wire.ProtoAdapter;

/**
 * A constant field that identifies a {@link ProtoAdapter}. This should be a string like like {@code
 * com.squareup.dinosaurs.Dinosaur#ADAPTER} with a fully qualified class name, a {@code #}, and a
 * field name.
 */
public final class AdapterConstant {
  public final ClassName className;
  public final String memberName;

  public AdapterConstant(ClassName className, String memberName) {
    this.className = className;
    this.memberName = memberName;
  }

  public AdapterConstant(String adapter) {
    String[] names = adapter.split("#");
    if (names.length != 2) {
      throw new IllegalArgumentException("Illegally formatted adapter: " + adapter + ".");
    }
    this.className = ClassName.bestGuess(names[0]);
    this.memberName = names[1];
  }

  @Override public boolean equals(Object o) {
    return o instanceof AdapterConstant
        && ((AdapterConstant) o).className.equals(className)
        && ((AdapterConstant) o).memberName.equals(memberName);
  }

  @Override public int hashCode() {
    return className.hashCode() * 37 + memberName.hashCode();
  }
}
