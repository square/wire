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
package com.squareup.protoss.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MessageType {
  final String name;
  final String documentation;
  final List<Field> fields;

  MessageType(String name, String documentation, List<Field> fields) {
    if (name == null) throw new NullPointerException("name");
    if (documentation == null) throw new NullPointerException("documentation");
    if (fields == null) throw new NullPointerException("fields");
    this.name = name;
    this.documentation = documentation;
    this.fields = Collections.unmodifiableList(new ArrayList<Field>(fields));
  }

  @Override public boolean equals(Object other) {
    if (other instanceof MessageType) {
      MessageType that = (MessageType) other;
      return name.equals(that.name)
          && documentation.equals(that.documentation)
          && fields.equals(that.fields);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(name);
    for (Field field : fields) {
      result.append("\n  ").append(field);
    }
    return result.toString();
  }

  enum Label {
    OPTIONAL, REQUIRED, REPEATED
  }

  static final class Field {
    final Label label;
    final String type;
    final String name;
    final int tag;
    final String defaultValue;
    final boolean deprecated;
    final String documentation;

    Field(Label label, String type, String name, int tag, String defaultValue, boolean deprecated,
        String documentation) {
      if (label == null) throw new NullPointerException("label");
      if (type == null) throw new NullPointerException("type");
      if (name == null) throw new NullPointerException("name");
      if (documentation == null) throw new NullPointerException("documentation");

      this.label = label;
      this.type = type;
      this.name = name;
      this.tag = tag;
      this.defaultValue = defaultValue;
      this.deprecated = deprecated;
      this.documentation = documentation;
    }

    @Override public boolean equals(Object other) {
      if (other instanceof Field) {
        Field that = (Field) other;
        return eq(label, that.label)
            && eq(type, that.type)
            && eq(name, that.name)
            && tag == that.tag
            && eq(defaultValue, that.defaultValue)
            && deprecated == that.deprecated
            && eq(documentation, that.documentation);
      }
      return false;
    }

    private static boolean eq(Object a, Object b) {
      return a == b || a != null && a.equals(b);
    }

    @Override public int hashCode() {
      return name.hashCode() + (37 * type.hashCode());
    }

    @Override public String toString() {
      return String.format("%s %s %s = %d", label, type, name, tag);
    }
  }
}
