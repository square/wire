// Copyright 2013 Square, Inc.
package com.squareup.omar;

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
   * The field's protocol buffer datatype, e.g., {@code Omar.INT32},
   * {@code Omar.MESSAGE}, or {@code Omar.ENUM}. Defaults to
   * {@code Omar.MESSAGE}.
   */
  int type() default Omar.MESSAGE;

  /**
   * The field's protocol buffer label, one of {@code Omar.OPTIONAL},
   * {@code Omar.REQUIRED}, or {@code Omar.REPEATED}. Defaults to
   * {@code Omar.OPTIONAL}.
   */
  int label() default Omar.OPTIONAL;

  /** True if the field has the '[packed = true]' extension. */
  boolean packed() default false;
}
