/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An enumerated type declaration.
 */
public final class EnumType implements Type {
  private final String name;
  private final String documentation;
  private final List<Value> values;

  EnumType(String name, String documentation, List<Value> values) {
    if (name == null) throw new NullPointerException("name");
    if (documentation == null) throw new NullPointerException("documentation");
    if (values == null) throw new NullPointerException("values");
    this.name = name;
    this.documentation = documentation;
    this.values = Collections.unmodifiableList(new ArrayList<Value>(values));
  }

  @Override public String getName() {
    return name;
  }

  public String getDocumentation() {
    return documentation;
  }

  public List<Value> getValues() {
    return values;
  }

  @Override public List<Type> getNestedTypes() {
    return Collections.emptyList();
  }

  @Override public boolean equals(Object other) {
    if (other instanceof EnumType) {
      EnumType that = (EnumType) other;
      return name.equals(that.name)
          && documentation.equals(that.documentation)
          && values.equals(that.values);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Value value : values) {
      result.append("\n  ").append(value);
    }
    return result.toString();
  }

  static final class Value {
    final String name;
    final int tag;
    final String documentation;

    Value(String name, int tag, String documentation) {
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");
      this.name = name;
      this.tag = tag;
      this.documentation = documentation;
    }

    @Override public boolean equals(Object other) {
      if (other instanceof Value) {
        Value that = (Value) other;
        return name.equals(that.name)
            && tag == that.tag
            && documentation.equals(that.documentation);
      }
      return false;
    }

    @Override public int hashCode() {
      return name.hashCode();
    }

    @Override public String toString() {
      return String.format("%s = %d", name, tag);
    }
  }
}
