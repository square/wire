// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static com.squareup.protoparser.Utils.appendIndented;

public final class OptionElement {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> optionsAsMap(List<OptionElement> options) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (OptionElement option : options) {
      String name = option.getName();
      Object value = option.getValue();

      if (value instanceof String || value instanceof List) {
        map.put(name, value);
      } else if (value instanceof OptionElement) {
        Map<String, Object> newMap = optionsAsMap(Arrays.asList((OptionElement) value));

        Object oldValue = map.get(name);
        if (oldValue instanceof Map) {
          Map<String, Object> oldMap = (Map<String, Object>) oldValue;
          // Existing nested maps are immutable. Make a mutable copy, update, and replace.
          oldMap = new LinkedHashMap<String, Object>(oldMap);
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
      if (option.getName().equals(name)) {
        if (found != null) {
          throw new IllegalStateException("Multiple options match name: " + name);
        }
        found = option;
      }
    }
    return found;
  }

  private final String name;
  private final Object value;
  private final boolean isParenthesized;

  public OptionElement(String name, Object value) {
    this(name, value, false);
  }

  public OptionElement(String name, Object value, boolean isParenthesized) {
    if (name == null) throw new NullPointerException("name");
    if (value == null) throw new NullPointerException("value");

    this.name = name;
    this.value = value;
    this.isParenthesized = isParenthesized;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof OptionElement)) return false;

    OptionElement that = (OptionElement) other;
    return name.equals(that.name)
        && value.equals(that.value)
        && isParenthesized == that.isParenthesized;
  }

  @Override public int hashCode() {
    return name.hashCode() + (37 * value.hashCode()) + (37 * (isParenthesized ? 1 : 0));
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    if (value instanceof Boolean || value instanceof Number) {
      builder.append(formatName()).append(" = ").append(value);
    } else if (value instanceof String) {
      String stringValue = (String) value;
      builder.append(formatName()).append(" = \"").append(escape(stringValue)).append('"');
    } else if (value instanceof OptionElement) {
      OptionElement optionValue = (OptionElement) value;
      // Treat nested options as non-parenthesized always, prevents double parentheses.
      optionValue = new OptionElement(optionValue.name, optionValue.value, false);
      builder.append(formatName()).append('.').append(optionValue.toString());
    } else if (value instanceof EnumElement.Value) {
      EnumElement.Value enumValue = (EnumElement.Value) value;
      builder.append(name).append(" = ").append(enumValue.getName());
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

  public String toDeclaration() {
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
    if (isParenthesized) {
      return '(' + name + ')';
    } else {
      return name;
    }
  }
}
