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
package com.squareup.wire.internal;

import com.squareup.wire.model.WireOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Util {
  private Util() {
  }

  public static <T> List<T> concatenate(List<T> a, T b) {
    List<T> result = new ArrayList<T>();
    result.addAll(a);
    result.add(b);
    return result;
  }

  public static WireOption findOption(List<WireOption> options, String name) {
    checkNotNull(options, "options");
    checkNotNull(name, "name");

    WireOption found = null;
    for (WireOption option : options) {
      if (option.name().equals(name)) {
        if (found != null) {
          throw new IllegalStateException("Multiple options match name: " + name);
        }
        found = option;
      }
    }
    return found;
  }

  /**
   * Returns true if any of the options in {@code options} matches both of the regular expressions
   * provided: its name matches the option's name and its value matches the option's value.
   */
  public static boolean optionMatches(
      List<WireOption> options, String namePattern, String valuePattern) {
    Matcher nameMatcher = Pattern.compile(namePattern).matcher("");
    Matcher valueMatcher = Pattern.compile(valuePattern).matcher("");
    for (WireOption option : options) {
      if (nameMatcher.reset(option.name()).matches()
          && valueMatcher.reset(String.valueOf(option.value())).matches()) {
        return true;
      }
    }
    return false;
  }
}
