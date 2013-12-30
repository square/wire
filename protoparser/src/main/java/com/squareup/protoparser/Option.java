// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public final class Option {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> optionsAsMap(List<Option> options) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : options) {
      String name = option.getName();
      Object value = option.getValue();

      if (value instanceof String || value instanceof List) {
        map.put(name, value);
      } else if (value instanceof Option) {
        Map<String, Object> newMap = optionsAsMap(Arrays.asList((Option) value));

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

  private final String name;
  private final Object value;

  public Option(String name, Object value) {
    if (name == null) throw new NullPointerException("name");
    if (value == null) throw new NullPointerException("value");

    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Option)) return false;

    Option that = (Option) other;
    return name.equals(that.name) && value.equals(that.value);
  }

  @Override public int hashCode() {
    return name.hashCode() + (37 * value.hashCode());
  }

  @Override public String toString() {
    return String.format("%s=%s", name, value);
  }
}
