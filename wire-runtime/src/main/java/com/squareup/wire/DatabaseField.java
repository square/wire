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
public @interface DatabaseField {

  /**
   * 
   */
   String dataType();
  /**
   * The field's columnName
   */
  String columnName(); 
  /**
   * The field's protocol buffer data type. This is either a scalar (like {@code int32} or {@code
   * string}), a message type (like {@code squareup.protos.Person}), or an enum type (like {@code
   * squareup.protos.CurrencyCode}).
   */
  String type();

  /**
   * True if the field is marked as deprecated.
   */
  boolean deprecated() default false;

  /**
   * Redacted fields are omitted from toString() to protect sensitive data. Defaults to false.
   */
  boolean redacted() default false;
}
