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

/**
 * Annotates generated {@link Message} fields with metadata for serialization and
 * deserialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WireField {
  /** The tag number used to store the field's value. */
  int tag();

  /**
   * Reference to the static field that holds a {@link ProtoAdapter} that can encode and decode this
   * field. The reference is a string like {@code com.squareup.wire.protos.person.Person#ADAPTER}
   * and contains a fully-qualified class name followed by a hash symbol and a field name.
   */
  String adapter();

  /**
   * The field's protocol buffer label, one of {@link Label#OPTIONAL},
   * {@link Label#REQUIRED}, {@link Label#REPEATED}, or {@link Label#PACKED}.
   * Defaults to {@link Label#OPTIONAL}.
   */
  Label label() default Label.OPTIONAL;

  /**
   * Redacted fields are omitted from toString() to protect sensitive data. Defaults to false.
   */
  boolean redacted() default false;

  /** A protocol buffer label. */
  enum Label {
    REQUIRED, OPTIONAL, REPEATED, ONE_OF,
    /** Implies {@link #REPEATED}. */
    PACKED;

    boolean isRepeated() {
      return this == REPEATED || this == PACKED;
    }

    boolean isPacked() {
      return this == PACKED;
    }

    boolean isOneOf() {
      return this == ONE_OF;
    }
  }
}
