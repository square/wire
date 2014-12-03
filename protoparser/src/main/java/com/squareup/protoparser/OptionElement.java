// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.google.auto.value.AutoValue;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static com.squareup.protoparser.Utils.appendIndented;

@AutoValue
public abstract class OptionElement {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> optionsAsMap(List<OptionElement> options) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (OptionElement option : options) {
      String name = option.name();
      Object value = option.value();

      if (value instanceof String || value instanceof List) {
        map.put(name, value);
      } else if (value instanceof OptionElement) {
        Map<String, Object> newMap = optionsAsMap(Arrays.asList((OptionElement) value));

        Object oldValue = map.get(name);
        if (oldValue instanceof Map) {
          Map<String, Object> oldMap = (Map<String, Object>) oldValue;
          // Existing nested maps are immutable. Make a mutable copy, update, and replace.
          oldMap = new LinkedHashMap<>(oldMap);
          oldMap.putAll(newMap);
          map.put(name, oldMap);
        } else {
          map.put(name, newMap);
        }
      } else if (value instanceof Map) {
        Object oldValue = map.get(name);
        if (oldValue instanceof Map) {
          ((Map<String, Object>) oldValue).putAll((Map<String, Object>) value);
        } else {
          map.put(name, value);
        }
      } else {
        throw new AssertionError("Option value must be String, Option, List, or Map<String, ?>");
      }
    }
    return unmodifiableMap(map);
  }

  /** Return the option with the specified name from the supplied list or null. */
  public static OptionElement findByName(List<OptionElement> options, String name) {
    if (options == null) throw new NullPointerException("options");
    if (name == null) throw new NullPointerException("name");

    OptionElement found = null;
    for (OptionElement option : options) {
      if (option.name().equals(name)) {
        if (found != null) {
          throw new IllegalStateException("Multiple options match name: " + name);
        }
        found = option;
      }
    }
    return found;
  }

  public static OptionElement create(String name, Object value, boolean isParenthesized) {
    return new AutoValue_OptionElement(name, value, isParenthesized);
  }

  OptionElement() {
  }

  public abstract String name();
  public abstract Object value();
  public abstract boolean isParenthesized();

  @Override public final String toString() {
    Object value = value();
    StringBuilder builder = new StringBuilder();
    if (value instanceof Boolean || value instanceof Number) {
      builder.append(formatName()).append(" = ").append(value);
    } else if (value instanceof String) {
      String stringValue = (String) value;
      builder.append(formatName()).append(" = \"").append(escape(stringValue)).append('"');
    } else if (value instanceof OptionElement) {
      OptionElement optionValue = (OptionElement) value;
      // Treat nested options as non-parenthesized always, prevents double parentheses.
      optionValue = OptionElement.create(optionValue.name(), optionValue.value(), false);
      builder.append(formatName()).append('.').append(optionValue.toString());
    } else if (value instanceof EnumConstantElement) {
      EnumConstantElement enumValue = (EnumConstantElement) value;
      builder.append(name()).append(" = ").append(enumValue.name());
    } else if (value instanceof List) {
      builder.append(formatName()).append(" = [\n");
      //noinspection unchecked
      List<OptionElement> optionList = (List<OptionElement>) value;
      formatOptionList(builder, optionList);
      builder.append(']');
    } else {
      throw new IllegalStateException("Unknown value type " + value.getClass().getCanonicalName());
    }
    return builder.toString();
  }

  public final String toDeclaration() {
    return "option " + toString() + ";\n";
  }

  static String escape(String string) {
    return string.replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  static StringBuilder formatOptionList(StringBuilder builder, List<OptionElement> optionList) {
    for (int i = 0, count = optionList.size(); i < count; i++) {
      String endl = (i < count - 1) ? "," : "";
      appendIndented(builder, optionList.get(i).toString() + endl);
    }
    return builder;
  }

  private String formatName() {
    if (isParenthesized()) {
      return '(' + name() + ')';
    } else {
      return name();
    }
  }
}
