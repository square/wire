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

import java.util.Locale;
import java.util.Objects;

import static java.lang.String.format;

public final class JavaPackage implements Comparable<JavaPackage> {

  public static final JavaPackage ROOT = new JavaPackage("");

  private final String javaPackage;

  private JavaPackage(String javaPackage) {
    this.javaPackage = javaPackage;
  }

  public static JavaPackage parse(String javaPackage) {
    if (javaPackage == null) {
      throw new NullPointerException("Java package may not be null");
    }
    if (!javaPackage.trim().equals(javaPackage)) {
      throw new IllegalArgumentException("Java package may not start of end with whitespace");
    }
    if (javaPackage.startsWith(".")) {
      throw new IllegalArgumentException("Java package may not start with a dot");
    }
    if (javaPackage.endsWith(".")) {
      throw new IllegalArgumentException("Java package may not end with a dot");
    }
    return new JavaPackage(javaPackage);
  }

  public String asString() {
    return javaPackage;
  }

  public JavaPackage plus(String otherJavaPackage) {
    return plus(JavaPackage.parse(otherJavaPackage));
  }

  public JavaPackage plus(JavaPackage other) {
    if (javaPackage.isEmpty() || other.javaPackage.isEmpty()) {
      return new JavaPackage(format(Locale.ROOT, "%s%s", javaPackage, other.javaPackage));
    }
    return new JavaPackage(format(Locale.ROOT, "%s.%s", javaPackage, other.javaPackage));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.javaPackage);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.javaPackage, ((JavaPackage) obj).javaPackage);
  }

  @Override
  public int compareTo(JavaPackage other) {
    return javaPackage.compareTo(other.javaPackage);
  }

  @Override
  public String toString() {
    if (javaPackage.equals("")) {
      return "JavaPackage.ROOT";
    }
    return format(Locale.ROOT, "JavaPackage[%s]", javaPackage);
  }

}
