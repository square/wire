// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * An annotation placed on {@link Enum} values in generated code to provide necessary
 * metadata for the protocol buffer runtime to perform serialization and deserialization.
 */
public @interface ProtoEnum {
  int value();
}
