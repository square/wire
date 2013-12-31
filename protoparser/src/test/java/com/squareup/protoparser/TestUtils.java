package com.squareup.protoparser;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestUtils {
  static final List<Type> NO_TYPES = Collections.emptyList();
  static final List<Service> NO_SERVICES = Collections.emptyList();
  static final List<String> NO_STRINGS = Collections.emptyList();
  static final List<ExtendDeclaration> NO_EXTEND_DECLARATIONS = Collections.emptyList();
  static final List<Extensions> NO_EXTENSIONS = Collections.emptyList();
  static final List<Option> NO_OPTIONS = Collections.emptyList();
  static final List<MessageType.Field> NO_FIELDS = Collections.emptyList();
  static final List<EnumType.Value> NO_VALUES = Collections.emptyList();
  static final List<Service.Method> NO_METHODS = Collections.emptyList();

  static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }

  static <T> List<T> list(T... values) {
    return Arrays.asList(values);
  }

  private TestUtils() {
    throw new AssertionError("No instances.");
  }
}
