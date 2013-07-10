// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation placed on {@link Message} fields in generated code to provide necessary
 * metadata for the protocol buffer runtime to perform serialization and deserialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoField {
  /** The tag number used to store the field's value. */
  int tag();

  /**
   * The field's protocol buffer datatype, e.g., {@code Wire.INT32},
   * {@code Wire.MESSAGE}, or {@code Wire.ENUM}. Defaults to
   * {@code Wire.MESSAGE}.
   */
  int type() default Wire.MESSAGE;

  /**
   * The field's protocol buffer label, one of {@code Wire.OPTIONAL},
   * {@code Wire.REQUIRED}, or {@code Wire.REPEATED}. Defaults to
   * {@code Wire.OPTIONAL}.
   */
  int label() default Wire.OPTIONAL;

  /** True if the field has the '[packed = true]' extension. */
  boolean packed() default false;
}
