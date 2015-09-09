/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.wire.schema.internal.Util.appendIndented;

@AutoValue
public abstract class OptionElement {
  public enum Kind {
    STRING,
    BOOLEAN,
    NUMBER,
    ENUM,
    MAP,
    LIST,
    OPTION
  }

  public static OptionElement create(String name, Kind kind, Object value) {
    return create(name, kind, value, false);
  }

  public static OptionElement create(String name, Kind kind, Object value,
      boolean isParenthesized) {
    return new AutoValue_OptionElement(name, kind, value, isParenthesized);
  }

  public abstract String name();
  public abstract Kind kind();
  public abstract Object value();
  public abstract boolean isParenthesized();

  public final String toSchema() {
    Object value = value();
    switch (kind()) {
      case STRING:
        return formatName() + " = \"" + value + '"';
      case BOOLEAN:
      case NUMBER:
      case ENUM:
        return formatName() + " = " + value;
      case OPTION: {
        StringBuilder builder = new StringBuilder();
        OptionElement optionValue = (OptionElement) value;
        // Treat nested options as non-parenthesized always, prevents double parentheses.
        optionValue =
            OptionElement.create(optionValue.name(), optionValue.kind(), optionValue.value());
        builder.append(formatName()).append('.').append(optionValue.toSchema());
        return builder.toString();
      }
      case MAP: {
        StringBuilder builder = new StringBuilder();
        builder.append(formatName()).append(" = {\n");
        //noinspection unchecked
        Map<String, ?> valueMap = (Map<String, ?>) value;
        formatOptionMap(builder, valueMap);
        builder.append('}');
        return builder.toString();
      }
      case LIST: {
        StringBuilder builder = new StringBuilder();
        builder.append(formatName()).append(" = [\n");
        //noinspection unchecked
        List<OptionElement> optionList = (List<OptionElement>) value;
        formatOptionList(builder, optionList);
        builder.append(']');
        return builder.toString();
      }
      default:
        throw new AssertionError();
    }
  }

  public final String toSchemaDeclaration() {
    return "option " + toSchema() + ";\n";
  }

  static void formatOptionList(StringBuilder builder, List<OptionElement> optionList) {
    for (int i = 0, count = optionList.size(); i < count; i++) {
      String endl = (i < count - 1) ? "," : "";
      appendIndented(builder, optionList.get(i).toSchema() + endl);
    }
  }

  static void formatOptionMap(StringBuilder builder, Map<String, ?> valueMap) {
    List<? extends Map.Entry<String, ?>> entries = new ArrayList<>(valueMap.entrySet());
    for (int i = 0, count = entries.size(); i < count; i++) {
      Map.Entry<String, ?> entry = entries.get(i);
      String endl = (i < count - 1) ? "," : "";
      appendIndented(builder,
          entry.getKey() + ": " + formatOptionMapValue(entry.getValue()) + endl);
    }
  }

  static String formatOptionMapValue(Object value) {
    checkNotNull(value, "value == null");
    if (value instanceof String) {
      return "\"" + value + '"';
    }
    if (value instanceof Map) {
      StringBuilder builder = new StringBuilder().append("{\n");
      //noinspection unchecked
      Map<String, ?> map = (Map<String, ?>) value;
      formatOptionMap(builder, map);
      return builder.append('}').toString();
    }
    if (value instanceof List) {
      StringBuilder builder = new StringBuilder().append("[\n");
      List<?> list = (List<?>) value;
      for (int i = 0, count = list.size(); i < count; i++) {
        String endl = (i < count - 1) ? "," : "";
        appendIndented(builder, formatOptionMapValue(list.get(i)) + endl);
      }
      return builder.append("]").toString();
    }
    return value.toString();
  }

  private String formatName() {
    return isParenthesized() ? '(' + name() + ')' : name();
  }
}
