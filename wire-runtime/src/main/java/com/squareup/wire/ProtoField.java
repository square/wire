/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.squareup.wire.Message.Label;

/**
 * Annotates generated {@link Message} fields with metadata for serialization and
 * deserialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoField {
  /** The tag number used to store the field's value. */
  int tag();

  /**
   * The field's protocol buffer datatype, e.g., {@code Datatype#INT32},
   * {@code Datatype#MESSAGE}, or {@code Datatype#ENUM}. Defaults to
   * {@code Datatype#MESSAGE}.
   */
  Message.Datatype type() default Message.Datatype.MESSAGE;

  /**
   * The field's protocol buffer label, one of {@link Label#OPTIONAL},
   * {@link Label#REQUIRED}, {@link Label#REPEATED}, or {@link Label#PACKED}.
   * Defaults to {@link Label#OPTIONAL}.
   */
  Label label() default Label.OPTIONAL;

  /**
   * For repeated Message fields, the underlying Message type.
   */
  // The exact supertype 'Message.class' is used as a sentinel for 'no value'.
  Class<? extends Message> messageType() default Message.class;

  /**
   * For repeated Enum fields, the underlying Enum type.
   */
  // The exact supertype 'Enum.class' is used as a sentinel for 'no value'.
  Class<? extends Enum> enumType() default Enum.class;

  /**
   * True if the field is marked as deprecated.
   */
  boolean deprecated() default false;

  /**
   * Redacted fields are omitted from toString() to protect sensitive data. Defaults to false.
   */
  boolean redacted() default false;
}
