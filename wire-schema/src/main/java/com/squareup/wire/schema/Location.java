/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import java.io.File;

/**
 * Locates a .proto file, or a position within a .proto file, on the file system. This includes a
 * base directory or a .jar file, and a path relative to that base.
 */
@AutoValue
public abstract class Location {
  public static Location get(String path) {
    return get("", path);
  }

  public static Location get(String base, String path) {
    base = CharMatcher.is('/').trimTrailingFrom(base);
    path = CharMatcher.is('/').trimLeadingFrom(path);
    return new AutoValue_Location(base, path, -1, -1);
  }

  public Location at(int line, int column) {
    return new AutoValue_Location(base(), path(), line, column);
  }

  /** Returns a copy of this location with an empty base. */
  public Location withoutBase() {
    return new AutoValue_Location("", path(), line(), column());
  }

  /** Returns the base of this location; typically a directory or .jar file. */
  public abstract String base();

  /** Returns the path to this location relative to {@link #base}. */
  public abstract String path();

  /** Returns the line number of this location, or -1 for no specific line number. */
  public abstract int line();

  /** Returns the column on the line of this location, or -1 for no specific column. */
  public abstract int column();

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    if (!base().isEmpty()) {
      result.append(base()).append(File.separator);
    }
    result.append(path());
    if (line() != -1) {
      result.append(" at ").append(line());
      if (column() != -1) {
        result.append(':').append(column());
      }
    }
    return result.toString();
  }
}
