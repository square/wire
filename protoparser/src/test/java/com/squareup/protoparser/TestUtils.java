package com.squareup.protoparser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestUtils {
  static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }

  @SafeVarargs
  static <T> List<T> list(T... values) {
    return Arrays.asList(values);
  }

  private TestUtils() {
    throw new AssertionError("No instances.");
  }
}
