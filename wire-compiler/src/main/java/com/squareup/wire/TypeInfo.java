package com.squareup.wire;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for type analysis.
 */
public final class TypeInfo {

  private static final Map<String, String> JAVA_TYPES = new LinkedHashMap<String, String>();
  static {
    JAVA_TYPES.put("bool", "Boolean");
    JAVA_TYPES.put("bytes", "ByteString");
    JAVA_TYPES.put("double", "Double");
    JAVA_TYPES.put("float", "Float");
    JAVA_TYPES.put("fixed32", "Integer");
    JAVA_TYPES.put("fixed64", "Long");
    JAVA_TYPES.put("int32", "Integer");
    JAVA_TYPES.put("int64", "Long");
    JAVA_TYPES.put("sfixed32", "Integer");
    JAVA_TYPES.put("sfixed64", "Long");
    JAVA_TYPES.put("sint32", "Integer");
    JAVA_TYPES.put("sint64", "Long");
    JAVA_TYPES.put("string", "String");
    JAVA_TYPES.put("uint32", "Integer");
    JAVA_TYPES.put("uint64", "Long");
  }

  private TypeInfo() {
  }

  /**
   * Returns true if the given type name is one of the standard .proto
   * scalar types, e.g., {@code int32}, {@code string}, etc.
   */
  public static boolean isScalar(String type) {
    return JAVA_TYPES.containsKey(type);
  }

  /**
   * Returns the Java type associated with a standard .proto
   * scalar type, e.g., {@code int32}, {@code string}, etc.,
   * or null if the name is not that of a scalar type.
   */
  public static String scalarType(String type) {
    return JAVA_TYPES.get(type);
  }
}
