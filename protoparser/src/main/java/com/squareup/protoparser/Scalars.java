// Copyright 2014 Square, Inc.
package com.squareup.protoparser;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/** Utilities for dealing with scalar types. */
public final class Scalars {
  private static final Set<String> SCALAR_TYPES;

  public static final String TYPE_BOOL = "bool";
  public static final String TYPE_BYTES = "bytes";
  public static final String TYPE_DOUBLE = "double";
  public static final String TYPE_FLOAT = "float";
  public static final String TYPE_FIXED_32 = "fixed32";
  public static final String TYPE_FIXED_64 = "fixed64";
  public static final String TYPE_INT_32 = "int32";
  public static final String TYPE_INT_64 = "int64";
  public static final String TYPE_SFIXED_32 = "sfixed32";
  public static final String TYPE_SFIXED_64 = "sfixed64";
  public static final String TYPE_SINT_32 = "sint32";
  public static final String TYPE_SINT_64 = "sint64";
  public static final String TYPE_STRING = "string";
  public static final String TYPE_UINT_32 = "uint32";
  public static final String TYPE_UINT_64 = "uint64";

  static {
    Set<String> scalarTypes = new LinkedHashSet<>();
    scalarTypes.add(TYPE_BOOL);
    scalarTypes.add(TYPE_BYTES);
    scalarTypes.add(TYPE_DOUBLE);
    scalarTypes.add(TYPE_FLOAT);
    scalarTypes.add(TYPE_FIXED_32);
    scalarTypes.add(TYPE_FIXED_64);
    scalarTypes.add(TYPE_INT_32);
    scalarTypes.add(TYPE_INT_64);
    scalarTypes.add(TYPE_SFIXED_32);
    scalarTypes.add(TYPE_SFIXED_64);
    scalarTypes.add(TYPE_SINT_32);
    scalarTypes.add(TYPE_SINT_64);
    scalarTypes.add(TYPE_STRING);
    scalarTypes.add(TYPE_UINT_32);
    scalarTypes.add(TYPE_UINT_64);
    SCALAR_TYPES = unmodifiableSet(scalarTypes);
  }

  /** Returns true if the supplied type is scalar. */
  public static boolean isScalarType(String type) {
    return SCALAR_TYPES.contains(type);
  }

  private Scalars() {
    throw new AssertionError("No instances.");
  }
}
