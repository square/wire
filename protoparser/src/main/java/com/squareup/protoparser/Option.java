// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

public final class Option {
  private final String name;
  private final Object value;

  Option(String name, Object value) {
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
    if (other instanceof Option) {
      Option that = (Option) other;
      return name.equals(that.name) && value.equals(that.value);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode() + (37 * value.hashCode());
  }

  @Override public String toString() {
    return String.format("%s=%s", name, value);
  }
}
