// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation placed on {@link Enum} values in generated code to provide necessary
 * metadata for the protocol buffer runtime to perform serialization and deserialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoEnum {
  /** The value of the enum constant, as declared in the .proto source file. */
  int value();
}
